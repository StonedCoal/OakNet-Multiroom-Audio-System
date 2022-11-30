
using NAudio.Wave;
using Streamer;
using System.Net.Sockets;
using System.Text;

// Some constants 
var address = "192.168.2.20";
var port = 6980;
var packetSize = 1280;
var channels = 2;
var bytesPerSample = 2;
var samplerate = 44100;
var framesPerPacket = packetSize / (channels * bytesPerSample);
var timePerPacketInNs = (int)(1000000000 / (samplerate / (double)framesPerPacket));



// Create socket to send data
var socket = new UdpClient();

// packetCounter is used to determine if the packets arrive in the right order
// shall be counted upwards by one per packet
long packetCounter = 0;

var buffer = new RingBuffer(200*packetSize);
var capture = new WasapiLoopbackCapture();
capture.WaveFormat = new WaveFormat(44100, 16, 2);
capture.DataAvailable += (sender, args) =>
{
    Monitor.Enter(buffer);
    buffer.putBunch(args.Buffer, args.BytesRecorded);
    Monitor.PulseAll(buffer);
    Monitor.Exit(buffer);
};
capture.StartRecording();

var command = new ASCIIEncoding().GetBytes("TAKIESTREAM");
var name = new ASCIIEncoding().GetBytes("Test-Stream");
var packet = new byte[packetSize + 11 + 32 + 8]; // PacketSize + Command(8byte) + name(32byte) + PacketID (8byte) 
Buffer.BlockCopy(command, 0, packet, 0, 11);
Buffer.BlockCopy(name, 0, packet, 11, name.Length);

//sendLoop
while (true)
{
    while (buffer.getCapacity() > packetSize)
    {
        // copy the PacketID into the packet
        // Note: Network uses Big Endian so we need to reverse the byteorder of the counter
        Buffer.BlockCopy(BitConverter.GetBytes(packetCounter++).Reverse().ToArray(), 0, packet, 11 + 32, 8);
        //Add audioData
        byte[] audioData;
        Monitor.Enter(buffer);
        buffer.popBunch(out audioData, packetSize);
        Monitor.Exit(buffer);
        Buffer.BlockCopy(audioData, 0, packet, 11 + 32 + 8, packetSize);
        // send the packet to the router
        socket.Send(packet, address, port);

    }
    Monitor.Enter(buffer);
    Monitor.Wait(buffer);
    Monitor.Exit(buffer);
}

