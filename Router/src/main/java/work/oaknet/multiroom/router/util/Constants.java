package work.oaknet.multiroom.router.util;

import javax.sound.sampled.AudioFormat;

public class Constants {

    public static final int PACKET_SIZE = 1280;
    public static final int CHANNELS = 2;
    public static final int BYTES_PER_SAMPLE = 2;
    public static final int FRAMES_PER_PACKET = PACKET_SIZE/(BYTES_PER_SAMPLE*CHANNELS);
    public static final int SAMPLERATE = 44100;
    public static final AudioFormat STANDARD_FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, (float) SAMPLERATE, BYTES_PER_SAMPLE, CHANNELS, 256, (float) SAMPLERATE, false);
    public static final String SPOTIFY_CONNECT_DEVICE_NAME = "OakNet Audio";
    public static final String MAGICGET = "GIMMESTREAM";
    public static final String MAGICPOST = "TAKIESTREAM";
}
