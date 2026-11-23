package app.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.model.CommandRecord;
import app.model.RecordingMetadata;
import app.storage.InitializationResult;
import app.storage.PartialFilesDecision;
import app.storage.RecordingLibrary;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RecordingDeletionServiceTest {

    @Test
    void deletesTheExactRequestedName() throws IOException {
        FakeRecordingLibrary library = new FakeRecordingLibrary();
        RecordingDeletionService service = new RecordingDeletionService(library);

        assertEquals(RecordingDeletionService.Result.DELETED, service.delete("Selected"));
        assertEquals("Selected", library.deletedName);
        assertEquals(1, library.deleteCalls);
    }

    @Test
    void reportsWhenTheRequestedNameNoLongerExists() throws IOException {
        FakeRecordingLibrary library = new FakeRecordingLibrary();
        library.deleteResult = false;
        RecordingDeletionService service = new RecordingDeletionService(library);

        assertEquals(RecordingDeletionService.Result.NOT_FOUND, service.delete("Missing"));
        assertEquals("Missing", library.deletedName);
    }

    @Test
    void propagatesStorageErrors() {
        FakeRecordingLibrary library = new FakeRecordingLibrary();
        library.deleteError = new IOException("disk unavailable");
        RecordingDeletionService service = new RecordingDeletionService(library);

        IOException error = assertThrows(IOException.class, () -> service.delete("Selected"));

        assertEquals("disk unavailable", error.getMessage());
    }

    private static final class FakeRecordingLibrary implements RecordingLibrary {

        private boolean deleteResult = true;
        private IOException deleteError;
        private int deleteCalls;
        private String deletedName;

        @Override
        public InitializationResult initialize(PartialFilesDecision pDecision) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean recordingExists(String pName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addRecording(String pName, Instant pCreatedAt, List<CommandRecord> pCommands) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean deleteRecording(String pName) throws IOException {
            deleteCalls++;
            deletedName = pName;
            if (deleteError != null) {
                throw deleteError;
            }
            return deleteResult;
        }

        @Override
        public List<RecordingMetadata> listRecordings() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<List<CommandRecord>> readRecording(String pName) {
            throw new UnsupportedOperationException();
        }
    }
}
