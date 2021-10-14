import pyaudio
import audioop
import array
import datetime
import network
from datetime import timedelta,  datetime
import asyncio

activeInputDevice = 5
p = pyaudio.PyAudio()
peaks = dict()
buffer = list()
isBuffering = 0
bufferGoal = 50
bufferRange = 5
bufferRangeTight = 3
lastTimestamp=0
frameBufferSizeMultiplicator = 1

framesPerBuffer = (network.packetSize/4)*frameBufferSizeMultiplicator

def setConfig(config):
    global bufferGoal, bufferRange, bufferRangeTight
    bufferGoal = config["audioBackend"]["bufferGoal"]
    bufferRange = config["audioBackend"]["bufferRange"]
    bufferRangeTight = config["audioBackend"]["bufferRangeTight"]

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

def addInputDevice(inDeviceId, channels, samprate):
    stream = p.open(format=p.get_format_from_width(2), channels=channels,rate=samprate, input=True,input_device_index = inDeviceId, frames_per_buffer=int(framesPerBuffer), stream_callback=createCallback(inDeviceId))
    stream.start_stream()

def outCallback(inData, frame_count, time_info, status):
    global isBuffering, bufferRange, bufferRangeTight, lastTimestamp
    if(lastTimestamp>=850):
        lastTimestamp=0
        if(len(buffer)>bufferGoal+bufferRangeTight):
            buffer.pop(0)
        elif(len(buffer)<bufferGoal-bufferRangeTight):
            return b'\x00'*(network.packetSize*frameBufferSizeMultiplicator), pyaudio.paContinue
    else:
        lastTimestamp=lastTimestamp+1
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
    return buildBuffer, pyaudio.paContinue

inStream=None
lastDeviceId=None
def setVBanOutputDevice(outDeviceId, channels, samprate):
    global lastDeviceId, inStream
    if(outDeviceId == None):
        outDeviceId=lastDeviceId
    else:
        lastDeviceId=outDeviceId

    if (inStream != None):
        inStream.close()

    inStream = p.open(format = p.get_format_from_width(2), channels = channels, rate = samprate, output = True, output_device_index=outDeviceId, stream_callback=outCallback, frames_per_buffer=int(framesPerBuffer))
    inStream.start_stream()