package app.model;

public final class TabletInput {

    private final float x;
    private final float y;
    private final boolean touching;

    public TabletInput(float pX, float pY, boolean pTouching) {
        if (!Float.isFinite(pX) || pX < 0f || pX > 1f) {
            throw new IllegalArgumentException("x must be between 0 and 1");
        }
        if (!Float.isFinite(pY) || pY < 0f || pY > 1f) {
            throw new IllegalArgumentException("y must be between 0 and 1");
        }
        x = pX;
        y = pY;
        touching = pTouching;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public boolean isTouching() {
        return touching;
    }
}
