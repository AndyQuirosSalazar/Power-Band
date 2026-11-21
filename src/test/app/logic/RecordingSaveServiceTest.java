package app.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.model.CommandRecord;
import app.model.RecordingMetadata;
import app.storage.InitializationResult;
import app.storage.PartialFilesDecision;
import app.storage.RecordingLibrary;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RecordingSaveServiceTest {

    @Test
    void trimsNamesValidatesPrintableAsciiAndKeepsCaseSensitiveDuplicatesDistinct() throws IOException {
        FakeRecordingLibrary library = new FakeRecordingLibrary();
        RecordingSaveService service = new RecordingSaveService(library);

        assertEquals(
                RecordingSaveService.Result.INVALID_NAME,
                service.save(" música ", List.of(), Instant.EPOCH, null));
        for (String invalidName : List.of("", "x".repeat(32), "\tJam\n")) {
            assertEquals(
                    RecordingSaveService.Result.INVALID_NAME,
                    service.save(invalidName, List.of(), Instant.EPOCH, null));
        }
        assertEquals(0, library.initializeCalls);
        assertEquals(
                RecordingSaveService.Result.SAVED,
                service.save(" Piano ", List.of(), Instant.EPOCH, null));
        assertEquals(
                RecordingSaveService.Result.SAVED,
                service.save("piano", List.of(), Instant.EPOCH, null));
        assertEquals(List.of("Piano", "piano"), library.savedNames);
    }

    @Test
    void warnsForDuplicateAndRequiresAChoiceBeforeChangingPartialLibrary() throws IOException {
        FakeRecordingLibrary library = new FakeRecordingLibrary();
        library.savedNames.add("Piano");
        RecordingSaveService service = new RecordingSaveService(library);

        assertEquals(
                RecordingSaveService.Result.DUPLICATE_NAME,
                service.save("Piano", List.of(), Instant.EPOCH, null));
        assertEquals(0, library.addCalls);

        library.initializationResult = InitializationResult.DECISION_REQUIRED;
        assertEquals(
                RecordingSaveService.Result.PARTIAL_FILES_DECISION_REQUIRED,
                service.save("New", List.of(), Instant.EPOCH, null));
        assertEquals(
                RecordingSaveService.Result.CANCELLED,
                service.save("New", List.of(), Instant.EPOCH, PartialFilesDecision.CANCEL));
        assertFalse(library.savedNames.contains("New"));
        assertEquals(0, library.addCalls);
    }

    private static final class FakeRecordingLibrary implements RecordingLibrary {

        private final List<String> savedNames = new ArrayList<>();
        private int initializeCalls;
        private int addCalls;
        private InitializationResult initializationResult = InitializationResult.READY;

        @Override
        public InitializationResult initialize(PartialFilesDecision pDecision) {
            initializeCalls++;
            if (pDecision == PartialFilesDecision.CANCEL) {
                return InitializationResult.CANCELLED;
            }
            return initializationResult;
        }

        @Override
        public boolean recordingExists(String pName) {
            return savedNames.contains(pName);
        }

        @Override
        public void addRecording(String pName, Instant pCreatedAt, List<CommandRecord> pCommands) {
            addCalls++;
            savedNames.add(pName);
        }

        @Override
        public List<RecordingMetadata> listRecordings() {
            return List.of();
        }

        @Override
        public Optional<List<CommandRecord>> readRecording(String pName) {
            return Optional.empty();
        }
    }
}
