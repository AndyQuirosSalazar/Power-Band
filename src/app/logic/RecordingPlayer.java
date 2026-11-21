package app.logic;

import app.bridge.commands.BassCommand;
import app.bridge.commands.OrganCommand;
import app.bridge.commands.PianoCommand;
import app.bridge.commands.SynthCommand;
import app.exceptions.RecordingStorageException;
import app.model.CommandRecord;
import app.model.RecordingMetadata;
import app.storage.RecordingLibrary;
import app.supercollider.BandClient;
import app.util.IConstants;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public final class RecordingPlayer {

    public enum Result {
        COMPLETED,
        NOT_FOUND,
        CANCELLED
    }

    public interface PlaybackSession extends AutoCloseable {

        void execute(SynthCommand pCommand) throws IOException;

        @Override
        default void close() throws IOException {
        }
    }

    @FunctionalInterface
    public interface PlaybackSessionFactory {

        PlaybackSession open() throws IOException;
    }

    @FunctionalInterface
    public interface Sleeper {

        void sleep(long pNanoseconds) throws InterruptedException;
    }

    private final RecordingLibrary recordingLibrary;
    private final PlaybackSessionFactory sessionFactory;
    private final LongSupplier nanoTime;
    private final Sleeper sleeper;

    public RecordingPlayer(RecordingLibrary pRecordingLibrary) {
        this(
                pRecordingLibrary,
                RecordingPlayer::openBandClientSession,
                System::nanoTime,
                pNanoseconds -> TimeUnit.NANOSECONDS.sleep(pNanoseconds));
    }

    public RecordingPlayer(
            RecordingLibrary pRecordingLibrary,
            PlaybackSessionFactory pSessionFactory,
            LongSupplier pNanoTime,
            Sleeper pSleeper) {
        recordingLibrary = Objects.requireNonNull(pRecordingLibrary, "pRecordingLibrary");
        sessionFactory = Objects.requireNonNull(pSessionFactory, "pSessionFactory");
        nanoTime = Objects.requireNonNull(pNanoTime, "pNanoTime");
        sleeper = Objects.requireNonNull(pSleeper, "pSleeper");
    }

    public List<RecordingMetadata> listRecordings() throws IOException {
        return recordingLibrary.listRecordings();
    }

    public Result play(String pName) throws IOException {
        Optional<List<CommandRecord>> selected = recordingLibrary.readRecording(pName);
        if (selected.isEmpty()) {
            return Result.NOT_FOUND;
        }

        try (PlaybackSession session = sessionFactory.open()) {
            List<CommandRecord> commands = List.copyOf(selected.orElseThrow());
            long startTime = nanoTime.getAsLong();
            long previousTimeMillis = -1;
            for (CommandRecord command : commands) {
                long relativeTimeMillis = command.getRelativeTimeMillis();
                if (relativeTimeMillis < previousTimeMillis) {
                    throw new RecordingStorageException("recording command timestamps are not ordered");
                }
                previousTimeMillis = relativeTimeMillis;

                long relativeTimeNanos;
                try {
                    relativeTimeNanos = Math.multiplyExact(relativeTimeMillis, 1_000_000L);
                } catch (ArithmeticException pError) {
                    throw new RecordingStorageException("recording timeline is too long to play", pError);
                }
                if (Thread.currentThread().isInterrupted()
                        || !waitUntil(startTime + relativeTimeNanos)) {
                    return Result.CANCELLED;
                }
                session.execute(toSynthCommand(command));
            }
            return Result.COMPLETED;
        }
    }

    private boolean waitUntil(long pDeadlineNanos) {
        while (!Thread.currentThread().isInterrupted()) {
            long remainingNanos = pDeadlineNanos - nanoTime.getAsLong();
            if (remainingNanos <= 0) {
                return true;
            }
            try {
                sleeper.sleep(remainingNanos);
            } catch (InterruptedException pError) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private static SynthCommand toSynthCommand(CommandRecord pRecord) {
        return switch (pRecord.getSynthId()) {
            case IConstants.SYNTH_ID_BASS ->
                new BassCommand(pRecord.getNote(), pRecord.getDurationMillis());
            case IConstants.SYNTH_ID_PIANO ->
                new PianoCommand(
                        pRecord.getNote(),
                        pRecord.getDurationMillis(),
                        pRecord.getMajor() == 1);
            case IConstants.SYNTH_ID_ORGAN -> new OrganCommand(pRecord.getVolume());
            default -> throw new IllegalArgumentException("unknown synth id: " + pRecord.getSynthId());
        };
    }

    private static PlaybackSession openBandClientSession() throws IOException {
        BandClient bandClient = new BandClient();
        return new PlaybackSession() {
            @Override
            public void execute(SynthCommand pCommand) throws IOException {
                pCommand.execute(bandClient);
            }

            @Override
            public void close() {
                bandClient.close();
            }
        };
    }
}
