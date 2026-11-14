package app.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.model.TabletInput;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class TabletInputPanelTest {

    @Test
    void convertsLocalPointerEventsToNormalizedPenPositionAndTouchState()
            throws InvocationTargetException, InterruptedException {
        List<TabletInput> inputs = new ArrayList<>();

        SwingUtilities.invokeAndWait(() -> {
            TabletInputPanel panel = new TabletInputPanel(inputs::add);
            panel.setSize(100, 100);
            long now = System.currentTimeMillis();
            panel.dispatchEvent(new MouseEvent(
                    panel, MouseEvent.MOUSE_PRESSED, now, 0, 25, 75, 1, false, MouseEvent.BUTTON1));
            panel.dispatchEvent(new MouseEvent(
                    panel, MouseEvent.MOUSE_RELEASED, now + 1, 0, 25, 75, 1, false, MouseEvent.BUTTON1));
        });

        assertEquals(2, inputs.size());
        assertEquals(25f / 99f, inputs.get(0).getX(), 0.001f);
        assertEquals(75f / 99f, inputs.get(0).getY(), 0.001f);
        assertTrue(inputs.get(0).isTouching());
        assertFalse(inputs.get(1).isTouching());
    }
}
