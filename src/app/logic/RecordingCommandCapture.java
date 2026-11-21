package app.logic;

import app.bridge.commands.SynthCommand;
import app.model.CommandRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RecordingCommandCapture {

    private final List<CommandRecord> commands = new ArrayList<>();
    private long startedAtNanos;
    private boolean recording;

    public synchronized void start() {
        if (recording) {
            throw new IllegalStateException("recording has already started");
        }
        commands.clear();
        startedAtNanos = System.nanoTime();
        recording = true;
    }

    public synchronized void record(SynthCommand pCommand) {
        Objects.requireNonNull(pCommand, "pCommand");
        if (recording) {
            long relativeTimeMillis = (System.nanoTime() - startedAtNanos) / 1_000_000L;
            commands.add(pCommand.toCommandRecord(relativeTimeMillis));
        }
    }

    public synchronized List<CommandRecord> finish() {
        recording = false;
        return List.copyOf(commands);
    }
}
