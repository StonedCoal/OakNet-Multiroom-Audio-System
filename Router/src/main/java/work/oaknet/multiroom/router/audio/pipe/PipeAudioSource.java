package work.oaknet.multiroom.router.audio.pipe;

import work.oaknet.multiroom.router.audio.AudioSource;
import work.oaknet.multiroom.router.util.Constants;

import java.io.File;

public class PipeAudioSource extends AudioSource {

    boolean isWindows = System.getProperty("os.name")
            .toLowerCase().startsWith("windows");

    public PipeAudioSource(String name, String command) {
        super(name);

        ProcessBuilder builder = new ProcessBuilder();
        if (isWindows) {
            builder.command("cmd.exe", "/c", command);
        } else {
            builder.command("sh", "-c", command);
        }
        Process process = builder.start();
        Thread t = new Thread(()->{
            while(true){
                addAudioData(process.inputStream.readNBytes(4096));
            }
        }, name + "-provider-thread");
        t.start();
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
