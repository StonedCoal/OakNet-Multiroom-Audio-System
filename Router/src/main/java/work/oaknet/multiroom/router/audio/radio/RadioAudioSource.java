package work.oaknet.multiroom.router.audio.radio;

import work.oaknet.multiroom.router.audio.AudioSource;
import work.oaknet.multiroom.router.util.Constants;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Arrays;

public class RadioAudioSource extends AudioSource {

    private InputStream audioInputStream;

    public RadioAudioSource() {
        super("Radio");
        var radioThread = new Thread(()->{
            while(!Thread.interrupted()){
                if(audioInputStream != null) {
                    try {
                        var frame = new byte[128*Constants.CHANNELS*Constants.BYTES_PER_SAMPLE];
                        var bytesRead = 0;
                        while(bytesRead < frame.length){
                            bytesRead += audioInputStream.read(frame, bytesRead, frame.length-bytesRead);
                        }
                        synchronized (buf) {
                            if(buf.size()> 10*1024*1024)
                                buf.clear();

                            for (byte data : frame) {
                                buf.add(data);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        audioInputStream = null;
                        break;
                    } catch (StringIndexOutOfBoundsException | ArrayIndexOutOfBoundsException ignored){
                        ignored.printStackTrace();
                    }

                }else{
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        radioThread.setName("Radio-AudioSource-Provider-Thread");
        radioThread.setDaemon(true);
        radioThread.start();
    }

    public void stream(String urlString){
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return;
        }
        try {
            var inputStream = new BufferedInputStream(url.openStream());
            var mp3stream = AudioSystem.getAudioInputStream(inputStream);
            audioInputStream =  AudioSystem.getAudioInputStream(Constants.STANDARD_FORMAT, mp3stream);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        }
    }
}
