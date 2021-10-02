import pyaudio
import audioop

activeInputDevice = 5
p = pyaudio.PyAudio()
peaks = dict()
buffer = list()
isBuffering = False
bufferGoal = 15
bufferRange = 5

def setVBANSend(vbanSend_):
    global vbanSendObj
    vbanSendObj = vbanSend_

def createCallback(deviceId):
    def callbackFunc(in_data, frame_count, time_info, status):
        peakVal = audioop.rms(in_data, 2)    
        peaks[deviceId] = peakVal
        if(activeInputDevice == deviceId):
            vbanSendObj.runonce(in_data)
        return None, pyaudio.paContinue
    return callbackFunc

def addInputDevice(inDeviceId, channels, samprate):
    stream = p.open(format=p.get_format_from_width(2), channels=channels,rate=samprate, input=True,input_device_index = inDeviceId, frames_per_buffer=255, stream_callback=createCallback(inDeviceId))
    stream.start_stream()

def outCallback(inData, frame_count, time_info, status):
    global isBuffering, bufferRange
    if(len(buffer) < bufferGoal - bufferRange):
        isBuffering = True
    if(isBuffering and len(buffer) >= bufferGoal):
        isBuffering = False
    while(len(buffer) > bufferGoal + bufferRange):
        buffer.pop(0)
    if(isBuffering):
        return b'\x00'*1024, pyaudio.paContinue
    return buffer.pop(0), pyaudio.paContinue

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

    inStream = p.open(format = p.get_format_from_width(2), channels = channels, rate = samprate, output = True, output_device_index=outDeviceId, stream_callback=outCallback, frames_per_buffer=255)
    inStream.start_stream()