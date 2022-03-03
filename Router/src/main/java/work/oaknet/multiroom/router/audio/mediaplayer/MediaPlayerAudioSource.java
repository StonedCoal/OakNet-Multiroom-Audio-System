package work.oaknet.multiroom.router.audio.mediaplayer;

import work.oaknet.multiroom.router.audio.AudioSource;

public class MediaPlayerAudioSource extends AudioSource {
    public MediaPlayerAudioSource() {
        super("Media-Player");
        var mediaPlayerThread = new Thread(()->{

        });
        mediaPlayerThread.setName("Media-Player-Thread");
        mediaPlayerThread.setDaemon(true);
        mediaPlayerThread.start();
    }

    public void stream(String URL){

    }
}
