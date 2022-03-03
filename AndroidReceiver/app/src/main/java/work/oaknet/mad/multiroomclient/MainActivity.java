package work.oaknet.mad.multiroomclient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.JobIntentService;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    ArrayList<byte[]> bufferList = new ArrayList<>();
    ArrayList<Integer> bufferListSizeMedian = new ArrayList<>();

    static Thread networkThread;
    static Thread audioThread;

    static MainActivity instance;


    // Audio vars
    // How full should the buffer be
    int bufferGoal = 100;
    // enforce the buffer size in this +- Range
    int bufferRange = 15;
    // this buffer +- range will be reached overtime
    int bufferRangeTight = 1;
    // ever n't Framebuffer the tight buffer range will be approached
    int bufferTightCyclus = 850;

    //System
    int tightCounter = 0;
    String routerAddress="";
    String clientName="";


    @Override
    protected void onPause() {
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("bufferGoal", bufferGoal);
        editor.putInt("bufferRange", bufferRange);
        editor.putInt("bufferRangeTight", bufferRangeTight);
        editor.putString("routerAddress", routerAddress);
        editor.putString("clientName", clientName);
        editor.apply();
        super.onPause();
    }

    @Override
    protected void onResume() {
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        bufferGoal = prefs.getInt("bufferGoal", 100);
        bufferRange = prefs.getInt("bufferRange", 15);
        bufferRangeTight = prefs.getInt("bufferRangeTight", 1);
        routerAddress = prefs.getString("routerAddress", "127.0.0.1");
        clientName = prefs.getString("clientName", "OakNet-Client");
        ((EditText) findViewById(R.id.bufferGoalField)).setText(""+bufferGoal);
        ((EditText) findViewById(R.id.bufferRangeField)).setText(""+bufferRange);
        ((EditText) findViewById(R.id.bufferRangeTightField)).setText(""+bufferRangeTight);
        ((EditText) findViewById(R.id.routerAddressField)).setText(routerAddress);
        ((EditText) findViewById(R.id.clientNameField)).setText(clientName);

        if(audioThread!=null && networkThread != null && (audioThread.isAlive() || networkThread.isAlive())){
            ((Button) MainActivity.instance.findViewById(R.id.button2)).setText("Stop Stream");
        }
        super.onResume();
    }

    //Thanx Stack Overflow https://stackoverflow.com/questions/4485128/how-do-i-convert-long-to-byte-and-back-in-java
    public long bytesToLong(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    void buildThreads(){
        networkThread = new InterruptableUdpThread(() -> {
            try {
                ((InterruptableUdpThread)networkThread).sock = new DatagramSocket(6980);

                int cnt = 0;

                long oldFrameCount = 0l;
                new Thread(()->{
                   while(!Thread.interrupted()){
                       String handshakeText = "GIMMESTREAM" + clientName;
                       for (int i = 0; i < 32-clientName.length(); i++) {
                           handshakeText+="\0";
                       }
                       byte[] text = handshakeText.getBytes(StandardCharsets.US_ASCII);
                       DatagramPacket handshakePacket = new DatagramPacket(text, text.length, new InetSocketAddress(routerAddress, 6980));
                       try {
                           ((InterruptableUdpThread)networkThread).sock.send(handshakePacket);
                           Thread.sleep(1000);
                       } catch (IOException | InterruptedException e) {
                       }
                   }
                }).start();
                while (true) {
                    byte[] buffer = new byte[1288];
                    DatagramPacket packet = new DatagramPacket(buffer, 1288);
                    ((InterruptableUdpThread)networkThread).sock.receive(packet);
                    //Log.d("multiroomClient", "Received Packet: " + buffer.length);

                    // On Router side this is unsigned, could lead to problems here...
                    // Should only be relevant after 180 days of continuous streaming
                    long frameCount = bytesToLong(Arrays.copyOfRange(packet.getData(), 0, 8));

                    //Log.d("multiroomClient", "Old Frame: " + oldFrameCount + " New Frame:" + frameCount);

                    // Are we in the future?
                    if (oldFrameCount - frameCount > 5)
                        oldFrameCount = frameCount - 1;
                    //Out of order Frame
                    if (frameCount <= oldFrameCount) {
                        Log.w("multiroomClient", "Received Packet out of order");
                        continue;
                    }
                    int missingFrames = (int) (frameCount - 1 - oldFrameCount);
                    //This should never be greater than 5. if it is just ignore it... ;D
                    if (Math.abs(missingFrames) < 5) {
                        while (missingFrames > 0) {
                            //Add some silence. TODO is adding noise better?
                            synchronized (bufferList) {
                                bufferList.add(new byte[1280]);
                            }
                            Log.w("multiroomClient", "Missing Frame");
                            missingFrames--;
                        }
                    }
                    oldFrameCount = frameCount;
                    synchronized (bufferList) {
                        bufferList.add(Arrays.copyOfRange(packet.getData(), 8, packet.getLength()));
                    }
                    //player.write(packet.getData(), 0, 1280);
                }
            } catch (SocketException se) {
                if(((InterruptableUdpThread)networkThread).sock.isClosed()){
                    //interrupted
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "Network Thread");

        audioThread = new Thread(() -> {
            AudioTrack player = null;
            try {
                player= new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(44100)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                                .build())
                        .setBufferSizeInBytes(1280)
                        .build();
                player.play();

                // wait until we first filled the buffer
                while (bufferList.size() < bufferGoal) {
                    player.write(new byte[1280], 0, 1280);
                }
                while (true) {
                    byte[] audioData;

                    synchronized (bufferList) {
                        // The tight approach
                        // calculate the median of the bufferlists size. Because the Android network stack is unreliable as f**** regarding timings
                        bufferListSizeMedian.add(bufferList.size());
                        if (tightCounter <= 0) {
                            tightCounter = 0;
                            int sum = 0;
                            for (int i: bufferListSizeMedian) {
                                sum += i;
                            }
                            int median = sum/bufferListSizeMedian.size();
                            bufferListSizeMedian.clear();
                            MainActivity.instance.runOnUiThread(() ->{
                                ((TextView) findViewById(R.id.bufferSizeMedianLabel)).setText("Current buffer size: " + median);
                            });
                            //if buffer greater take one Packet out
                            if (median > bufferGoal + bufferRangeTight) {
                                bufferList.remove(0);
                                Log.i("multiroomClient", "Tight adjustment removed a frame ");
                                //if buffer  to small add one frame of silence;
                            } else if (median < bufferGoal - bufferRangeTight) {
                                bufferList.add(new byte[1280]);
                                Log.i("multiroomClient", "Tight adjustment added a frame ");
                            }
                            int divisor = Math.abs(median - bufferGoal) - bufferRangeTight;
                            if (divisor < 1)
                                divisor = 1;
                            tightCounter = bufferTightCyclus/divisor;

                        } else {
                            tightCounter--;
                        }
                        // forced approach
                        if (bufferList.size() > bufferGoal + bufferRange) {
                            Log.w("multiroomClient", "Overflow");
                            bufferList.remove(0);
                        } else if (bufferList.size() < bufferGoal - bufferRange) {
                            Log.w("multiroomClient", "Underrun");
                            bufferList.add(new byte[1280]);
                        }
                        if (bufferList.size() == 0) {
                            bufferList.add(new byte[1280]);
                            Log.w("multiroomClient", "Buffer is empty");
                        }
                        audioData = bufferList.get(0);
                        bufferList.remove(0);
                    }
                    player.write(audioData, 0, 1280);
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                }
            }catch  (InterruptedException ie){
                if(player != null) {
                    player.stop();
                    player.release();
                }
                bufferList.clear();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "Audio Thread");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;
    }

    public void setBufferGoalClicked(View view) {
        int bufferGoalTemp = Integer.parseInt(((EditText)findViewById(R.id.bufferGoalField)).getText().toString());
        int bufferRangeTemp = Integer.parseInt(((EditText)findViewById(R.id.bufferRangeField)).getText().toString());
        int bufferRangeTightTemp = Integer.parseInt(((EditText)findViewById(R.id.bufferRangeTightField)).getText().toString());
        if(bufferGoalTemp < bufferRangeTemp)
            bufferGoalTemp = bufferRangeTemp;
        if(bufferGoalTemp > 1000)
            bufferGoalTemp = 1000;
        if(bufferRangeTightTemp > bufferRangeTemp)
            bufferRangeTightTemp = bufferRangeTemp;
        if(bufferRangeTightTemp < 0)
            bufferRangeTightTemp = 0;
        bufferGoal = bufferGoalTemp;
        bufferRange = bufferRangeTemp;
        bufferRangeTight = bufferRangeTightTemp;
        routerAddress = ((EditText)findViewById(R.id.routerAddressField)).getText().toString();
        clientName = ((EditText)findViewById(R.id.clientNameField)).getText().toString();
        ((TextView) findViewById(R.id.bufferGoalField)).setText(""+bufferGoal);
        ((TextView) findViewById(R.id.bufferRangeField)).setText(""+bufferRange);
        ((TextView) findViewById(R.id.bufferRangeTightField)).setText(""+bufferRangeTight);
    }

    public void startStopClicked(View view) {
         if(audioThread!=null && networkThread != null && (audioThread.isAlive() || networkThread.isAlive())){
             audioThread.interrupt();
             networkThread.interrupt();
             try {
                 audioThread.join();
                 networkThread.join();
             } catch (InterruptedException e) {
                 e.printStackTrace();
             }
             ((Button) view).setText("Start Stream");
         }else{
             buildThreads();
             Intent intent = new Intent(this, StreamIntentService.class);
             startService(intent);
         }
    }

    public static class StreamIntentService extends IntentService {

        public StreamIntentService() {
            super("AudioStreamService");
        }

        @Override
        protected void onHandleIntent(@Nullable Intent intent) {
            networkThread.start();
            audioThread.start();
            MainActivity.instance.runOnUiThread(() -> {
                ((Button) MainActivity.instance.findViewById(R.id.button2)).setText("Stop Stream");
            });
        }
    }
}