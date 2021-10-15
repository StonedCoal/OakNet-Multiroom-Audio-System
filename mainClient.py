import network
import threading
import platform
if(platform.system() == "Linux"):  
    import audioBackendPyAlsaAudio as audioBackend
else:
    import audioBackendPyAudio as audioBackend
import socket
import os
from os import path
import json

#Config stuff

def saveConfig(config):
    jsonfile=json.dumps(config, indent=4)
    try:
        with open(os.getcwd()+"/data/client.json","w") as file:
            file.write(jsonfile)
    except IOError:
        print("Cannot open client.json")
if(not path.exists(os.getcwd()+"/data")):
    os.mkdir(os.getcwd()+"/data")

def createDefaultConfig():
    return {
        "network": {
            "routerAddress": "127.0.0.1",
            "routerPort": 6980
        },
        "audioBackend":{
            "bufferGoal": 50,
            "bufferRange": 15,
            "bufferRangeTight": 5,
            "frameBufferSizeMultiplicator": 1,
            "inDeviceId": -1
        },
    }

def loadConfig():
    if(os.path.exists(os.getcwd()+"/data/client.json")):
        try:
            with open(os.getcwd()+"/data/client.json") as file:
                jsonfile=file.read()
                config=json.loads(jsonfile)
        except IOError:
            print("Cannot open client.json")

    else:
        config=createDefaultConfig()
        saveConfig(config)
    
    if (config["audioBackend"]["inDeviceId"] == -1):
        for key, val in audioBackend.availableOutputDevices.items():
            print(str(key) + ": " + val)
        while(True):
            try:
                userInput = int(input("Which output device id: \t"))
                config["audioBackend"]["inDeviceId"] = userInput
                saveConfig(config)
                break
            except:
                print("Wrong Input!")
    return config

config = loadConfig();

#Net Socket
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM) # UDP
sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
sock.bind(("0.0.0.0", config["VBAN"]["port"]))

# networkstuff
network.setSocket(sock)

#Audio Backend stuff
audioBackend.setConfig(config)
audioBackend.setOutputDevice(config["VBAN"]["inDeviceId"])

def recvFunc():
    while True:
        result = network.receiveOnce();

recvThread = threading.Thread(target=recvFunc, daemon=True)
recvThread.start();


import time
import os

# Main Loop
counter = 0
while True:
    time.sleep(0.1)
    #print (len(audioBackend.buffer))
    if(counter % 10 == 0):
        network.sendHandshake((config["network"]["routerAddress"], config["network"]["routerPort"]))
    counter+=1
    if(counter >=100000):
        counter = 0
    #result= "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n"
    #for id, val in audioBackend.peaks.items():
    #        bar = ":" * int(val/20)
    #        result+='RMS: [%d] %s' % (id, bar)+"\n"
    #print(result ,end="\r")