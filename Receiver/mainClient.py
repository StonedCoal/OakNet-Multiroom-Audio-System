
import network
import threading
import platform
if(platform.system() == "Linux"):  
    import audioBackendPyAlsaAudio as audioBackend
    from audioBackendPyAlsaAudio import getCurrentBufferSize
else:
    import audioBackendPyAudio as audioBackend
    from audioBackendPyAudio import getCurrentBufferSize
import socket
import os
from os import path
import config

#Net Socket
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM) # UDP
sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
sock.bind(("0.0.0.0", config.getConfig()["network"]["port"]))

# networkstuff
network.setSocket(sock)

#Audio Backend stuff
audioBackend.setOutputDevice(config.getConfig()["audioBackend"]["inDeviceId"])
audioBackend.setConfig(config.getConfig())

def recvFunc():
    while True:
        result = network.receiveOnce();

recvThread = threading.Thread(target=recvFunc, daemon=True)
recvThread.start();


import time
import os

while True:
    time.sleep(0.5)
    network.sendHandshake()