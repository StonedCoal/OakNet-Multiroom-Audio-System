package work.oaknet.multiroom.router.audio;

import work.oaknet.multiroom.router.net.ClientManager;
import work.oaknet.multiroom.router.util.Constants;
import work.oaknet.multiroom.router.util.Utils;
import work.oaknet.multiroom.router.net.Client;
import work.oaknet.multiroom.router.net.Communicator;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.ArrayList;

public class AudioSource {

    ArrayDeque<Integer> offset = new ArrayDeque<Integer>();
    ArrayDeque<Byte> buf = new ArrayDeque<>();
    public ArrayList<Client> activeClients = new ArrayList<>();
    boolean isProcessing = false;
    boolean initBuffer = false;
    long lastTimeStamp = 0;
    long frameCount = 0;
    int currentAudioLevel = 0;

    int median=0;

    private Thread audioThread;

    final String name;

    static final int TIME_PER_PACKET_IN_NS = (int) (1000000000 / (Constants.SAMPLERATE/(double)Constants.FRAMES_PER_PACKET));


    //Blocks if to much data is accepted
    public void addAudioData(byte[] frame) {
        synchronized (buf) {
            for (byte data : frame) {
                buf.add(data);
            }
        }
        while(buf.size() > Constants.PACKET_SIZE * 3){
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public int getCurrentAudioLevel() {
        return currentAudioLevel;
    }

    public String getName() {
        return name;
    }

    long timeSpan = 0;
    long actualTime = 0;
    public AudioSource(String name){
        this.name = name;
        audioThread = new Thread(() ->{
            while (!Thread.interrupted()) {
                if(actualTime == 0)
                    actualTime = System.nanoTime();
                while (timeSpan > TIME_PER_PACKET_IN_NS){
                    byte[] data = new byte[Constants.PACKET_SIZE + 8];
                    if (buf.size() >= Constants.PACKET_SIZE) {
                        synchronized (buf) {
                            for (int i = 0; i < Constants.PACKET_SIZE; i++) {
                                data[i + 8] = buf.peekFirst() != null ? buf.pollFirst() : 0;
                            }
                        }
                    }else{
                        for (int i = 0; i < Constants.PACKET_SIZE; i++) {
                            data[i + 8] = (byte) 0x00;
                        }
                    }
                    System.arraycopy(Utils.longToBytes(frameCount++), 0, data, 0, 8);
                    // check if Client is still connected
                    activeClients.removeIf((client) -> !ClientManager.getInstance().getConnectedOutClients().contains(client));
                    synchronized (activeClients){
                        for (var client : activeClients) {
                            Communicator.getInstance().sendData(data, client);
                        }
                    }
                    timeSpan-=TIME_PER_PACKET_IN_NS;

                    currentAudioLevel=(int) Utils.getRMS(data, Constants.STANDARD_FORMAT, 8);
                }
                try {
                    Thread.sleep(TIME_PER_PACKET_IN_NS/1000000, TIME_PER_PACKET_IN_NS%1000000);

                    timeSpan+=System.nanoTime() - actualTime;
                    actualTime = System.nanoTime();
                } catch (InterruptedException e) {
                    break;
                }


            }
        });
        audioThread.setDaemon(true);
        audioThread.setName(name+"-AudioSource-Thread");
        audioThread.start();
    }

    public void stop(){
        audioThread.interrupt();
    }

}
