package app.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bridge.commands.BassCommand;
import app.logic.RecordingDeletionService;
import app.logic.RecordingPlayer;
import app.model.CommandRecord;
import app.model.RecordingMetadata;
import app.storage.InitializationResult;
import app.storage.PartialFilesDecision;
import app.storage.RecordingLibrary;
import app.util.IConstants;
import java.awt.Component;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.junit.jupiter.api.Test;

class RecordingPlaybackPanelTest {

    @Test
    void loadsChronologicalChoicesOffEdtAndPlaysOnlyTheConfirmedSelection() throws Exception {
        CountDownLatch listStarted = new CountDownLatch(1);
        CountDownLatch allowListToFinish = new CountDownLatch(1);
        CountDownLatch listLoaded = new CountDownLatch(1);
        CountDownLatch playbackFinished = new CountDownLatch(1);
        FakeRecordingLibrary library = new FakeRecordingLibrary(
                List.of(
                        new RecordingMetadata("first", Instant.parse("2026-11-17T20:00:00Z")),
                        new RecordingMetadata("second", Instant.parse("2026-11-17T20:00:01Z"))),
                List.of(new BassCommand(43, 250).toCommandRecord(0L)),
                listStarted,
                allowListToFinish);
        AtomicInteger sentCommands = new AtomicInteger();
        AtomicReference<RecordingPlaybackPanel> panelReference = new AtomicReference<>();
        RecordingPlaybackPanel.Interaction interaction = new FakeInteraction(playbackFinished);
        RecordingPlayer player = new RecordingPlayer(
                library,
                () -> pCommand -> sentCommands.incrementAndGet(),
                () -> 0L,
                pNanoseconds -> { });

        SwingUtilities.invokeAndWait(() -> panelReference.set(
                new RecordingPlaybackPanel(player, new RecordingDeletionService(library), () -> { }, interaction)));
        assertTrue(listStarted.await(5, TimeUnit.SECONDS));

        AtomicInteger edtAction = new AtomicInteger();
        SwingUtilities.invokeAndWait(edtAction::incrementAndGet);
        assertEquals(1, edtAction.get());

        SwingUtilities.invokeAndWait(() -> panelReference.get()
                .getRecordingList()
                .getModel()
                .addListDataListener(new ListDataListener() {
                    @Override
                    public void intervalAdded(ListDataEvent pEvent) {
                        listLoaded.countDown();
                    }

                    @Override
                    public void intervalRemoved(ListDataEvent pEvent) {
                    }

                    @Override
                    public void contentsChanged(ListDataEvent pEvent) {
                    }
                }));
        allowListToFinish.countDown();
        assertTrue(listLoaded.await(5, TimeUnit.SECONDS));

        SwingUtilities.invokeAndWait(() -> {
            RecordingPlaybackPanel panel = panelReference.get();
            assertEquals(
                    List.of("first", "second"),
                    java.util.stream.IntStream.range(0, panel.getRecordingList().getModel().getSize())
                            .mapToObj(index -> panel.getRecordingList().getModel().getElementAt(index).getName())
                            .toList());
            panel.getRecordingList().setSelectedIndex(1);
            assertTrue(panel.getPlayButton().isEnabled());
            panel.getPlayButton().doClick(0);
            assertFalse(panel.getPlayButton().isEnabled());
        });

        assertTrue(playbackFinished.await(5, TimeUnit.SECONDS));
        assertEquals("second", library.requestedName);
        assertEquals(1, sentCommands.get());
    }

    @Test
    void cancellingTheSelectorDoesNotStartPlayback() throws Exception {
        CountDownLatch finished = new CountDownLatch(1);
        FakeRecordingLibrary library = new FakeRecordingLibrary(
                List.of(new RecordingMetadata("saved", Instant.EPOCH)),
                List.of(new BassCommand(43, 250).toCommandRecord(0L)),
                new CountDownLatch(0),
                new CountDownLatch(0));
        AtomicInteger sessionOpenCount = new AtomicInteger();
        RecordingPlayer player = new RecordingPlayer(
                library,
                () -> {
                    sessionOpenCount.incrementAndGet();
                    return pCommand -> { };
                },
                () -> 0L,
                pNanoseconds -> { });
        AtomicReference<RecordingPlaybackPanel> panelReference = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> panelReference.set(
                new RecordingPlaybackPanel(
                        player,
                        new RecordingDeletionService(library),
                        finished::countDown,
                        new FakeInteraction(new CountDownLatch(0)))));
        SwingUtilities.invokeAndWait(() -> panelReference.get().getCancelButton().doClick(0));

        assertTrue(finished.await(5, TimeUnit.SECONDS));
        assertFalse(panelReference.get().getPlayButton().isEnabled());
        assertEquals(0, sessionOpenCount.get());
    }

    @Test
    void showsTheEmptyLibraryMessageWithoutOpeningPlayback() throws Exception {
        CountDownLatch emptyShown = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        FakeRecordingLibrary library = new FakeRecordingLibrary(
                List.of(),
                List.of(),
                new CountDownLatch(0),
                new CountDownLatch(0));
        RecordingPlayer player = new RecordingPlayer(
                library,
                () -> pCommand -> { },
                () -> 0L,
                pNanoseconds -> { });
        AtomicReference<RecordingPlaybackPanel> panelReference = new AtomicReference<>();
        RecordingPlaybackPanel.Interaction interaction = new FakeInteraction(null, emptyShown, null);

        SwingUtilities.invokeAndWait(() -> panelReference.set(
                new RecordingPlaybackPanel(player, new RecordingDeletionService(library), finished::countDown, interaction)));

        assertTrue(emptyShown.await(5, TimeUnit.SECONDS));
        assertTrue(finished.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(IConstants.PLAYBACK_EMPTY_MESSAGE, panelReference.get().getStatusText());
            assertFalse(panelReference.get().getPlayButton().isEnabled());
        });
    }

    @Test
    void reportsLibraryReadErrorsInsteadOfShowingAnEmptySuccess() throws Exception {
        CountDownLatch errorShown = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        FakeRecordingLibrary library = new FakeRecordingLibrary(
                List.of(),
                List.of(),
                new CountDownLatch(0),
                new CountDownLatch(0),
                new IOException("read failed"));
        RecordingPlayer player = new RecordingPlayer(
                library,
                () -> pCommand -> { },
                () -> 0L,
                pNanoseconds -> { });
        RecordingPlaybackPanel.Interaction interaction = new FakeInteraction(null, null, errorShown);

        SwingUtilities.invokeAndWait(() -> new RecordingPlaybackPanel(
                player,
                new RecordingDeletionService(library),
                finished::countDown,
                interaction));

        assertTrue(errorShown.await(5, TimeUnit.SECONDS));
        assertTrue(finished.await(5, TimeUnit.SECONDS));
    }

    @Test
    void cancellingDeleteConfirmationDoesNotCallTheDeletionService() throws Exception {
        CountDownLatch listStarted = new CountDownLatch(1);
        CountDownLatch allowListToFinish = new CountDownLatch(1);
        CountDownLatch listLoaded = new CountDownLatch(1);
        FakeRecordingLibrary library = new FakeRecordingLibrary(
                List.of(new RecordingMetadata("saved", Instant.EPOCH)),
                List.of(),
                listStarted,
                allowListToFinish);
        FakeInteraction interaction = new FakeInteraction(null);
        interaction.confirmDeleteResult = false;
        AtomicReference<RecordingPlaybackPanel> panelReference = new AtomicReference<>();
        RecordingPlayer player = new RecordingPlayer(library, () -> pCommand -> { }, () -> 0L, pTime -> { });

        SwingUtilities.invokeAndWait(() -> panelReference.set(
                new RecordingPlaybackPanel(player, new RecordingDeletionService(library), () -> { }, interaction)));
        assertTrue(listStarted.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> panelReference.get().getRecordingList().getModel()
                .addListDataListener(listLoadListener(listLoaded)));
        allowListToFinish.countDown();
        assertTrue(listLoaded.await(5, TimeUnit.SECONDS));

        SwingUtilities.invokeAndWait(() -> {
            RecordingPlaybackPanel panel = panelReference.get();
            assertFalse(panel.getDeleteButton().isEnabled());
            panel.getRecordingList().setSelectedIndex(0);
            assertTrue(panel.getDeleteButton().isEnabled());
            panel.getDeleteButton().doClick(0);
        });

        assertEquals(1, interaction.confirmCalls);
        assertEquals("saved", interaction.confirmationName);
        assertEquals(0, library.deleteCalls);
        SwingUtilities.invokeAndWait(() -> assertEquals(
                1,
                panelReference.get().getRecordingList().getModel().getSize()));
    }

    @Test
    void confirmedDeleteRemovesOnlyTheSelectedRecordingAndRefreshesTheList() throws Exception {
        CountDownLatch listStarted = new CountDownLatch(1);
        CountDownLatch allowListToFinish = new CountDownLatch(1);
        CountDownLatch listLoaded = new CountDownLatch(1);
        CountDownLatch deletionShown = new CountDownLatch(1);
        FakeRecordingLibrary library = new FakeRecordingLibrary(
                List.of(
                        new RecordingMetadata("first", Instant.EPOCH),
                        new RecordingMetadata("second", Instant.EPOCH.plusSeconds(1))),
                List.of(),
                listStarted,
                allowListToFinish);
        FakeInteraction interaction = new FakeInteraction(null);
        interaction.confirmDeleteResult = true;
        interaction.deletionShown = deletionShown;
        AtomicReference<RecordingPlaybackPanel> panelReference = new AtomicReference<>();
        RecordingPlayer player = new RecordingPlayer(library, () -> pCommand -> { }, () -> 0L, pTime -> { });

        SwingUtilities.invokeAndWait(() -> panelReference.set(
                new RecordingPlaybackPanel(player, new RecordingDeletionService(library), () -> { }, interaction)));
        assertTrue(listStarted.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> panelReference.get().getRecordingList().getModel()
                .addListDataListener(listLoadListener(listLoaded)));
        allowListToFinish.countDown();
        assertTrue(listLoaded.await(5, TimeUnit.SECONDS));

        SwingUtilities.invokeAndWait(() -> {
            RecordingPlaybackPanel panel = panelReference.get();
            panel.getRecordingList().setSelectedIndex(1);
            panel.getDeleteButton().doClick(0);
        });

        assertTrue(deletionShown.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
            RecordingPlaybackPanel panel = panelReference.get();
            assertEquals(1, panel.getRecordingList().getModel().getSize());
            assertEquals("first", panel.getRecordingList().getModel().getElementAt(0).getName());
            assertEquals(IConstants.PLAYBACK_SELECT_MESSAGE, panel.getStatusText());
            assertFalse(panel.getDeleteButton().isEnabled());
        });
        assertEquals("second", library.deletedName);
        assertEquals(1, library.deleteCalls);
        assertEquals("second", interaction.deletedName);
        assertEquals("second", interaction.confirmationName);
    }

    @Test
    void reportsDeleteErrorsAndKeepsTheRecordingVisible() throws Exception {
        CountDownLatch listStarted = new CountDownLatch(1);
        CountDownLatch allowListToFinish = new CountDownLatch(1);
        CountDownLatch listLoaded = new CountDownLatch(1);
        CountDownLatch errorShown = new CountDownLatch(1);
        FakeRecordingLibrary library = new FakeRecordingLibrary(
                List.of(new RecordingMetadata("saved", Instant.EPOCH)),
                List.of(),
                listStarted,
                allowListToFinish);
        library.deleteError = new IOException("disk unavailable");
        FakeInteraction interaction = new FakeInteraction(null);
        interaction.confirmDeleteResult = true;
        interaction.deleteErrorShown = errorShown;
        AtomicReference<RecordingPlaybackPanel> panelReference = new AtomicReference<>();
        RecordingPlayer player = new RecordingPlayer(library, () -> pCommand -> { }, () -> 0L, pTime -> { });

        SwingUtilities.invokeAndWait(() -> panelReference.set(
                new RecordingPlaybackPanel(player, new RecordingDeletionService(library), () -> { }, interaction)));
        assertTrue(listStarted.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> panelReference.get().getRecordingList().getModel()
                .addListDataListener(listLoadListener(listLoaded)));
        allowListToFinish.countDown();
        assertTrue(listLoaded.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
            panelReference.get().getRecordingList().setSelectedIndex(0);
            panelReference.get().getDeleteButton().doClick(0);
        });

        assertTrue(errorShown.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> assertEquals(
                1,
                panelReference.get().getRecordingList().getModel().getSize()));
        assertEquals("disk unavailable", interaction.deleteError.getMessage());
    }

    private static ListDataListener listLoadListener(CountDownLatch pListLoaded) {
        return new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent pEvent) {
                pListLoaded.countDown();
            }

            @Override
            public void intervalRemoved(ListDataEvent pEvent) {
            }

            @Override
            public void contentsChanged(ListDataEvent pEvent) {
            }
        };
    }

    private static final class FakeRecordingLibrary implements RecordingLibrary {

        private final List<RecordingMetadata> recordings;
        private final List<CommandRecord> commands;
        private final CountDownLatch listStarted;
        private final CountDownLatch allowListToFinish;
        private final IOException listError;
        private IOException deleteError;
        private int deleteCalls;
        private String deletedName;
        private String requestedName;

        private FakeRecordingLibrary(
                List<RecordingMetadata> pRecordings,
                List<CommandRecord> pCommands,
                CountDownLatch pListStarted,
                CountDownLatch pAllowListToFinish) {
            this(pRecordings, pCommands, pListStarted, pAllowListToFinish, null);
        }

        private FakeRecordingLibrary(
                List<RecordingMetadata> pRecordings,
                List<CommandRecord> pCommands,
                CountDownLatch pListStarted,
                CountDownLatch pAllowListToFinish,
                IOException pListError) {
            recordings = new ArrayList<>(pRecordings);
            commands = pCommands;
            listStarted = pListStarted;
            allowListToFinish = pAllowListToFinish;
            listError = pListError;
        }

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
            return recordings.removeIf(pRecording -> pName.equals(pRecording.getName()));
        }

        @Override
        public List<RecordingMetadata> listRecordings() throws IOException {
            listStarted.countDown();
            try {
                if (!allowListToFinish.await(5, TimeUnit.SECONDS)) {
                    throw new IOException("timed out waiting to release fake recording list");
                }
            } catch (InterruptedException pError) {
                Thread.currentThread().interrupt();
                throw new IOException("fake recording list was interrupted", pError);
            }
            if (listError != null) {
                throw listError;
            }
            return recordings;
        }

        @Override
        public Optional<List<CommandRecord>> readRecording(String pName) {
            requestedName = pName;
            return Optional.of(commands);
        }
    }

    private static final class FakeInteraction implements RecordingPlaybackPanel.Interaction {

        private final CountDownLatch playbackFinished;
        private final CountDownLatch emptyShown;
        private final CountDownLatch errorShown;
        private CountDownLatch deletionShown;
        private CountDownLatch deleteErrorShown;
        private boolean confirmDeleteResult;
        private int confirmCalls;
        private String confirmationName;
        private String deletedName;
        private Throwable deleteError;

        private FakeInteraction(CountDownLatch pPlaybackFinished) {
            this(pPlaybackFinished, null, null);
        }

        private FakeInteraction(
                CountDownLatch pPlaybackFinished,
                CountDownLatch pEmptyShown,
                CountDownLatch pErrorShown) {
            playbackFinished = pPlaybackFinished;
            emptyShown = pEmptyShown;
            errorShown = pErrorShown;
        }

        @Override
        public void showEmpty(Component pParent) {
            if (emptyShown == null) {
                throw new AssertionError("unexpected empty library");
            }
            emptyShown.countDown();
        }

        @Override
        public void showError(Component pParent, Throwable pError) {
            if (errorShown != null) {
                errorShown.countDown();
                return;
            }
            throw new AssertionError(pError);
        }

        @Override
        public void showCompleted(Component pParent, String pName) {
            if (playbackFinished == null) {
                throw new AssertionError("unexpected playback completion");
            }
            playbackFinished.countDown();
        }

        @Override
        public void showNotFound(Component pParent, String pName) {
            throw new AssertionError("unexpected not-found result for " + pName);
        }

        @Override
        public boolean confirmDelete(Component pParent, String pName) {
            confirmCalls++;
            confirmationName = pName;
            return confirmDeleteResult;
        }

        @Override
        public void showDeleted(Component pParent, String pName) {
            deletedName = pName;
            if (deletionShown == null) {
                throw new AssertionError("unexpected deletion of " + pName);
            }
            deletionShown.countDown();
        }

        @Override
        public void showDeleteError(Component pParent, Throwable pError) {
            deleteError = pError;
            if (deleteErrorShown == null) {
                throw new AssertionError(pError);
            }
            deleteErrorShown.countDown();
        }
    }
}
