package work.oaknet.multiroom.router.net;

import java.net.InetAddress;
import java.security.PublicKey;

public class Client {

    public String name;
    public InetAddress address;
    public int port;
    public long frameCounter = 0;
    public long lastTimeStamp;

    public int currentBufferSize;
    public int desiredBufferSize;
    public int currentVolume;
    public int maxVolume;
}
