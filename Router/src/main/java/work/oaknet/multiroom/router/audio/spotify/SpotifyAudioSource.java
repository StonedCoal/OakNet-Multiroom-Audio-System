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
        try{
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
                    if (lastPlayer != null) lastPlayer.close();
                }

                @Override
                public void sessionChanged(@NotNull Session session) {
                    lastPlayer = new Player(playerConfig, session);

                    if (shellEvents != null) {
                        session.addReconnectionListener(shellEvents);
                        lastPlayer.addEventsListener(shellEvents);
                    }
                }
            });

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    server.closeSession();
                    server.close();
                } catch (IOException ignored) {
                }
            }));
        }catch (IOException ioE){

        }

    }

    @Override
    public void addAudioData(byte[] frame) {
        super.addAudioData(frame);

    }
}
