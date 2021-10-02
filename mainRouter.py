from typing import Protocol
import pyaudio
p = pyaudio.PyAudio()
info = p.get_host_api_info_by_index(0)
numdevices = info.get('deviceCount')

print("--- INPUT DEVICES ---")
for i in range(0, numdevices):
        if (p.get_device_info_by_host_api_device_index(0, i).get('maxInputChannels')) > 0:
            print("Input Device id ", i, " - ", p.get_device_info_by_host_api_device_index(0, i).get('name'))
            
print("--- OUTPUT DEVICES ---")
for i in range(0, numdevices):
        if (p.get_device_info_by_host_api_device_index(0, i).get('maxOutputChannels')) > 0:
            print("Input Device id ", i, " - ", p.get_device_info_by_host_api_device_index(0, i).get('name'))

from pyVBAN import *
import threading
import datetime
from datetime import datetime
import socket
from audioBackend import *

#create AudioBackend
audioBackend = PcAudioBackend()

#VBAN Socket
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM) # UDP
sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
sock.bind(("0.0.0.0", 6981))

#VBan Send Stuff
vbanSend = VBAN_Send(streamName="Stream1", socket=sock, sampRate=44100, verbose=False)

#Audio Backend stuff Note: this approach is not great, should be reworked
audioBackend.setVBANSend(vbanSend)

audioBackend.addInputDevice(5, 2, 44100)
audioBackend.addInputDevice(7, 2, 44100)

# VBAN Receive Stuff
vbanRecv = VBAN_Recv(streamName="Stream1", socket=sock, audioBackend=audioBackend, verbose=False)

connectedClients = dict();
def recvFunc():
    while True:
        Text, addr = vbanRecv.runonce()
        if(Text == "GIMMESTREAM"):
                if(not addr in connectedClients):
                        print("Client "+addr[0] +" connected")
                        vbanSend.addClient(addr)
                connectedClients[addr] = datetime.now()
                

recvThread = threading.Thread(target=recvFunc, daemon=True)
recvThread.start();

#Audio Backend stuff
#audioBackend.setVBanOutputDevice(18, 2, 44100)

import time
import os
clear = lambda: os.system('cls')
#time.sleep(0.1)
#audioBackend.isBuffering=False;
counter = 0
while True:
    time.sleep(0.1)
    #print (len(audioBackend.buffer))
    result= "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n"
    for id, val in audioBackend.peaks.items():
            bar = ":" * int(val/20)
            result+='RMS: [%d] %s' % (id, bar)+"\n"
    #print(result ,end="\r")
    if(counter % 20 == 0):
            for addr, timestamp in list(connectedClients.items()):
                    if((datetime.now() - timestamp).total_seconds()>=4):
                        print("Client "+addr[0] +" disconnected")
                        connectedClients.pop(addr, None);
                        vbanSend.removeClient(addr)
    counter+=1
    if(counter >=100000):
        counter = 0

