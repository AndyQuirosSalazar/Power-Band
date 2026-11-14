package app.ui;

import app.util.IConstants;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Objects;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public final class MainMenu extends JPanel {

    private static final Dimension MENU_SIZE = new Dimension(440, 360);
    private static final Dimension BUTTON_SIZE = new Dimension(220, 42);

    public MainMenu(Runnable pExitAction) {
        this(pExitAction, () -> { });
    }

    public MainMenu(Runnable pExitAction, Runnable pTestAction) {
        super(new GridBagLayout());
        Objects.requireNonNull(pExitAction, "pExitAction");
        Objects.requireNonNull(pTestAction, "pTestAction");
        setPreferredSize(MENU_SIZE);

        JLabel title = new JLabel(IConstants.WINDOW_TITLE, SwingConstants.CENTER);
        title.setForeground(IConstants.TITLE_COLOR);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 30f));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(0, 0, 18, 0);
        add(title, constraints);

        String[] buttonLabels = {
            IConstants.BUTTON_TEST,
            IConstants.BUTTON_RECORD,
            IConstants.BUTTON_PLAY,
            IConstants.BUTTON_EXIT
        };

        for (int index = 0; index < buttonLabels.length; index++) {
            JButton button = new JButton(buttonLabels[index]);
            button.setPreferredSize(BUTTON_SIZE);

            constraints.gridy = index + 1;
            constraints.insets = new Insets(6, 0, 6, 0);
            add(button, constraints);

            if (IConstants.BUTTON_EXIT.equals(buttonLabels[index])) {
                button.addActionListener(pEvent -> pExitAction.run());
            } else if (IConstants.BUTTON_TEST.equals(buttonLabels[index])) {
                button.addActionListener(pEvent -> pTestAction.run());
            }
        }
    }
}
