package app.ui;

import app.logic.RecordingDeletionService;
import app.logic.RecordingPlayer;
import app.util.IConstants;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import javax.swing.JDialog;
import javax.swing.WindowConstants;

public final class RecordingPlaybackDialog extends JDialog {

    private final RecordingPlaybackPanel playbackPanel;

    public RecordingPlaybackDialog(
            Window pOwner,
            RecordingPlayer pPlayer,
            RecordingDeletionService pDeletionService) {
        super(pOwner, IConstants.PLAYBACK_DIALOG_TITLE, ModalityType.MODELESS);
        Objects.requireNonNull(pPlayer, "pPlayer");
        Objects.requireNonNull(pDeletionService, "pDeletionService");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        playbackPanel = new RecordingPlaybackPanel(pPlayer, pDeletionService, this::dispose);
        setContentPane(playbackPanel);
        setSize(440, 320);
        setLocationRelativeTo(pOwner);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent pEvent) {
                playbackPanel.cancelWork();
            }
        });
    }
}
