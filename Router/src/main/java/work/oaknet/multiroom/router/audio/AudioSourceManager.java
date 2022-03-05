package work.oaknet.multiroom.router.audio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import work.oaknet.multiroom.router.audio.net.StreamAudioSource;
import work.oaknet.multiroom.router.audio.spotify.SpotifyAudioSource;
import work.oaknet.multiroom.router.net.Client;
import work.oaknet.multiroom.router.web.Webserver;
import work.oaknet.multiroom.router.web.entities.Audio.AudioInfo;
import work.oaknet.multiroom.router.web.entities.Audio.Input;
import work.oaknet.multiroom.router.web.entities.Audio.Output;
import work.oaknet.multiroom.router.web.entities.Command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class AudioSourceManager {

    private static AudioSourceManager instance;

    public static AudioSourceManager getInstance() {
        return instance;
    }

    private HashMap<Client, StreamAudioSource> streamAudioSources = new HashMap<Client, StreamAudioSource>();
    public AudioSource spotifyAudioSource = new SpotifyAudioSource();
    public ArrayList<AudioSource> sources = new ArrayList<>();

    public AudioSourceManager(){
        instance = this;
        sources.add(spotifyAudioSource);
        var audioInfoThread = new Thread(()->{
            while(!Thread.interrupted()){
                var inputs = new ArrayList<Input>();
                for(var source : sources){
                    var input = new Input();
                    input.setName(source.getName());
                    input.setLevel(source.getCurrentAudioLevel());
                    var activeOutputs = new ArrayList<Output>();
                    for(var activeOutput : source.activeClients){
                        var output = new Output();
                        output.setName(activeOutput.name);
                        activeOutputs.add(output);
                    }
                    input.setActiveOutputs(activeOutputs);
                    inputs.add(input);
                }
                var audioInfo = new AudioInfo();
                audioInfo.setInputs(inputs);
                var mapper = new ObjectMapper();
                var command = new Command();
                command.setCommand("info");

                try {
                    command.setData(mapper.writeValueAsString(audioInfo));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }

                try {
                    Webserver.getInstance().getSocket().notifyClients(command);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        audioInfoThread.setDaemon(true);
        audioInfoThread.start();
    }

    public void setActiveSourceForClient(AudioSource source, Client client){
        for (var otherSource : sources) {
            synchronized (otherSource.activeClients){
                otherSource.activeClients.removeIf((otherClient) -> client == otherClient);
            }
        }
        if(source!= null) {
            synchronized (source.activeClients) {
                source.activeClients.add(client);
            }
        }
    }

    public AudioSource getSourceByName(String name){
        var result = sources.stream().filter((source)->source.name.equals(name)).findFirst();
        if(result.isPresent())
            return result.get();
        return null;
    }

    public StreamAudioSource getSourceForClient(Client client){
        if(!streamAudioSources.containsKey(client))
            streamAudioSources.put(client, new StreamAudioSource(client));
        if(!sources.contains(streamAudioSources.get(client)))
            sources.add(streamAudioSources.get(client));
        return streamAudioSources.get(client);
    }

    public void removeSourceForClient(Client client){
        if(streamAudioSources.containsKey(client)){
            if(sources.contains(streamAudioSources.get(client)))
                sources.remove(streamAudioSources.get(client));
            streamAudioSources.remove(client);
        }
    }
}
