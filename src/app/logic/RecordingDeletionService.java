package app.logic;

import app.storage.RecordingLibrary;
import java.io.IOException;
import java.util.Objects;

public final class RecordingDeletionService {

    public enum Result {
        DELETED,
        NOT_FOUND
    }

    private final RecordingLibrary library;

    public RecordingDeletionService(RecordingLibrary pLibrary) {
        library = Objects.requireNonNull(pLibrary, "pLibrary");
    }

    public Result delete(String pName) throws IOException {
        return library.deleteRecording(pName) ? Result.DELETED : Result.NOT_FOUND;
    }
}
