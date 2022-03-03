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
bufferTightCyclus = 850
bufferMedian = list()
lastTimestamp = 0
frameBufferSizeMultiplicator = 1

isInitialized = False

# constants
CHANNELS    = 1
INFORMAT    = alsaaudio.PCM_FORMAT_S16_LE
RATE        = 44100
framesPerBuffer   = int((network.packetSize/4)*frameBufferSizeMultiplicator)

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
    framesPerBuffer = int((network.packetSize/4)*frameBufferSizeMultiplicator)

def addInputDevice(inDeviceId):
    # set up audio input
    recorder=alsaaudio.PCM(type=alsaaudio.PCM_CAPTURE, device=availableInputDevices[inDeviceId])
    recorder.setchannels(CHANNELS)
    recorder.setrate(RATE)
    recorder.setformat(INFORMAT)
    recorder.setperiodsize(framesPerBuffer)
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
    player=alsaaudio.PCM(type=alsaaudio.PCM_PLAYBACK, device=availableOutputDevices[outDeviceId])
    player.setchannels(CHANNELS)
    player.setrate(RATE)
    player.setformat(INFORMAT)
    player.setperiodsize(framesPerBuffer)
    def audioFunc():
        while run:
            global isBuffering, isInitialized, bufferRange, bufferRangeTight, bufferTightCyclus, bufferMedian, lastTimestamp
            if(not isInitialized):
                if(len(buffer) < bufferGoal):
                    player.write(b'\x00'*(network.packetSize*frameBufferSizeMultiplicator))
                    continue
                else:
                    isInitialized = True
                    
            if(len(buffer) == 0):
                isInitialized = False
                                
            bufferMedian.append(len(buffer))
            if(lastTimestamp<=0):
                print("Current Buffer Size: " + str(len(buffer)) + " | Goal: " + str(bufferGoal))
                sum = 0
                for value in bufferMedian:
                    sum = sum + value;
                median = sum / len(bufferMedian)
                bufferMedian.clear()
                divisor = abs(median-bufferGoal) - bufferRangeTight
                if(divisor < 1):
                    divisor = 1
                lastTimestamp = bufferTightCyclus / divisor
                
                if(median>bufferGoal+bufferRangeTight):
                    buffer.pop(0)
                elif(median<bufferGoal-bufferRangeTight):
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
    
    run=True
    outAudioThread = threading.Thread(target=audioFunc, daemon=True)
    outAudioThread.start()
