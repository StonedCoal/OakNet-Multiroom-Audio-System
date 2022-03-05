package work.oaknet.multiroom.router.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import spark.ModelAndView;
import spark.Spark;
import spark.template.jinjava.JinjavaEngine;
import work.oaknet.multiroom.router.audio.AudioSource;
import work.oaknet.multiroom.router.audio.AudioSourceManager;
import work.oaknet.multiroom.router.net.ClientManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Webserver {

    private static Webserver instance;

    public static Webserver getInstance() {
        return instance;
    }

    private Websocket socket;

    public Websocket getSocket() {
        return socket;
    }

    public Webserver(){
        instance = this;

        socket = new Websocket();
        Spark.staticFiles.location("/static");
        Spark.port(80);

        // Websockets
        Spark.webSocket("/ws", socket);

        // HTTP Endpoints

        Spark.get("/", (request, response) -> {
            Map<String, Object> attributes = new HashMap<>();
            Map<String, Map<String, Boolean>> inputs = new HashMap<>();
            for (var input : AudioSourceManager.getInstance().sources) {
                Map<String, Boolean> outputs = new HashMap<>();
                for (var output : ClientManager.getInstance().getConnectedOutClients()) {
                    outputs.put(output.name, input.activeClients.contains(output));
                }
                inputs.put(input.getName(), outputs);

            }
            attributes.put("inputs", inputs);
            return new ModelAndView(attributes, "template/index.jin");
        }, new JinjavaEngine());
    }
}
