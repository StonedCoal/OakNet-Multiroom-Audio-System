package work.oaknet.multiroom.router.audio.radio;

import work.oaknet.multiroom.router.audio.AudioSource;
import work.oaknet.multiroom.router.util.Constants;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

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
                        addAudioData(frame);
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
            var mp3Format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, (float) mp3stream.getFormat().getSampleRate(), Constants.BYTES_PER_SAMPLE  * 8 , Constants.CHANNELS, 4, (float) mp3stream.getFormat().getSampleRate(), false);
            var temp = AudioSystem.getAudioInputStream(mp3Format, mp3stream);
            audioInputStream = AudioSystem.getAudioInputStream(Constants.STANDARD_FORMAT, temp);
            buf.clear();
        } catch (IOException | UnsupportedAudioFileException e) {
            e.printStackTrace();
        }
    }
}
