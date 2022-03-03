package work.oaknet.multiroom.router.audio;

import work.oaknet.multiroom.router.audio.net.StreamAudioSource;
import work.oaknet.multiroom.router.audio.spotify.SpotifyAudioSource;
import work.oaknet.multiroom.router.net.Client;

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
    }

    public void setActiveSourceForClient(AudioSource source, Client client){
        for (var otherSource : sources) {
            synchronized (otherSource.activeClients){
                otherSource.activeClients.removeIf((otherClient) -> client == otherClient);
            }
        }
        synchronized (source.activeClients) {
            source.activeClients.add(client);
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
