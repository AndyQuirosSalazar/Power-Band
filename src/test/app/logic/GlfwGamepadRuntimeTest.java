package app.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.model.GamepadDirection;
import java.nio.FloatBuffer;
import org.junit.jupiter.api.Test;

class GlfwGamepadRuntimeTest {

    @Test
    void mapsTheFirstTwoAxesOfAnUnmappedJoystick() {
        FloatBuffer axes = FloatBuffer.wrap(new float[] { -0.8f, 0.1f, 0f, 0f });

        assertEquals(GamepadDirection.LEFT, GlfwGamepadRuntime.directionFromJoystickAxes(axes));
    }

    @Test
    void rejectsJoysticksWithoutTwoAxes() {
        FloatBuffer axes = FloatBuffer.wrap(new float[] { 0.8f });

        assertThrows(
                IllegalArgumentException.class,
                () -> GlfwGamepadRuntime.directionFromJoystickAxes(axes));
    }
}
