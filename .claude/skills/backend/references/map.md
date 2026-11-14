# Backend

After you change this layer, update this file so the next run stays current.

Source: `src/app/supercollider/BandClient.java`

## Implemented
- OSC client for sending synth commands to SuperCollider: `src/app/supercollider/BandClient.java`

## Classes
### BandClient
Sends OSC messages to the configured SuperCollider server. `src/app/supercollider/BandClient.java`
- `BandClient()` — connects to the default host and port.
- `BandClient(String host, int port)` — connects to the specified host and port.
- `playPiano(int note, int durMs)` — plays a major piano chord.
- `playPiano(int note, int durMs, boolean major)` — plays a major or minor piano chord.
- `playBass(int note, int durMs)` — plays a bass note.
- `noteOn(int note)` — starts an organ note with default partials.
- `noteOn(int note, float[] ratios, float[] amps)` — starts an organ note with the specified partials.
- `noteOff(int note)` — stops the active organ note for the MIDI note.
- `allNotesOff()` — stops all active organ notes.
- `close()` — releases active organ notes and closes the UDP socket.
- `main(String[] args)` — demonstrates piano, bass, and organ playback.

## Constants
- `DEFAULT_HOST` — default SuperCollider host.
- `DEFAULT_PORT` — default SuperCollider UDP port.
