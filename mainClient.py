import network
import threading
import audioBackend
import socket
import os
from os import path
import json
import pyaudio
p = pyaudio.PyAudio()
info = p.get_host_api_info_by_index(0)
numdevices = info.get('deviceCount')
availableInputDevices = dict()
availableOutputDevices = dict()

for i in range(0, numdevices):
    if (p.get_device_info_by_host_api_device_index(0, i).get('maxInputChannels')) > 0:
        availableInputDevices[i] = p.get_device_info_by_host_api_device_index(0, i).get('name');          

for i in range(0, numdevices):
    if (p.get_device_info_by_host_api_device_index(0, i).get('maxOutputChannels')) > 0:
        availableOutputDevices[i] = p.get_device_info_by_host_api_device_index(0, i).get('name');

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
        "VBAN":{
            "port":6980,
            "inStreamName": "Stream1",
            "inDeviceId": -1
        },
        "audioBackend":{
            "bufferGoal": 50,
            "bufferRange": 10,
            "bufferRangeTight": 3
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
    
    if (config["VBAN"]["inDeviceId"] == -1):
        for key, val in availableOutputDevices.items():
            print(str(key) + ": " + val)
        while(True):
            try:
                userInput = int(input("Which output device id: \t"))
                config["VBAN"]["inDeviceId"] = userInput
                saveConfig(config)
                break
            except:
                print("Wrong Input!")
    return config

config = loadConfig();

#VBAN Socket
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM) # UDP
sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
sock.bind(("0.0.0.0", config["VBAN"]["port"]))

# networkstuff
network.setSocket(sock)

#Audio Backend stuff
audioBackend.setConfig(config)
audioBackend.setVBanOutputDevice(config["VBAN"]["inDeviceId"], 2, 44100)

def recvFunc():
    while True:
        result = network.receiveOnce();

recvThread = threading.Thread(target=recvFunc, daemon=True)
recvThread.start();


import time
import os

# Main Loop
counter = 0;
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