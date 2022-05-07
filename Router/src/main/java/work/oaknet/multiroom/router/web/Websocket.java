package work.oaknet.multiroom.router.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import work.oaknet.multiroom.router.audio.AudioSourceManager;
import work.oaknet.multiroom.router.net.Client;
import work.oaknet.multiroom.router.net.ClientManager;
import work.oaknet.multiroom.router.web.entities.Audio.ActivationPayload;
import work.oaknet.multiroom.router.web.entities.Audio.Input;
import work.oaknet.multiroom.router.web.entities.Audio.Output;
import work.oaknet.multiroom.router.web.entities.Command;
import work.oaknet.multiroom.router.web.entities.radio.PlayStationPayload;
import work.oaknet.multiroom.router.web.entities.radio.RadioStation;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
/* Note: The Websocket communicates with JSON payloads.
/* The JSON payloads are wrapped in command Objects.
/* Available commands can be seen in the Command.Factory class
**/
@WebSocket
public class Websocket {

    // Store sessions if you want to, for example, broadcast a message to all users
    private final Queue<Session> sessions = new ConcurrentLinkedQueue<>();

    @OnWebSocketConnect
    public void connected(Session session) {
        sessions.add(session);
        // This sends all existing data to the new client
        // Inputs
        for (var input : AudioSourceManager.getInstance().sources) {
            var inputEntity = new Input();
            inputEntity.name = input.getName();
            inputEntity.level = input.getCurrentAudioLevel();

            var addInputCommand = Command.Factory.newInputCommand(inputEntity);
            sendCommandToClient(addInputCommand, session);
        }
        // Outputs
        for (var output : ClientManager.getInstance().getConnectedOutClients()) {
            var outputEntity = new Output();
            outputEntity.name = output.name;

            var addOutputCommand = Command.Factory.newOutputCommand(outputEntity);
            sendCommandToClient(addOutputCommand, session);
        }
        // Activations
        for (var input : AudioSourceManager.getInstance().sources) {
            var inputEntity = new Input();
            inputEntity.name = input.name;
            inputEntity.level = input.currentAudioLevel;
            for (var activeClient : input.activeClients) {
                var outputEntity = new Output();
                outputEntity.name = activeClient.name;

                var activationPayload = new ActivationPayload();
                activationPayload.input = inputEntity;
                activationPayload.output = outputEntity;

                var activateCommand = Command.Factory.activationCommand(activationPayload);
                sendCommandToClient(activateCommand, session);
            }
        }
        // RadioStations
        for(var stationName : RadioPlayer.instance.radioStations.keySet()){
            var stationUrl = RadioPlayer.instance.radioStations.get(stationName);

            var stationEntity = new RadioStation();
            stationEntity.name = stationName;
            stationEntity.url = stationUrl;

            var command = Command.Factory.radioStationAddedCommand(stationEntity);

            Webserver.instance.socket.sendCommandToClient(command, session);
        }
    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {
        sessions.remove(session);
    }

    @OnWebSocketMessage
    public void message(Session session, String message) throws IOException {
        var mapper = new ObjectMapper();
        Command command = mapper.readValue(message, Command.class);
        Command.Parser.parseCommand(command);
    }

    public void sendStringToClient(String message, Session session){
        if(session.isOpen()){
            try{
                session.getRemote().sendString(message);
                session.getRemote().flush();
            }catch(Exception ignored){ }
        }
    }

    public void sendCommandToClient(Command command, Session session){
        var mapper = new ObjectMapper();
        var json = mapper.writeValueAsString(command);
        sendStringToClient(json, session);
    }

    public void notifyClients(Command message){
        var mapper = new ObjectMapper();
        var json = mapper.writeValueAsString(message);
        for(var session : sessions){
           sendStringToClient(json, session);
        }
    }
}
