package app.storage;

import app.exceptions.RecordingStorageException;
import app.model.CommandRecord;
import app.sorting.BalancedMergeSort;
import app.util.IConstants;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class RecordingStorage {

    private final Path directory;
    private final Path musicFile;
    private final Path indexFile;
    private final Path listFile;

    private boolean initialized;

    public RecordingStorage(Path pDirectory) {
        directory = Objects.requireNonNull(pDirectory, "pDirectory").toAbsolutePath().normalize();
        musicFile = directory.resolve(IConstants.RECORDINGS_MUSIC_FILE_NAME);
        indexFile = directory.resolve(IConstants.RECORDINGS_INDEX_FILE_NAME);
        listFile = directory.resolve(IConstants.RECORDINGS_LIST_FILE_NAME);
    }

    public synchronized InitializationResult initialize(PartialFilesDecision pDecision) throws IOException {
        if (initialized) {
            return InitializationResult.READY;
        }

        Files.createDirectories(directory);
        boolean musicExists = Files.exists(musicFile);
        boolean indexExists = Files.exists(indexFile);
        boolean listExists = Files.exists(listFile);
        validateRegularFileIfPresent(musicFile, musicExists);
        validateRegularFileIfPresent(indexFile, indexExists);
        validateRegularFileIfPresent(listFile, listExists);

        boolean anyExists = musicExists || indexExists || listExists;
        boolean allExist = musicExists && indexExists && listExists;
        if (!anyExists) {
            createAllFiles();
        } else if (!allExist) {
            if (pDecision == null) {
                return InitializationResult.DECISION_REQUIRED;
            }
            if (pDecision == PartialFilesDecision.CANCEL) {
                return InitializationResult.CANCELLED;
            }
            if (pDecision == PartialFilesDecision.RESET) {
                Files.deleteIfExists(musicFile);
                Files.deleteIfExists(indexFile);
                Files.deleteIfExists(listFile);
                createAllFiles();
            } else {
                createMissingFile(musicFile, musicExists);
                createMissingFile(indexFile, indexExists);
                createMissingFile(listFile, listExists);
            }
        }

        validateFixedFileSizes();
        initialized = true;
        return InitializationResult.READY;
    }

    public synchronized boolean recordingExists(String pName) throws IOException {
        requireInitialized();
        byte[] name = encodeName(pName);
        validateFixedFileSizes();

        long recordSize = IConstants.RECORDING_INDEX_RECORD_BYTES;
        try (RandomAccessFile index = new RandomAccessFile(indexFile.toFile(), "r")) {
            long recordCount = index.length() / recordSize;
            long low = 0;
            long high = recordCount - 1;
            byte[] searchRecord = createIndexRecord(name, 0L);
            while (low <= high) {
                long middle = low + (high - low) / 2;
                index.seek(middle * recordSize);
                byte[] candidate = new byte[IConstants.RECORDING_INDEX_RECORD_BYTES];
                index.readFully(candidate);
                int comparison = BalancedMergeSort.compareIndexNames(candidate, searchRecord);
                if (comparison == 0) {
                    return true;
                }
                if (comparison < 0) {
                    low = middle + 1;
                } else {
                    high = middle - 1;
                }
            }
        }
        return false;
    }

    public synchronized void addRecording(
            String pName,
            Instant pCreatedAt,
            List<CommandRecord> pCommands) throws IOException {
        requireInitialized();
        byte[] name = encodeName(pName);
        Objects.requireNonNull(pCreatedAt, "pCreatedAt");
        Objects.requireNonNull(pCommands, "pCommands");
        for (CommandRecord command : pCommands) {
            Objects.requireNonNull(command, "pCommands contains null");
        }
        validateFixedFileSizes();
        if (recordingExists(pName)) {
            throw new RecordingStorageException("recording name already exists: " + pName);
        }
        validateListAppendable();

        long musicOffset = Files.size(musicFile);
        long commandsStart;
        long commandsEnd;
        try {
            commandsStart = Math.addExact(musicOffset, IConstants.RECORDING_HEADER_BYTES);
            long commandsBytes = Math.multiplyExact(
                    (long) pCommands.size(),
                    IConstants.COMMAND_RECORD_BYTES);
            commandsEnd = Math.addExact(commandsStart, commandsBytes);
        } catch (ArithmeticException pError) {
            throw new RecordingStorageException("recording is too large to store", pError);
        }

        appendMusicRecord(name, commandsStart, commandsEnd, pCommands);
        appendIndexRecord(name, musicOffset);
        appendListRecord(pName, pCreatedAt);
    }

    private void appendMusicRecord(
            byte[] pName,
            long pCommandsStart,
            long pCommandsEnd,
            List<CommandRecord> pCommands) throws IOException {
        try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(
                Files.newOutputStream(musicFile, StandardOpenOption.WRITE, StandardOpenOption.APPEND)))) {
            output.write(pName);
            output.writeLong(pCommandsStart);
            output.writeLong(pCommandsEnd);
            for (CommandRecord command : pCommands) {
                output.write(command.toByteArray());
            }
        }
    }

    private void appendIndexRecord(byte[] pName, long pMusicOffset) throws IOException {
        byte[] indexRecord = createIndexRecord(pName, pMusicOffset);
        Files.write(
                indexFile,
                indexRecord,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND);
        BalancedMergeSort.sort(indexFile);
    }

    private void appendListRecord(String pName, Instant pCreatedAt) throws IOException {
        String entry = pName + "\t" + pCreatedAt + "\n";
        Files.writeString(
                listFile,
                entry,
                StandardCharsets.UTF_8,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND);
    }

    private void validateListAppendable() throws IOException {
        long listSize = Files.size(listFile);
        if (listSize == 0) {
            return;
        }
        try (RandomAccessFile list = new RandomAccessFile(listFile.toFile(), "r")) {
            list.seek(listSize - 1);
            if (list.read() != '\n') {
                throw new RecordingStorageException("recordings.list does not end at a complete line");
            }
        }
    }

    private static byte[] encodeName(String pName) throws RecordingStorageException {
        if (pName == null
                || pName.isEmpty()
                || pName.length() > IConstants.RECORDING_NAME_MAX_LENGTH
                || pName.charAt(0) == ' '
                || pName.charAt(pName.length() - 1) == ' ') {
            throw new RecordingStorageException("recording name must contain 1 to 31 characters without outer spaces");
        }
        for (int index = 0; index < pName.length(); index++) {
            char character = pName.charAt(index);
            if (character < 0x20 || character > 0x7e) {
                throw new RecordingStorageException("recording name must contain printable ASCII characters only");
            }
        }

        byte[] encoded = new byte[IConstants.RECORDING_NAME_BYTES];
        java.util.Arrays.fill(encoded, (byte) ' ');
        byte[] nameBytes = pName.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(nameBytes, 0, encoded, 0, nameBytes.length);
        return encoded;
    }

    private static byte[] createIndexRecord(byte[] pName, long pMusicOffset) {
        ByteBuffer record = ByteBuffer.allocate(IConstants.RECORDING_INDEX_RECORD_BYTES)
                .order(ByteOrder.BIG_ENDIAN);
        record.put(pName);
        record.putLong(pMusicOffset);
        return record.array();
    }

    private void createAllFiles() throws IOException {
        Files.createFile(musicFile);
        Files.createFile(indexFile);
        Files.createFile(listFile);
    }

    private static void createMissingFile(Path pFile, boolean pExists) throws IOException {
        if (!pExists) {
            Files.createFile(pFile);
        }
    }

    private static void validateRegularFileIfPresent(Path pFile, boolean pExists)
            throws RecordingStorageException {
        if (pExists && !Files.isRegularFile(pFile)) {
            throw new RecordingStorageException("recording path is not a regular file: " + pFile.getFileName());
        }
    }

    private void validateFixedFileSizes() throws IOException {
        validateRecordAlignment(musicFile, IConstants.RECORDING_HEADER_BYTES);
        validateRecordAlignment(indexFile, IConstants.RECORDING_INDEX_RECORD_BYTES);
    }

    private static void validateRecordAlignment(Path pFile, int pRecordSize) throws IOException {
        if (Files.size(pFile) % pRecordSize != 0) {
            throw new RecordingStorageException(
                    pFile.getFileName() + " size is not a multiple of " + pRecordSize + " bytes");
        }
    }

    private void requireInitialized() throws RecordingStorageException {
        if (!initialized) {
            throw new RecordingStorageException("recording storage has not been initialized");
        }
    }

}
