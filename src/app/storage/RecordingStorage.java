package app.storage;

import app.exceptions.RecordingStorageException;
import app.model.CommandRecord;
import app.model.RecordingMetadata;
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
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class RecordingStorage implements RecordingLibrary {

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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public synchronized List<RecordingMetadata> listRecordings() throws IOException {
        List<RecordingMetadata> recordings = readLibraryList();
        return List.copyOf(recordings);
    }

    @Override
    public synchronized Optional<List<CommandRecord>> readRecording(String pName) throws IOException {
        byte[] searchName = encodeName(pName);
        List<RecordingMetadata> recordings = readLibraryList();
        if (!Files.exists(indexFile, LinkOption.NOFOLLOW_LINKS)) {
            return Optional.empty();
        }
        boolean listed = recordings.stream().anyMatch(pRecording -> pName.equals(pRecording.getName()));

        long recordSize = IConstants.RECORDING_INDEX_RECORD_BYTES;
        byte[] searchRecord = createIndexRecord(searchName, 0L);
        try (RandomAccessFile index = new RandomAccessFile(indexFile.toFile(), "r")) {
            long low = 0;
            long high = index.length() / recordSize - 1;
            while (low <= high) {
                long middle = low + (high - low) / 2;
                index.seek(middle * recordSize);
                byte[] candidate = new byte[IConstants.RECORDING_INDEX_RECORD_BYTES];
                index.readFully(candidate);
                int comparison = BalancedMergeSort.compareIndexNames(candidate, searchRecord);
                if (comparison == 0) {
                    if (!listed) {
                        throw new RecordingStorageException("recordings.index contains a name absent from recordings.list");
                    }
                    long musicOffset = ByteBuffer.wrap(candidate)
                            .order(ByteOrder.BIG_ENDIAN)
                            .getLong(IConstants.RECORDING_NAME_BYTES);
                    return Optional.of(readCommands(pName, musicOffset));
                }
                if (comparison < 0) {
                    low = middle + 1;
                } else {
                    high = middle - 1;
                }
            }
        }
        if (listed) {
            throw new RecordingStorageException("recordings.list contains a name absent from recordings.index");
        }
        return Optional.empty();
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

    private List<RecordingMetadata> readLibraryList() throws IOException {
        boolean musicExists = Files.exists(musicFile, LinkOption.NOFOLLOW_LINKS);
        boolean indexExists = Files.exists(indexFile, LinkOption.NOFOLLOW_LINKS);
        boolean listExists = Files.exists(listFile, LinkOption.NOFOLLOW_LINKS);
        boolean anyExists = musicExists || indexExists || listExists;
        boolean allExist = musicExists && indexExists && listExists;
        if (!anyExists) {
            return List.of();
        }
        if (!allExist) {
            throw new RecordingStorageException("recording library is incomplete");
        }

        validateRegularFileIfPresent(musicFile, true);
        validateRegularFileIfPresent(indexFile, true);
        validateRegularFileIfPresent(listFile, true);
        validateFixedFileSizes();

        List<RecordingMetadata> recordings = readListRecords();
        long indexCount = Files.size(indexFile) / IConstants.RECORDING_INDEX_RECORD_BYTES;
        if (recordings.size() != indexCount) {
            throw new RecordingStorageException("recordings.list and recordings.index have different record counts");
        }
        if (indexCount == 0 && Files.size(musicFile) != 0) {
            throw new RecordingStorageException("recordings.music contains data without an index record");
        }
        return recordings;
    }

    private List<RecordingMetadata> readListRecords() throws IOException {
        long listSize = Files.size(listFile);
        if (listSize > 0) {
            try (RandomAccessFile list = new RandomAccessFile(listFile.toFile(), "r")) {
                list.seek(listSize - 1);
                if (list.read() != '\n') {
                    throw new RecordingStorageException("recordings.list does not end at a complete line");
                }
            }
        }
        List<String> lines = Files.readAllLines(listFile, StandardCharsets.UTF_8);
        List<RecordingMetadata> recordings = new ArrayList<>(lines.size());
        Set<String> names = new HashSet<>();
        for (String line : lines) {
            int separator = line.indexOf('\t');
            if (separator <= 0 || separator != line.lastIndexOf('\t') || separator == line.length() - 1) {
                throw new RecordingStorageException("recordings.list contains an invalid entry");
            }
            String name = line.substring(0, separator);
            encodeName(name);
            if (!names.add(name)) {
                throw new RecordingStorageException("recordings.list contains a duplicate name: " + name);
            }
            try {
                recordings.add(new RecordingMetadata(name, Instant.parse(line.substring(separator + 1))));
            } catch (DateTimeException pError) {
                throw new RecordingStorageException("recordings.list contains an invalid timestamp", pError);
            }
        }
        return recordings;
    }

    private List<CommandRecord> readCommands(String pName, long pMusicOffset) throws IOException {
        long musicLength = Files.size(musicFile);
        long headerBytes = IConstants.RECORDING_HEADER_BYTES;
        if (pMusicOffset < 0
                || pMusicOffset % headerBytes != 0
                || pMusicOffset > musicLength - headerBytes) {
            throw new RecordingStorageException("recordings.index contains an invalid music offset");
        }

        try (RandomAccessFile music = new RandomAccessFile(musicFile.toFile(), "r")) {
            music.seek(pMusicOffset);
            byte[] header = new byte[IConstants.RECORDING_HEADER_BYTES];
            music.readFully(header);
            String headerName = decodeName(header, 0);
            if (!pName.equals(headerName)) {
                throw new RecordingStorageException("recordings.index and recordings.music names do not match");
            }

            ByteBuffer headerFields = ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN);
            long commandsStart = headerFields.getLong(IConstants.RECORDING_NAME_BYTES);
            long commandsEnd = headerFields.getLong(IConstants.RECORDING_NAME_BYTES + Long.BYTES);
            long expectedStart;
            try {
                expectedStart = Math.addExact(pMusicOffset, headerBytes);
            } catch (ArithmeticException pError) {
                throw new RecordingStorageException("recording header offset overflows", pError);
            }
            long commandBytes = IConstants.COMMAND_RECORD_BYTES;
            if (commandsStart != expectedStart
                    || commandsEnd < commandsStart
                    || commandsEnd > musicLength
                    || commandsStart % commandBytes != 0
                    || commandsEnd % commandBytes != 0
                    || (commandsEnd - commandsStart) % commandBytes != 0) {
                throw new RecordingStorageException("recording header contains an invalid command range");
            }

            long commandCount = (commandsEnd - commandsStart) / commandBytes;
            if (commandCount > Integer.MAX_VALUE) {
                throw new RecordingStorageException("recording contains too many commands to load");
            }
            List<CommandRecord> commands = new ArrayList<>((int) commandCount);
            long previousTime = -1;
            music.seek(commandsStart);
            while (music.getFilePointer() < commandsEnd) {
                byte[] record = new byte[IConstants.COMMAND_RECORD_BYTES];
                music.readFully(record);
                CommandRecord command;
                try {
                    command = CommandRecord.fromByteArray(record);
                } catch (IllegalArgumentException pError) {
                    throw new RecordingStorageException("recording contains an invalid command record", pError);
                }
                if (command.getRelativeTimeMillis() < previousTime) {
                    throw new RecordingStorageException("recording command timestamps are not ordered");
                }
                previousTime = command.getRelativeTimeMillis();
                commands.add(command);
            }
            return List.copyOf(commands);
        }
    }

    private static String decodeName(byte[] pBytes, int pOffset) throws RecordingStorageException {
        byte[] encoded = Arrays.copyOfRange(pBytes, pOffset, pOffset + IConstants.RECORDING_NAME_BYTES);
        int length = encoded.length;
        while (length > 0 && encoded[length - 1] == ' ') {
            length--;
        }
        if (length == 0) {
            throw new RecordingStorageException("recording file contains an empty name");
        }
        for (int index = 0; index < length; index++) {
            if (encoded[index] < 0x20 || encoded[index] > 0x7e) {
                throw new RecordingStorageException("recording file contains an invalid name");
            }
        }
        String name = new String(encoded, 0, length, StandardCharsets.US_ASCII);
        if (!Arrays.equals(encoded, encodeName(name))) {
            throw new RecordingStorageException("recording file contains an invalid name encoding");
        }
        return name;
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
