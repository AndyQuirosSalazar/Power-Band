package app.supercollider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bridge.commands.BassCommand;
import app.bridge.commands.OrganCommand;
import app.bridge.commands.PianoCommand;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BandClientTest {

    private DatagramSocket receiver;
    private BandClient bandClient;

    @BeforeEach
    void setUp() throws IOException {
        InetAddress loopback = InetAddress.getByName(BandClient.DEFAULT_HOST);
        receiver = new DatagramSocket(0, loopback);
        receiver.setSoTimeout(2000);
        bandClient = new BandClient(BandClient.DEFAULT_HOST, receiver.getLocalPort());
    }

    @AfterEach
    void tearDown() {
        bandClient.close();
        receiver.close();
    }

    @Test
    void bassCommandSendsStringBassWithTheRequestedNoteAndDuration() throws IOException {
        new BassCommand(43, 250).execute(bandClient);

        OscMessage message = receiveMessage();
        assertEquals("/s_new", message.address());
        assertEquals("stringBass", message.arguments().get(0));
        assertEquals("note", message.arguments().get(4));
        assertEquals(43, message.arguments().get(5));
        assertEquals("durMs", message.arguments().get(6));
        assertEquals(250, message.arguments().get(7));
    }

    @Test
    void pianoCommandSendsClassicPianoAndTheRequestedChordQuality() throws IOException {
        new PianoCommand(48, 250, false).execute(bandClient);

        OscMessage message = receiveMessage();
        assertEquals("/s_new", message.address());
        assertEquals("classicPiano", message.arguments().get(0));
        assertEquals("note", message.arguments().get(4));
        assertEquals(48, message.arguments().get(5));
        assertEquals("durMs", message.arguments().get(6));
        assertEquals(250, message.arguments().get(7));
        assertEquals("third", message.arguments().get(8));
        assertEquals(3, message.arguments().get(9));
    }

    @Test
    void organCommandSendsOrganControlsAndStopsItsActiveNoteAtSilence() throws IOException {
        new OrganCommand(0.5f).execute(bandClient);

        OscMessage noteOn = receiveMessage();
        assertEquals("/s_new", noteOn.address());
        assertEquals("organ70s", noteOn.arguments().get(0));
        assertEquals("freq", noteOn.arguments().get(4));
        assertEquals(261.62558f, (Float) noteOn.arguments().get(5), 0.001f);
        assertEquals("r1", noteOn.arguments().get(6));
        assertEquals(1f, (Float) noteOn.arguments().get(7));
        assertEquals("r2", noteOn.arguments().get(8));
        assertEquals(2f, (Float) noteOn.arguments().get(9));
        assertEquals("r3", noteOn.arguments().get(10));
        assertEquals(3f, (Float) noteOn.arguments().get(11));
        assertEquals("a1", noteOn.arguments().get(12));
        assertEquals(0.5f, (Float) noteOn.arguments().get(13));
        assertEquals("a2", noteOn.arguments().get(14));
        assertEquals(0.25f, (Float) noteOn.arguments().get(15));
        assertEquals("a3", noteOn.arguments().get(16));
        assertEquals(0.125f, (Float) noteOn.arguments().get(17));

        new OrganCommand(0f).execute(bandClient);

        OscMessage noteOff = receiveMessage();
        assertEquals("/n_set", noteOff.address());
        assertEquals(noteOn.arguments().get(1), noteOff.arguments().get(0));
        assertEquals("gate", noteOff.arguments().get(1));
        assertEquals(0, noteOff.arguments().get(2));
    }

    @Test
    void closingClientStopsActiveOrganAndClosesItsSocket() throws IOException {
        new OrganCommand(0.5f).execute(bandClient);
        receiveMessage();

        bandClient.close();

        OscMessage noteOff = receiveMessage();
        assertEquals("/n_set", noteOff.address());
        assertEquals("gate", noteOff.arguments().get(1));
        assertEquals(0, noteOff.arguments().get(2));
        assertThrows(IOException.class, () -> new BassCommand(43, 250).execute(bandClient));
    }

    private OscMessage receiveMessage() throws IOException {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        receiver.receive(packet);
        OscReader reader = new OscReader(packet.getData(), packet.getLength());
        String address = reader.readString();
        String typeTags = reader.readString();
        List<Object> arguments = new ArrayList<>();
        for (int index = 1; index < typeTags.length(); index++) {
            switch (typeTags.charAt(index)) {
                case 'i' -> arguments.add(reader.readInt());
                case 'f' -> arguments.add(Float.intBitsToFloat(reader.readInt()));
                case 's' -> arguments.add(reader.readString());
                default -> throw new IllegalStateException("unsupported OSC type tag");
            }
        }
        return new OscMessage(address, arguments);
    }

    private record OscMessage(String address, List<Object> arguments) {
    }

    private static final class OscReader {

        private final byte[] data;
        private final int length;
        private int offset;

        private OscReader(byte[] pData, int pLength) {
            data = pData;
            length = pLength;
        }

        private String readString() {
            int start = offset;
            while (offset < length && data[offset] != 0) {
                offset++;
            }
            if (offset >= length) {
                throw new IllegalStateException("unterminated OSC string");
            }
            String value = new String(data, start, offset - start, StandardCharsets.US_ASCII);
            offset = (offset + 4) & ~3;
            return value;
        }

        private int readInt() {
            if (offset + Integer.BYTES > length) {
                throw new IllegalStateException("incomplete OSC integer");
            }
            int value = ((data[offset] & 0xff) << 24)
                    | ((data[offset + 1] & 0xff) << 16)
                    | ((data[offset + 2] & 0xff) << 8)
                    | (data[offset + 3] & 0xff);
            offset += Integer.BYTES;
            return value;
        }
    }
}
