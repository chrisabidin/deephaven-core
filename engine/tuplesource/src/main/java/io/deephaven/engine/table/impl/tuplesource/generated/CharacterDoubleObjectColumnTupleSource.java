package io.deephaven.engine.table.impl.tuplesource.generated;

import io.deephaven.chunk.CharChunk;
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
import io.deephaven.engine.table.impl.tuplesource.ThreeColumnTupleSourceFactory;
import io.deephaven.tuple.generated.CharDoubleObjectTuple;
import io.deephaven.util.type.TypeUtils;
import org.jetbrains.annotations.NotNull;


/**
 * <p>{@link TupleSource} that produces key column values from {@link ColumnSource} types Character, Double, and Object.
 * <p>Generated by io.deephaven.replicators.TupleSourceCodeGenerator.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class CharacterDoubleObjectColumnTupleSource extends AbstractTupleSource<CharDoubleObjectTuple> {

    /** {@link ThreeColumnTupleSourceFactory} instance to create instances of {@link CharacterDoubleObjectColumnTupleSource}. **/
    public static final ThreeColumnTupleSourceFactory<CharDoubleObjectTuple, Character, Double, Object> FACTORY = new Factory();

    private final ColumnSource<Character> columnSource1;
    private final ColumnSource<Double> columnSource2;
    private final ColumnSource<Object> columnSource3;

    public CharacterDoubleObjectColumnTupleSource(
            @NotNull final ColumnSource<Character> columnSource1,
            @NotNull final ColumnSource<Double> columnSource2,
            @NotNull final ColumnSource<Object> columnSource3
    ) {
        super(columnSource1, columnSource2, columnSource3);
        this.columnSource1 = columnSource1;
        this.columnSource2 = columnSource2;
        this.columnSource3 = columnSource3;
    }

    @Override
    public final CharDoubleObjectTuple createTuple(final long rowKey) {
        return new CharDoubleObjectTuple(
                columnSource1.getChar(rowKey),
                columnSource2.getDouble(rowKey),
                columnSource3.get(rowKey)
        );
    }

    @Override
    public final CharDoubleObjectTuple createPreviousTuple(final long rowKey) {
        return new CharDoubleObjectTuple(
                columnSource1.getPrevChar(rowKey),
                columnSource2.getPrevDouble(rowKey),
                columnSource3.getPrev(rowKey)
        );
    }

    @Override
    public final CharDoubleObjectTuple createTupleFromValues(@NotNull final Object... values) {
        return new CharDoubleObjectTuple(
                TypeUtils.unbox((Character)values[0]),
                TypeUtils.unbox((Double)values[1]),
                values[2]
        );
    }

    @Override
    public final CharDoubleObjectTuple createTupleFromReinterpretedValues(@NotNull final Object... values) {
        return new CharDoubleObjectTuple(
                TypeUtils.unbox((Character)values[0]),
                TypeUtils.unbox((Double)values[1]),
                values[2]
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <ELEMENT_TYPE> void exportElement(@NotNull final CharDoubleObjectTuple tuple, final int elementIndex, @NotNull final WritableColumnSource<ELEMENT_TYPE> writableSource, final long destinationRowKey) {
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
    public final Object exportToExternalKey(@NotNull final CharDoubleObjectTuple tuple) {
        return new SmartKey(
                TypeUtils.box(tuple.getFirstElement()),
                TypeUtils.box(tuple.getSecondElement()),
                tuple.getThirdElement()
        );
    }

    @Override
    public final Object exportElement(@NotNull final CharDoubleObjectTuple tuple, int elementIndex) {
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
    public final Object exportElementReinterpreted(@NotNull final CharDoubleObjectTuple tuple, int elementIndex) {
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
        WritableObjectChunk<CharDoubleObjectTuple, ? super Values> destinationObjectChunk = destination.asWritableObjectChunk();
        CharChunk<Values> chunk1 = chunks[0].asCharChunk();
        DoubleChunk<Values> chunk2 = chunks[1].asDoubleChunk();
        ObjectChunk<Object, Values> chunk3 = chunks[2].asObjectChunk();
        for (int ii = 0; ii < chunkSize; ++ii) {
            destinationObjectChunk.set(ii, new CharDoubleObjectTuple(chunk1.get(ii), chunk2.get(ii), chunk3.get(ii)));
        }
        destinationObjectChunk.setSize(chunkSize);
    }

    /** {@link ThreeColumnTupleSourceFactory} for instances of {@link CharacterDoubleObjectColumnTupleSource}. **/
    private static final class Factory implements ThreeColumnTupleSourceFactory<CharDoubleObjectTuple, Character, Double, Object> {

        private Factory() {
        }

        @Override
        public TupleSource<CharDoubleObjectTuple> create(
                @NotNull final ColumnSource<Character> columnSource1,
                @NotNull final ColumnSource<Double> columnSource2,
                @NotNull final ColumnSource<Object> columnSource3
        ) {
            return new CharacterDoubleObjectColumnTupleSource(
                    columnSource1,
                    columnSource2,
                    columnSource3
            );
        }
    }
}
