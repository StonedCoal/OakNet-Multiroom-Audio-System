from pyVBAN import *
import threading
from audioBackend import *
import socket

#create AudioBackend
audioBackend = PcAudioBackend()

#VBAN Socket
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM) # UDP
sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
sock.bind(("0.0.0.0", 6980))

# VBAN Receive Stuff
vbanRecv = VBAN_Recv(streamName="Stream1", socket=sock, audioBackend=audioBackend, verbose=False)

def recvFunc():
    while True:
        result = vbanRecv.runonce();

recvThread = threading.Thread(target=recvFunc, daemon=True)
recvThread.start();

#VBAN Text Stuff
vbanText = VBAN_SendText(streamName="Stream1", socket=sock, verbose=False)

#Audio Backend stuff
audioBackend.setVBanOutputDevice(16, 2, 44100)

import time
import os

# Main Loop
counter = 0;
while True:
    time.sleep(0.1)
    print (len(audioBackend.buffer))
    if(counter % 10 == 0):
        vbanText.send("GIMMESTREAM", "192.168.2.42", 6981)
    counter+=1
    if(counter >=100000):
        counter = 0
    #result= "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n"
    #for id, val in audioBackend.peaks.items():
    #        bar = ":" * int(val/20)
    #        result+='RMS: [%d] %s' % (id, bar)+"\n"
    #print(result ,end="\r")