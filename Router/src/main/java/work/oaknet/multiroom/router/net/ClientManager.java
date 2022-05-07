package work.oaknet.multiroom.router.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import work.oaknet.multiroom.router.audio.AudioSourceManager;
import work.oaknet.multiroom.router.web.Webserver;
import work.oaknet.multiroom.router.web.Websocket;
import work.oaknet.multiroom.router.web.entities.Audio.Input;
import work.oaknet.multiroom.router.web.entities.Audio.Output;
import work.oaknet.multiroom.router.web.entities.Command;

import java.io.IOException;
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
                        var outdatedClients = connectedInClients.stream().filter((client)->System.currentTimeMillis() - client.lastTimeStamp >3000).toList();
                        for (var client:outdatedClients) {
                            connectedInClients.remove(client);
                            AudioSourceManager.getInstance().sources.removeIf((source) -> source.getName().equals(client.name));

                            // WEBSERVER COMMAND
                            var mapper = new ObjectMapper();
                            var inputEntity = new Input();
                            inputEntity.name = client.name;
                            var command = new Command();
                            command.command = "removeInputEvent";
                            command.data = mapper.writeValueAsString(inputEntity);
                            Webserver.instance.socket.notifyClients(command);
                        }
                    }
                    synchronized (connectedOutClients) {
                        var outdatedClients = connectedOutClients.stream().filter((client)->System.currentTimeMillis() - client.lastTimeStamp >3000).toList();
                        for (var client:outdatedClients) {
                            connectedOutClients.remove(client);
                            AudioSourceManager.getInstance().sources.forEach((source) ->
                                    source.activeClients.removeIf((otherClient) ->
                                            otherClient.name.equals(client.name)));

                            // WEBSERVER COMMAND
                            var mapper = new ObjectMapper();
                            var outputEntity = new Output();
                            outputEntity.name = client.name;
                            var command = new Command();
                            command.command = "removeOutputEvent";
                            command.data = mapper.writeValueAsString(outputEntity);
                            Webserver.instance.socket.notifyClients(command);
                        }
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

    public Client tickOutClient(InetAddress ip, int port, String name, byte currentVolume, byte maxVolume, short currentBufferSize, short bufferGoal){
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

                // WEBSERVER COMMAND
                var mapper = new ObjectMapper();
                var outputEntity = new Output();
                outputEntity.name = result.name;

                var addOutputCommand = new Command();
                addOutputCommand.command = "newOutputEvent";
                addOutputCommand.data = mapper.writeValueAsString(outputEntity);
                Webserver.instance.socket.notifyClients(addOutputCommand);

            }
            result.lastTimeStamp = System.currentTimeMillis();
            result.currentVolume = currentVolume;
            result.maxVolume = maxVolume;
            result.currentBufferSize = currentBufferSize;
            result.desiredBufferSize = bufferGoal;
            System.out.println("Updated Client: " + result.name +", Volume: "+ currentVolume + "/" + maxVolume + ", Buffer: " + currentBufferSize + "/" + bufferGoal);
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

                // WEBSERVER COMMAND
                var mapper = new ObjectMapper();
                var inputEntity = new Input();
                inputEntity.name = result.getName();
                inputEntity.level = 0;

                var addInputCommand = new Command();
                addInputCommand.command = "newInputEvent";
                addInputCommand.data = mapper.writeValueAsString(inputEntity);
                Webserver.instance.socket.notifyClients(addInputCommand);
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
