package io.deephaven.engine.table.impl.tuplesource.generated;

import io.deephaven.chunk.Chunk;
import io.deephaven.chunk.FloatChunk;
import io.deephaven.chunk.IntChunk;
import io.deephaven.chunk.ObjectChunk;
import io.deephaven.chunk.WritableChunk;
import io.deephaven.chunk.WritableObjectChunk;
import io.deephaven.chunk.attributes.Values;
import io.deephaven.datastructures.util.SmartKey;
import io.deephaven.engine.table.ColumnSource;
import io.deephaven.engine.table.TupleSource;
import io.deephaven.engine.table.WritableColumnSource;
import io.deephaven.engine.table.impl.tuplesource.AbstractTupleSource;
import io.deephaven.engine.table.impl.tuplesource.ThreeColumnTupleSourceFactory;
import io.deephaven.tuple.generated.IntFloatObjectTuple;
import io.deephaven.util.type.TypeUtils;
import org.jetbrains.annotations.NotNull;


/**
 * <p>{@link TupleSource} that produces key column values from {@link ColumnSource} types Integer, Float, and Object.
 * <p>Generated by io.deephaven.replicators.TupleSourceCodeGenerator.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class IntegerFloatObjectColumnTupleSource extends AbstractTupleSource<IntFloatObjectTuple> {

    /** {@link ThreeColumnTupleSourceFactory} instance to create instances of {@link IntegerFloatObjectColumnTupleSource}. **/
    public static final ThreeColumnTupleSourceFactory<IntFloatObjectTuple, Integer, Float, Object> FACTORY = new Factory();

    private final ColumnSource<Integer> columnSource1;
    private final ColumnSource<Float> columnSource2;
    private final ColumnSource<Object> columnSource3;

    public IntegerFloatObjectColumnTupleSource(
            @NotNull final ColumnSource<Integer> columnSource1,
            @NotNull final ColumnSource<Float> columnSource2,
            @NotNull final ColumnSource<Object> columnSource3
    ) {
        super(columnSource1, columnSource2, columnSource3);
        this.columnSource1 = columnSource1;
        this.columnSource2 = columnSource2;
        this.columnSource3 = columnSource3;
    }

    @Override
    public final IntFloatObjectTuple createTuple(final long rowKey) {
        return new IntFloatObjectTuple(
                columnSource1.getInt(rowKey),
                columnSource2.getFloat(rowKey),
                columnSource3.get(rowKey)
        );
    }

    @Override
    public final IntFloatObjectTuple createPreviousTuple(final long rowKey) {
        return new IntFloatObjectTuple(
                columnSource1.getPrevInt(rowKey),
                columnSource2.getPrevFloat(rowKey),
                columnSource3.getPrev(rowKey)
        );
    }

    @Override
    public final IntFloatObjectTuple createTupleFromValues(@NotNull final Object... values) {
        return new IntFloatObjectTuple(
                TypeUtils.unbox((Integer)values[0]),
                TypeUtils.unbox((Float)values[1]),
                values[2]
        );
    }

    @Override
    public final IntFloatObjectTuple createTupleFromReinterpretedValues(@NotNull final Object... values) {
        return new IntFloatObjectTuple(
                TypeUtils.unbox((Integer)values[0]),
                TypeUtils.unbox((Float)values[1]),
                values[2]
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <ELEMENT_TYPE> void exportElement(@NotNull final IntFloatObjectTuple tuple, final int elementIndex, @NotNull final WritableColumnSource<ELEMENT_TYPE> writableSource, final long destinationRowKey) {
        if (elementIndex == 0) {
            writableSource.set(destinationRowKey, tuple.getFirstElement());
            return;
        }
        if (elementIndex == 1) {
            writableSource.set(destinationRowKey, tuple.getSecondElement());
            return;
        }
        if (elementIndex == 2) {
            writableSource.set(destinationRowKey, (ELEMENT_TYPE) tuple.getThirdElement());
            return;
        }
        throw new IndexOutOfBoundsException("Invalid element index " + elementIndex + " for export");
    }

    @Override
    public final Object exportToExternalKey(@NotNull final IntFloatObjectTuple tuple) {
        return new SmartKey(
                TypeUtils.box(tuple.getFirstElement()),
                TypeUtils.box(tuple.getSecondElement()),
                tuple.getThirdElement()
        );
    }

    @Override
    public final Object exportElement(@NotNull final IntFloatObjectTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return TypeUtils.box(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return TypeUtils.box(tuple.getSecondElement());
        }
        if (elementIndex == 2) {
            return tuple.getThirdElement();
        }
        throw new IllegalArgumentException("Bad elementIndex for 3 element tuple: " + elementIndex);
    }

    @Override
    public final Object exportElementReinterpreted(@NotNull final IntFloatObjectTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return TypeUtils.box(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return TypeUtils.box(tuple.getSecondElement());
        }
        if (elementIndex == 2) {
            return tuple.getThirdElement();
        }
        throw new IllegalArgumentException("Bad elementIndex for 3 element tuple: " + elementIndex);
    }

    @Override
    protected void convertChunks(@NotNull WritableChunk<? super Values> destination, int chunkSize, Chunk<Values> [] chunks) {
        WritableObjectChunk<IntFloatObjectTuple, ? super Values> destinationObjectChunk = destination.asWritableObjectChunk();
        IntChunk<Values> chunk1 = chunks[0].asIntChunk();
        FloatChunk<Values> chunk2 = chunks[1].asFloatChunk();
        ObjectChunk<Object, Values> chunk3 = chunks[2].asObjectChunk();
        for (int ii = 0; ii < chunkSize; ++ii) {
            destinationObjectChunk.set(ii, new IntFloatObjectTuple(chunk1.get(ii), chunk2.get(ii), chunk3.get(ii)));
        }
        destinationObjectChunk.setSize(chunkSize);
    }

    /** {@link ThreeColumnTupleSourceFactory} for instances of {@link IntegerFloatObjectColumnTupleSource}. **/
    private static final class Factory implements ThreeColumnTupleSourceFactory<IntFloatObjectTuple, Integer, Float, Object> {

        private Factory() {
        }

        @Override
        public TupleSource<IntFloatObjectTuple> create(
                @NotNull final ColumnSource<Integer> columnSource1,
                @NotNull final ColumnSource<Float> columnSource2,
                @NotNull final ColumnSource<Object> columnSource3
        ) {
            return new IntegerFloatObjectColumnTupleSource(
                    columnSource1,
                    columnSource2,
                    columnSource3
            );
        }
    }
}
