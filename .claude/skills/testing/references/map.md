# Testing

After you change this layer, update this file so the next run stays current.

Source: `src/test/app`

## Test classes
### DeviceInstrumentTest
Verifies direction, tablet, and microphone-to-command mappings. `src/test/app/bridge/instruments/DeviceInstrumentTest.java`
- `mapsOnlyDirectionalGamepadChangesToBassNotes()` — checks directional filtering and bass notes.
- `mapsTabletCoordinatesAndPlaysOnlyWhileTouching()` — checks normalized position mapping, chord quality, and hover silence.
- `mapsMicrophoneVolumeToQuantizedOrganAmplitudeAndSilence()` — checks volume quantization and note-off at silence.

### DeviceTestSessionTest
Verifies that each source reaches only its assigned synth command, gamepad direction changes are reported once, microphone levels are reported for every sample, and sources close. `src/test/app/logic/DeviceTestSessionTest.java`
- `routesEachSourceToOnlyItsAssignedSynthAndClosesSources()` — feeds fake inputs and checks emitted commands.

### GlfwGamepadRuntimeTest
Verifies direction mapping for generic joystick axes and rejects incomplete axis data. `src/test/app/logic/GlfwGamepadRuntimeTest.java`
- `mapsTheFirstTwoAxesOfAnUnmappedJoystick()` — maps the first two axes to a directional input.
- `rejectsJoysticksWithoutTwoAxes()` — rejects axis buffers with fewer than two axes.

### MainMenuTest
Verifies menu title, button order, visibility, and actions. `src/test/app/ui/MainMenuTest.java`
- `showsTheTitleAndButtonsInTheRequiredOrder()` — checks menu presentation.
- `testButtonRunsItsActionAndExitRunsItsAction()` — checks `Probar` and `Salir` callbacks.

### TabletInputPanelTest
Verifies local AWT pointer normalization and touch state. `src/test/app/ui/TabletInputPanelTest.java`
- `convertsLocalPointerEventsToNormalizedPenPositionAndTouchState()` — checks press/release inputs.

## Test command
- `mvn test` — runs the JUnit 5 suite defined by `pom.xml`.
