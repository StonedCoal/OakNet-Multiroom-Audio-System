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
            List<String> inputs = new ArrayList<>();
            for (var input : AudioSourceManager.getInstance().sources) {
                inputs.add(input.getName());
            }
            attributes.put("inputs", inputs);
            List<String> outputs = new ArrayList<>();
            for (var output : ClientManager.getInstance().getConnectedOutClients()) {
                outputs.add(output.name);
            }
            attributes.put("outputs", outputs);
            return new ModelAndView(attributes, "template/index.jin");
        }, new JinjavaEngine());
    }
}
