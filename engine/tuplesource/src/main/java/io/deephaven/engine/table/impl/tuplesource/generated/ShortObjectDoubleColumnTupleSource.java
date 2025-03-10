package io.deephaven.engine.table.impl.tuplesource.generated;

import io.deephaven.chunk.Chunk;
import io.deephaven.chunk.DoubleChunk;
import io.deephaven.chunk.ObjectChunk;
import io.deephaven.chunk.ShortChunk;
import io.deephaven.chunk.WritableChunk;
import io.deephaven.chunk.WritableObjectChunk;
import io.deephaven.chunk.attributes.Values;
import io.deephaven.datastructures.util.SmartKey;
import io.deephaven.engine.table.ColumnSource;
import io.deephaven.engine.table.TupleSource;
import io.deephaven.engine.table.WritableColumnSource;
import io.deephaven.engine.table.impl.tuplesource.AbstractTupleSource;
import io.deephaven.engine.table.impl.tuplesource.ThreeColumnTupleSourceFactory;
import io.deephaven.tuple.generated.ShortObjectDoubleTuple;
import io.deephaven.util.type.TypeUtils;
import org.jetbrains.annotations.NotNull;


/**
 * <p>{@link TupleSource} that produces key column values from {@link ColumnSource} types Short, Object, and Double.
 * <p>Generated by io.deephaven.replicators.TupleSourceCodeGenerator.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class ShortObjectDoubleColumnTupleSource extends AbstractTupleSource<ShortObjectDoubleTuple> {

    /** {@link ThreeColumnTupleSourceFactory} instance to create instances of {@link ShortObjectDoubleColumnTupleSource}. **/
    public static final ThreeColumnTupleSourceFactory<ShortObjectDoubleTuple, Short, Object, Double> FACTORY = new Factory();

    private final ColumnSource<Short> columnSource1;
    private final ColumnSource<Object> columnSource2;
    private final ColumnSource<Double> columnSource3;

    public ShortObjectDoubleColumnTupleSource(
            @NotNull final ColumnSource<Short> columnSource1,
            @NotNull final ColumnSource<Object> columnSource2,
            @NotNull final ColumnSource<Double> columnSource3
    ) {
        super(columnSource1, columnSource2, columnSource3);
        this.columnSource1 = columnSource1;
        this.columnSource2 = columnSource2;
        this.columnSource3 = columnSource3;
    }

    @Override
    public final ShortObjectDoubleTuple createTuple(final long rowKey) {
        return new ShortObjectDoubleTuple(
                columnSource1.getShort(rowKey),
                columnSource2.get(rowKey),
                columnSource3.getDouble(rowKey)
        );
    }

    @Override
    public final ShortObjectDoubleTuple createPreviousTuple(final long rowKey) {
        return new ShortObjectDoubleTuple(
                columnSource1.getPrevShort(rowKey),
                columnSource2.getPrev(rowKey),
                columnSource3.getPrevDouble(rowKey)
        );
    }

    @Override
    public final ShortObjectDoubleTuple createTupleFromValues(@NotNull final Object... values) {
        return new ShortObjectDoubleTuple(
                TypeUtils.unbox((Short)values[0]),
                values[1],
                TypeUtils.unbox((Double)values[2])
        );
    }

    @Override
    public final ShortObjectDoubleTuple createTupleFromReinterpretedValues(@NotNull final Object... values) {
        return new ShortObjectDoubleTuple(
                TypeUtils.unbox((Short)values[0]),
                values[1],
                TypeUtils.unbox((Double)values[2])
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <ELEMENT_TYPE> void exportElement(@NotNull final ShortObjectDoubleTuple tuple, final int elementIndex, @NotNull final WritableColumnSource<ELEMENT_TYPE> writableSource, final long destinationRowKey) {
        if (elementIndex == 0) {
            writableSource.set(destinationRowKey, tuple.getFirstElement());
            return;
        }
        if (elementIndex == 1) {
            writableSource.set(destinationRowKey, (ELEMENT_TYPE) tuple.getSecondElement());
            return;
        }
        if (elementIndex == 2) {
            writableSource.set(destinationRowKey, tuple.getThirdElement());
            return;
        }
        throw new IndexOutOfBoundsException("Invalid element index " + elementIndex + " for export");
    }

    @Override
    public final Object exportToExternalKey(@NotNull final ShortObjectDoubleTuple tuple) {
        return new SmartKey(
                TypeUtils.box(tuple.getFirstElement()),
                tuple.getSecondElement(),
                TypeUtils.box(tuple.getThirdElement())
        );
    }

    @Override
    public final Object exportElement(@NotNull final ShortObjectDoubleTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return TypeUtils.box(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return tuple.getSecondElement();
        }
        if (elementIndex == 2) {
            return TypeUtils.box(tuple.getThirdElement());
        }
        throw new IllegalArgumentException("Bad elementIndex for 3 element tuple: " + elementIndex);
    }

    @Override
    public final Object exportElementReinterpreted(@NotNull final ShortObjectDoubleTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return TypeUtils.box(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return tuple.getSecondElement();
        }
        if (elementIndex == 2) {
            return TypeUtils.box(tuple.getThirdElement());
        }
        throw new IllegalArgumentException("Bad elementIndex for 3 element tuple: " + elementIndex);
    }

    @Override
    protected void convertChunks(@NotNull WritableChunk<? super Values> destination, int chunkSize, Chunk<Values> [] chunks) {
        WritableObjectChunk<ShortObjectDoubleTuple, ? super Values> destinationObjectChunk = destination.asWritableObjectChunk();
        ShortChunk<Values> chunk1 = chunks[0].asShortChunk();
        ObjectChunk<Object, Values> chunk2 = chunks[1].asObjectChunk();
        DoubleChunk<Values> chunk3 = chunks[2].asDoubleChunk();
        for (int ii = 0; ii < chunkSize; ++ii) {
            destinationObjectChunk.set(ii, new ShortObjectDoubleTuple(chunk1.get(ii), chunk2.get(ii), chunk3.get(ii)));
        }
        destinationObjectChunk.setSize(chunkSize);
    }

    /** {@link ThreeColumnTupleSourceFactory} for instances of {@link ShortObjectDoubleColumnTupleSource}. **/
    private static final class Factory implements ThreeColumnTupleSourceFactory<ShortObjectDoubleTuple, Short, Object, Double> {

        private Factory() {
        }

        @Override
        public TupleSource<ShortObjectDoubleTuple> create(
                @NotNull final ColumnSource<Short> columnSource1,
                @NotNull final ColumnSource<Object> columnSource2,
                @NotNull final ColumnSource<Double> columnSource3
        ) {
            return new ShortObjectDoubleColumnTupleSource(
                    columnSource1,
                    columnSource2,
                    columnSource3
            );
        }
    }
}
