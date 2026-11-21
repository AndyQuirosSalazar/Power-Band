package app.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.logic.RecordingSaveService;
import app.model.CommandRecord;
import app.model.RecordingMetadata;
import app.storage.InitializationResult;
import app.storage.PartialFilesDecision;
import app.storage.RecordingLibrary;
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
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class RecordingSavePanelTest {

    @Test
    void validatesNamesKeepsTheRecordingForCorrectionsAndSavesOnlyAfterSuccess() throws Exception {
        FakeRecordingLibrary library = new FakeRecordingLibrary();
        library.savedNames.add("Piano");
        TestInteraction interaction = new TestInteraction();
        CountDownLatch duplicateWarning = new CountDownLatch(1);
        CountDownLatch invalidNameWarning = new CountDownLatch(1);
        CountDownLatch saved = new CountDownLatch(1);
        interaction.warningLatch = duplicateWarning;
        interaction.savedAction = saved::countDown;
        AtomicInteger finished = new AtomicInteger();
        AtomicReference<RecordingSavePanel> panelReference = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> panelReference.set(new RecordingSavePanel(
                new RecordingSaveService(library),
                List.of(),
                Instant.EPOCH,
                finished::incrementAndGet,
                interaction)));

        SwingUtilities.invokeAndWait(() -> {
            setNameAndSave(panelReference.get(), " Piano ");
        });
        await(duplicateWarning);
        interaction.warningLatch = invalidNameWarning;
        SwingUtilities.invokeAndWait(() -> setNameAndSave(panelReference.get(), "música"));
        await(invalidNameWarning);
        SwingUtilities.invokeAndWait(() -> setNameAndSave(panelReference.get(), " piano "));
        await(saved);

        assertEquals(List.of("Piano", "piano"), library.savedNames);
        assertEquals(1, library.addCalls);
        assertEquals(1, finished.get());
    }

    @Test
    void decliningPartialInitializationDoesNotWrite() throws Exception {
        FakeRecordingLibrary library = new FakeRecordingLibrary();
        library.initializationResult = InitializationResult.DECISION_REQUIRED;
        TestInteraction interaction = new TestInteraction();
        interaction.partialDecision = PartialFilesDecision.CANCEL;
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<RecordingSavePanel> panelReference = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> panelReference.set(new RecordingSavePanel(
                new RecordingSaveService(library),
                List.of(),
                Instant.EPOCH,
                finished::countDown,
                interaction)));
        SwingUtilities.invokeAndWait(() -> setNameAndSave(panelReference.get(), "Jam"));
        await(finished);
        assertEquals(0, library.addCalls);
        assertTrue(library.savedNames.isEmpty());
        assertEquals(1, library.initializeCalls);
    }

    @Test
    void cancelButtonDoesNotInitializeOrWrite() throws Exception {
        FakeRecordingLibrary library = new FakeRecordingLibrary();
        AtomicInteger finished = new AtomicInteger();
        AtomicReference<RecordingSavePanel> panelReference = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> panelReference.set(new RecordingSavePanel(
                new RecordingSaveService(library),
                List.of(),
                Instant.EPOCH,
                finished::incrementAndGet,
                new TestInteraction())));
        SwingUtilities.invokeAndWait(() -> findButton(panelReference.get(), "Cancelar").doClick(0));

        assertEquals(0, library.initializeCalls);
        assertEquals(0, library.addCalls);
        assertTrue(library.savedNames.isEmpty());
        assertEquals(1, finished.get());
    }

    @Test
    void reportsStorageErrorsWithoutShowingSuccess() throws Exception {
        FakeRecordingLibrary library = new FakeRecordingLibrary();
        library.addError = new IOException("disk unavailable");
        TestInteraction interaction = new TestInteraction();
        CountDownLatch error = new CountDownLatch(1);
        interaction.errorAction = error::countDown;
        AtomicReference<RecordingSavePanel> panelReference = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> panelReference.set(new RecordingSavePanel(
                new RecordingSaveService(library),
                List.of(),
                Instant.EPOCH,
                () -> { },
                interaction)));
        SwingUtilities.invokeAndWait(() -> setNameAndSave(panelReference.get(), "Jam"));
        await(error);

        assertEquals(0, interaction.successCount);
        assertTrue(interaction.error instanceof IOException);
        assertEquals(1, library.addCalls);
    }

    private static void setNameAndSave(RecordingSavePanel pPanel, String pName) {
        findTextField(pPanel).setText(pName);
        findButton(pPanel, "Guardar").doClick(0);
    }

    private static JTextField findTextField(RecordingSavePanel pPanel) {
        return findComponent(pPanel, JTextField.class);
    }

    private static JButton findButton(RecordingSavePanel pPanel, String pText) {
        return components(pPanel).stream()
                .filter(JButton.class::isInstance)
                .map(JButton.class::cast)
                .filter(pButton -> pText.equals(pButton.getText()))
                .findFirst()
                .orElseThrow();
    }

    private static <T extends Component> T findComponent(RecordingSavePanel pPanel, Class<T> pType) {
        return components(pPanel).stream()
                .filter(pType::isInstance)
                .map(pType::cast)
                .findFirst()
                .orElseThrow();
    }

    private static List<Component> components(Component pRoot) {
        List<Component> result = new ArrayList<>();
        result.add(pRoot);
        if (pRoot instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                result.addAll(components(child));
            }
        }
        return result;
    }

    private static void await(CountDownLatch pLatch) throws InterruptedException {
        assertTrue(pLatch.await(3, TimeUnit.SECONDS));
    }

    private static final class TestInteraction implements RecordingSavePanel.Interaction {

        private Runnable savedAction = () -> { };
        private Runnable errorAction = () -> { };
        private CountDownLatch warningLatch = new CountDownLatch(0);
        private PartialFilesDecision partialDecision = PartialFilesDecision.CANCEL;
        private Throwable error;
        private int successCount;

        @Override
        public void showWarning(Component pParent, String pMessage) {
            warningLatch.countDown();
        }

        @Override
        public PartialFilesDecision choosePartialFilesDecision(Component pParent) {
            return partialDecision;
        }

        @Override
        public void showSaved(Component pParent, String pName) {
            successCount++;
            savedAction.run();
        }

        @Override
        public void showError(Component pParent, Throwable pError) {
            error = pError;
            errorAction.run();
        }
    }

    private static final class FakeRecordingLibrary implements RecordingLibrary {

        private final List<String> savedNames = new ArrayList<>();
        private InitializationResult initializationResult = InitializationResult.READY;
        private IOException addError;
        private int initializeCalls;
        private int addCalls;

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
        public void addRecording(String pName, Instant pCreatedAt, List<CommandRecord> pCommands)
                throws IOException {
            addCalls++;
            if (addError != null) {
                throw addError;
            }
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
