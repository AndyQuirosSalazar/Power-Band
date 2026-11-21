package app.ui;

import app.logic.RecordingSaveService;
import app.model.CommandRecord;
import app.storage.RecordingLibrary;
import java.awt.Dialog;
import java.awt.Window;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import javax.swing.JDialog;
import javax.swing.WindowConstants;

public final class RecordingSaveDialog {

    private RecordingSaveDialog() {
    }

    public static void showDialog(
            Window pOwner,
            RecordingLibrary pLibrary,
            List<CommandRecord> pCommands,
            Instant pCreatedAt) {
        Objects.requireNonNull(pLibrary, "pLibrary");
        JDialog dialog = new JDialog(pOwner, "Guardar grabación", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        RecordingSavePanel panel = new RecordingSavePanel(
                new RecordingSaveService(pLibrary),
                pCommands,
                pCreatedAt,
                dialog::dispose);
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(pOwner);
        dialog.setVisible(true);
    }
}
