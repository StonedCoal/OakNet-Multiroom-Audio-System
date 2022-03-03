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

    public Webserver(){
        Spark.staticFiles.location("/static");
        Spark.port(80);

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

        Spark.get("/info", (request, response) ->{
            var jo = new JsonObject();
            var inputs = new JsonArray();
            for (var source: AudioSourceManager.getInstance().sources) {
                var input = new JsonObject();
                input.addProperty("name", source.getName());
                input.addProperty("level", source.getCurrentAudioLevel());
                var activeOutputs = new JsonArray();
                for (var activeClient:source.activeClients) {
                    var activeOutput = new JsonObject();
                    activeOutput.addProperty("name", activeClient.name);
                    activeOutputs.add(activeOutput);
                }
                input.add("activeOutputs", activeOutputs);
                inputs.add(input);
            }
            jo.add("inputs", inputs);
            return jo.toString();
        });

        Spark.post("/activate", (request, response) -> {

            var command = request.body().split(":");
            var source = AudioSourceManager.getInstance().getSourceByName(command[0]);
            var output = ClientManager.getInstance().getClientByName(command[1]);
            if(source == null || output == null)
                return"nuuu";
            AudioSourceManager.getInstance().setActiveSourceForClient(source, output);
            return "yaay";
        });
    }
}
