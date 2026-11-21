package app.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bridge.commands.BassCommand;
import app.bridge.commands.OrganCommand;
import app.bridge.commands.PianoCommand;
import app.bridge.commands.SynthCommand;
import app.model.CommandRecord;
import app.storage.RecordingStorage;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RecordingPlayerTest {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void mapsCommandsAndUsesAbsoluteDeadlinesWithoutReorderingEqualTimestamps() throws IOException {
        RecordingStorage storage = createLibrary(List.of(
                new BassCommand(42, 250).toCommandRecord(0L),
                new PianoCommand(65, 400, false).toCommandRecord(10L),
                new OrganCommand(0.7f).toCommandRecord(10L),
                new BassCommand(48, 300).toCommandRecord(30L)));
        AtomicLong clock = new AtomicLong();
        List<Long> waits = new ArrayList<>();
        List<SynthCommand> sent = new ArrayList<>();
        RecordingPlayer player = new RecordingPlayer(
                storage,
                () -> pCommand -> {
                    sent.add(pCommand);
                    clock.addAndGet(4_000_000L);
                },
                clock::get,
                pNanoseconds -> {
                    waits.add(pNanoseconds);
                    clock.addAndGet(pNanoseconds);
                });

        assertEquals(RecordingPlayer.Result.COMPLETED, player.play("Jam"));

        assertEquals(List.of(6_000_000L, 12_000_000L), waits);
        assertEquals(4, sent.size());
        assertTrue(sent.get(0) instanceof BassCommand);
        assertEquals(42, ((BassCommand) sent.get(0)).getNote());
        assertEquals(250, ((BassCommand) sent.get(0)).getDurationMillis());
        assertTrue(sent.get(1) instanceof PianoCommand);
        assertEquals(65, ((PianoCommand) sent.get(1)).getNote());
        assertFalse(((PianoCommand) sent.get(1)).isMajor());
        assertTrue(sent.get(2) instanceof OrganCommand);
        assertEquals(0.7f, ((OrganCommand) sent.get(2)).getVolume());
        assertTrue(sent.get(3) instanceof BassCommand);
        assertEquals(48, ((BassCommand) sent.get(3)).getNote());
    }

    @Test
    void returnsNotFoundWithoutOpeningAPlaybackSession() throws IOException {
        RecordingStorage storage = createLibrary(List.of());
        AtomicBoolean sessionOpened = new AtomicBoolean();
        RecordingPlayer player = new RecordingPlayer(
                storage,
                () -> {
                    sessionOpened.set(true);
                    return pCommand -> { };
                },
                () -> 0L,
                pNanoseconds -> { });

        assertEquals(RecordingPlayer.Result.NOT_FOUND, player.play("Missing"));
        assertFalse(sessionOpened.get());
    }

    @Test
    void propagatesTheFirstSendFailureAndClosesThePlaybackSession() throws IOException {
        RecordingStorage storage = createLibrary(List.of(
                new BassCommand(42, 250).toCommandRecord(0L),
                new BassCommand(48, 300).toCommandRecord(0L)));
        AtomicInteger sendCalls = new AtomicInteger();
        AtomicBoolean sessionClosed = new AtomicBoolean();
        RecordingPlayer player = new RecordingPlayer(
                storage,
                () -> new RecordingPlayer.PlaybackSession() {
                    @Override
                    public void execute(SynthCommand pCommand) throws IOException {
                        sendCalls.incrementAndGet();
                        throw new IOException("send failed");
                    }

                    @Override
                    public void close() {
                        sessionClosed.set(true);
                    }
                },
                () -> 0L,
                pNanoseconds -> { });

        IOException error = assertThrows(IOException.class, () -> player.play("Jam"));

        assertEquals("send failed", error.getMessage());
        assertEquals(1, sendCalls.get());
        assertTrue(sessionClosed.get());
    }

    private RecordingStorage createLibrary(List<CommandRecord> pCommands) throws IOException {
        RecordingStorage storage = new RecordingStorage(temporaryDirectory);
        storage.initialize(null);
        storage.addRecording("Jam", Instant.EPOCH, pCommands);
        return storage;
    }
}
