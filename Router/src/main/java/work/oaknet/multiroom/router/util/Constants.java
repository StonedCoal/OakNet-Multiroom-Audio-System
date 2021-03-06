package work.oaknet.multiroom.router.util;

import javax.sound.sampled.AudioFormat;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Constants {

    public static final int PACKET_SIZE = 1280;
    public static final int CHANNELS = 2;
    public static final int BYTES_PER_SAMPLE = 2;
    public static final int FRAMES_PER_PACKET = PACKET_SIZE/(BYTES_PER_SAMPLE*CHANNELS);
    public static final int SAMPLERATE = 44100;
    public static final int PORT = 6980;
    public static final AudioFormat STANDARD_FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, (float) SAMPLERATE, BYTES_PER_SAMPLE*8, CHANNELS, 4, (float) SAMPLERATE, false);
    public static final String SPOTIFY_CONNECT_DEVICE_NAME = "OakNet Audio";
    public static final String MAGICGET = "GIMMESTREAM";
    public static final String MAGICPOST = "TAKIESTREAM";

    public static InetAddress BROADCAST_ADDRESS = null;

    static {
        try {
            BROADCAST_ADDRESS = Inet4Address.getByName("192.168.2.255");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
