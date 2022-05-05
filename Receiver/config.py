import json
import os
from os import path
import platform
if(platform.system() == "Linux"):  
    import audioBackendPyAlsaAudio as audioBackend
else:
    import audioBackendPyAudio as audioBackend


#Config stuff

config = None;

def saveConfig():
    global config
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
            "broadcastAddress": "255.255.255.255",
            "port": 6980,
            "clientName": "clientName"
        },
        "audioBackend":{
            "bufferGoal": 50,
            "bufferRange": 15,
            "bufferRangeTight": 5,
            "frameBufferSizeMultiplicator": 1,
            "maxVolume":"100",
            "availableAudioDevices": audioBackend.availableOutputDevices,
            "inDeviceId": -1
        },
    }

def loadConfig():
    global config
    if(os.path.exists(os.getcwd()+"/data/client.json")):
        try:
            with open(os.getcwd()+"/data/client.json") as file:
                jsonfile=file.read()
                config=json.loads(jsonfile)
        except IOError:
            print("Cannot open client.json")

    else:
        config=createDefaultConfig()
        saveConfig()
    
    if (config["audioBackend"]["inDeviceId"] == -1):
        pass
    
def getConfig():
    global config
    if(config == None):
        loadConfig()
    return config
