package app.ui;

import app.logic.RecordingSaveService;
import app.model.CommandRecord;
import app.storage.PartialFilesDecision;
import app.util.IConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

public final class RecordingSavePanel extends JPanel {

    public interface Interaction {

        void showWarning(Component pParent, String pMessage);

        PartialFilesDecision choosePartialFilesDecision(Component pParent);

        void showSaved(Component pParent, String pName);

        void showError(Component pParent, Throwable pError);
    }

    private final RecordingSaveService saveService;
    private final List<CommandRecord> commands;
    private final Instant createdAt;
    private final Interaction interaction;
    private final Runnable finishedAction;
    private final JTextField nameField = new JTextField(24);
    private final JButton saveButton = new JButton(IConstants.BUTTON_SAVE);
    private final JButton cancelButton = new JButton(IConstants.BUTTON_CANCEL);

    public RecordingSavePanel(
            RecordingSaveService pSaveService,
            List<CommandRecord> pCommands,
            Instant pCreatedAt,
            Runnable pFinishedAction) {
        this(pSaveService, pCommands, pCreatedAt, pFinishedAction, new SwingInteraction());
    }

    public RecordingSavePanel(
            RecordingSaveService pSaveService,
            List<CommandRecord> pCommands,
            Instant pCreatedAt,
            Runnable pFinishedAction,
            Interaction pInteraction) {
        super(new BorderLayout(8, 8));
        saveService = Objects.requireNonNull(pSaveService, "pSaveService");
        commands = List.copyOf(Objects.requireNonNull(pCommands, "pCommands"));
        createdAt = Objects.requireNonNull(pCreatedAt, "pCreatedAt");
        finishedAction = Objects.requireNonNull(pFinishedAction, "pFinishedAction");
        interaction = Objects.requireNonNull(pInteraction, "pInteraction");

        JPanel nameInput = new JPanel(new GridLayout(0, 1, 4, 4));
        nameInput.add(new JLabel("Nombre de la grabación:"));
        nameInput.add(nameField);
        add(nameInput, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        actions.add(saveButton);
        actions.add(cancelButton);
        add(actions, BorderLayout.SOUTH);

        saveButton.addActionListener(pEvent -> save(null));
        cancelButton.addActionListener(pEvent -> finishedAction.run());
    }

    private void save(PartialFilesDecision pDecision) {
        String name = RecordingSaveService.normalizeName(nameField.getText());
        nameField.setText(name == null ? "" : name);
        setBusy(true);

        new SwingWorker<RecordingSaveService.Result, Void>() {
            @Override
            protected RecordingSaveService.Result doInBackground() throws IOException {
                return saveService.save(name, commands, createdAt, pDecision);
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    handleResult(get(), name);
                } catch (InterruptedException pError) {
                    Thread.currentThread().interrupt();
                    interaction.showError(
                            RecordingSavePanel.this,
                            new IOException("saving the recording was interrupted", pError));
                } catch (ExecutionException pError) {
                    Throwable cause = pError.getCause();
                    interaction.showError(RecordingSavePanel.this, cause == null ? pError : cause);
                }
            }
        }.execute();
    }

    private void handleResult(RecordingSaveService.Result pResult, String pName) {
        switch (pResult) {
            case INVALID_NAME ->
                interaction.showWarning(
                        this,
                        "El nombre debe tener de 1 a 31 caracteres ASCII imprimibles.");
            case DUPLICATE_NAME ->
                interaction.showWarning(this, "Ya existe una grabación con ese nombre.");
            case PARTIAL_FILES_DECISION_REQUIRED -> {
                PartialFilesDecision decision = interaction.choosePartialFilesDecision(this);
                if (decision == null || decision == PartialFilesDecision.CANCEL) {
                    finishedAction.run();
                } else {
                    save(decision);
                }
            }
            case CANCELLED -> finishedAction.run();
            case SAVED -> {
                interaction.showSaved(this, pName);
                finishedAction.run();
            }
        }
    }

    private void setBusy(boolean pBusy) {
        saveButton.setEnabled(!pBusy);
        cancelButton.setEnabled(!pBusy);
        nameField.setEnabled(!pBusy);
    }

    private static final class SwingInteraction implements Interaction {

        private static final String[] PARTIAL_FILE_OPTIONS = {
            "Borrar los tres archivos y empezar de nuevo",
            "Crear solo los archivos faltantes",
            "Cancelar sin cambios"
        };

        @Override
        public void showWarning(Component pParent, String pMessage) {
            JOptionPane.showMessageDialog(pParent, pMessage, "Nombre de grabación", JOptionPane.WARNING_MESSAGE);
        }

        @Override
        public PartialFilesDecision choosePartialFilesDecision(Component pParent) {
            int choice = JOptionPane.showOptionDialog(
                    pParent,
                    "La biblioteca de grabaciones está incompleta. Elige cómo continuar.",
                    "Archivos de grabaciones incompletos",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    PARTIAL_FILE_OPTIONS,
                    PARTIAL_FILE_OPTIONS[2]);
            return switch (choice) {
                case 0 -> PartialFilesDecision.RESET;
                case 1 -> PartialFilesDecision.CREATE_MISSING;
                default -> PartialFilesDecision.CANCEL;
            };
        }

        @Override
        public void showSaved(Component pParent, String pName) {
            JOptionPane.showMessageDialog(
                    pParent,
                    "La grabación \"" + pName + "\" se guardó correctamente.",
                    IConstants.BUTTON_SAVE,
                    JOptionPane.INFORMATION_MESSAGE);
        }

        @Override
        public void showError(Component pParent, Throwable pError) {
            String detail = pError.getMessage() == null ? pError.toString() : pError.getMessage();
            JOptionPane.showMessageDialog(
                    pParent,
                    "No se pudo guardar la grabación: " + detail,
                    IConstants.BUTTON_SAVE,
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
