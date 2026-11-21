package app.logic;

import app.model.CommandRecord;
import app.storage.InitializationResult;
import app.storage.PartialFilesDecision;
import app.storage.RecordingLibrary;
import app.util.IConstants;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class RecordingSaveService {

    public enum Result {
        INVALID_NAME,
        DUPLICATE_NAME,
        PARTIAL_FILES_DECISION_REQUIRED,
        CANCELLED,
        SAVED
    }

    private final RecordingLibrary library;

    public RecordingSaveService(RecordingLibrary pLibrary) {
        library = Objects.requireNonNull(pLibrary, "pLibrary");
    }

    public Result save(
            String pRawName,
            List<CommandRecord> pCommands,
            Instant pCreatedAt,
            PartialFilesDecision pPartialFilesDecision) throws IOException {
        String name = normalizeName(pRawName);
        if (!isValidName(name)) {
            return Result.INVALID_NAME;
        }
        Objects.requireNonNull(pCommands, "pCommands");
        Objects.requireNonNull(pCreatedAt, "pCreatedAt");

        InitializationResult initialization = library.initialize(pPartialFilesDecision);
        if (initialization == InitializationResult.DECISION_REQUIRED) {
            return Result.PARTIAL_FILES_DECISION_REQUIRED;
        }
        if (initialization == InitializationResult.CANCELLED) {
            return Result.CANCELLED;
        }
        if (library.recordingExists(name)) {
            return Result.DUPLICATE_NAME;
        }

        library.addRecording(name, pCreatedAt, pCommands);
        return Result.SAVED;
    }

    public static String normalizeName(String pRawName) {
        if (pRawName == null) {
            return null;
        }
        int start = 0;
        int end = pRawName.length();
        while (start < end && pRawName.charAt(start) == ' ') {
            start++;
        }
        while (end > start && pRawName.charAt(end - 1) == ' ') {
            end--;
        }
        return pRawName.substring(start, end);
    }

    private static boolean isValidName(String pName) {
        if (pName == null || pName.isEmpty() || pName.length() > IConstants.RECORDING_NAME_MAX_LENGTH) {
            return false;
        }
        for (int index = 0; index < pName.length(); index++) {
            char character = pName.charAt(index);
            if (character < 0x20 || character > 0x7e) {
                return false;
            }
        }
        return true;
    }
}
