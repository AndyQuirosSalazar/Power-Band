package app.ui;

import app.logic.RecordingDeletionService;
import app.logic.RecordingPlayer;
import app.model.RecordingMetadata;
import app.util.IConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;

public final class RecordingPlaybackPanel extends JPanel {

    public interface Interaction {

        void showEmpty(Component pParent);

        void showError(Component pParent, Throwable pError);

        void showCompleted(Component pParent, String pName);

        void showNotFound(Component pParent, String pName);

        boolean confirmDelete(Component pParent, String pName);

        void showDeleted(Component pParent, String pName);

        void showDeleteError(Component pParent, Throwable pError);
    }

    private final RecordingPlayer player;
    private final RecordingDeletionService deletionService;
    private final Runnable finishedAction;
    private final Interaction interaction;
    private final DefaultListModel<RecordingMetadata> recordingModel = new DefaultListModel<>();
    private final JList<RecordingMetadata> recordingList = new JList<>(recordingModel);
    private final JButton playButton = new JButton(IConstants.BUTTON_PLAY);
    private final JButton deleteButton = new JButton(IConstants.BUTTON_DELETE);
    private final JButton cancelButton = new JButton(IConstants.BUTTON_CANCEL);
    private final JLabel status = new JLabel(IConstants.PLAYBACK_LOADING_MESSAGE);
    private final DefaultListCellRenderer listCellRenderer = new DefaultListCellRenderer();

    private SwingWorker<List<RecordingMetadata>, Void> loadWorker;
    private SwingWorker<RecordingPlayer.Result, Void> playWorker;
    private SwingWorker<RecordingDeletionService.Result, Void> deleteWorker;
    private boolean loading = true;

    public RecordingPlaybackPanel(
            RecordingPlayer pPlayer,
            RecordingDeletionService pDeletionService,
            Runnable pFinishedAction) {
        this(pPlayer, pDeletionService, pFinishedAction, new SwingInteraction());
    }

    public RecordingPlaybackPanel(
            RecordingPlayer pPlayer,
            RecordingDeletionService pDeletionService,
            Runnable pFinishedAction,
            Interaction pInteraction) {
        super(new BorderLayout(8, 8));
        player = Objects.requireNonNull(pPlayer, "pPlayer");
        deletionService = Objects.requireNonNull(pDeletionService, "pDeletionService");
        finishedAction = Objects.requireNonNull(pFinishedAction, "pFinishedAction");
        interaction = Objects.requireNonNull(pInteraction, "pInteraction");

        recordingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recordingList.setCellRenderer((pList, pValue, pIndex, pSelected, pFocus) -> {
            JLabel label = (JLabel) listCellRenderer
                    .getListCellRendererComponent(pList, pValue, pIndex, pSelected, pFocus);
            label.setText(pValue.getName() + " — " + pValue.getCreatedAt());
            return label;
        });
        recordingList.addListSelectionListener(this::selectionChanged);
        add(status, BorderLayout.NORTH);
        add(new JScrollPane(recordingList), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        actions.add(playButton);
        actions.add(deleteButton);
        actions.add(cancelButton);
        add(actions, BorderLayout.SOUTH);

        playButton.setEnabled(false);
        deleteButton.setEnabled(false);
        playButton.addActionListener(pEvent -> playSelected());
        deleteButton.addActionListener(pEvent -> deleteSelected());
        cancelButton.addActionListener(pEvent -> cancelAndClose());
        loadRecordings();
    }

    void cancelWork() {
        if (loadWorker != null && !loadWorker.isDone()) {
            loadWorker.cancel(true);
        }
        if (playWorker != null && !playWorker.isDone()) {
            playWorker.cancel(true);
        }
        if (deleteWorker != null && !deleteWorker.isDone()) {
            deleteWorker.cancel(true);
        }
    }

    JList<RecordingMetadata> getRecordingList() {
        return recordingList;
    }

    JButton getPlayButton() {
        return playButton;
    }

    JButton getDeleteButton() {
        return deleteButton;
    }

    JButton getCancelButton() {
        return cancelButton;
    }

    String getStatusText() {
        return status.getText();
    }

    private void loadRecordings() {
        loadWorker = new SwingWorker<>() {
            @Override
            protected List<RecordingMetadata> doInBackground() throws IOException {
                return player.listRecordings();
            }

            @Override
            protected void done() {
                loading = false;
                try {
                    List<RecordingMetadata> recordings = get();
                    if (recordings.isEmpty()) {
                        status.setText(IConstants.PLAYBACK_EMPTY_MESSAGE);
                        interaction.showEmpty(RecordingPlaybackPanel.this);
                        finishedAction.run();
                        return;
                    }
                    for (RecordingMetadata recording : recordings) {
                        recordingModel.addElement(recording);
                    }
                    status.setText(IConstants.PLAYBACK_SELECT_MESSAGE);
                    updateActionButtons();
                } catch (InterruptedException pError) {
                    Thread.currentThread().interrupt();
                    interaction.showError(RecordingPlaybackPanel.this, pError);
                    finishedAction.run();
                } catch (ExecutionException pError) {
                    interaction.showError(RecordingPlaybackPanel.this, rootCause(pError));
                    finishedAction.run();
                } catch (CancellationException pError) {
                    finishedAction.run();
                }
            }
        };
        loadWorker.execute();
    }

    private void selectionChanged(ListSelectionEvent pEvent) {
        if (!pEvent.getValueIsAdjusting()) {
            updateActionButtons();
        }
    }

    private void playSelected() {
        RecordingMetadata selected = recordingList.getSelectedValue();
        if (loading || selected == null || isBusy()) {
            return;
        }
        setPlaying(true);
        status.setText(IConstants.PLAYBACK_ACTIVE_MESSAGE);
        playWorker = new SwingWorker<>() {
            @Override
            protected RecordingPlayer.Result doInBackground() throws IOException {
                return player.play(selected.getName());
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    return;
                }
                setPlaying(false);
                try {
                    switch (get()) {
                        case COMPLETED -> {
                            status.setText(IConstants.PLAYBACK_COMPLETED_MESSAGE);
                            interaction.showCompleted(RecordingPlaybackPanel.this, selected.getName());
                        }
                        case NOT_FOUND -> {
                            status.setText(IConstants.PLAYBACK_SELECT_MESSAGE);
                            interaction.showNotFound(RecordingPlaybackPanel.this, selected.getName());
                        }
                        case CANCELLED -> status.setText(IConstants.PLAYBACK_CANCELLED_MESSAGE);
                    }
                } catch (InterruptedException pError) {
                    Thread.currentThread().interrupt();
                    interaction.showError(RecordingPlaybackPanel.this, pError);
                } catch (ExecutionException pError) {
                    interaction.showError(RecordingPlaybackPanel.this, rootCause(pError));
                } catch (CancellationException pError) {
                    status.setText(IConstants.PLAYBACK_CANCELLED_MESSAGE);
                } finally {
                    updateActionButtons();
                }
            }
        };
        updateActionButtons();
        playWorker.execute();
    }

    private void deleteSelected() {
        RecordingMetadata selected = recordingList.getSelectedValue();
        if (loading || selected == null || isBusy()
                || !interaction.confirmDelete(this, selected.getName())) {
            return;
        }

        setDeleting(true);
        status.setText(IConstants.PLAYBACK_DELETING_MESSAGE);
        deleteWorker = new SwingWorker<>() {
            @Override
            protected RecordingDeletionService.Result doInBackground() throws IOException {
                return deletionService.delete(selected.getName());
            }

            @Override
            protected void done() {
                setDeleting(false);
                try {
                    switch (get()) {
                        case DELETED -> {
                            recordingModel.removeElement(selected);
                            status.setText(recordingModel.isEmpty()
                                    ? IConstants.PLAYBACK_EMPTY_MESSAGE
                                    : IConstants.PLAYBACK_SELECT_MESSAGE);
                            interaction.showDeleted(RecordingPlaybackPanel.this, selected.getName());
                        }
                        case NOT_FOUND -> {
                            recordingModel.removeElement(selected);
                            status.setText(recordingModel.isEmpty()
                                    ? IConstants.PLAYBACK_EMPTY_MESSAGE
                                    : IConstants.PLAYBACK_SELECT_MESSAGE);
                            interaction.showNotFound(RecordingPlaybackPanel.this, selected.getName());
                        }
                    }
                } catch (InterruptedException pError) {
                    Thread.currentThread().interrupt();
                    status.setText(IConstants.PLAYBACK_SELECT_MESSAGE);
                    interaction.showDeleteError(RecordingPlaybackPanel.this, pError);
                } catch (ExecutionException pError) {
                    status.setText(IConstants.PLAYBACK_SELECT_MESSAGE);
                    interaction.showDeleteError(RecordingPlaybackPanel.this, rootCause(pError));
                } catch (CancellationException pError) {
                    status.setText(IConstants.PLAYBACK_SELECT_MESSAGE);
                } finally {
                    updateActionButtons();
                }
            }
        };
        updateActionButtons();
        deleteWorker.execute();
    }

    private void cancelAndClose() {
        cancelWork();
        finishedAction.run();
    }

    private boolean isPlaying() {
        return playWorker != null && !playWorker.isDone();
    }

    private boolean isDeleting() {
        return deleteWorker != null && !deleteWorker.isDone();
    }

    private boolean isBusy() {
        return isPlaying() || isDeleting();
    }

    private void setPlaying(boolean pPlaying) {
        recordingList.setEnabled(!pPlaying);
        cancelButton.setEnabled(true);
        updateActionButtons();
    }

    private void setDeleting(boolean pDeleting) {
        recordingList.setEnabled(!pDeleting);
        cancelButton.setEnabled(true);
        updateActionButtons();
    }

    private void updateActionButtons() {
        boolean actionAvailable = !loading && !isBusy() && recordingList.getSelectedValue() != null;
        playButton.setEnabled(actionAvailable);
        deleteButton.setEnabled(actionAvailable);
    }

    private static Throwable rootCause(ExecutionException pError) {
        Throwable cause = pError.getCause();
        return cause == null ? pError : cause;
    }

    private static final class SwingInteraction implements Interaction {

        @Override
        public void showEmpty(Component pParent) {
            JOptionPane.showMessageDialog(
                    pParent,
                    IConstants.PLAYBACK_EMPTY_MESSAGE,
                    IConstants.BUTTON_PLAY,
                    JOptionPane.INFORMATION_MESSAGE);
        }

        @Override
        public void showError(Component pParent, Throwable pError) {
            String detail = pError.getMessage() == null ? pError.toString() : pError.getMessage();
            JOptionPane.showMessageDialog(
                    pParent,
                    IConstants.PLAYBACK_ERROR_MESSAGE_PREFIX + detail,
                    IConstants.BUTTON_PLAY,
                    JOptionPane.ERROR_MESSAGE);
        }

        @Override
        public void showCompleted(Component pParent, String pName) {
            JOptionPane.showMessageDialog(
                    pParent,
                    IConstants.PLAYBACK_COMPLETED_MESSAGE_PREFIX
                            + pName
                            + IConstants.PLAYBACK_COMPLETED_MESSAGE_SUFFIX,
                    IConstants.BUTTON_PLAY,
                    JOptionPane.INFORMATION_MESSAGE);
        }

        @Override
        public void showNotFound(Component pParent, String pName) {
            JOptionPane.showMessageDialog(
                    pParent,
                    IConstants.PLAYBACK_NOT_FOUND_MESSAGE_PREFIX
                            + pName
                            + IConstants.PLAYBACK_NOT_FOUND_MESSAGE_SUFFIX,
                    IConstants.PLAYBACK_DIALOG_TITLE,
                    JOptionPane.WARNING_MESSAGE);
        }

        @Override
        public boolean confirmDelete(Component pParent, String pName) {
            Object[] options = { IConstants.BUTTON_CANCEL, IConstants.BUTTON_CONFIRM };
            int choice = JOptionPane.showOptionDialog(
                    pParent,
                    IConstants.PLAYBACK_DELETE_CONFIRM_PREFIX
                            + pName
                            + IConstants.PLAYBACK_DELETE_CONFIRM_SUFFIX,
                    IConstants.PLAYBACK_DELETE_CONFIRM_TITLE,
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    IConstants.BUTTON_CANCEL);
            return choice == 1;
        }

        @Override
        public void showDeleted(Component pParent, String pName) {
            JOptionPane.showMessageDialog(
                    pParent,
                    IConstants.PLAYBACK_DELETE_COMPLETED_MESSAGE_PREFIX
                            + pName
                            + IConstants.PLAYBACK_DELETE_COMPLETED_MESSAGE_SUFFIX,
                    IConstants.BUTTON_DELETE,
                    JOptionPane.INFORMATION_MESSAGE);
        }

        @Override
        public void showDeleteError(Component pParent, Throwable pError) {
            String detail = pError.getMessage() == null ? pError.toString() : pError.getMessage();
            JOptionPane.showMessageDialog(
                    pParent,
                    IConstants.PLAYBACK_DELETE_ERROR_MESSAGE_PREFIX + detail,
                    IConstants.BUTTON_DELETE,
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
