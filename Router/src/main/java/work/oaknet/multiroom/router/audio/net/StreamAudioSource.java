package work.oaknet.multiroom.router.audio.net;

import work.oaknet.multiroom.router.audio.AudioSource;
import work.oaknet.multiroom.router.audio.AudioSourceManager;
import work.oaknet.multiroom.router.net.Client;
import work.oaknet.multiroom.router.net.ClientManager;

import java.util.LinkedList;

public class StreamAudioSource extends AudioSource {
    private static LinkedList<byte[]> packetBuf = new LinkedList<>();

    private Thread queueThread;

    public StreamAudioSource(Client client) {
        super(client.name);
        queueThread = new Thread(()->{
            while(!Thread.interrupted()){
                while(true){
                    // Check if our source Client is still connected if not stop
                    if(!ClientManager.getInstance().getConnectedInClients().contains(client)){
                        stop();
                        AudioSourceManager.getInstance().removeSourceForClient(client);
                    }
                    byte[] result = null;
                    synchronized (packetBuf){
                        if(packetBuf.size()>0) {
                            result=packetBuf.get(0);
                            packetBuf.remove(0);
                        }
                    }
                    if(result != null)
                        addAudioData(result);
                    else
                        break;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {

                    break;
                }
            }
        });
        queueThread.setDaemon(true);
        queueThread.setName(client.name+"-AudioSource-Thread");
        queueThread.start();
    }

    public void addAudioPacket(byte[] packet){
        synchronized (packetBuf){
            // Are we getting spammed????
            if(packetBuf.size() > 10){
                return;
            }
            packetBuf.add(packet);
        }
    }

    @Override
    public void stop() {
        queueThread.interrupt();
        super.stop();
    }
}
