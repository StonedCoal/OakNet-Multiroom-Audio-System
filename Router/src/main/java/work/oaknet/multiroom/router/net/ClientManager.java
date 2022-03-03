package work.oaknet.multiroom.router.net;

import work.oaknet.multiroom.router.audio.AudioSourceManager;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.stream.Stream;

public class ClientManager {

    private static ClientManager instance;

    public static ClientManager getInstance() {
        return instance;
    }

    private Thread managementThread;
    private final ArrayList<Client> connectedOutClients = new ArrayList<>();
    private final ArrayList<Client> connectedInClients = new ArrayList<>();

    public ArrayList<Client> getConnectedOutClients() {
        return connectedOutClients;
    }

    public ArrayList<Client> getConnectedInClients() {
        return connectedInClients;
    }

    public ClientManager(){
        instance = this;
        managementThread = new Thread(()->{
            try {
                while (true) {
                    synchronized (connectedInClients) {
                        connectedInClients.removeIf((client)->System.currentTimeMillis() - client.lastTimeStamp >3000);
                    }
                    synchronized (connectedOutClients) {
                        connectedOutClients.removeIf((client)->System.currentTimeMillis() - client.lastTimeStamp >3000);
                    }
                    Thread.sleep(1000);
                }
            }catch(InterruptedException e)
            {
                System.out.println("Client Management Thread has been stopped");
            }
        });
        managementThread.setDaemon(true);
        managementThread.setName("Client-Management-Thread");
        managementThread.start();
    }

    public Client tickOutClient(InetAddress ip, int port, String name){
        Client result = null;
        synchronized (connectedOutClients){
            for (var client:connectedOutClients) {
                if(client.address.equals(ip) && client.port == port){
                    result = client;
                    break;
                }
            }
            if(result == null){
                result = new Client();
                result.address = ip;
                result.port = port;
                result.name = name;
                connectedOutClients.add(result);
            }
            result.lastTimeStamp = System.currentTimeMillis();
        }
        return result;
    }
    public Client tickInClient(InetAddress ip, int port, String name){
        Client result = null;
        synchronized (connectedInClients){
            for (var client:connectedInClients) {
                if(client.address.equals(ip) && client.port == port){
                    result = client;
                    break;
                }
            }
            if(result == null){
                result = new Client();
                result.address = ip;
                result.port = port;
                result.name = name;
                connectedInClients.add(result);
            }
            result.lastTimeStamp = System.currentTimeMillis();
        }
        return result;
    }

    public Client getClientByName(String name){
        var result1 = connectedOutClients.stream().filter((client)->client.name.equals(name)).findFirst();
        if(result1.isPresent())
            return result1.get();

        var result2 = connectedInClients.stream().filter((client)->client.name.equals(name)).findFirst();
        if(result2.isPresent())
            return result2.get();
        return null;
    }
}
