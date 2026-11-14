# Backend

After you change this layer, update this file so the next run stays current.

Source: `src/app/bridge`, `src/app/logic`, `src/app/model`, `src/app/supercollider`

## Implemented
- Bridge instruments and synth commands for device testing.
- GLFW gamepad input, Java Sound volume input, and session routing.
- OSC client for SuperCollider: `src/app/supercollider/BandClient.java`.

## Classes
### SynthCommand
Command abstraction executed through `BandClient`. `src/app/bridge/commands/SynthCommand.java`
- `execute(BandClient pBandClient)` — sends the command to its synth.

### BassCommand
Timed bass note. `src/app/bridge/commands/BassCommand.java`
- `BassCommand(int pNote, int pDurationMillis)` — validates and stores the MIDI note and duration.
- `getNote()` — returns the note.
- `getDurationMillis()` — returns the duration.
- `execute(BandClient pBandClient)` — calls `playBass`.

### PianoCommand
Timed piano chord. `src/app/bridge/commands/PianoCommand.java`
- `PianoCommand(int pNote, int pDurationMillis, boolean pMajor)` — validates and stores chord settings.
- `getNote()` — returns the note.
- `getDurationMillis()` — returns the duration.
- `isMajor()` — returns the chord quality.
- `execute(BandClient pBandClient)` — calls `playPiano`.

### OrganCommand
Volume-controlled organ note. `src/app/bridge/commands/OrganCommand.java`
- `OrganCommand(float pVolume)` — validates normalized volume.
- `getVolume()` — returns the volume.
- `execute(BandClient pBandClient)` — starts or stops the organ note.

### Instrument<T>
Bridge abstraction translating input into an optional synth command. `src/app/bridge/instruments/Instrument.java`
- `translate(T pInput)` — returns the command for an input, if one is needed.

### GamepadInstrument
Maps directional transitions to bass notes. `src/app/bridge/instruments/GamepadInstrument.java`
- `directionFromAxes(float pHorizontal, float pVertical)` — returns the dominant direction above threshold.
- `translate(GamepadDirection pDirection)` — emits one bass command per direction change.

### TabletInstrument
Maps tablet-area position and touch state to piano chords. `src/app/bridge/instruments/TabletInstrument.java`
- `translate(TabletInput pInput)` — emits a piano command while touching; hover does not play.

### MicrophoneInstrument
Quantizes volume into organ amplitudes. `src/app/bridge/instruments/MicrophoneInstrument.java`
- `translate(Float pVolume)` — emits a command when the volume level changes.

### GamepadDirection
Directional axis states, including `NONE`. `src/app/model/GamepadDirection.java`

### TabletInput
Normalized pointer coordinates and touch state. `src/app/model/TabletInput.java`
- `TabletInput(float pX, float pY, boolean pTouching)` — validates normalized position.
- `getX()` — returns normalized X.
- `getY()` — returns normalized Y.
- `isTouching()` — returns contact state.

### DeviceTestSession
Connects source events to Bridge instruments and a command consumer, and reports gamepad direction changes and microphone levels to the UI. `src/app/logic/DeviceTestSession.java`
- `DeviceTestSession(GamepadInputSource pGamepadSource, MicrophoneInputSource pMicrophoneSource, Consumer<SynthCommand> pCommandConsumer, Consumer<Float> pMicrophoneLevelConsumer, Consumer<String> pStatusConsumer)` — configures sources, command, microphone-level, and status callbacks.
- `start()` — starts gamepad and microphone input and publishes statuses.
- `acceptTabletInput(TabletInput pInput)` — routes tablet input.
- `close()` — closes input sources and ignores later commands.

### GamepadInputSource
Gamepad source contract. `src/app/logic/GamepadInputSource.java`
- `start(Consumer<GamepadDirection> pInputConsumer, Consumer<String> pStatusConsumer)` — starts direction reporting.
- `close()` — releases the source.

### GlfwGamepadInputSource
Adapter registering the test session with the GLFW runtime. `src/app/logic/GlfwGamepadInputSource.java`
- `GlfwGamepadInputSource(GlfwGamepadRuntime pRuntime)` — binds the source to the main-thread runtime.
- `start(Consumer<GamepadDirection> pInputConsumer, Consumer<String> pStatusConsumer)` — registers callbacks.
- `close()` — unregisters the source.

### GlfwGamepadRuntime
Runs GLFW initialization, polling, and cleanup on its creating main thread; supports mapped gamepads and generic joysticks with two directional axes. `src/app/logic/GlfwGamepadRuntime.java`
- `GlfwGamepadRuntime()` — records the owner thread.
- `registerSource(Consumer<GamepadDirection> pInputConsumer, Consumer<String> pStatusConsumer)` — schedules a listener.
- `unregisterSource(long pSourceId)` — schedules listener removal.
- `runWhile(BooleanSupplier pKeepRunning)` — polls GLFW until the application stops.
- `close()` — terminates GLFW and releases callbacks.

### MicrophoneInputSource
Microphone level-source contract. `src/app/logic/MicrophoneInputSource.java`
- `start(Consumer<Float> pVolumeConsumer, Consumer<String> pStatusConsumer)` — starts normalized volume reporting.
- `close()` — stops capture.

### JavaSoundMicrophoneInputSource
Captures temporary PCM buffers from the configured/default recording mixer and reports RMS only. `src/app/logic/JavaSoundMicrophoneInputSource.java`
- `start(Consumer<Float> pVolumeConsumer, Consumer<String> pStatusConsumer)` — starts audio capture on a daemon thread.
- `close()` — stops and closes the target line.

### BandClient
Sends OSC messages to SuperCollider. `src/app/supercollider/BandClient.java`
- `BandClient()` — connects to the default host and port.
- `BandClient(String host, int port)` — connects to the specified host and port.
- `playPiano(int note, int durMs)` — plays a major piano chord.
- `playPiano(int note, int durMs, boolean major)` — plays a major or minor piano chord.
- `playBass(int note, int durMs)` — plays a bass note.
- `noteOn(int note)` — starts an organ note with default partials.
- `noteOn(int note, float[] ratios, float[] amps)` — starts an organ note with specified partials and amplitudes.
- `noteOff(int note)` — stops the active organ note.
- `allNotesOff()` — stops all active organ notes.
- `close()` — releases active organ notes and closes the UDP socket.
- `main(String[] args)` — demonstrates synth playback.
