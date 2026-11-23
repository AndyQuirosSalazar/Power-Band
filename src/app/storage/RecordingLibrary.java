package app.storage;

import app.model.CommandRecord;
import app.model.RecordingMetadata;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RecordingLibrary {

    InitializationResult initialize(PartialFilesDecision pDecision) throws IOException;

    boolean recordingExists(String pName) throws IOException;

    void addRecording(String pName, Instant pCreatedAt, List<CommandRecord> pCommands) throws IOException;

    boolean deleteRecording(String pName) throws IOException;

    List<RecordingMetadata> listRecordings() throws IOException;

    Optional<List<CommandRecord>> readRecording(String pName) throws IOException;
}
