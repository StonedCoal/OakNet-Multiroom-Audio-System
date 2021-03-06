package work.oaknet.multiroom.router.audio.spotify;

import com.spotify.connectstate.Connect;
import org.jetbrains.annotations.NotNull;
import work.oaknet.multiroom.router.util.Constants;
import work.oaknet.multiroom.router.audio.AudioSource;
import xyz.gianlu.librespot.ZeroconfServer;
import xyz.gianlu.librespot.audio.decoders.AudioQuality;
import xyz.gianlu.librespot.core.Session;
import xyz.gianlu.librespot.player.Player;
import xyz.gianlu.librespot.player.PlayerConfiguration;
import xyz.gianlu.librespot.player.ShellEvents;

import java.io.IOException;

public class SpotifyAudioSource extends AudioSource {

    long lastTimeStamp = System.nanoTime();

    public SpotifyAudioSource() {
        super("Spotify");
        var playerConfig = new PlayerConfiguration.Builder()
                .setOutput(PlayerConfiguration.AudioOutput.CUSTOM)
                .setOutputClass("work.oaknet.multiroom.router.audio.spotify.SpotifyAudioSink")
                .setAutoplayEnabled(true)
                .setPreferredQuality(AudioQuality.VERY_HIGH)
                .build();

        ShellEvents shellEvents;
        ShellEvents.Configuration eventsShellConf = new ShellEvents.Configuration.Builder()
                .setEnabled(false)
                .build();
        if (eventsShellConf.enabled) shellEvents = new ShellEvents(eventsShellConf);
        else shellEvents = null;

        var server = new ZeroconfServer.Builder()
                .setDeviceId("edcf2d00-923f-11ec-b909-0242ac120002ke83")
                .setDeviceName(Constants.SPOTIFY_CONNECT_DEVICE_NAME)
                .setDeviceType(Connect.DeviceType.SPEAKER)
                .create();

        server.addSessionListener(new ZeroconfServer.SessionListener() {

            Player lastPlayer = null;

            {
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    if (lastPlayer != null) lastPlayer.close();
                }));
            }

            @Override
            public void sessionClosing(@NotNull Session session) {

            }

            @Override
            public void sessionChanged(@NotNull Session session) {
                // You cant close not authentificated sessions? That sounds like a severe bug and memoryleak
                //if (lastPlayer != null) lastPlayer.close()
                lastPlayer = new Player(playerConfig, session);

            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.closeSession();
            server.close();
        }));
    }

    @Override
    public void addAudioData(byte[] frame) {
        super.addAudioData(frame);
        while(buf.size() > Constants.PACKET_SIZE * 2 ){
            if(buf.size() > Constants.PACKET_SIZE * 8){
                buf.clear();
            }
            Thread.sleep(1);
        }
    }
}
