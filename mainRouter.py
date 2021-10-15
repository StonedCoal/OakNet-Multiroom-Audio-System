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
        #print("Output Device id ", i, " - ", p.get_device_info_by_host_api_device_index(0, i).get('name'))

import threading
import audioBackend
import datetime
from datetime import datetime
import socket
import time
import os
import os.path
from os import path
from flask import Flask, render_template, request
import json
import sys
import network

#Config stuff
if(not path.exists(os.getcwd()+"/data")):
    os.mkdir(os.getcwd()+"/data")

def createDefaultConfig():
    return {
        "VBAN":{
            "port":6980,
            "outStreamName": "Stream1",
            "inStreamName": "Stream1",
            "inDeviceId": "-1",
            "selectedOutput": "-1"
        },
        "audioBackend":{
            "bufferGoal": 50,
            "bufferRange": 10,
            "bufferRangeTight": 12
        },
        "activeInputDevices":{}
    }

def loadConfig():
    if(os.path.exists(os.getcwd()+"/data/router.json")):
        try:
            with open(os.getcwd()+"/data/router.json") as file:
                jsonfile=file.read()
                config=json.loads(jsonfile)
        except IOError:
            print("Cannot open router.json")

    else:
        config=createDefaultConfig()
        saveConfig(config)
    return config


def saveConfig(config):
    jsonfile=json.dumps(config, indent=4)
    try:
        with open(os.getcwd()+"/data/router.json","w") as file:
            file.write(jsonfile)
    except IOError:
        print("Cannot open router.json")


config=loadConfig();
#VBAN Socket
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM) # UDP
sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
sock.bind(("0.0.0.0", int(config["VBAN"]["port"])))

#network Stuff
network.setSocket(sock)

#Audio Backend stuff
audioBackend.setConfig(config)
for deviceID in config["activeInputDevices"]:
    audioBackend.addInputDevice(int(deviceID), 2, 44100)

#Audio Backend stuff
#audioBackend.setVBanOutputDevice(int(config["VBAN"]["selectedOutput"]), 2, 44100)

connectedClients = dict();
def recvFunc():
    global connectedClients
    while True:
        addr = network.receiveOnce()
        if(addr[0]!=None):
            if(not addr in connectedClients):
                print("Client "+addr[0] +" connected")
                network.addClient(addr)
            connectedClients[addr] = datetime.now()
                

recvThread = threading.Thread(target=recvFunc, daemon=True)
recvThread.start();

clear = lambda: os.system('cls')
#time.sleep(0.1)
#audioBackend.isBuffering=False;
counter = 0

def mainFunc():
    global counter, connectedClients
    while True:
        time.sleep(0.1)
        #print(len(audioBackend.buffer))
        #result= "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n"
        #for id, val in audioBackend.peaks.items():
        #    bar = ":" * int(val/20)
        #    result+='RMS: [%d] %s' % (id, bar)+"\n"
        #print(result ,end="\r")
        #print(connectedClients)
        if(counter % 20 == 0):
            for addr, timestamp in list(connectedClients.items()):
                if(timestamp == -1):
                    continue
                if((datetime.now() - timestamp).total_seconds()>=4):
                    print("Client "+addr[0] +" disconnected")
                    connectedClients.pop(addr, None);
                    network.removeClient(addr)
        counter+=1
        if(counter >=100000):
            counter = 0

mainThread = threading.Thread(target=mainFunc, daemon=True)
mainThread.start();

#Webserver Stuff
app = Flask(__name__)

import logging
log = logging.getLogger('werkzeug')
log.setLevel(logging.ERROR)

@app.route("/")
def index():
    return render_template("index.html", inputs=audioBackend.peaks.keys(), availableInputs=availableInputDevices, availableOutputs=availableOutputDevices, config=config)

@app.route("/info")
def info():
    result = dict()
    result["inputs"]=dict();
    result["inputs"]["active_input"]=audioBackend.activeInputDevice
    result["inputs"]["input_levels"]=audioBackend.peaks
    rewritten=dict()
    for key, val in connectedClients.items():
        rewritten[key[0]+":"+str(key[1])] = str(val)
    result["clients"]=rewritten
    return json.dumps(result)

@app.route("/switchinput/<int:input>", methods = ['POST'])
def switchinput(input):
    audioBackend.activeInputDevice = input;
    return ""

@app.route("/addstaticstream/<string:address>", methods = ['POST'])
def addstaticstream(address):
    try:
        
        if(":" not in address):
            address=address+":6980"
        chunks = address.split(":")
        addr= (chunks[0], int(chunks[1]))
        network.addClient(addr)
        connectedClients[addr] = -1
        print("debug")
    except:
        pass
    return ""

@app.route("/savesettings", methods = ['POST'])
def saveSettings():
    response=request.json
    config["VBAN"] = response["VBAN"]
    config["activeInputDevices"] = response["activeInputDevices"]
    saveConfig(config)
    os.execl(sys.executable, os.path.abspath(__file__), *sys.argv)


if(__name__ == "__main__"):
    app.run(host="0.0.0.0", port=8080, debug=False)
