package app;

import app.ui.MainMenu;
import app.util.IConstants;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class powerBand {

    private powerBand() {
    }

    public static void main(String[] pArgs) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame(IConstants.WINDOW_TITLE);
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setContentPane(new MainMenu(window::dispose));
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }
}
