package work.oaknet.multiroom.router;

import work.oaknet.multiroom.router.audio.AudioSourceManager;
import work.oaknet.multiroom.router.web.RadioPlayer;
import work.oaknet.multiroom.router.web.Webserver;
import work.oaknet.multiroom.router.net.Client;
import work.oaknet.multiroom.router.net.ClientManager;
import work.oaknet.multiroom.router.net.Communicator;

import java.net.InetAddress;

public class Main {

    public static void main(String[] args){
        try {
            new RadioPlayer();
            new AudioSourceManager();
            new ClientManager();
            new Webserver();
            new Communicator(6980);
            /*
            var client = new Client();
            client.address = InetAddress.getByName("192.168.2.42");
            client.port = 6981;
            client.name = "Test";
            AudioSourceManager.getInstance().spotifyAudioSource.activeClients.add(client);

            var client2 = new Client();
            client2.address = InetAddress.getByName("192.168.2.50");
            client2.port = 6980;
            client2.name = "Test2";
            AudioSourceManager.getInstance().spotifyAudioSource.activeClients.add(client2);
            */


        }catch (Exception e){
            System.err.println("Fatal Exception occured: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

    }
}
