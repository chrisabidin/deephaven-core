/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.engine.table.impl;

import io.deephaven.api.JoinMatch;
import io.deephaven.api.Selectable;
import io.deephaven.api.SortColumn;
import io.deephaven.api.agg.Aggregation;
import io.deephaven.api.agg.spec.AggSpec;
import io.deephaven.api.filter.Filter;
import io.deephaven.base.verify.Assert;
import io.deephaven.engine.liveness.Liveness;
import io.deephaven.engine.rowset.TrackingRowSet;
import io.deephaven.engine.table.*;
import io.deephaven.engine.table.iterators.*;
import io.deephaven.engine.updategraph.ConcurrentMethod;
import io.deephaven.util.QueryConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

/**
 * Abstract class for uncoalesced tables. These tables have deferred work that must be done before data can be operated
 * on.
 */
public abstract class UncoalescedTable extends BaseTable implements TableWithDefaults {

    private final Object coalescingLock = new Object();

    private volatile Table coalesced;

    public UncoalescedTable(@NotNull final TableDefinition definition, @NotNull final String description) {
        super(definition, description);
    }

    // region coalesce support

    /**
     * Produce the actual coalesced result table, suitable for caching.
     * <p>
     * Note that if this table must have listeners registered, etc, setting these up is the implementation's
     * responsibility.
     * <p>
     * Also note that the implementation should copy attributes, as in
     * {@code copyAttributes(resultTable, CopyAttributeOperation.Coalesce)}.
     *
     * @return The coalesced result table, suitable for caching
     */
    protected abstract Table doCoalesce();

    public final Table coalesce() {
        Table localCoalesced;
        if (Liveness.verifyCachedObjectForReuse(localCoalesced = coalesced)) {
            return localCoalesced;
        }
        synchronized (coalescingLock) {
            if (Liveness.verifyCachedObjectForReuse(localCoalesced = coalesced)) {
                return localCoalesced;
            }
            return coalesced = doCoalesce();
        }
    }

    /**
     * Proactively set the coalesced result table. See {@link #doCoalesce()} for the caller's responsibilities. Note
     * that it is an error to call this more than once with a non-null input.
     *
     * @param coalesced The coalesced result table, suitable for caching
     */
    protected final void setCoalesced(final Table coalesced) {
        synchronized (coalescingLock) {
            Assert.eqNull(this.coalesced, "this.coalesced");
            this.coalesced = coalesced;
        }
    }

    protected @Nullable final Table getCoalesced() {
        return coalesced;
    }

    // endregion coalesce support

    // region uncoalesced listeners

    protected final void listenForUpdatesUncoalesced(@NotNull final TableUpdateListener listener) {
        super.listenForUpdates(listener);
    }

    protected final void removeUpdateListenerUncoalesced(@NotNull final TableUpdateListener listener) {
        super.removeUpdateListener(listener);
    }

    // endregion uncoalesced listeners

    // region non-delegated overrides

    @Override
    public long sizeForInstrumentation() {
        return QueryConstants.NULL_LONG;
    }

    @Override
    public boolean isFlat() {
        return false;
    }

    // endregion non-delegated methods

    // region delegated methods

    @Override
    public long size() {
        return coalesce().size();
    }

    @Override
    public TrackingRowSet getRowSet() {
        return coalesce().getRowSet();
    }

    @Override
    public <T> ColumnSource<T> getColumnSource(String sourceName) {
        return coalesce().getColumnSource(sourceName);
    }

    @Override
    public Map<String, ? extends ColumnSource<?>> getColumnSourceMap() {
        return coalesce().getColumnSourceMap();
    }

    @Override
    public Collection<? extends ColumnSource<?>> getColumnSources() {
        return coalesce().getColumnSources();
    }

    @Override
    public DataColumn[] getColumns() {
        return coalesce().getColumns();
    }

    @Override
    public DataColumn getColumn(String columnName) {
        return coalesce().getColumn(columnName);
    }

    @Override
    public <TYPE> Iterator<TYPE> columnIterator(@NotNull String columnName) {
        return coalesce().columnIterator(columnName);
    }

    @Override
    public ByteColumnIterator byteColumnIterator(@NotNull String columnName) {
        return coalesce().byteColumnIterator(columnName);
    }

    @Override
    public CharacterColumnIterator characterColumnIterator(@NotNull String columnName) {
        return coalesce().characterColumnIterator(columnName);
    }

    @Override
    public DoubleColumnIterator doubleColumnIterator(@NotNull String columnName) {
        return coalesce().doubleColumnIterator(columnName);
    }

    @Override
    public FloatColumnIterator floatColumnIterator(@NotNull String columnName) {
        return coalesce().floatColumnIterator(columnName);
    }

    @Override
    public IntegerColumnIterator integerColumnIterator(@NotNull String columnName) {
        return coalesce().integerColumnIterator(columnName);
    }

    @Override
    public LongColumnIterator longColumnIterator(@NotNull String columnName) {
        return coalesce().longColumnIterator(columnName);
    }

    @Override
    public ShortColumnIterator shortColumnIterator(@NotNull String columnName) {
        return coalesce().shortColumnIterator(columnName);
    }

    @Override
    public Object[] getRecord(long rowNo, String... columnNames) {
        return coalesce().getRecord(rowNo, columnNames);
    }

    @Override
    @ConcurrentMethod
    public Table where(Collection<? extends Filter> filters) {
        return coalesce().where(filters);
    }

    @Override
    @ConcurrentMethod
    public Table wouldMatch(WouldMatchPair... matchers) {
        return coalesce().wouldMatch(matchers);
    }

    @Override
    public Table whereIn(Table rightTable, Collection<? extends JoinMatch> columnsToMatch) {
        return coalesce().whereIn(rightTable, columnsToMatch);
    }

    @Override
    public Table whereNotIn(Table rightTable, Collection<? extends JoinMatch> columnsToMatch) {
        return coalesce().whereNotIn(rightTable, columnsToMatch);
    }

    @Override
    public Table select(Collection<? extends Selectable> columns) {
        return coalesce().select(columns);
    }

    @Override
    @ConcurrentMethod
    public Table selectDistinct(Collection<? extends Selectable> columns) {
        return coalesce().selectDistinct(columns);
    }

    @Override
    public Table update(Collection<? extends Selectable> columns) {
        return coalesce().update(columns);
    }

    @Override
    public Table lazyUpdate(Collection<? extends Selectable> newColumns) {
        return coalesce().lazyUpdate(newColumns);
    }

    @Override
    @ConcurrentMethod
    public Table view(Collection<? extends Selectable> columns) {
        return coalesce().view(columns);
    }

    @Override
    @ConcurrentMethod
    public Table updateView(Collection<? extends Selectable> columns) {
        return coalesce().updateView(columns);
    }

    @Override
    @ConcurrentMethod
    public Table dropColumns(String... columnNames) {
        return coalesce().dropColumns(columnNames);
    }

    @Override
    public Table renameColumns(MatchPair... pairs) {
        return coalesce().renameColumns(pairs);
    }

    @Override
    @ConcurrentMethod
    public Table formatColumns(String... columnFormats) {
        return coalesce().formatColumns(columnFormats);
    }

    @Override
    @ConcurrentMethod
    public Table moveColumns(int index, boolean moveToEnd, String... columnsToMove) {
        return coalesce().moveColumns(index, moveToEnd, columnsToMove);
    }

    @Override
    @ConcurrentMethod
    public Table dateTimeColumnAsNanos(String dateTimeColumnName, String nanosColumnName) {
        return coalesce().dateTimeColumnAsNanos(dateTimeColumnName, nanosColumnName);
    }

    @Override
    @ConcurrentMethod
    public Table head(long size) {
        return coalesce().head(size);
    }

    @Override
    @ConcurrentMethod
    public Table tail(long size) {
        return coalesce().tail(size);
    }

    @Override
    @ConcurrentMethod
    public Table slice(long firstPositionInclusive, long lastPositionExclusive) {
        return coalesce().slice(firstPositionInclusive, lastPositionExclusive);
    }

    @Override
    @ConcurrentMethod
    public Table headPct(double percent) {
        return coalesce().headPct(percent);
    }

    @Override
    @ConcurrentMethod
    public Table tailPct(double percent) {
        return coalesce().tailPct(percent);
    }

    @Override
    public Table exactJoin(Table rightTable, MatchPair[] columnsToMatch, MatchPair[] columnsToAdd) {
        return coalesce().exactJoin(rightTable, columnsToMatch, columnsToAdd);
    }

    @Override
    public Table aj(Table rightTable, MatchPair[] columnsToMatch, MatchPair[] columnsToAdd,
            AsOfMatchRule asOfMatchRule) {
        return coalesce().aj(rightTable, columnsToMatch, columnsToAdd, asOfMatchRule);
    }

    @Override
    public Table raj(Table rightTable, MatchPair[] columnsToMatch, MatchPair[] columnsToAdd,
            AsOfMatchRule asOfMatchRule) {
        return coalesce().raj(rightTable, columnsToMatch, columnsToAdd, asOfMatchRule);
    }

    @Override
    public Table naturalJoin(Table rightTable, MatchPair[] columnsToMatch, MatchPair[] columnsToAdd) {
        return coalesce().naturalJoin(rightTable, columnsToMatch, columnsToAdd);
    }

    @Override
    public Table join(Table rightTable, MatchPair[] columnsToMatch, MatchPair[] columnsToAdd,
            int numRightBitsToReserve) {
        return coalesce().join(rightTable, columnsToMatch, columnsToAdd, numRightBitsToReserve);
    }

    @Override
    @ConcurrentMethod
    public Table groupBy(Collection<? extends Selectable> groupByColumns) {
        return coalesce().groupBy(groupByColumns);
    }

    @Override
    @ConcurrentMethod
    public Table aggAllBy(AggSpec spec, Selectable... groupByColumns) {
        return coalesce().aggAllBy(spec, groupByColumns);
    }

    @Override
    @ConcurrentMethod
    public Table aggBy(Collection<? extends Aggregation> aggregations,
            Collection<? extends Selectable> groupByColumns) {
        return coalesce().aggBy(aggregations, groupByColumns);
    }

    @Override
    public Table headBy(long nRows, String... groupByColumnNames) {
        return coalesce().headBy(nRows, groupByColumnNames);
    }

    @Override
    public Table tailBy(long nRows, String... groupByColumnNames) {
        return coalesce().tailBy(nRows, groupByColumnNames);
    }

    @Override
    @ConcurrentMethod
    public Table applyToAllBy(String formulaColumn, String columnParamName,
            Collection<? extends Selectable> groupByColumns) {
        return coalesce().applyToAllBy(formulaColumn, columnParamName, groupByColumns);
    }

    @Override
    @ConcurrentMethod
    public Table sumBy(Selectable... groupByColumns) {
        return coalesce().sumBy(groupByColumns);
    }

    @Override
    @ConcurrentMethod
    public Table absSumBy(Selectable... groupByColumns) {
        return coalesce().absSumBy(groupByColumns);
    }

    @Override
    @ConcurrentMethod
    public Table avgBy(Selectable... groupByColumns) {
        return coalesce().avgBy(groupByColumns);
    }

    @Override
    @ConcurrentMethod
    public Table wavgBy(String weightColumn, Selectable... groupByColumns) {
        return coalesce().wavgBy(weightColumn, groupByColumns);
    }

    @Override
    @ConcurrentMethod
    public Table wsumBy(String weightColumn, Selectable... groupByColumns) {
        return coalesce().wsumBy(weightColumn, groupByColumns);
    }

    @Override
    @ConcurrentMethod
    public Table stdBy(Selectable... groupByColumns) {
        return coalesce().stdBy(groupByColumns);
    }

    @Override
    @ConcurrentMethod
    public Table varBy(Selectable... groupByColumns) {
        return coalesce().varBy(groupByColumns);
    }

    @Override
    @ConcurrentMethod
    public Table lastBy(Selectable... groupByColumns) {
        return coalesce().lastBy(groupByColumns);
    }

    @Override
    @ConcurrentMethod
    public Table firstBy(Selectable... groupByColumns) {
        return coalesce().firstBy(groupByColumns);
    }

    @Override
    @ConcurrentMethod
    public Table minBy(Selectable... groupByColumns) {
        return coalesce().minBy(groupByColumns);
    }

    @Override
    @ConcurrentMethod
    public Table maxBy(Selectable... groupByColumns) {
        return coalesce().maxBy(groupByColumns);
    }

    @Override
    @ConcurrentMethod
    public Table medianBy(Selectable... groupByColumns) {
        return coalesce().medianBy(groupByColumns);
    }

    @Override
    @ConcurrentMethod
    public Table countBy(String countColumnName, Selectable... groupByColumns) {
        return coalesce().countBy(countColumnName, groupByColumns);
    }

    @Override
    public Table ungroup(boolean nullFill, String... columnsToUngroup) {
        return coalesce().ungroup(nullFill, columnsToUngroup);
    }

    @Override
    @ConcurrentMethod
    public TableMap partitionBy(boolean dropKeys, String... keyColumnNames) {
        return coalesce().partitionBy(dropKeys, keyColumnNames);
    }

    @Override
    @ConcurrentMethod
    public Table rollup(Collection<? extends Aggregation> aggregations, boolean includeConstituents,
            Selectable... columns) {
        return coalesce().rollup(aggregations, includeConstituents, columns);
    }

    @Override
    @ConcurrentMethod
    public Table treeTable(String idColumn, String parentColumn) {
        return coalesce().treeTable(idColumn, parentColumn);
    }

    @Override
    @ConcurrentMethod
    public Table sort(Collection<SortColumn> columnsToSortBy) {
        return coalesce().sort(columnsToSortBy);
    }

    @Override
    @ConcurrentMethod
    public Table reverse() {
        return coalesce().reverse();
    }

    @Override
    public Table snapshot(Table baseTable, boolean doInitialSnapshot, String... stampColumns) {
        return coalesce().snapshot(baseTable, doInitialSnapshot, stampColumns);
    }

    @Override
    public Table snapshotIncremental(Table rightTable, boolean doInitialSnapshot, String... stampColumns) {
        return coalesce().snapshotIncremental(rightTable, doInitialSnapshot, stampColumns);
    }

    @Override
    public Table snapshotHistory(Table rightTable) {
        return coalesce().snapshotHistory(rightTable);
    }

    @Override
    public Table getSubTable(TrackingRowSet rowSet) {
        return coalesce().getSubTable(rowSet);
    }

    @Override
    public <R> R apply(Function<Table, R> function) {
        return coalesce().apply(function);
    }

    @Override
    @ConcurrentMethod
    public Table flatten() {
        return coalesce().flatten();
    }

    @Override
    public void awaitUpdate() throws InterruptedException {
        coalesce().awaitUpdate();
    }

    @Override
    public boolean awaitUpdate(long timeout) throws InterruptedException {
        return coalesce().awaitUpdate(timeout);
    }

    @Override
    public void listenForUpdates(ShiftObliviousListener listener, boolean replayInitialImage) {
        coalesce().listenForUpdates(listener, replayInitialImage);
    }

    @Override
    public void listenForUpdates(TableUpdateListener listener) {
        coalesce().listenForUpdates(listener);
    }

    @Override
    public void removeUpdateListener(ShiftObliviousListener listener) {
        coalesce().removeUpdateListener(listener);
    }

    @Override
    public void removeUpdateListener(TableUpdateListener listener) {
        coalesce().removeUpdateListener(listener);
    }

    // endregion delegated methods
}
