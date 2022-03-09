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

        if (format.getSampleSizeInBits()/8 == 2) {
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
        } else if (format.getSampleSizeInBits()/8 == 1) {
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

    public static byte[] downsample(byte[] raw,
                                     int srIn,
                                     int srOut, boolean bigEndian) {


        short[] samples = new short[raw.length / 2];
        for (int i = 0; i < samples.length; i++) {
            if (bigEndian) {
                samples[i] = (short) ((raw[i * 2] << 8) | (raw[i * 2 + 1] & 0xFF));
            } else {
                samples[i] = (short) ((raw[i * 2 + 0] & 0xFF) | (raw[i * 2 + 1] << 8));
            }
        }

        short[] temp = new short[samples.length];
        int inSampleIndex = -1;
        int outSampleIndex = 0;
        int k = srOut;
        boolean done = false;
        while (!done) {
            int sum = 0;
            for (int i = 0; i < srIn; i++) {
                if (k == srOut) {
                    inSampleIndex++;
                    if (inSampleIndex >= samples.length) {
                        done = true;
                        break;
                    }
                    k = 0;
                }
                sum += samples[inSampleIndex];
                k++;
            }
            temp[outSampleIndex++] = (short) (sum / srIn);
        }

        byte[] result = new byte[outSampleIndex*2];
        for (int i = 0; i < outSampleIndex; i++) {
            if (bigEndian) {
                result[i*2] = (byte) (temp[i] >> 8);
                result[i*2+1] = (byte) (temp[i] & 0xFF);
            } else {
                result[i*2+1] = (byte) (temp[i] >> 8);
                result[i*2] = (byte) (temp[i] & 0xFF);
            }
        }

        return result;
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
