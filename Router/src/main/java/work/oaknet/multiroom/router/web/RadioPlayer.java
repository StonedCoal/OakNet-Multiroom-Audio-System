package work.oaknet.multiroom.router.web;

import manifold.ext.props.rt.api.val;
import manifold.ext.props.rt.api.var;
import work.oaknet.multiroom.router.audio.AudioSource;
import work.oaknet.multiroom.router.audio.AudioSourceManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;

public class RadioPlayer {

    @var static RadioPlayer instance;
    @val HashMap<String, String> radioStations = new HashMap<>();

    public RadioPlayer() {
        instance = this;
        loadRadioStations();
        saveRadioStations();
    }

    public void loadRadioStations(){
        var path = Paths.get("stations.txt");
        synchronized (radioStations) {
            radioStations.clear();
        }
        if(Files.exists(path)){
            try {
                for (var line : Files.readAllLines(path)){
                    var chunks = line.split("\\|");
                    synchronized (radioStations) {
                        radioStations.put(chunks[0], chunks[1]);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveRadioStations(){
        var path = Paths.get("stations.txt");
        try {
            Files.deleteIfExists(path);
            var data = "";
            synchronized (radioStations) {
                for (var name : radioStations.keySet()) {
                    data += name + "|" + radioStations.get(name) + "\n";
                }
            }
            Files.writeString(path, data, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addRadioStation(String name, String url){
        synchronized (radioStations){
            if(radioStations.containsKey(name)){
               radioStations.remove(name);
            }
            radioStations.put(name, url);
        }
    }

    public void removeRadioStation(String name){
        synchronized (radioStations){
            if(radioStations.containsKey(name))
                radioStations.remove(name);
        }
    }

    public void play(String url){
        AudioSourceManager.getInstance().radioAudioSource.stream(url);
    }
}
