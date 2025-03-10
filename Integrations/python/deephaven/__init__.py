#
# Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
#

"""
Main Deephaven python module.

For convenient usage in the python console, the main sub-packages of deephaven have been imported here with aliases:

* Calendars imported as cals

* DateTimeUtils imported as dtu

* MovingAverages imported as mavg

* Plot imported as plt

* ParquetTools imported as pt

* TableTools imported as ttools *(`tt` is frequently used for time table)*

Additionally, the following methods have been imported into the main deephaven namespace:

* from Plot import figure_wrapper as figw

* from java_to_python import tableToDataFrame, columnToNumpyArray, convertJavaArray

* from python_to_java import dataFrameToTable, createTableFromData

* from conversion_utils import convertToJavaArray, convertToJavaList, convertToJavaArrayList,
       convertToJavaHashSet, convertToJavaHashMap

* from TableManipulation import Aggregation, ColumnName, ColumnRenderersBuilder, DistinctFormatter,
       DownsampledWhereFilter, DynamicTableWriter, LayoutHintBuilder,  
       Replayer, SmartKey, SortColumn, TotalsTableBuilder, WindowCheck

For ease of namespace population in a python console, consider::

>>> from deephaven import *  # this will import the submodules into the main namespace
>>> print(dir())  # this will display the contents of the main namespace
>>> help(plt)  # will display the help entry (doc strings) for the deephaven.plot module
>>> help(columnToNumpyArray)  # will display the help entry for the columnToNumpyArray method
"""

__all__ = [
    "PythonFunction", "PythonListenerAdapter", "PythonShiftAwareListenerAdapter",
    "TableListenerHandle", "listen", "doLocked",  # from here

    "convertJavaArray", "tableToDataFrame", "columnToNumpyArray",  # from java_to_python

    "createTableFromData", "dataFrameToTable",  # from python_to_java

    "convertToJavaArray", "convertToJavaList", "convertToJavaArrayList", "convertToJavaHashSet",
    "convertToJavaHashMap",  # from conversion_utils

    'Aggregation', 'ColumnName', 'ColumnRenderersBuilder', 'DistinctFormatter', 'DownsampledWhereFilter', 'DynamicTableWriter',
    'LayoutHintBuilder', 'Replayer', 'SmartKey', 'SortColumn', 'TotalsTableBuilder', 'WindowCheck',  # from TableManipulation

    "cals", "dtu", "figw", "mavg", "plt", "pt", "ttools", "tloggers",  # subpackages with abbreviated names

    "read_csv", "write_csv" # from csv
]


import jpy
import sys
import base64  # builtin
import inspect

import wrapt  # dependencies
import dill

from .TableManipulation import *

from .java_to_python import convertJavaArray, tableToDataFrame, columnToNumpyArray
from .python_to_java import dataFrameToTable, createTableFromData
from .conversion_utils import convertToJavaArray, convertToJavaList, convertToJavaArrayList, convertToJavaHashSet, \
    convertToJavaHashMap, getJavaClassObject


from . import Calendars as cals, \
    DateTimeUtils as dtu, \
    MovingAverages as mavg, \
    ConsumeKafka as ck, \
    ProduceKafka as pk, \
    Plot as plt, \
    ParquetTools as pt, \
    TableTools as ttools, \
    TableLoggers as tloggers, \
    Types as dh, \
    filter as _filter

from .Plot import figure_wrapper as figw

from .csv import read as read_csv
from .csv import write as write_csv

# NB: this must be defined BEFORE importing .jvm_init or .start_jvm (circular import)
def initialize():
    __initializer__(jpy.get_type("io.deephaven.configuration.Configuration"), Config)
    __initializer__(jpy.get_type("io.deephaven.engine.util.PickledResult"), PickledResult)
    __initializer__(jpy.get_type("io.deephaven.integrations.python.PythonShiftObliviousListenerAdapter"), PythonListenerAdapter)
    __initializer__(jpy.get_type("io.deephaven.integrations.python.PythonFunction"), PythonFunction)

    # ensure that all the symbols are called and reimport the broken symbols
    cals._defineSymbols()
    dtu._defineSymbols()
    dh._defineSymbols()
    ck._defineSymbols()
    pk._defineSymbols()
    mavg._defineSymbols()
    plt._defineSymbols()
    figw._defineSymbols()
    pt._defineSymbols()
    ttools._defineSymbols()
    tloggers._defineSymbols()
    csv._defineSymbols()
    _filter._defineSymbols()

    import deephaven.TableManipulation
    deephaven.TableManipulation._defineSymbols()
    global Aggregation, ColumnName, ColumnRenderersBuilder, DistinctFormatter, DownsampledWhereFilter, DynamicTableWriter, \
        LayoutHintBuilder, Replayer, SmartKey, SortColumn, TotalsTableBuilder
    from deephaven.TableManipulation import Aggregation, ColumnName, ColumnRenderersBuilder, DistinctFormatter, \
        DownsampledWhereFilter, DynamicTableWriter, LayoutHintBuilder, Replayer, SmartKey, \
        SortColumn, TotalsTableBuilder

    WindowCheck._defineSymbols()

    global Figure, PlottingConvenience
    Figure = FigureUnsupported
    PlottingConvenience = FigureUnsupported

from .jvm_init import jvm_init
from .start_jvm import start_jvm


Figure = None  # variable for initialization
PlottingConvenience = None  # variable for initialization


def verifyPicklingCompatibility(otherPythonVersion):
    """
    Check a provided python version string versus the present instance string for pickling safety.

    :param otherPythonVersion: other version string
    :return: True is safe, False otherwise
    """

    if otherPythonVersion is None:
        return False
    sPyVer = otherPythonVersion.split('.')
    if len(sPyVer) < 3:
        return False
    try:
        major, minor = int(sPyVer[0]), int(sPyVer[1])
        # We need both major and minor agreement for pickling compatibility
        return major == sys.version_info[0] and minor == sys.version_info[1]
    except Exception:
        return False

class FigureUnsupported(object):
    def __init__(self):
        raise Exception("Can not create a plot outside of the console.")


def __initializer__(jtype, obj):
    for key, value in jtype.__dict__.items():
        obj.__dict__.update({key: value})


def PickledResult(pickled):
    return jpy.get_type("io.deephaven.engine.util.PickledResult")(pickled, sys.version)


def Config():
    return jpy.get_type("io.deephaven.configuration.Configuration").getInstance()


def PythonListenerAdapter(table, implementation, description=None, retain=True, replayInitialImage=False):
    """
    Constructs the ShiftObliviousInstrumentedListenerAdapter, implemented in Python, and plugs it into the table's
    listenForUpdates method.

    :param table: table to which to listen
    :param implementation: the body of the implementation for the ShiftObliviousInstrumentedListenerAdapter.onUpdate method, and
      must either be a class with onUpdate method or a callable.
    :param description: A description for the UpdatePerformanceTracker to append to its entry description.
    :param retain: Whether a hard reference to this listener should be maintained to prevent it from being collected.
    :param replayInitialImage: False to only process new rows, ignoring any previously existing rows in the Table;
      True to process updates for all initial rows in the table PLUS all new row changes.
    """

    jtype = jpy.get_type('io.deephaven.integrations.python.PythonShiftObliviousListenerAdapter')
    ListenerInstance = jtype(description, table, retain, implementation)
    table.listenForUpdates(ListenerInstance, replayInitialImage)


def PythonShiftAwareListenerAdapter(table, implementation, description=None, retain=True):
    """
    Constructs the InstrumentedTableUpdateListenerAdapter, implemented in Python, and plugs it into the table's
    listenForUpdates method.

    :param table: table to which to listen
    :param implementation: the body of the implementation for the InstrumentedTableUpdateListenerAdapter.onUpdate method, and
      must either be a class with onUpdate method or a callable.
    :param description: A description for the UpdatePerformanceTracker to append to its entry description.
    :param retain: Whether a hard reference to this listener should be maintained to prevent it from being collected.
    """

    jtype = jpy.get_type('io.deephaven.integrations.python.PythonListenerAdapter')
    ListenerInstance = jtype(description, table, retain, implementation)
    table.listenForUpdates(ListenerInstance)


def PythonFunction(func, classString):
    """
    Constructs a Java Function<PyObject, Object> implementation from the given python function `func`. The proper
    Java object interpretation for the return of `func` must be provided.

    :param func: Python callable or class instance with `apply` method (single argument)
    :param classString: the fully qualified class path of the return for `func`. This is really anticipated to be one
                        of `java.lang.String`, `double`, 'float`, `long`, `int`, `short`, `byte`, or `boolean`,
                        and any other value will result in `java.lang.Object` and likely be unusable.
    :return: io.deephaven.integrations.python.PythonFunction instance, primarily intended for use in PivotWidgetBuilder usage
    """

    jtype = jpy.get_type('io.deephaven.integrations.python.PythonFunction')
    return jtype(func, getJavaClassObject(classString))


class TableListenerHandle:
    """
    A handle for a table listener.
    """

    def __init__(self, t, listener):
        """
        Creates a new table listener handle.

        :param t: table being listened to.
        :param listener: listener object.
        """
        self.t = t
        self.listener = listener
        self.isRegistered = False


    def register(self):
        """
        Register the listener with the table and listen for updates.
        """

        if self.isRegistered:
            raise RuntimeError("Attempting to register an already registered listener..")

        self.t.listenForUpdates(self.listener)

        self.isRegistered = True


    def deregister(self):
        """
        Deregister the listener from the table and stop listening for updates.
        """

        if not self.isRegistered:
            raise RuntimeError("Attempting to deregister an unregistered listener..")

        self.t.removeUpdateListener(self.listener)
        self.isRegistered = False


def _nargsListener(listener):
    """
    Returns the number of arguments the listener takes.

    :param listener: listener
    :return: number of arguments the listener takes.
    """

    if callable(listener):
        f = listener
    elif hasattr(listener, 'onUpdate'):
        f = listener.onUpdate
    else:
        raise ValueError("ShiftObliviousListener is neither callable nor has an 'onUpdate' method")

    if sys.version[0] == "2":
        import inspect
        return len(inspect.getargspec(f).args)
    elif sys.version[0] == "3":
        from inspect import signature
        return len(signature(f).parameters)
    else:
        raise NotImplementedError("Unsupported python version: version={}".format(sys.version))


def listen(t, listener, description=None, retain=True, ltype="auto", start_listening=True, replay_initial=False, lock_type="shared"):
    """
    Listen to table changes.

    Table change events are processed by listener, which can be either (1) a callable (e.g. function) or
    (2) an object which provides an "onUpdate" method.
    In either case, the method must have one of the following signatures.

    * (added, removed, modified): shift_oblivious or legacy
    * (isReplay, added, removed, modified): shift_oblivious + replay
    * (update): shift-aware
    * (isReplay, update): shift-aware + replay

    For legacy listeners, added, removed, and modified are the indices of the rows which changed.
    For shift-aware listeners, update is an object which describes the table update.
    Listeners which support replaying the initial table snapshot have an additional parameter, inReplay, which is
    true when replaying the initial snapshot and false during normal updates.

    See the Deephaven listener documentation for details on processing update events.  This documentation covers the
    details of the added, removed, and modified row sets; row shift information; as well as the details of how to apply
    the update object.  It also has examples on how to access current and previous-tick table values.

    :param t: table to listen to.
    :param listener: listener to process changes.
    :param description: description for the UpdatePerformanceTracker to append to the listener's entry description.
    :param retain: whether a hard reference to this listener should be maintained to prevent it from being collected.
    :param ltype: listener type.  Valid values are "auto", "legacy", and "shift_aware".  "auto" (default) uses inspection to automatically determine the type of input listener.  "legacy" is for a legacy listener, which takes three (added, removed, modified) or four (isReplay, added, removed, modified) arguments.  "shift_aware" is for a shift-aware listener, which takes one (update) or two (isReplay, update) arguments.
    :param start_listening: True to create the listener and register the listener with the table.  The listener will see updates.  False to create the listener, but do not register the listener with the table.  The listener will not see updates.
    :param replay_initial: True to replay the initial table contents to the listener.  False to only listen to new table changes.  To replay the initial image, the listener must support replay.
    :param lock_type: UGP lock type.  Used when replay_initial=True.  See :func:`doLocked` for valid values.
    :return: table listener handle.
    """

    if replay_initial and not start_listening:
        raise ValueError("Unable to create listener.  Inconsistent arguments.  If the initial snapshot is replayed (replay_initial=True), then the listener must be registered to start listening (start_listening=True).")

    nargs = _nargsListener(listener)

    if ltype == None or ltype == "auto":
        if nargs == 1 or nargs == 2:
            ltype = "shift_aware"
        elif nargs == 3 or nargs == 4:
            ltype = "shift_oblivious"
        else:
            raise ValueError("Unable to autodetect listener type.  ShiftObliviousListener does not take an expected number of arguments.  args={}".format(nargs))

    if ltype == "shift_oblivious" or ltype == "legacy":
        if nargs == 3:
            if replay_initial:
                raise ValueError("ShiftObliviousListener does not support replay: ltype={} nargs={}".format(ltype, nargs))

            ListenerAdapter = jpy.get_type("io.deephaven.integrations.python.PythonShiftObliviousListenerAdapter")
            listener_adapter = ListenerAdapter(description, t, retain, listener)
        elif nargs == 4:
            ListenerAdapter = jpy.get_type("io.deephaven.integrations.python.PythonReplayShiftObliviousListenerAdapter")
            listener_adapter = ListenerAdapter(description, t, retain, listener)
        else:
            raise ValueError("Legacy listener must take 3 (added, removed, modified) or 4 (isReplay, added, removed, modified) arguments.")

    elif ltype == "shift_aware":
        if nargs == 1:
            if replay_initial:
                raise ValueError("ShiftObliviousListener does not support replay: ltype={} nargs={}".format(ltype, nargs))

            ListenerAdapter = jpy.get_type("io.deephaven.integrations.python.PythonListenerAdapter")
            listener_adapter = ListenerAdapter(description, t, retain, listener)
        elif nargs == 2:
            ListenerAdapter = jpy.get_type("io.deephaven.integrations.python.PythonReplayListenerAdapter")
            listener_adapter = ListenerAdapter(description, t, retain, listener)
        else:
            raise ValueError("Shift-aware listener must take 1 (update) or 2 (isReplay, update) arguments.")

    else:
        raise ValueError("Unsupported listener type: ltype={}".format(ltype))

    handle = TableListenerHandle(t, listener_adapter)

    def start():
        if replay_initial:
            listener_adapter.replay()

        if start_listening:
            handle.register()

    if replay_initial:
        doLocked(start, lock_type=lock_type)
    else:
        start()

    return handle


def doLocked(f, lock_type="shared"):
    """
    Executes a function while holding the UpdateGraphProcessor (UGP) lock.  Holding the UGP lock
    ensures that the contents of a table will not change during a computation, but holding
    the lock also prevents table updates from happening.  The lock should be held for as little
    time as possible.

    :param f: function to execute while holding the UGP lock.  f must be callable or have an apply attribute which is callable.
    :param lock_type: UGP lock type.  Valid values are "exclusive" and "shared".  "exclusive" allows only a single reader or writer to hold the lock.  "shared" allows multiple readers or a single writer to hold the lock.
    """
    ThrowingRunnable = jpy.get_type("io.deephaven.integrations.python.PythonThrowingRunnable")
    UpdateGraphProcessor = jpy.get_type("io.deephaven.engine.updategraph.UpdateGraphProcessor")

    if lock_type == "exclusive":
        UpdateGraphProcessor.DEFAULT.exclusiveLock().doLocked(ThrowingRunnable(f))
    elif lock_type == "shared":
        UpdateGraphProcessor.DEFAULT.sharedLock().doLocked(ThrowingRunnable(f))
    else:
        raise ValueError("Unsupported lock type: lock_type={}".format(lock_type))


def as_list(values):
    """
    Creates a Java list containing the values.

    :param values: values
    :return: Java list containing the values.
    """
    _JArrayList = jpy.get_type("java.util.ArrayList")
    j_list = _JArrayList(len(values))
    for value in values:
        j_list.add(value)
    return j_list

