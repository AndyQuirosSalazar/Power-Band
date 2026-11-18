package app.sorting;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.util.IConstants;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BalancedMergeSortTest {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void sortsSeveralDiskRunsAndPreservesOffsets() throws IOException {
        Path indexFile = temporaryDirectory.resolve("recordings.index");
        String[] inputNames = { "delta", "Alpha", "golf", "bravo", "Foxtrot", "charlie", "Echo" };
        writeIndex(indexFile, inputNames);

        BalancedMergeSort.sort(indexFile);

        String[] expectedNames = { "Alpha", "Echo", "Foxtrot", "bravo", "charlie", "delta", "golf" };
        Map<String, Long> expectedOffsets = Map.of(
                "delta", 0L,
                "Alpha", 40L,
                "golf", 80L,
                "bravo", 120L,
                "Foxtrot", 160L,
                "charlie", 200L,
                "Echo", 240L);
        assertEquals(
                (long) inputNames.length * IConstants.RECORDING_INDEX_RECORD_BYTES,
                Files.size(indexFile));
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(indexFile)))) {
            for (String expectedName : expectedNames) {
                byte[] record = new byte[IConstants.RECORDING_INDEX_RECORD_BYTES];
                input.readFully(record);
                assertEquals(expectedName, decodeName(record));
                assertEquals(expectedOffsets.get(expectedName), readOffset(record));
            }
            assertEquals(-1, input.read());
        }
        assertEquals(Set.of(indexFile), listFiles(temporaryDirectory));
    }

    @Test
    void handlesEmptyAndSingleRecordIndexes() throws IOException {
        Path indexFile = temporaryDirectory.resolve("recordings.index");
        Files.createFile(indexFile);

        BalancedMergeSort.sort(indexFile);
        assertEquals(0L, Files.size(indexFile));

        byte[] singleRecord = createIndexRecord("Only", 96L);
        Files.write(indexFile, singleRecord);
        BalancedMergeSort.sort(indexFile);

        assertArrayEquals(singleRecord, Files.readAllBytes(indexFile));
        assertEquals(Set.of(indexFile), listFiles(temporaryDirectory));
    }

    @Test
    void rejectsMisalignedIndexWithoutChangingItOrLeavingTemporaryFiles() throws IOException {
        Path indexFile = temporaryDirectory.resolve("recordings.index");
        byte[] corruptContents = new byte[IConstants.RECORDING_INDEX_RECORD_BYTES + 1];
        Files.write(indexFile, corruptContents);

        assertThrows(IOException.class, () -> BalancedMergeSort.sort(indexFile));

        assertArrayEquals(corruptContents, Files.readAllBytes(indexFile));
        assertEquals(Set.of(indexFile), listFiles(temporaryDirectory));
    }

    private static void writeIndex(Path pIndexFile, String[] pNames) throws IOException {
        try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(pIndexFile)))) {
            for (int index = 0; index < pNames.length; index++) {
                output.write(createIndexRecord(pNames[index], (long) index * IConstants.RECORDING_INDEX_RECORD_BYTES));
            }
        }
    }

    private static byte[] createIndexRecord(String pName, long pOffset) {
        byte[] name = pName.getBytes(StandardCharsets.US_ASCII);
        if (name.length > IConstants.RECORDING_NAME_BYTES) {
            throw new IllegalArgumentException("test name is too long");
        }
        byte[] record = new byte[IConstants.RECORDING_INDEX_RECORD_BYTES];
        java.util.Arrays.fill(record, 0, IConstants.RECORDING_NAME_BYTES, (byte) ' ');
        System.arraycopy(name, 0, record, 0, name.length);
        ByteBuffer.wrap(record, IConstants.RECORDING_NAME_BYTES, Long.BYTES)
                .order(ByteOrder.BIG_ENDIAN)
                .putLong(pOffset);
        return record;
    }

    private static String decodeName(byte[] pRecord) {
        return new String(pRecord, 0, IConstants.RECORDING_NAME_BYTES, StandardCharsets.US_ASCII).stripTrailing();
    }

    private static long readOffset(byte[] pRecord) {
        return ByteBuffer.wrap(pRecord, IConstants.RECORDING_NAME_BYTES, Long.BYTES)
                .order(ByteOrder.BIG_ENDIAN)
                .getLong();
    }

    private static Set<Path> listFiles(Path pDirectory) throws IOException {
        try (var paths = Files.list(pDirectory)) {
            return paths.collect(Collectors.toSet());
        }
    }
}
