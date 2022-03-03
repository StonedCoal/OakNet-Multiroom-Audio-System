
using NAudio.Wave;
using System.Diagnostics;
using System.Net.Sockets;
using System.Text;

// Some constants 
var address = "127.0.0.1";
var port = 6980;
var packetSize = 1280;
var channels = 2;
var bytesPerSample = 2;
var samplerate = 44100;
var framesPerPacket = packetSize / (channels * bytesPerSample);
var timePerPacketInNs = (int)(1000000000 / (samplerate / (double)framesPerPacket));


// Read WAV File using NAudio lib
byte[] audioData;

using (var wav = new AudioFileReader(args[0]))
{
    audioData = new byte[wav.Length];
    wav.Read(audioData, 0, audioData.Length);
}


//Build Packets we're just looping through one WAV File so we can prepare them in advance
var packets = new List<byte[]>();
for (int i = 0; i < audioData.Length; i += packetSize)
{
    //Look at PROTOCOLL in the root of the repo
    //Note: packetID will be filled in later
    var command = new ASCIIEncoding().GetBytes("TAKIESTREAM");
    var name = new ASCIIEncoding().GetBytes("Test-Stream");
    var payload = new byte[packetSize + 11 + 32 + 8]; // PacketSize + Command(8byte) + name(32byte) + PacketID (8byte) 
    Buffer.BlockCopy(command, 0, payload, 0, 11); 
    Buffer.BlockCopy(name, 0, payload, 11, name.Length);
    Buffer.BlockCopy(audioData, i, payload, 11 + 32 + 8, Math.Min(packetSize, audioData.Length-i));
    packets.Add(payload);
}

// Create socket to send data
var socket = new UdpClient();

// packetCounter is used to determine if the packets arrive in the right order
// shall be counted upwards by one per packet
long packetCounter = 0;

// Timing stuff because we don't have a real soundcard as source
long packetTimer = 0;
long actualTime;

//sendLoop
while (true)
{
    // Save StartTime
    actualTime = nanoTime();

    // Send as many packets as we missed while sleeping
    // Should optimally only be one but due to Thread.Sleep not being accurate 
    // it's probably more than one after a couple of cycles
    while (packetTimer > timePerPacketInNs)
    {
        // get the next packet from the prepared list
        var packet = packets[(int)(packetCounter % packets.Count)];
        // copy the PacketID into the packet
        Buffer.BlockCopy(BitConverter.GetBytes(packetCounter++), 0, packet, 11 + 32, 8);
        // send the packet to the router
        socket.Send(packet, address, port);
        // subtract one packet-time from the overall packet timer
        packetTimer-=timePerPacketInNs;
    }
    // Not accurate sleeping
    Thread.Sleep(new TimeSpan(timePerPacketInNs / 100));
    // Messure the time we actually slept - the time that the sending procesure took
    packetTimer += nanoTime() - actualTime;
}

//https://stackoverflow.com/questions/1551742/what-is-the-equivalent-to-system-nanotime-in-net
long nanoTime()
{
    long nano = 10000L * Stopwatch.GetTimestamp();
    nano /= TimeSpan.TicksPerMillisecond;
    nano *= 100L;
    return nano;
}
