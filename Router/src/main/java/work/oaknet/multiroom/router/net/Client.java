package work.oaknet.multiroom.router.net;

import manifold.ext.props.rt.api.var;

import java.net.InetAddress;
import java.security.PublicKey;

public class Client {

    @var String name;
    @var InetAddress address;
    @var int port;
    @var public long frameCounter = 0;
    @var long lastTimeStamp;

    @var int currentBufferSize;
    @var int desiredBufferSize;
    @var int currentVolume;
    @var int maxVolume;
}
