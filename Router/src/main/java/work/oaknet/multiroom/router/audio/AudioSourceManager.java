package work.oaknet.multiroom.router.audio;

import com.fasterxml.jackson.databind.ObjectMapper;
import work.oaknet.multiroom.router.audio.net.StreamAudioSource;
import work.oaknet.multiroom.router.audio.radio.RadioAudioSource;
import work.oaknet.multiroom.router.audio.spotify.SpotifyAudioSource;
import work.oaknet.multiroom.router.net.Client;
import work.oaknet.multiroom.router.web.Webserver;
import work.oaknet.multiroom.router.web.entities.Audio.ActivationPayload;
import work.oaknet.multiroom.router.web.entities.Audio.AudioInfo;
import work.oaknet.multiroom.router.web.entities.Audio.Input;
import work.oaknet.multiroom.router.web.entities.Audio.Output;
import work.oaknet.multiroom.router.web.entities.Command;

import java.util.ArrayList;
import java.util.HashMap;

public class AudioSourceManager {

    private static AudioSourceManager instance;

    public static AudioSourceManager getInstance() {
        return instance;
    }

    private HashMap<Client, StreamAudioSource> streamAudioSources = new HashMap<Client, StreamAudioSource>();
    public SpotifyAudioSource spotifyAudioSource = new SpotifyAudioSource();
    public RadioAudioSource radioAudioSource = new RadioAudioSource();
    public ArrayList<AudioSource> sources = new ArrayList<>();

    public AudioSourceManager(){
        instance = this;

        sources.add(spotifyAudioSource);
        sources.add(radioAudioSource);

        var audioInfoThread = new Thread(()->{
            while(!Thread.interrupted()){
                var inputs = new ArrayList<Input>();
                for(var source : sources){
                    var input = new Input();
                    input.setName(source.getName());
                    input.setLevel(source.getCurrentAudioLevel());
                    var activeOutputs = new ArrayList<Output>();
                    //for(var activeOutput : source.activeClients){
                    //    var output = new Output();
                    //    output.setName(activeOutput.name);
                    //    activeOutputs.add(output);
                    //}
                    //input.setActiveOutputs(activeOutputs);
                    inputs.add(input);
                }

                // WEBSERVER COMMAND
                var audioInfo = new AudioInfo();
                audioInfo.setInputs(inputs);
                var mapper = new ObjectMapper();
                var command = new Command();
                command.setCommand("info");
                command.setData(mapper.writeValueAsString(audioInfo));

                Webserver.getInstance().getSocket().notifyClients(command);

                Thread.sleep(50);

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

        // WEBSERVER COMMAND
        var payload = new ActivationPayload();
        var input=new Input();
        input.setName(source!=null?source.name:"NOTHINGTOSEEHERELOLXD_JUSTALONGSTRINGWITHMORETHAN32CHARACTERSTOPREVENTACCIDENTIALBLOCKING");
        payload.setInput(input);
        var output = new Output();
        output.setName(client.name);
        payload.setOutput(output);
        var command = new Command();
        command.setCommand("activationEvent");
        var mapper = new ObjectMapper();
        command.setData(mapper.writeValueAsString(payload));

        Webserver.getInstance().getSocket().notifyClients(command);

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
