package app.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bridge.commands.BassCommand;
import app.bridge.commands.PianoCommand;
import app.model.CommandRecord;
import app.util.IConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecordingCommandCaptureTest {

    @Test
    void capturesSynthCommandsWithRelativeTimesAndStopsAfterFinishing() {
        RecordingCommandCapture capture = new RecordingCommandCapture();
        capture.start();
        capture.record(new BassCommand(43, 250));
        capture.record(new PianoCommand(64, 500, true));

        List<CommandRecord> records = capture.finish();
        capture.record(new BassCommand(36, 250));

        assertEquals(2, records.size());
        assertEquals(IConstants.SYNTH_ID_BASS, records.get(0).getSynthId());
        assertEquals(IConstants.SYNTH_ID_PIANO, records.get(1).getSynthId());
        assertTrue(records.get(0).getRelativeTimeMillis() >= 0);
        assertTrue(records.get(1).getRelativeTimeMillis() >= records.get(0).getRelativeTimeMillis());
        assertEquals(2, records.size());
    }
}
