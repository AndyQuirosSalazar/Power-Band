package app.ui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.util.IConstants;
import java.awt.Component;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class MainMenuTest {

    @Test
    void showsTheTitleAndButtonsInTheRequiredOrder() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(() -> {
            MainMenu menu = new MainMenu(() -> { });
            JLabel title = Arrays.stream(menu.getComponents())
                    .filter(JLabel.class::isInstance)
                    .map(JLabel.class::cast)
                    .findFirst()
                    .orElseThrow();
            List<JButton> buttons = getButtons(menu);

            assertEquals(IConstants.WINDOW_TITLE, title.getText());
            assertEquals(SwingConstants.CENTER, title.getHorizontalAlignment());
            assertEquals(IConstants.TITLE_COLOR, title.getForeground());
            assertEquals(
                    List.of(
                            IConstants.BUTTON_TEST,
                            IConstants.BUTTON_RECORD,
                            IConstants.BUTTON_PLAY,
                            IConstants.BUTTON_EXIT),
                    buttons.stream().map(JButton::getText).toList());
            assertTrue(buttons.stream().allMatch(Component::isVisible));
            assertTrue(buttons.stream().allMatch(JButton::isEnabled));
        });
    }

    @Test
    void testButtonRunsItsActionAndExitRunsItsAction()
            throws InvocationTargetException, InterruptedException {
        AtomicInteger exitActionCalls = new AtomicInteger();
        AtomicInteger testActionCalls = new AtomicInteger();

        SwingUtilities.invokeAndWait(() -> {
            MainMenu menu = new MainMenu(exitActionCalls::incrementAndGet, testActionCalls::incrementAndGet);
            List<JButton> buttons = getButtons(menu);

            assertDoesNotThrow(() -> buttons.get(0).doClick(0));
            assertEquals(1, testActionCalls.get());

            for (JButton button : buttons.subList(1, 3)) {
                assertDoesNotThrow(() -> button.doClick(0));
            }
            assertEquals(0, exitActionCalls.get());
            assertEquals(1, testActionCalls.get());

            buttons.get(3).doClick(0);
            assertEquals(1, exitActionCalls.get());
        });
    }

    private static List<JButton> getButtons(MainMenu pMenu) {
        return Arrays.stream(pMenu.getComponents())
                .filter(JButton.class::isInstance)
                .map(JButton.class::cast)
                .toList();
    }
}
