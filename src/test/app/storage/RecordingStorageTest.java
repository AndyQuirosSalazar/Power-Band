package app.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bridge.commands.BassCommand;
import app.bridge.commands.PianoCommand;
import app.exceptions.RecordingStorageException;
import app.model.CommandRecord;
import app.util.IConstants;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RecordingStorageTest {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void initializesEmptyLibraryAndPersistsBinaryAndChronologicalRecords() throws IOException {
        RecordingStorage storage = new RecordingStorage(temporaryDirectory);
        assertEquals(InitializationResult.READY, storage.initialize(null));
        List<CommandRecord> commands = List.of(
                new BassCommand(43, 250).toCommandRecord(0L),
                new PianoCommand(64, 500, true).toCommandRecord(120L));
        Instant createdAt = Instant.parse("2026-11-17T20:00:00Z");

        storage.addRecording("Jam", createdAt, commands);

        Path musicFile = musicFile(temporaryDirectory);
        byte[] music = Files.readAllBytes(musicFile);
        assertEquals(
                IConstants.RECORDING_HEADER_BYTES + 2L * IConstants.COMMAND_RECORD_BYTES,
                music.length);
        byte[] header = Arrays.copyOfRange(music, 0, IConstants.RECORDING_HEADER_BYTES);
        assertArrayEquals(encodeName("Jam"), Arrays.copyOfRange(header, 0, IConstants.RECORDING_NAME_BYTES));
        ByteBuffer offsets = ByteBuffer.wrap(header, IConstants.RECORDING_NAME_BYTES, 2 * Long.BYTES)
                .order(ByteOrder.BIG_ENDIAN);
        assertEquals(IConstants.RECORDING_HEADER_BYTES, offsets.getLong());
        assertEquals(
                IConstants.RECORDING_HEADER_BYTES + 2L * IConstants.COMMAND_RECORD_BYTES,
                offsets.getLong());
        assertEquals(43, CommandRecord.fromByteArray(Arrays.copyOfRange(
                music,
                IConstants.RECORDING_HEADER_BYTES,
                IConstants.RECORDING_HEADER_BYTES + IConstants.COMMAND_RECORD_BYTES)).getNote());
        assertEquals(64, CommandRecord.fromByteArray(Arrays.copyOfRange(
                music,
                IConstants.RECORDING_HEADER_BYTES + IConstants.COMMAND_RECORD_BYTES,
                music.length)).getNote());

        Path indexFile = indexFile(temporaryDirectory);
        assertEquals(IConstants.RECORDING_INDEX_RECORD_BYTES, Files.size(indexFile));
        byte[] indexRecord = Files.readAllBytes(indexFile);
        assertArrayEquals(encodeName("Jam"), Arrays.copyOfRange(indexRecord, 0, IConstants.RECORDING_NAME_BYTES));
        assertEquals(
                0L,
                ByteBuffer.wrap(indexRecord, IConstants.RECORDING_NAME_BYTES, Long.BYTES)
                        .order(ByteOrder.BIG_ENDIAN)
                        .getLong());
        assertEquals("Jam\t" + createdAt + "\n", Files.readString(listFile(temporaryDirectory)));
        assertTrue(storage.recordingExists("Jam"));
    }

    @Test
    void searchesSortedIndexAndRejectsDuplicateWithoutChangingAnyFile() throws IOException {
        RecordingStorage storage = new RecordingStorage(temporaryDirectory);
        storage.initialize(null);
        Instant createdAt = Instant.parse("2026-11-17T20:00:00Z");
        storage.addRecording("zeta", createdAt, List.of());
        storage.addRecording("Piano", createdAt.plusSeconds(1), List.of());
        storage.addRecording("Alpha", createdAt.plusSeconds(2), List.of());
        storage.addRecording("piano", createdAt.plusSeconds(3), List.of());

        assertTrue(storage.recordingExists("Piano"));
        assertTrue(storage.recordingExists("piano"));
        assertFalse(storage.recordingExists("PIANO"));
        assertEquals(List.of("Alpha", "Piano", "piano", "zeta"), readIndexNames(indexFile(temporaryDirectory)));

        byte[] musicBefore = Files.readAllBytes(musicFile(temporaryDirectory));
        byte[] indexBefore = Files.readAllBytes(indexFile(temporaryDirectory));
        byte[] listBefore = Files.readAllBytes(listFile(temporaryDirectory));
        assertThrows(
                RecordingStorageException.class,
                () -> storage.addRecording("Piano", createdAt.plusSeconds(4), List.of()));
        assertArrayEquals(musicBefore, Files.readAllBytes(musicFile(temporaryDirectory)));
        assertArrayEquals(indexBefore, Files.readAllBytes(indexFile(temporaryDirectory)));
        assertArrayEquals(listBefore, Files.readAllBytes(listFile(temporaryDirectory)));
    }

    @Test
    void createsOnlyMissingFilesWhenSelected() throws IOException {
        Path existingMusic = musicFile(temporaryDirectory);
        byte[] existingContents = new byte[IConstants.RECORDING_HEADER_BYTES];
        Files.write(existingMusic, existingContents);
        RecordingStorage storage = new RecordingStorage(temporaryDirectory);

        assertEquals(InitializationResult.DECISION_REQUIRED, storage.initialize(null));
        assertFalse(Files.exists(indexFile(temporaryDirectory)));
        assertArrayEquals(existingContents, Files.readAllBytes(existingMusic));

        assertEquals(
                InitializationResult.READY,
                storage.initialize(PartialFilesDecision.CREATE_MISSING));

        assertArrayEquals(existingContents, Files.readAllBytes(existingMusic));
        assertTrue(Files.exists(indexFile(temporaryDirectory)));
        assertTrue(Files.exists(listFile(temporaryDirectory)));
    }

    @Test
    void resetsOnlyRecordingFilesWhenSelected() throws IOException {
        Path existingIndex = indexFile(temporaryDirectory);
        byte[] existingContents = new byte[IConstants.RECORDING_INDEX_RECORD_BYTES];
        Files.write(existingIndex, existingContents);
        Path unrelatedFile = temporaryDirectory.resolve("keep.txt");
        Files.writeString(unrelatedFile, "preserve");
        RecordingStorage storage = new RecordingStorage(temporaryDirectory);

        assertEquals(InitializationResult.DECISION_REQUIRED, storage.initialize(null));
        assertArrayEquals(existingContents, Files.readAllBytes(existingIndex));
        assertEquals(InitializationResult.READY, storage.initialize(PartialFilesDecision.RESET));

        assertEquals(0L, Files.size(musicFile(temporaryDirectory)));
        assertEquals(0L, Files.size(indexFile(temporaryDirectory)));
        assertEquals(0L, Files.size(listFile(temporaryDirectory)));
        assertEquals("preserve", Files.readString(unrelatedFile));
    }

    @Test
    void cancelsPartialInitializationWithoutChangingFiles() throws IOException {
        Path existingList = listFile(temporaryDirectory);
        byte[] existingContents = "existing list\n".getBytes(StandardCharsets.UTF_8);
        Files.write(existingList, existingContents);
        RecordingStorage storage = new RecordingStorage(temporaryDirectory);

        assertEquals(InitializationResult.CANCELLED, storage.initialize(PartialFilesDecision.CANCEL));

        assertArrayEquals(existingContents, Files.readAllBytes(existingList));
        assertFalse(Files.exists(musicFile(temporaryDirectory)));
        assertFalse(Files.exists(indexFile(temporaryDirectory)));
    }

    @Test
    void rejectsMisalignedMusicAndIndexFilesWithoutRepairingThem() throws IOException {
        Path invalidMusicDirectory = temporaryDirectory.resolve("invalid-music");
        Files.createDirectories(invalidMusicDirectory);
        writeCompleteFileSet(invalidMusicDirectory);
        Path invalidMusic = musicFile(invalidMusicDirectory);
        byte[] musicContents = new byte[] { 1 };
        Files.write(invalidMusic, musicContents);

        RecordingStorage invalidMusicStorage = new RecordingStorage(invalidMusicDirectory);
        assertThrows(RecordingStorageException.class, () -> invalidMusicStorage.initialize(null));
        assertArrayEquals(musicContents, Files.readAllBytes(invalidMusic));

        Path invalidIndexDirectory = temporaryDirectory.resolve("invalid-index");
        Files.createDirectories(invalidIndexDirectory);
        writeCompleteFileSet(invalidIndexDirectory);
        Path invalidIndex = indexFile(invalidIndexDirectory);
        byte[] indexContents = new byte[] { 2 };
        Files.write(invalidIndex, indexContents);

        RecordingStorage invalidIndexStorage = new RecordingStorage(invalidIndexDirectory);
        assertThrows(RecordingStorageException.class, () -> invalidIndexStorage.initialize(null));
        assertArrayEquals(indexContents, Files.readAllBytes(invalidIndex));
    }

    @Test
    void rejectsInvalidNamesBeforeChangingFiles() throws IOException {
        RecordingStorage storage = new RecordingStorage(temporaryDirectory);
        storage.initialize(null);
        List<String> invalidNames = List.of("", " leading", "trailing ", "a".repeat(32), "música");

        for (String invalidName : invalidNames) {
            assertThrows(
                    RecordingStorageException.class,
                    () -> storage.addRecording(invalidName, Instant.EPOCH, List.of()));
        }

        assertEquals(0L, Files.size(musicFile(temporaryDirectory)));
        assertEquals(0L, Files.size(indexFile(temporaryDirectory)));
        assertEquals(0L, Files.size(listFile(temporaryDirectory)));
    }

    @Test
    void rejectsIncompleteListLineBeforeChangingBinaryFiles() throws IOException {
        RecordingStorage storage = new RecordingStorage(temporaryDirectory);
        storage.initialize(null);
        Files.writeString(listFile(temporaryDirectory), "incomplete entry");

        assertThrows(
                RecordingStorageException.class,
                () -> storage.addRecording("Jam", Instant.EPOCH, List.of()));

        assertEquals(0L, Files.size(musicFile(temporaryDirectory)));
        assertEquals(0L, Files.size(indexFile(temporaryDirectory)));
        assertEquals("incomplete entry", Files.readString(listFile(temporaryDirectory)));
    }

    private static void writeCompleteFileSet(Path pDirectory) throws IOException {
        Files.createFile(musicFile(pDirectory));
        Files.createFile(indexFile(pDirectory));
        Files.createFile(listFile(pDirectory));
    }

    private static List<String> readIndexNames(Path pIndexFile) throws IOException {
        List<String> names = new ArrayList<>();
        try (DataInputStream input = new DataInputStream(Files.newInputStream(pIndexFile))) {
            int recordCount = (int) (Files.size(pIndexFile) / IConstants.RECORDING_INDEX_RECORD_BYTES);
            for (int recordIndex = 0; recordIndex < recordCount; recordIndex++) {
                byte[] record = new byte[IConstants.RECORDING_INDEX_RECORD_BYTES];
                input.readFully(record);
                names.add(new String(
                        record,
                        0,
                        IConstants.RECORDING_NAME_BYTES,
                        StandardCharsets.US_ASCII).stripTrailing());
            }
        }
        return names;
    }

    private static byte[] encodeName(String pName) {
        byte[] name = new byte[IConstants.RECORDING_NAME_BYTES];
        Arrays.fill(name, (byte) ' ');
        byte[] bytes = pName.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(bytes, 0, name, 0, bytes.length);
        return name;
    }

    private static Path musicFile(Path pDirectory) {
        return pDirectory.resolve(IConstants.RECORDINGS_MUSIC_FILE_NAME);
    }

    private static Path indexFile(Path pDirectory) {
        return pDirectory.resolve(IConstants.RECORDINGS_INDEX_FILE_NAME);
    }

    private static Path listFile(Path pDirectory) {
        return pDirectory.resolve(IConstants.RECORDINGS_LIST_FILE_NAME);
    }
}
