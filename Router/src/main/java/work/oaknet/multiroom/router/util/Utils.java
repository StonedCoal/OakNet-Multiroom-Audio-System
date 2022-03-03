package work.oaknet.multiroom.router.util;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class Utils {

    static int MAX_SIZE_RMS = 100;

    public static double getRMS(byte[] audioData, AudioFormat format, int offset) {

        byte[] sample = new byte[Math.min(50, audioData.length)];
        System.arraycopy(audioData, audioData.length - sample.length, sample, 0, sample.length);

        // we must first convert the raw array of bytes into integers
        int[] samples = convertByteArray(audioData, format, offset);

        long sumOfSquares = Arrays.stream(samples).mapToLong(i -> i * i).sum();
        double rootMeanSquare = Math.sqrt(sumOfSquares / samples.length);
        return rootMeanSquare;
    }

    private static int[] convertByteArray(byte[] audioData, AudioFormat format, int byteOffset) {

        if (format.getSampleSizeInBits() == 2) {
            int[] samples = new int[Math.min(audioData.length / 2, MAX_SIZE_RMS)];
            int offset = audioData.length - 2 * samples.length;
            for (int i = byteOffset; i < samples.length; i++) {
                if (format.isBigEndian()) {
                    samples[i] = ((audioData[offset + i * 2] << 8) | (audioData[offset + i * 2 + 1] & 0xFF));
                } else {
                    samples[i] = ((audioData[offset + i * 2 + 0] & 0xFF) | (audioData[offset + i * 2 + 1] << 8));
                }
            }
            return samples;
        } else if (format.getSampleSizeInBits() == 1) {
            int[] samples = new int[Math.min(audioData.length, MAX_SIZE_RMS)];
            int offset = audioData.length - samples.length;
            for (int i = byteOffset; i < samples.length; i++) {
                samples[i] = (audioData[offset + i] << 8);
            }
            return samples;
        } else {
            throw new RuntimeException("unsupported frame size: " + format.getFrameSize());
        }
    }

    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.order(ByteOrder.LITTLE_ENDIAN).putLong(x);
        return buffer.array();
    }

    public static long bytesToLong(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }
}
