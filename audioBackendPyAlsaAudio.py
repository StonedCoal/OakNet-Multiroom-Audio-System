import threading
import alsaaudio
import audioop
import array
import datetime
import network
from datetime import timedelta,  datetime
import asyncio

activeInputDevice = 5
peaks = dict()
buffer = list()
isBuffering = 0
bufferGoal = 50
bufferRange = 5
bufferRangeTight = 3
lastTimestamp=0
frameBufferSizeMultiplicator = 1

# constants
CHANNELS    = 1
INFORMAT    = alsaaudio.PCM_FORMAT_S16_LE
RATE        = 44100
FRAMESIZE   = 1024

framesPerBuffer = (network.packetSize/4)*frameBufferSizeMultiplicator

availableInputDevices = dict()
availableOutputDevices = dict()

counter = 0
for device in alsaaudio.pcms(alsaaudio.PCM_CAPTURE):
    availableInputDevices[counter] = device
    counter = counter + 1

for device in alsaaudio.pcms(alsaaudio.PCM_PLAYBACK ):
    availableOutputDevices[counter] = device
    counter = counter + 1


def getAvailableInputDevices():
    return availableInputDevices

def getAvailableOutputDevices():
    return availableOutputDevices

def setConfig(config):
    global bufferGoal, bufferRange, bufferRangeTight
    bufferGoal = config["audioBackend"]["bufferGoal"]
    bufferRange = config["audioBackend"]["bufferRange"]
    bufferRangeTight = config["audioBackend"]["bufferRangeTight"]

def addInputDevice(inDeviceId):
    # set up audio input
    recorder=alsaaudio.PCM(type=alsaaudio.PCM_CAPTURE, device=availableInputDevices[inDeviceId])
    recorder.setchannels(CHANNELS)
    recorder.setrate(RATE)
    recorder.setformat(INFORMAT)
    recorder.setperiodsize(FRAMESIZE)
    def audioFunc():
        while True:
            data = bytearray(recorder.read()[1])
            peakVal = audioop.rms(data, 2)    
            peaks[inDeviceId] = peakVal

    audioThread = threading.Thread(audioFunc, daemon=True)
    audioThread.start()
