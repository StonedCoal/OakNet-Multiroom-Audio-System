package work.oaknet.multiroom.router.audio.spotify;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import work.oaknet.multiroom.router.audio.AudioSourceManager;
import xyz.gianlu.librespot.player.mixing.output.OutputAudioFormat;
import xyz.gianlu.librespot.player.mixing.output.SinkException;
import xyz.gianlu.librespot.player.mixing.output.SinkOutput;

import java.io.IOException;

public class SpotifyAudioSink implements SinkOutput {
    @Override
    public boolean start(@NotNull OutputAudioFormat format) throws SinkException {
        return true;
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
        if(length == 0)
            return;
        AudioSourceManager.getInstance().spotifyAudioSource.addAudioData(bytes);

    }

    @Override
    public boolean setVolume(@Range(from = 0L, to = 1L) float volume) {
        return false;
    }

    @Override
    public void release() {
    }

    @Override
    public void drain() {
    }

    @Override
    public void flush() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void close() throws IOException {

    }
}
