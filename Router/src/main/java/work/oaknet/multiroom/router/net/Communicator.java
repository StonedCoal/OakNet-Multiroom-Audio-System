package work.oaknet.multiroom.router.net;

import work.oaknet.multiroom.router.audio.AudioSource;
import work.oaknet.multiroom.router.audio.AudioSourceManager;
import work.oaknet.multiroom.router.util.Constants;
import work.oaknet.multiroom.router.util.Utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

public class Communicator {

    private Thread networkThread;
    private DatagramSocket socket;

    private static Communicator instance;

    public static Communicator getInstance() {
        return instance;
    }

    public Communicator(int port){
        instance = this;
        networkThread = new Thread(()->{
            try{
                socket = new DatagramSocket(port);

                //ReceiveLoop:
                while(true){
                    byte[] buffer = new byte[2048];
                    DatagramPacket receivedPacket = new DatagramPacket(buffer, 2048);
                    socket.receive(receivedPacket);
                    if(receivedPacket.getLength() < 11+32){
                        continue;
                    }
                    var command = new String(Arrays.copyOfRange(buffer, 0, 11));
                    var name = new String(Arrays.copyOfRange(buffer, 11, 11+32));
                    name = name.replace("\0","");
                    switch (command){
                        case Constants.MAGICGET -> {
                            // Client Requesting Stream
                            ClientManager.getInstance().tickOutClient(receivedPacket.getAddress(), receivedPacket.getPort(), name);

                        }
                        case Constants.MAGICPOST -> {
                            // Client Sending Stream to Input
                            var client = ClientManager.getInstance().tickInClient(receivedPacket.getAddress(), receivedPacket.getPort(), name);
                            var packetID = Utils.bytesToLong(Arrays.copyOfRange(buffer, 11+32, 11+32+8));
                            // this is very basic
                            // missing packets will be skipped
                            // Out of order Packets will be skipped
                            // if we're more than 5 Packets ahead the Sender has probably reset
                            // so we reset aswell
                            if(client.frameCounter - packetID > 5){
                                client.frameCounter = packetID-1;
                            }
                            if(packetID <= client.frameCounter){
                                //Received Packet from the past
                                continue;
                            }
                            client.frameCounter = packetID;
                            var audioData = Arrays.copyOfRange(buffer, 11+32+8, Constants.PACKET_SIZE + 11+32+8);

                            AudioSourceManager.getInstance().getSourceForClient(client).addAudioPacket(audioData);


                        }
                    }
                }

            }catch (SocketException e){
                System.err.println("A Socket Exception occured: " + e.getMessage());
                e.printStackTrace();
                System.exit(2);
            } catch (IOException e) {
                System.err.println("An IO Exception occured: " + e.getMessage());
                e.printStackTrace();
            }

        });
        networkThread.setDaemon(true);
        networkThread.setName("Network-Thread");
        networkThread.start();
    }

    public void sendData(byte[] data, Client recipient){
        if(socket == null || socket.isClosed())
            return;

        var packet = new DatagramPacket(data, data.length, recipient.address, recipient.port);
        try {
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("Can't send packet: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
