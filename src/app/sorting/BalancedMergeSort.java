package app.sorting;

import app.util.IConstants;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public final class BalancedMergeSort {

    private static final String TEMP_DIRECTORY_PREFIX = "recordings-index-sort-";

    private BalancedMergeSort() {
    }

    public static void sort(Path pIndexFile) throws IOException {
        Objects.requireNonNull(pIndexFile, "pIndexFile");
        Path indexFile = pIndexFile.toAbsolutePath().normalize();
        long indexSize = Files.size(indexFile);
        int recordSize = IConstants.RECORDING_INDEX_RECORD_BYTES;
        if (indexSize % recordSize != 0) {
            throw new IOException("recordings index size is not a multiple of " + recordSize + " bytes");
        }

        long runCount = indexSize / recordSize;
        if (runCount < 2) {
            return;
        }

        Path temporaryDirectory = Files.createTempDirectory(indexFile.getParent(), TEMP_DIRECTORY_PREFIX);
        Path splitA = temporaryDirectory.resolve("split-a.bin");
        Path splitB = temporaryDirectory.resolve("split-b.bin");
        Path mergeA = temporaryDirectory.resolve("merge-a.bin");
        Path mergeB = temporaryDirectory.resolve("merge-b.bin");
        IOException operationError = null;
        try {
            distributeRuns(indexFile, splitA, splitB, runCount);
            Path sourceA = splitA;
            Path sourceB = splitB;
            Path destinationA = mergeA;
            Path destinationB = mergeB;
            long runLength = 1;

            while (runCount > 1) {
                mergePass(
                        sourceA,
                        sourceB,
                        destinationA,
                        destinationB,
                        runCount,
                        runLength,
                        recordSize);
                runCount = runCount / 2 + runCount % 2;
                runLength *= 2;

                Path previousSourceA = sourceA;
                Path previousSourceB = sourceB;
                sourceA = destinationA;
                sourceB = destinationB;
                destinationA = previousSourceA;
                destinationB = previousSourceB;
            }

            moveSortedIndex(sourceA, indexFile);
        } catch (IOException pError) {
            operationError = pError;
            throw pError;
        } finally {
            IOException cleanupError = deleteTemporaryFiles(
                    temporaryDirectory,
                    splitA,
                    splitB,
                    mergeA,
                    mergeB);
            if (cleanupError != null) {
                if (operationError != null) {
                    operationError.addSuppressed(cleanupError);
                } else {
                    throw cleanupError;
                }
            }
        }
    }

    private static void distributeRuns(
            Path pIndexFile,
            Path pSplitA,
            Path pSplitB,
            long pRecordCount) throws IOException {
        int recordSize = IConstants.RECORDING_INDEX_RECORD_BYTES;
        try (DataInputStream input = openInput(pIndexFile);
                DataOutputStream outputA = openOutput(pSplitA);
                DataOutputStream outputB = openOutput(pSplitB)) {
            for (long recordIndex = 0; recordIndex < pRecordCount; recordIndex++) {
                byte[] record = readRecord(input, recordSize);
                DataOutputStream output = recordIndex % 2 == 0 ? outputA : outputB;
                output.write(record);
            }
        }
    }

    private static void mergePass(
            Path pSourceA,
            Path pSourceB,
            Path pDestinationA,
            Path pDestinationB,
            long pRunCount,
            long pRunLength,
            int pRecordSize) throws IOException {
        long sourceARecords = getRecordCount(pSourceA, pRecordSize);
        long sourceBRecords = getRecordCount(pSourceB, pRecordSize);
        long runsRemaining = pRunCount;
        long destinationRunIndex = 0;

        try (DataInputStream inputA = openInput(pSourceA);
                DataInputStream inputB = openInput(pSourceB);
                DataOutputStream outputA = openOutput(pDestinationA);
                DataOutputStream outputB = openOutput(pDestinationB)) {
            while (runsRemaining > 0) {
                long leftRunLength = Math.min(pRunLength, sourceARecords);
                if (leftRunLength == 0) {
                    throw new IOException("balanced merge sort found an incomplete left run");
                }
                long rightRunLength = 0;
                if (runsRemaining > 1) {
                    rightRunLength = Math.min(pRunLength, sourceBRecords);
                    if (rightRunLength == 0) {
                        throw new IOException("balanced merge sort found an incomplete right run");
                    }
                }

                DataOutputStream output = destinationRunIndex % 2 == 0 ? outputA : outputB;
                mergeRuns(
                        inputA,
                        leftRunLength,
                        inputB,
                        rightRunLength,
                        output,
                        pRecordSize);
                sourceARecords -= leftRunLength;
                sourceBRecords -= rightRunLength;
                runsRemaining -= Math.min(2, runsRemaining);
                destinationRunIndex++;
            }
        }

        if (sourceARecords != 0 || sourceBRecords != 0) {
            throw new IOException("balanced merge sort left unmerged index records");
        }
    }

    private static void mergeRuns(
            DataInputStream pInputA,
            long pRunLengthA,
            DataInputStream pInputB,
            long pRunLengthB,
            DataOutputStream pOutput,
            int pRecordSize) throws IOException {
        long remainingA = pRunLengthA;
        long remainingB = pRunLengthB;
        byte[] recordA = remainingA > 0 ? readRecord(pInputA, pRecordSize) : null;
        byte[] recordB = remainingB > 0 ? readRecord(pInputB, pRecordSize) : null;

        while (remainingA > 0 || remainingB > 0) {
            if (remainingA == 0 || (remainingB > 0 && compareNames(recordA, recordB) > 0)) {
                pOutput.write(recordB);
                remainingB--;
                recordB = remainingB > 0 ? readRecord(pInputB, pRecordSize) : null;
            } else {
                pOutput.write(recordA);
                remainingA--;
                recordA = remainingA > 0 ? readRecord(pInputA, pRecordSize) : null;
            }
        }
    }

    private static int compareNames(byte[] pRecordA, byte[] pRecordB) {
        for (int index = 0; index < IConstants.RECORDING_NAME_BYTES; index++) {
            int valueA = Byte.toUnsignedInt(pRecordA[index]);
            int valueB = Byte.toUnsignedInt(pRecordB[index]);
            if (valueA != valueB) {
                return Integer.compare(valueA, valueB);
            }
        }
        return 0;
    }

    private static long getRecordCount(Path pFile, int pRecordSize) throws IOException {
        long size = Files.size(pFile);
        if (size % pRecordSize != 0) {
            throw new IOException("temporary index file has an invalid record size");
        }
        return size / pRecordSize;
    }

    private static byte[] readRecord(DataInputStream pInput, int pRecordSize) throws IOException {
        byte[] record = new byte[pRecordSize];
        try {
            pInput.readFully(record);
        } catch (EOFException pError) {
            throw new IOException("unexpected end of index file", pError);
        }
        return record;
    }

    private static DataInputStream openInput(Path pPath) throws IOException {
        return new DataInputStream(new BufferedInputStream(Files.newInputStream(pPath)));
    }

    private static DataOutputStream openOutput(Path pPath) throws IOException {
        return new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(
                pPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)));
    }

    private static void moveSortedIndex(Path pSortedFile, Path pIndexFile) throws IOException {
        try {
            Files.move(
                    pSortedFile,
                    pIndexFile,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException pError) {
            Files.move(pSortedFile, pIndexFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static IOException deleteTemporaryFiles(Path pDirectory, Path... pFiles) {
        IOException firstError = null;
        for (Path file : pFiles) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException pError) {
                firstError = addCleanupError(firstError, pError);
            }
        }
        try {
            Files.deleteIfExists(pDirectory);
        } catch (IOException pError) {
            firstError = addCleanupError(firstError, pError);
        }
        return firstError;
    }

    private static IOException addCleanupError(IOException pFirstError, IOException pNewError) {
        if (pFirstError == null) {
            return pNewError;
        }
        pFirstError.addSuppressed(pNewError);
        return pFirstError;
    }
}
