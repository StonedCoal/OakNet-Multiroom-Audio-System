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
FRAMESIZE   = int((network.packetSize/4)*frameBufferSizeMultiplicator)

availableInputDevices = dict()
availableOutputDevices = dict()

counter = 0
for device in alsaaudio.pcms(alsaaudio.PCM_CAPTURE):
    availableInputDevices[counter] = device
    counter = counter + 1

for device in alsaaudio.pcms(alsaaudio.PCM_PLAYBACK):
    availableOutputDevices[counter] = device
    counter = counter + 1


def getAvailableInputDevices():
    return availableInputDevices

def getAvailableOutputDevices():
    return availableOutputDevices

def setConfig(config):
    global bufferGoal, bufferRange, bufferRangeTight, frameBufferSizeMultiplicator, framesPerBuffer
    bufferGoal = config["audioBackend"]["bufferGoal"]
    bufferRange = config["audioBackend"]["bufferRange"]
    bufferRangeTight = config["audioBackend"]["bufferRangeTight"]
    frameBufferSizeMultiplicator = config["audioBackend"]["frameBufferSizeMultiplicator"]
    framesPerBuffer = (network.packetSize/4)*frameBufferSizeMultiplicator

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
            if(activeInputDevice == inDeviceId):
                network.sendBatch(data)

    audioThread = threading.Thread(target=audioFunc, daemon=True)
    audioThread.start()

outAudioThread= None
run=True
def setOutputDevice(outDeviceId):
    global run, outAudioThread
    run=False
    if(outAudioThread != None):
        outAudioThread.join()
    # set up audio input
    player=alsaaudio.PCM(type=alsaaudio.PCM_PLAYBACK, device=availableInputDevices[outDeviceId])
    player.setchannels(CHANNELS)
    player.setrate(RATE)
    player.setformat(INFORMAT)
    player.setperiodsize(FRAMESIZE)
    def audioFunc():
        while run:
            global isBuffering, bufferRange, bufferRangeTight, lastTimestamp
            if(lastTimestamp>=850):
                lastTimestamp=0
                if(len(buffer)>bufferGoal+bufferRangeTight):
                    buffer.pop(0)
                elif(len(buffer)<bufferGoal-bufferRangeTight):
                    player.write(b'\x00'*(network.packetSize*frameBufferSizeMultiplicator))
                    continue
            else:
                lastTimestamp=lastTimestamp+1
            if(len(buffer) < bufferGoal - bufferRange):
                print("Underrun")
                if(isBuffering>0):
                    isBuffering=isBuffering-1
                    player.write(b'\x00'*(network.packetSize*frameBufferSizeMultiplicator))
                    continue
                else:
                    isBuffering = frameBufferSizeMultiplicator
            if(len(buffer) > bufferGoal + bufferRange):
                print("Overflow")
                buffer.pop(0)
            if(len(buffer)<frameBufferSizeMultiplicator):
                player.write(b'\x00'*(network.packetSize*frameBufferSizeMultiplicator))
                continue
            buildBuffer=buffer.pop(0)
            for i in range(0, frameBufferSizeMultiplicator-1):
                buildBuffer= buildBuffer+buffer.pop(0)
            player.write(buildBuffer)

    outAudioThread = threading.Thread(target=audioFunc, daemon=True)
    outAudioThread.start()
