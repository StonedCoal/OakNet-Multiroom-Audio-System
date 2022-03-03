package work.oaknet.mad.multiroomclient;


import java.net.DatagramSocket;

//This class closes a provided socket when the underlying thread is closed
// this allows sock.receive() to be canceled;
public class InterruptableUdpThread extends Thread{

    public DatagramSocket sock;

    public InterruptableUdpThread(Runnable run, String name){
        super(run, name);
    }

    @Override
    public void interrupt() {
        super.interrupt();
        sock.close();
    }
}
