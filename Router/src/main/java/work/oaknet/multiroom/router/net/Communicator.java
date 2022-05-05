package work.oaknet.multiroom.router.net;

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

    public Communicator(int port) {
        instance = this;
        networkThread = new Thread(() -> {
            try {
                socket = new DatagramSocket(port);

                //ReceiveLoop:
                while (true) {
                    byte[] buffer = new byte[2048];
                    DatagramPacket receivedPacket = new DatagramPacket(buffer, 2048);
                    socket.receive(receivedPacket);
                    if (receivedPacket.getLength() < 11 + 32) {
                        continue;
                    }
                    var index = 0;
                    var command = new String(Arrays.copyOfRange(buffer, index, index+11)); index+=11;
                    var name = new String(Arrays.copyOfRange(buffer, index, index + 32)); index+=32;
                    name = name.replace("\0", "");
                    switch (command) {
                        case Constants.MAGICGET -> {
                            var currentVolume = buffer[index]; index++;
                            var maxVolume = buffer[index]; index++;
                            var currentBufferSize = Utils.bytesToShort(Arrays.copyOfRange(buffer, index, index+2)); index+=2;
                            var bufferGoal = Utils.bytesToShort(Arrays.copyOfRange(buffer, index, index+2)); index+=2;
                            // Client Requesting Stream
                            ClientManager.getInstance().tickOutClient(receivedPacket.getAddress(), receivedPacket.getPort(), name, currentVolume, maxVolume, currentBufferSize, bufferGoal);

                        }
                        case Constants.MAGICPOST -> {
                            // Client Sending Stream to Input
                            var client = ClientManager.getInstance().tickInClient(receivedPacket.getAddress(), receivedPacket.getPort(), name);
                            var packetID = Utils.bytesToLong(Arrays.copyOfRange(buffer, index, index+8)); index+=8;
                            // this is very basic
                            // missing packets will be skipped
                            // Out of order Packets will be skipped
                            // if we're more than 5 Packets ahead the Sender has probably reset,
                            // so we reset as well
                            if (client.frameCounter - packetID > 5) {
                                client.frameCounter = packetID - 1;
                            }
                            if (packetID <= client.frameCounter) {
                                //Received Packet from the past
                                continue;
                            }
                            client.frameCounter = packetID;
                            var audioData = Arrays.copyOfRange(buffer, index, Constants.PACKET_SIZE + index); index+=Constants.PACKET_SIZE;

                            AudioSourceManager.getInstance().getSourceForClient(client).addAudioPacket(audioData);
                        }
                    }
                }

            } catch (SocketException e) {
                System.err.println("A Socket Exception occurred: " + e.getMessage());
                e.printStackTrace();
                System.exit(2);
            } catch (IOException e) {
                System.err.println("An IO Exception occurred: " + e.getMessage());
                e.printStackTrace();
            }

        });
        networkThread.setDaemon(true);
        networkThread.setName("Network-Thread");
        networkThread.start();
        new Thread(() -> {

        });
        /*
        // is this even used?
        var pingThread = new Thread(()->{
            while(!Thread.interrupted()){
                try {
                    Thread.sleep(1000);
                    sendData();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        pingThread.setDaemon(true);
        pingThread.setName("Ping-Thread");
        pingThread.start();
         */
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

    public void sendBroadcast(byte[] data){
        if(socket == null || socket.isClosed())
            return;

        var packet = new DatagramPacket(data, data.length, Constants.BROADCAST_ADDRESS, Constants.PORT);
        try {
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("Can't send packet: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
