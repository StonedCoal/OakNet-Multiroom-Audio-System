
packetSize=1280

import time
import struct
from typing import Counter
import platform
if(platform.system() == "Linux"):  
    import audioBackendPyAlsaAudio as audioBackend
else:
    import audioBackendPyAudio as audioBackend
import config


connectedClients = list()
socket = None
#frameCounterOut = 0
frameCounterIn = 0


def addClient(addr):
    connectedClients.append(addr)


def removeClient(addr):
    connectedClients.pop(connectedClients.index(addr))


def setSocket(socketIn):
    global socket
    socket = socketIn

#def sendBatch(data):
#    global socket, frameCounterOut
#    elapsed = time.perf_counter()
#    for i in range(0, int(len(data)/packetSize)):
#        packet = bytearray() + struct.pack(">Q", frameCounterOut) + data[i*packetSize:i*packetSize+packetSize]
#        frameCounterOut = frameCounterOut+1
#        for addr in connectedClients:
#            socket.sendto(packet, addr)


def sendHandshake():
    global socket
    clientName = config.getConfig()["network"]["clientName"]
    peer = (config.getConfig()["network"]["broadcastAddress"], config.getConfig()["network"]["port"])
    socket.sendto(b"GIMMESTREAM"+ clientName.encode("ASCII") +
                  ((32-len(clientName))*b"\0") +
                  struct.pack("!B", int(audioBackend.getVolume())) +
                  struct.pack("!B", int(audioBackend.maxVolume)) +
                  struct.pack("!H", int(audioBackend.getCurrentBufferSize())) +
                  struct.pack("!H", int(audioBackend.bufferGoal)), peer)


def receiveOnce():
    global socket, frameCounterIn
    try:
        # buffer size is normally 1436 bytes Max size for vban
        data, addr = socket.recvfrom(2048)
    except:
        return (None, None)
    message = data[:11]
    if(message == b"IMTHEROUTER"):
        return (None, None)
    if(message == b"GIMMESTREAM"):
        return (None, None)
    if(message == b"TAKECOMMAND"):
        data = data[11:]
        command = data[12:]
        if(command == b"CHANGEVOLUME"):
            newVolume = data[12]
            audioBackend.setVolume()
        elif(command ==b"CHANGEBUFFER"):
            audioBackend
        return (None, None)
    if(not(message == b"TAKIESTREAM")):
        return (None, None)
    data = data [11:]
    name = data[:32]
    data = data[32:]
    rawPcm = data[8:] 
    currentFrameCount = struct.unpack("!Q", data[:8])[0]
    if(frameCounterIn-currentFrameCount>5):
        frameCounterIn= currentFrameCount-1
    # Out of order Frame
    if(currentFrameCount <= frameCounterIn):
        print("Received out of order Frame")
        return(None, None)
    # Missing Frames
    missingframes = currentFrameCount - 1 - frameCounterIn
    if(abs(missingframes) < 5):
        while (missingframes > 0):
            print("Missing Frame")
            audioBackend.buffer.append(b'\x00'*packetSize)
            missingframes = missingframes-1
    frameCounterIn = currentFrameCount
    audioBackend.buffer.append(rawPcm[:packetSize])
    return (None, None)
