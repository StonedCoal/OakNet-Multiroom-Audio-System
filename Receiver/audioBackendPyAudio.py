import pyaudio
import audioop
import array
import datetime
import network
from datetime import timedelta,  datetime
import asyncio

import pyaudio

import numpy

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

activeInputDevice = 5
p = pyaudio.PyAudio()
peaks = dict()
buffer = list()
isBuffering = 0
bufferGoal = 50
bufferRange = 5
bufferRangeTight = 3
bufferTightCyclus = 850
lastTimestamp=0
frameBufferSizeMultiplicator = 1
bufferSizeMedian = list()
maxVolume = 100
volume = 100

isInitialized = False

framesPerBuffer = (network.packetSize/4)*frameBufferSizeMultiplicator


def getAvailableInputDevices():
    return availableInputDevices

def getAvailableOutputDevices():
    return availableOutputDevices

def setConfig(config):
    global bufferGoal, bufferRange, bufferRangeTight, frameBufferSizeMultiplicator, framesPerBuffer, maxVolume
    bufferGoal = config["audioBackend"]["bufferGoal"]
    bufferRange = config["audioBackend"]["bufferRange"]
    bufferRangeTight = config["audioBackend"]["bufferRangeTight"]
    frameBufferSizeMultiplicator = config["audioBackend"]["frameBufferSizeMultiplicator"]
    maxVolume = config["audioBackend"]["maxVolume"]
    framesPerBuffer = (network.packetSize/4)*frameBufferSizeMultiplicator
    
def changeVolume(chunkRaw, volume):
    global maxVolume
    volumeNormalized = (maxVolume/100.0)*volume
    sound_level = (volumeNormalized / 100.0)

    dt = numpy.dtype(numpy.int16)
    dt = dt.newbyteorder("<")
    chunkNumpy = numpy.frombuffer(chunkRaw, dtype=dt)
    chunkNumpy = chunkNumpy * sound_level
    chunkNumpy = chunkNumpy.astype(dt)
    return chunkNumpy.tobytes()


async def send(data):
    network.sendBatch(data)

async def calcPeak(data, deviceId):
    peakVal = audioop.rms(data, 2)    
    peaks[deviceId] = peakVal

def createCallback(deviceId):
    def callbackFunc(in_data, frame_count, time_info, status):
        in_data = bytearray(in_data)
        asyncio.run(calcPeak(in_data[:128], deviceId))
        if(activeInputDevice == deviceId):
            asyncio.run(send(in_data))
        return None, pyaudio.paContinue
    return callbackFunc

def addInputDevice(inDeviceId):
    stream = p.open(format=p.get_format_from_width(2), channels=2,rate=44100, input=True,input_device_index = inDeviceId, frames_per_buffer=int(framesPerBuffer), stream_callback=createCallback(inDeviceId))
    stream.start_stream()

def outCallback(inData, frame_count, time_info, status):
    global isBuffering, isInitialized, bufferRange, bufferRangeTight, bufferTightCyclus, lastTimestamp, bufferSizeMedian, volume
    bufferSizeMedian.append(len(buffer))
    if(not isInitialized):
        if(len(buffer) < bufferGoal):
            return b'\x00'*(network.packetSize*frameBufferSizeMultiplicator), pyaudio.paContinue
        else:
            isInitialized = True
    
    if(len(buffer) == 0):
        isInitialized = False
    
    if(lastTimestamp<=0):
        print("Current Buffer Size: " + str(len(buffer)) + " | Goal: " + str(bufferGoal))
        sum = 0
        for value in bufferSizeMedian:
            sum = sum + value;
        median = sum / len(bufferSizeMedian)
        bufferSizeMedian.clear()
        divisor = abs(median-bufferGoal) - bufferRangeTight
        if(divisor < 1):
            divisor = 1
        lastTimestamp = bufferTightCyclus / divisor
        if(median>bufferGoal+bufferRangeTight):
            buffer.pop(0)
        elif(median<bufferGoal-bufferRangeTight):
            return b'\x00'*(network.packetSize*frameBufferSizeMultiplicator), pyaudio.paContinue
       
    else:
        lastTimestamp=lastTimestamp-1
    if(len(buffer) < bufferGoal - bufferRange):
        print("Underrun")
        if(isBuffering>0):
            isBuffering=isBuffering-1
            return b'\x00'*(network.packetSize*frameBufferSizeMultiplicator), pyaudio.paContinue
        else:
            isBuffering = frameBufferSizeMultiplicator
    if(len(buffer) > bufferGoal + bufferRange):
        print("Overflow")
        buffer.pop(0)
    if(len(buffer)<frameBufferSizeMultiplicator):
        return b'\x00'*(network.packetSize*frameBufferSizeMultiplicator), pyaudio.paContinue
    buildBuffer=buffer.pop(0)
    for i in range(0, frameBufferSizeMultiplicator-1):
        buildBuffer= buildBuffer+buffer.pop(0)
    
    buildBuffer = changeVolume(buildBuffer, volume)
    
    return buildBuffer, pyaudio.paContinue

inStream=None
lastDeviceId=None
def setOutputDevice(outDeviceId):
    global lastDeviceId, inStream
    if(outDeviceId == None):
        outDeviceId=lastDeviceId
    else:
        lastDeviceId=outDeviceId

    if (inStream != None):
        inStream.close()

    inStream = p.open(format = p.get_format_from_width(2), channels = 2, rate = 44100, output = True, output_device_index=outDeviceId, stream_callback=outCallback, frames_per_buffer=int(framesPerBuffer))
    inStream.start_stream()