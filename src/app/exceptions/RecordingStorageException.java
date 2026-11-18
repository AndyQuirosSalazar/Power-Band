package app.exceptions;

import java.io.IOException;

public final class RecordingStorageException extends IOException {

    public RecordingStorageException(String pMessage) {
        super(pMessage);
    }

    public RecordingStorageException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);
    }
}
