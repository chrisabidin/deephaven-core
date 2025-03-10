package io.deephaven.engine.table.impl.tuplesource.generated;

import io.deephaven.chunk.Chunk;
import io.deephaven.chunk.DoubleChunk;
import io.deephaven.chunk.ObjectChunk;
import io.deephaven.chunk.WritableChunk;
import io.deephaven.chunk.WritableObjectChunk;
import io.deephaven.chunk.attributes.Values;
import io.deephaven.datastructures.util.SmartKey;
import io.deephaven.engine.table.ColumnSource;
import io.deephaven.engine.table.TupleSource;
import io.deephaven.engine.table.WritableColumnSource;
import io.deephaven.engine.table.impl.tuplesource.AbstractTupleSource;
import io.deephaven.engine.table.impl.tuplesource.TwoColumnTupleSourceFactory;
import io.deephaven.tuple.generated.DoubleObjectTuple;
import io.deephaven.util.type.TypeUtils;
import org.jetbrains.annotations.NotNull;


/**
 * <p>{@link TupleSource} that produces key column values from {@link ColumnSource} types Double and Object.
 * <p>Generated by io.deephaven.replicators.TupleSourceCodeGenerator.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class DoubleObjectColumnTupleSource extends AbstractTupleSource<DoubleObjectTuple> {

    /** {@link TwoColumnTupleSourceFactory} instance to create instances of {@link DoubleObjectColumnTupleSource}. **/
    public static final TwoColumnTupleSourceFactory<DoubleObjectTuple, Double, Object> FACTORY = new Factory();

    private final ColumnSource<Double> columnSource1;
    private final ColumnSource<Object> columnSource2;

    public DoubleObjectColumnTupleSource(
            @NotNull final ColumnSource<Double> columnSource1,
            @NotNull final ColumnSource<Object> columnSource2
    ) {
        super(columnSource1, columnSource2);
        this.columnSource1 = columnSource1;
        this.columnSource2 = columnSource2;
    }

    @Override
    public final DoubleObjectTuple createTuple(final long rowKey) {
        return new DoubleObjectTuple(
                columnSource1.getDouble(rowKey),
                columnSource2.get(rowKey)
        );
    }

    @Override
    public final DoubleObjectTuple createPreviousTuple(final long rowKey) {
        return new DoubleObjectTuple(
                columnSource1.getPrevDouble(rowKey),
                columnSource2.getPrev(rowKey)
        );
    }

    @Override
    public final DoubleObjectTuple createTupleFromValues(@NotNull final Object... values) {
        return new DoubleObjectTuple(
                TypeUtils.unbox((Double)values[0]),
                values[1]
        );
    }

    @Override
    public final DoubleObjectTuple createTupleFromReinterpretedValues(@NotNull final Object... values) {
        return new DoubleObjectTuple(
                TypeUtils.unbox((Double)values[0]),
                values[1]
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <ELEMENT_TYPE> void exportElement(@NotNull final DoubleObjectTuple tuple, final int elementIndex, @NotNull final WritableColumnSource<ELEMENT_TYPE> writableSource, final long destinationRowKey) {
        if (elementIndex == 0) {
            writableSource.set(destinationRowKey, tuple.getFirstElement());
            return;
        }
        if (elementIndex == 1) {
            writableSource.set(destinationRowKey, (ELEMENT_TYPE) tuple.getSecondElement());
            return;
        }
        throw new IndexOutOfBoundsException("Invalid element index " + elementIndex + " for export");
    }

    @Override
    public final Object exportToExternalKey(@NotNull final DoubleObjectTuple tuple) {
        return new SmartKey(
                TypeUtils.box(tuple.getFirstElement()),
                tuple.getSecondElement()
        );
    }

    @Override
    public final Object exportElement(@NotNull final DoubleObjectTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return TypeUtils.box(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return tuple.getSecondElement();
        }
        throw new IllegalArgumentException("Bad elementIndex for 2 element tuple: " + elementIndex);
    }

    @Override
    public final Object exportElementReinterpreted(@NotNull final DoubleObjectTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return TypeUtils.box(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return tuple.getSecondElement();
        }
        throw new IllegalArgumentException("Bad elementIndex for 2 element tuple: " + elementIndex);
    }

    protected void convertChunks(@NotNull WritableChunk<? super Values> destination, int chunkSize, Chunk<Values> [] chunks) {
        WritableObjectChunk<DoubleObjectTuple, ? super Values> destinationObjectChunk = destination.asWritableObjectChunk();
        DoubleChunk<Values> chunk1 = chunks[0].asDoubleChunk();
        ObjectChunk<Object, Values> chunk2 = chunks[1].asObjectChunk();
        for (int ii = 0; ii < chunkSize; ++ii) {
            destinationObjectChunk.set(ii, new DoubleObjectTuple(chunk1.get(ii), chunk2.get(ii)));
        }
        destination.setSize(chunkSize);
    }

    /** {@link TwoColumnTupleSourceFactory} for instances of {@link DoubleObjectColumnTupleSource}. **/
    private static final class Factory implements TwoColumnTupleSourceFactory<DoubleObjectTuple, Double, Object> {

        private Factory() {
        }

        @Override
        public TupleSource<DoubleObjectTuple> create(
                @NotNull final ColumnSource<Double> columnSource1,
                @NotNull final ColumnSource<Object> columnSource2
        ) {
            return new DoubleObjectColumnTupleSource(
                    columnSource1,
                    columnSource2
            );
        }
    }
}
