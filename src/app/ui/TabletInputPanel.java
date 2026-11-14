package app.ui;

import app.model.TabletInput;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.JPanel;

public final class TabletInputPanel extends JPanel {

    private static final Dimension PANEL_SIZE = new Dimension(360, 260);

    private final Consumer<TabletInput> inputConsumer;
    private int pointerX;
    private int pointerY;
    private boolean touching;

    public TabletInputPanel(Consumer<TabletInput> pInputConsumer) {
        inputConsumer = Objects.requireNonNull(pInputConsumer, "pInputConsumer");
        setPreferredSize(PANEL_SIZE);
        setBackground(Color.WHITE);
        setFocusable(false);

        MouseAdapter pointerListener = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent pEvent) {
                reportPointer(pEvent, false);
            }

            @Override
            public void mouseDragged(MouseEvent pEvent) {
                reportPointer(pEvent, true);
            }

            @Override
            public void mousePressed(MouseEvent pEvent) {
                reportPointer(pEvent, true);
            }

            @Override
            public void mouseReleased(MouseEvent pEvent) {
                reportPointer(pEvent, false);
            }
        };
        addMouseMotionListener(pointerListener);
        addMouseListener(pointerListener);
    }

    private void reportPointer(MouseEvent pEvent, boolean pTouching) {
        pointerX = pEvent.getX();
        pointerY = pEvent.getY();
        touching = pTouching;
        int width = Math.max(1, getWidth() - 1);
        int height = Math.max(1, getHeight() - 1);
        float normalizedX = Math.max(0f, Math.min(1f, pointerX / (float) width));
        float normalizedY = Math.max(0f, Math.min(1f, pointerY / (float) height));
        inputConsumer.accept(new TabletInput(normalizedX, normalizedY, touching));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics pGraphics) {
        super.paintComponent(pGraphics);
        pGraphics.setColor(Color.DARK_GRAY);
        pGraphics.drawString(
                touching ? "Lápiz tocando" : "Lápiz flotando",
                12,
                20);
        pGraphics.drawOval(pointerX - 5, pointerY - 5, 10, 10);
    }
}
