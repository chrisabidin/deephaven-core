/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */
package io.deephaven.server.barrage;

import com.google.common.io.LittleEndianDataOutputStream;
import com.google.flatbuffers.FlatBufferBuilder;
import com.google.protobuf.ByteStringAccess;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.WireFormat;
import gnu.trove.list.array.TIntArrayList;
import io.deephaven.UncheckedDeephavenException;
import io.deephaven.barrage.flatbuf.*;
import io.deephaven.chunk.WritableLongChunk;
import io.deephaven.chunk.WritableObjectChunk;
import io.deephaven.chunk.attributes.Values;
import io.deephaven.engine.rowset.RowSet;
import io.deephaven.engine.rowset.RowSetBuilderSequential;
import io.deephaven.engine.rowset.RowSetFactory;
import io.deephaven.engine.rowset.RowSetShiftData;
import io.deephaven.engine.rowset.impl.ExternalizableRowSetUtils;
import io.deephaven.engine.table.TableDefinition;
import io.deephaven.engine.table.impl.util.BarrageMessage;
import io.deephaven.extensions.barrage.BarrageSubscriptionOptions;
import io.deephaven.extensions.barrage.BarrageSnapshotOptions;
import io.deephaven.extensions.barrage.chunk.ChunkInputStreamGenerator;
import io.deephaven.extensions.barrage.util.BarrageProtoUtil.ExposedByteArrayOutputStream;
import io.deephaven.extensions.barrage.util.BarrageUtil;
import io.deephaven.extensions.barrage.util.DefensiveDrainable;
import io.deephaven.extensions.barrage.util.StreamReaderOptions;
import io.deephaven.internal.log.LoggerFactory;
import io.deephaven.io.logger.Logger;
import io.deephaven.proto.flight.util.MessageHelper;
import io.deephaven.util.SafeCloseable;
import io.grpc.Drainable;
import org.apache.arrow.flatbuf.Buffer;
import org.apache.arrow.flatbuf.FieldNode;
import org.apache.arrow.flatbuf.RecordBatch;
import org.apache.arrow.flight.impl.Flight;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableLong;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static io.deephaven.extensions.barrage.chunk.BaseChunkInputStreamGenerator.PADDING_BUFFER;

public class BarrageStreamGenerator implements
        BarrageMessageProducer.StreamGenerator<BarrageStreamGenerator.View> {
    private static final Logger log = LoggerFactory.getLogger(BarrageStreamGenerator.class);

    public interface View {

        void forEachStream(Consumer<InputStream> visitor) throws IOException;

        boolean isViewport();

        StreamReaderOptions options();

        RowSet keyspaceViewport();
    }

    @Singleton
    public static class Factory
            implements BarrageMessageProducer.StreamGenerator.Factory<View> {
        @Inject
        public Factory() {}

        @Override
        public BarrageMessageProducer.StreamGenerator<View> newGenerator(
                final BarrageMessage message) {
            return new BarrageStreamGenerator(message);
        }

        @Override
        public View getSchemaView(final TableDefinition table,
                final Map<String, Object> attributes) {
            final FlatBufferBuilder builder = new FlatBufferBuilder();
            final int schemaOffset = BarrageUtil.makeSchemaPayload(builder, table, attributes);
            builder.finish(MessageHelper.wrapInMessage(builder, schemaOffset,
                    org.apache.arrow.flatbuf.MessageHeader.Schema));
            return new SchemaView(builder.dataBuffer());
        }
    }

    public static class ModColumnData {
        public final RowSetGenerator rowsModified;
        public final ChunkInputStreamGenerator data;

        ModColumnData(final BarrageMessage.ModColumnData col) throws IOException {
            rowsModified = new RowSetGenerator(col.rowsModified);
            data = ChunkInputStreamGenerator.makeInputStreamGenerator(col.data.getChunkType(), col.type, col.data);
        }
    }

    public final BarrageMessage message;

    public final long firstSeq;
    public final long lastSeq;
    public final long step;

    public final boolean isSnapshot;

    public final RowSetGenerator rowsAdded;
    public final RowSetGenerator rowsIncluded;
    public final RowSetGenerator rowsRemoved;
    public final RowSetShiftDataGenerator shifted;

    public final ChunkInputStreamGenerator[] addColumnData;
    public final ModColumnData[] modColumnData;

    /**
     * Create a barrage stream generator that can slice and dice the barrage message for delivery to clients.
     *
     * @param message the generator takes ownership of the message and its internal objects
     */
    public BarrageStreamGenerator(final BarrageMessage message) {
        this.message = message;
        try {
            firstSeq = message.firstSeq;
            lastSeq = message.lastSeq;
            step = message.step;
            isSnapshot = message.isSnapshot;

            rowsAdded = new RowSetGenerator(message.rowsAdded);
            rowsIncluded = new RowSetGenerator(message.rowsIncluded);
            rowsRemoved = new RowSetGenerator(message.rowsRemoved);
            shifted = new RowSetShiftDataGenerator(message.shifted);

            addColumnData = new ChunkInputStreamGenerator[message.addColumnData.length];
            for (int i = 0; i < message.addColumnData.length; ++i) {
                final BarrageMessage.AddColumnData acd = message.addColumnData[i];
                addColumnData[i] =
                        ChunkInputStreamGenerator.makeInputStreamGenerator(acd.data.getChunkType(), acd.type, acd.data);
            }
            modColumnData = new ModColumnData[message.modColumnData.length];
            for (int i = 0; i < modColumnData.length; ++i) {
                modColumnData[i] = new ModColumnData(message.modColumnData[i]);
            }

            if (message.snapshotRowSet != null) {
                message.snapshotRowSet.close();
            }
        } catch (final IOException e) {
            throw new UncheckedDeephavenException("unexpected IOException while creating barrage message stream", e);
        } finally {
            if (message.snapshotRowSet != null) {
                message.snapshotRowSet.close();
            }
        }
    }

    @Override
    public BarrageMessage getMessage() {
        return message;
    }

    @Override
    public void close() {
        rowsAdded.close();
        rowsIncluded.close();
        rowsRemoved.close();

        if (addColumnData != null) {
            for (final ChunkInputStreamGenerator in : addColumnData) {
                in.close();
            }
        }
        if (modColumnData != null) {
            for (final ModColumnData mcd : modColumnData) {
                mcd.rowsModified.close();
                mcd.data.close();
            }
        }
    }

    /**
     * Obtain a View of this StreamGenerator that can be sent to a single subscriber.
     *
     * @param options serialization options for this specific view
     * @param isInitialSnapshot indicates whether or not this is the first snapshot for the listener
     * @param viewport is the position-space viewport
     * @param keyspaceViewport is the key-space viewport
     * @param subscribedColumns are the columns subscribed for this view
     * @return a MessageView filtered by the subscription properties that can be sent to that subscriber
     */
    @Override
    public SubView getSubView(final BarrageSubscriptionOptions options,
            final boolean isInitialSnapshot,
            @Nullable final RowSet viewport,
            @Nullable final RowSet keyspaceViewport,
            @Nullable final BitSet subscribedColumns) {
        return new SubView(this, options, isInitialSnapshot, viewport, keyspaceViewport, subscribedColumns);
    }

    /**
     * Obtain a Full-Subscription View of this StreamGenerator that can be sent to a single subscriber.
     *
     * @param options serialization options for this specific view
     * @param isInitialSnapshot indicates whether or not this is the first snapshot for the listener
     * @return a MessageView filtered by the subscription properties that can be sent to that subscriber
     */
    @Override
    public SubView getSubView(BarrageSubscriptionOptions options, boolean isInitialSnapshot) {
        return getSubView(options, isInitialSnapshot, null, null, null);
    }

    public static class SubView implements View {
        public final BarrageStreamGenerator generator;
        public final BarrageSubscriptionOptions options;
        public final boolean isInitialSnapshot;
        public final RowSet viewport;
        public final RowSet keyspaceViewport;
        public final BitSet subscribedColumns;
        public final boolean hasAddBatch;
        public final boolean hasModBatch;

        public SubView(final BarrageStreamGenerator generator,
                final BarrageSubscriptionOptions options,
                final boolean isInitialSnapshot,
                @Nullable final RowSet viewport,
                @Nullable final RowSet keyspaceViewport,
                @Nullable final BitSet subscribedColumns) {
            this.generator = generator;
            this.options = options;
            this.isInitialSnapshot = isInitialSnapshot;
            this.viewport = viewport;
            this.keyspaceViewport = keyspaceViewport;
            this.subscribedColumns = subscribedColumns;
            this.hasModBatch = generator.doesSubViewHaveMods(this);
            // require an add batch if the mod batch is being skipped
            this.hasAddBatch = !this.hasModBatch || generator.rowsIncluded.original.isNonempty();
        }

        @Override
        public void forEachStream(Consumer<InputStream> visitor) throws IOException {
            ByteBuffer metadata = generator.getSubscriptionMetadata(this);
            if (hasAddBatch) {
                visitor.accept(generator.getInputStream(this, metadata, generator::appendAddColumns));
                metadata = null;
            }
            if (hasModBatch) {
                visitor.accept(generator.getInputStream(this, metadata, generator::appendModColumns));
            }
        }

        public boolean isViewport() {
            return this.viewport != null;
        }

        public final StreamReaderOptions options() {
            return options;
        }

        public final RowSet keyspaceViewport() {
            return this.keyspaceViewport;
        };

    }

    /**
     * Obtain a View of this StreamGenerator that can be sent to a single snapshot requestor.
     *
     * @param options serialization options for this specific view
     * @param viewport is the position-space viewport
     * @param keyspaceViewport is the key-space viewport
     * @param snapshotColumns are the columns subscribed for this view
     * @return a MessageView filtered by the snapshot properties that can be sent to that subscriber
     */
    @Override
    public SnapshotView getSnapshotView(final BarrageSnapshotOptions options,
            @Nullable final RowSet viewport,
            @Nullable final RowSet keyspaceViewport,
            @Nullable final BitSet snapshotColumns) {
        return new SnapshotView(this, options, viewport, keyspaceViewport, snapshotColumns);
    }

    /**
     * Obtain a Full-Snapshot View of this StreamGenerator that can be sent to a single snapshot requestor.
     *
     * @param options serialization options for this specific view
     * @return a MessageView filtered by the snapshot properties that can be sent to that subscriber
     */
    @Override
    public SnapshotView getSnapshotView(BarrageSnapshotOptions options) {
        return getSnapshotView(options, null, null, null);
    }

    public static class SnapshotView implements View {
        public final BarrageStreamGenerator generator;
        public final BarrageSnapshotOptions options;
        public final RowSet viewport;
        public final RowSet keyspaceViewport;
        public final BitSet subscribedColumns;
        public final boolean hasAddBatch;
        public final boolean hasModBatch;

        public SnapshotView(final BarrageStreamGenerator generator,
                final BarrageSnapshotOptions options,
                @Nullable final RowSet viewport,
                @Nullable final RowSet keyspaceViewport,
                @Nullable final BitSet subscribedColumns) {
            this.generator = generator;
            this.options = options;
            this.viewport = viewport;
            this.keyspaceViewport = keyspaceViewport;
            this.subscribedColumns = subscribedColumns;
            this.hasModBatch = false;
            this.hasAddBatch = generator.rowsIncluded.original.isNonempty();
        }

        @Override
        public void forEachStream(Consumer<InputStream> visitor) throws IOException {
            if (hasAddBatch) {
                visitor.accept(generator.getInputStream(this, null, generator::appendAddColumns));
            }
        }

        public boolean isViewport() {
            return this.viewport != null;
        }

        public final StreamReaderOptions options() {
            return options;
        }

        public final RowSet keyspaceViewport() {
            return this.keyspaceViewport;
        };
    }

    public static class SchemaView implements View {
        final byte[] msgBytes;

        public SchemaView(final ByteBuffer buffer) {
            this.msgBytes = Flight.FlightData.newBuilder()
                    .setDataHeader(ByteStringAccess.wrap(buffer))
                    .build()
                    .toByteArray();
        }

        @Override
        public void forEachStream(Consumer<InputStream> visitor) {
            visitor.accept(new DrainableByteArrayInputStream(msgBytes, 0, msgBytes.length));
        }

        @Override
        public boolean isViewport() {
            return false;
        }

        @Override
        public StreamReaderOptions options() {
            return null;
        }

        @Override
        public RowSet keyspaceViewport() {
            return null;
        }
    }

    @FunctionalInterface
    private interface ColumnVisitor {
        long visit(final View view,
                final Consumer<InputStream> addStream,
                final ChunkInputStreamGenerator.FieldNodeListener fieldNodeListener,
                final ChunkInputStreamGenerator.BufferListener bufferListener) throws IOException;
    }

    /**
     * Returns an InputStream of the message filtered to the viewport.
     *
     * @param view the view of the overall chunk to generate a RecordBatch for
     * @param metadata the optional flight data metadata to attach to the message
     * @param columnVisitor the helper method responsible for appending the payload columns to the RecordBatch
     * @return an InputStream ready to be drained by GRPC
     */
    private InputStream getInputStream(final View view, final ByteBuffer metadata, final ColumnVisitor columnVisitor)
            throws IOException {
        final ArrayDeque<InputStream> streams = new ArrayDeque<>();
        final MutableInt size = new MutableInt();

        final Consumer<InputStream> addStream = (final InputStream is) -> {
            try {
                final int sz = is.available();
                if (sz == 0) {
                    is.close();
                    return;
                }

                streams.add(is);
                size.add(sz);
            } catch (final IOException e) {
                throw new UncheckedDeephavenException("Unexpected IOException", e);
            }

            // These buffers must be aligned to an 8-byte boundary in order for efficient alignment in languages like
            // C++.
            if (size.intValue() % 8 != 0) {
                final int paddingBytes = (8 - (size.intValue() % 8));
                size.add(paddingBytes);
                streams.add(new DrainableByteArrayInputStream(PADDING_BUFFER, 0, paddingBytes));
            }
        };

        final FlatBufferBuilder header = new FlatBufferBuilder();

        final long numRows;
        final int nodesOffset;
        final int buffersOffset;
        try (final WritableObjectChunk<ChunkInputStreamGenerator.FieldNodeInfo, Values> nodeOffsets =
                WritableObjectChunk.makeWritableChunk(addColumnData.length);
                final WritableLongChunk<Values> bufferInfos =
                        WritableLongChunk.makeWritableChunk(addColumnData.length * 3)) {
            nodeOffsets.setSize(0);
            bufferInfos.setSize(0);

            final MutableLong totalBufferLength = new MutableLong();
            final ChunkInputStreamGenerator.FieldNodeListener fieldNodeListener =
                    (numElements, nullCount) -> nodeOffsets
                            .add(new ChunkInputStreamGenerator.FieldNodeInfo(numElements, nullCount));

            final ChunkInputStreamGenerator.BufferListener bufferListener = (length) -> {
                totalBufferLength.add(length);
                bufferInfos.add(length);
            };
            numRows = columnVisitor.visit(view, addStream, fieldNodeListener, bufferListener);

            RecordBatch.startNodesVector(header, nodeOffsets.size());
            for (int i = nodeOffsets.size() - 1; i >= 0; --i) {
                final ChunkInputStreamGenerator.FieldNodeInfo node = nodeOffsets.get(i);
                FieldNode.createFieldNode(header, node.numElements, node.nullCount);
            }
            nodesOffset = header.endVector();

            RecordBatch.startBuffersVector(header, bufferInfos.size());
            for (int i = bufferInfos.size() - 1; i >= 0; --i) {
                totalBufferLength.subtract(bufferInfos.get(i));
                Buffer.createBuffer(header, totalBufferLength.longValue(), bufferInfos.get(i));
            }
            buffersOffset = header.endVector();
        }

        RecordBatch.startRecordBatch(header);
        RecordBatch.addNodes(header, nodesOffset);
        RecordBatch.addBuffers(header, buffersOffset);
        RecordBatch.addLength(header, numRows);
        final int headerOffset = RecordBatch.endRecordBatch(header);

        header.finish(MessageHelper.wrapInMessage(header, headerOffset,
                org.apache.arrow.flatbuf.MessageHeader.RecordBatch, size.intValue()));

        // now create the proto header
        try (final ExposedByteArrayOutputStream baos = new ExposedByteArrayOutputStream()) {
            final CodedOutputStream cos = CodedOutputStream.newInstance(baos);

            cos.writeByteBuffer(Flight.FlightData.DATA_HEADER_FIELD_NUMBER, header.dataBuffer().slice());
            if (metadata != null) {
                cos.writeByteBuffer(Flight.FlightData.APP_METADATA_FIELD_NUMBER, metadata);
            }

            cos.writeTag(Flight.FlightData.DATA_BODY_FIELD_NUMBER, WireFormat.WIRETYPE_LENGTH_DELIMITED);
            cos.writeUInt32NoTag(size.intValue());
            cos.flush();

            streams.addFirst(new DrainableByteArrayInputStream(baos.peekBuffer(), 0, baos.size()));

            return new ConsecutiveDrainableStreams(streams.toArray(new InputStream[0]));
        } catch (final IOException ex) {
            throw new UncheckedDeephavenException("Unexpected IOException", ex);
        }
    }

    private static int createByteVector(final FlatBufferBuilder builder, final byte[] data, final int offset,
            final int length) {
        builder.startVector(1, length, 1);

        if (length > 0) {
            builder.prep(1, length - 1);

            for (int i = length - 1; i >= 0; --i) {
                builder.putByte(data[offset + i]);
            }
        }

        return builder.endVector();
    }

    private long appendAddColumns(final View view,
            final Consumer<InputStream> addStream,
            final ChunkInputStreamGenerator.FieldNodeListener fieldNodeListener,
            final ChunkInputStreamGenerator.BufferListener bufferListener) throws IOException {
        // Added Chunk Data:
        final RowSet myAddedOffsets;
        if (view.isViewport()) {
            // only include added rows that are within the viewport
            myAddedOffsets = rowsIncluded.original.invert(view.keyspaceViewport().intersect(rowsIncluded.original));
        } else if (!rowsAdded.original.equals(rowsIncluded.original)) {
            // there are scoped rows included in the chunks that need to be removed
            myAddedOffsets = rowsIncluded.original.invert(rowsAdded.original);
        } else {
            // use chunk data as-is
            myAddedOffsets = null;
        }

        // add the add-column streams
        for (final ChunkInputStreamGenerator col : addColumnData) {
            final ChunkInputStreamGenerator.DrainableColumn drainableColumn =
                    col.getInputStream(view.options(), myAddedOffsets);
            drainableColumn.visitFieldNodes(fieldNodeListener);
            drainableColumn.visitBuffers(bufferListener);

            // Add the drainable last as it is allowed to immediately close a row set the visitors need
            addStream.accept(drainableColumn);
        }
        return rowsAdded.original.size();
    }

    private long appendModColumns(final View view,
            final Consumer<InputStream> addStream,
            final ChunkInputStreamGenerator.FieldNodeListener fieldNodeListener,
            final ChunkInputStreamGenerator.BufferListener bufferListener) throws IOException {
        // now add mod-column streams, and write the mod column indexes
        long numRows = 0;
        for (final ModColumnData mcd : modColumnData) {
            RowSet myModOffsets = null;
            if (view.isViewport()) {
                // only include added rows that are within the viewport
                myModOffsets =
                        mcd.rowsModified.original.invert(view.keyspaceViewport().intersect(mcd.rowsModified.original));
                numRows = Math.max(numRows, myModOffsets.size());
            } else {
                numRows = Math.max(numRows, mcd.rowsModified.original.size());
            }

            final ChunkInputStreamGenerator.DrainableColumn drainableColumn =
                    mcd.data.getInputStream(view.options(), myModOffsets);

            drainableColumn.visitFieldNodes(fieldNodeListener);
            drainableColumn.visitBuffers(bufferListener);

            // See comment in appendAddColumns
            addStream.accept(drainableColumn);
        }
        return numRows;
    }

    private boolean doesSubViewHaveMods(final SubView view) {
        for (final ModColumnData mcd : modColumnData) {
            RowSet myModOffsets = null;
            if (view.isViewport()) {
                // only include added rows that are within the viewport
                if (view.keyspaceViewport.overlaps(mcd.rowsModified.original)) {
                    return true;
                }
            } else {
                if (mcd.rowsModified.original.isNonempty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private ByteBuffer getSubscriptionMetadata(final SubView view) throws IOException {
        final FlatBufferBuilder metadata = new FlatBufferBuilder();

        int effectiveViewportOffset = 0;
        if (isSnapshot && view.isViewport()) {
            try (final RowSetGenerator viewportGen = new RowSetGenerator(view.viewport)) {
                effectiveViewportOffset = viewportGen.addToFlatBuffer(metadata);
            }
        }

        int effectiveColumnSetOffset = 0;
        if (isSnapshot && view.subscribedColumns != null) {
            effectiveColumnSetOffset = new BitSetGenerator(view.subscribedColumns).addToFlatBuffer(metadata);
        }

        final int rowsAddedOffset;
        if (isSnapshot && !view.isInitialSnapshot) {
            // client's don't need/want to receive the full RowSet on every snapshot
            rowsAddedOffset = EmptyRowSetGenerator.INSTANCE.addToFlatBuffer(metadata);
        } else {
            rowsAddedOffset = rowsAdded.addToFlatBuffer(metadata);
        }

        final int rowsRemovedOffset = rowsRemoved.addToFlatBuffer(metadata);
        final int shiftDataOffset = shifted.addToFlatBuffer(metadata);

        // Added Chunk Data:
        int addedRowsIncludedOffset = 0;
        if (view.isViewport()) {
            addedRowsIncludedOffset = rowsIncluded.addToFlatBuffer(view.keyspaceViewport, metadata);
        }

        // now add mod-column streams, and write the mod column indexes
        TIntArrayList modOffsets = new TIntArrayList(modColumnData.length);
        for (final ModColumnData mcd : modColumnData) {
            final int myModRowOffset;
            if (view.isViewport()) {
                myModRowOffset = mcd.rowsModified.addToFlatBuffer(view.keyspaceViewport, metadata);
            } else {
                myModRowOffset = mcd.rowsModified.addToFlatBuffer(metadata);
            }
            modOffsets.add(BarrageModColumnMetadata.createBarrageModColumnMetadata(metadata, myModRowOffset));
        }

        BarrageUpdateMetadata.startModColumnNodesVector(metadata, modOffsets.size());
        modOffsets.forEachDescending(offset -> {
            metadata.addOffset(offset);
            return true;
        });
        final int nodesOffset = metadata.endVector();

        BarrageUpdateMetadata.startBarrageUpdateMetadata(metadata);
        BarrageUpdateMetadata.addNumAddBatches(metadata, view.hasAddBatch ? 1 : 0);
        BarrageUpdateMetadata.addNumModBatches(metadata, view.hasModBatch ? 1 : 0);
        BarrageUpdateMetadata.addIsSnapshot(metadata, isSnapshot);
        BarrageUpdateMetadata.addFirstSeq(metadata, firstSeq);
        BarrageUpdateMetadata.addLastSeq(metadata, lastSeq);
        BarrageUpdateMetadata.addEffectiveViewport(metadata, effectiveViewportOffset);
        BarrageUpdateMetadata.addEffectiveColumnSet(metadata, effectiveColumnSetOffset);
        BarrageUpdateMetadata.addAddedRows(metadata, rowsAddedOffset);
        BarrageUpdateMetadata.addRemovedRows(metadata, rowsRemovedOffset);
        BarrageUpdateMetadata.addShiftData(metadata, shiftDataOffset);
        BarrageUpdateMetadata.addAddedRowsIncluded(metadata, addedRowsIncludedOffset);
        BarrageUpdateMetadata.addModColumnNodes(metadata, nodesOffset);
        metadata.finish(BarrageUpdateMetadata.endBarrageUpdateMetadata(metadata));

        final FlatBufferBuilder header = new FlatBufferBuilder();
        final int payloadOffset = BarrageMessageWrapper.createMsgPayloadVector(header, metadata.dataBuffer());
        BarrageMessageWrapper.startBarrageMessageWrapper(header);
        BarrageMessageWrapper.addMagic(header, BarrageUtil.FLATBUFFER_MAGIC);
        BarrageMessageWrapper.addMsgType(header, BarrageMessageType.BarrageUpdateMetadata);
        BarrageMessageWrapper.addMsgPayload(header, payloadOffset);
        header.finish(BarrageMessageWrapper.endBarrageMessageWrapper(header));

        return header.dataBuffer().slice();
    }

    public static abstract class ByteArrayGenerator {
        protected int len;
        protected byte[] raw;

        protected int addToFlatBuffer(final FlatBufferBuilder builder) {
            return createByteVector(builder, raw, 0, len);
        }
    }

    public static class RowSetGenerator extends ByteArrayGenerator implements SafeCloseable {
        public final RowSet original;

        public RowSetGenerator(final RowSet rowSet) throws IOException {
            this.original = rowSet.copy();
            // noinspection UnstableApiUsage
            try (final ExposedByteArrayOutputStream baos = new ExposedByteArrayOutputStream();
                    final LittleEndianDataOutputStream oos = new LittleEndianDataOutputStream(baos)) {
                ExternalizableRowSetUtils.writeExternalCompressedDeltas(oos, rowSet);
                oos.flush();
                raw = baos.peekBuffer();
                len = baos.size();
            }
        }

        @Override
        public void close() {
            original.close();
        }

        public DrainableByteArrayInputStream getInputStream() {
            return new DrainableByteArrayInputStream(raw, 0, len);
        }

        /**
         * Appends the intersection of the viewport and the originally provided RowSet.
         *
         * @param viewport the key-space version of the viewport
         * @param builder the flatbuffer builder
         * @return offset of the item in the flatbuffer
         */
        protected int addToFlatBuffer(final RowSet viewport, final FlatBufferBuilder builder) throws IOException {
            if (original.subsetOf(viewport)) {
                return addToFlatBuffer(builder);
            }

            final int nlen;
            final byte[] nraw;
            // noinspection UnstableApiUsage
            try (final ExposedByteArrayOutputStream baos = new ExposedByteArrayOutputStream();
                    final LittleEndianDataOutputStream oos = new LittleEndianDataOutputStream(baos);
                    final RowSet viewOfOriginal = original.intersect(viewport)) {
                ExternalizableRowSetUtils.writeExternalCompressedDeltas(oos, viewOfOriginal);
                oos.flush();
                nraw = baos.peekBuffer();
                nlen = baos.size();
            }

            return createByteVector(builder, nraw, 0, nlen);
        }
    }

    public static class BitSetGenerator extends ByteArrayGenerator {
        public final BitSet original;

        public BitSetGenerator(final BitSet bitset) throws IOException {
            this.original = bitset == null ? new BitSet() : bitset;
            this.raw = original.toByteArray();
            final int nBits = original.previousSetBit(Integer.MAX_VALUE - 1) + 1;
            this.len = (int) ((long) nBits + 7) / 8;
        }

        public int addToFlatBuffer(final BitSet mine, final FlatBufferBuilder builder) throws IOException {
            if (mine.equals(original)) {
                return addToFlatBuffer(builder);
            }

            final byte[] nraw = mine.toByteArray();
            final int nBits = mine.previousSetBit(Integer.MAX_VALUE - 1) + 1;
            final int nlen = (int) ((long) nBits + 7) / 8;
            return createByteVector(builder, nraw, 0, nlen);
        }
    }

    public static class RowSetShiftDataGenerator extends ByteArrayGenerator {
        public final RowSetShiftData original;

        public RowSetShiftDataGenerator(final RowSetShiftData shifted) throws IOException {
            this.original = shifted;

            final RowSetBuilderSequential sRangeBuilder = RowSetFactory.builderSequential();
            final RowSetBuilderSequential eRangeBuilder = RowSetFactory.builderSequential();
            final RowSetBuilderSequential destBuilder = RowSetFactory.builderSequential();

            if (shifted != null) {
                for (int i = 0; i < shifted.size(); ++i) {
                    long s = shifted.getBeginRange(i);
                    final long dt = shifted.getShiftDelta(i);

                    if (dt < 0 && s < -dt) {
                        s = -dt;
                    }

                    sRangeBuilder.appendKey(s);
                    eRangeBuilder.appendKey(shifted.getEndRange(i));
                    destBuilder.appendKey(s + dt);
                }
            }

            // noinspection UnstableApiUsage
            try (final RowSet sRange = sRangeBuilder.build();
                    final RowSet eRange = eRangeBuilder.build();
                    final RowSet dest = destBuilder.build();
                    final ExposedByteArrayOutputStream baos = new ExposedByteArrayOutputStream();
                    final LittleEndianDataOutputStream oos = new LittleEndianDataOutputStream(baos)) {
                ExternalizableRowSetUtils.writeExternalCompressedDeltas(oos, sRange);
                ExternalizableRowSetUtils.writeExternalCompressedDeltas(oos, eRange);
                ExternalizableRowSetUtils.writeExternalCompressedDeltas(oos, dest);
                oos.flush();
                raw = baos.peekBuffer();
                len = baos.size();
            }
        }
    }

    public static class DrainableByteArrayInputStream extends DefensiveDrainable {

        private byte[] buf;
        private final int offset;
        private final int length;

        public DrainableByteArrayInputStream(final byte[] buf, final int offset, final int length) {
            this.buf = Objects.requireNonNull(buf);
            this.offset = offset;
            this.length = length;
        }

        @Override
        public int available() {
            if (buf == null) {
                return 0;
            }
            return length;
        }

        @Override
        public int drainTo(final OutputStream outputStream) throws IOException {
            if (buf != null) {
                try {
                    outputStream.write(buf, offset, length);
                } finally {
                    buf = null;
                }
                return length;
            }
            return 0;
        }
    }

    public static class ConsecutiveDrainableStreams extends DefensiveDrainable {
        final InputStream[] streams;

        ConsecutiveDrainableStreams(final InputStream... streams) {
            this.streams = streams;
            for (final InputStream stream : streams) {
                if (!(stream instanceof Drainable)) {
                    throw new IllegalArgumentException("expecting sub-class of Drainable; found: " + stream.getClass());
                }
            }
        }

        @Override
        public int drainTo(final OutputStream outputStream) throws IOException {
            int total = 0;
            for (final InputStream stream : streams) {
                final int expected = total + stream.available();
                total += ((Drainable) stream).drainTo(outputStream);
                if (expected != total) {
                    throw new IllegalStateException("drained message drained wrong number of bytes");
                }
                if (total < 0) {
                    throw new IllegalStateException("drained message is too large; exceeds Integer.MAX_VALUE");
                }
            }
            return total;
        }

        @Override
        public int available() throws IOException {
            int total = 0;
            for (final InputStream stream : streams) {
                total += stream.available();
                if (total < 0) {
                    throw new IllegalStateException("drained message is too large; exceeds Integer.MAX_VALUE");
                }
            }
            return total;
        }

        @Override
        public void close() throws IOException {
            for (final InputStream stream : streams) {
                try {
                    stream.close();
                } catch (final IOException e) {
                    throw new UncheckedDeephavenException("unexpected IOException", e);
                }
            }
            super.close();
        }
    }

    private static final class EmptyRowSetGenerator extends RowSetGenerator {
        public static final EmptyRowSetGenerator INSTANCE;
        static {
            try {
                INSTANCE = new EmptyRowSetGenerator();
            } catch (final IOException ioe) {
                throw new UncheckedDeephavenException(ioe);
            }
        }

        EmptyRowSetGenerator() throws IOException {
            super(RowSetFactory.empty());
        }

        @Override
        public void close() {
            // no-op; this is very re-usable
        }
    }
}
