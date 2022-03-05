package work.oaknet.multiroom.router.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import work.oaknet.multiroom.router.audio.AudioSourceManager;
import work.oaknet.multiroom.router.net.ClientManager;
import work.oaknet.multiroom.router.web.entities.Audio.ActivationPayload;
import work.oaknet.multiroom.router.web.entities.Command;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@WebSocket
public class Websocket {

    // Store sessions if you want to, for example, broadcast a message to all users
    private final Queue<Session> sessions = new ConcurrentLinkedQueue<>();

    @OnWebSocketConnect
    public void connected(Session session) {
        sessions.add(session);
    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {
        sessions.remove(session);
    }

    @OnWebSocketMessage
    public void message(Session session, String message) throws IOException {
        var mapper = new ObjectMapper();
        Command command = mapper.readValue(message, Command.class);
        parseCommand(command);
    }

    private void parseCommand(Command command){
        var mapper = new ObjectMapper();
        switch(command.getCommand()){
            case "activate" ->{
                try {
                    ActivationPayload payload = mapper.readValue(command.getData(), ActivationPayload.class);
                    var source = AudioSourceManager.getInstance().getSourceByName(payload.getInput().getName());
                    var output = ClientManager.getInstance().getClientByName(payload.getOutput().getName());
                    if(source == null || output == null)
                        return;
                    AudioSourceManager.getInstance().setActiveSourceForClient(source, output);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
            case "deactivate" ->{
                try {
                    ActivationPayload payload = mapper.readValue(command.getData(), ActivationPayload.class);
                    var output = ClientManager.getInstance().getClientByName(payload.getOutput().getName());
                    if(output == null)
                        return;
                    AudioSourceManager.getInstance().setActiveSourceForClient(null, output);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    public void notifyClients(Command message) throws IOException {
        var mapper = new ObjectMapper();
        var json = mapper.writeValueAsString(message);

        for(var session : sessions){
            if(session.isOpen()){
                session.getRemote().sendString(json);
                session.getRemote().flush();
            }
        }
    }
}
