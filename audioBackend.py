from typing import SupportsRound


class PcAudioBackend(object):
    import pyaudio
    import audioop
    def __init__(self) -> None:
        self.activeInputDevice = 5
        self.p = self.pyaudio.PyAudio()
        self.peaks = dict()
        self.buffer = list()
        self.isBuffering = False
        self.bufferGoal = 15
        self.bufferRange = 5    
        self.inStream=None
        self.lastDeviceId=None

    def setVBANSend(self, vbanSend_):
        self.vbanSendObj = vbanSend_

    def createCallback(self,deviceId):
        def callbackFunc(in_data, frame_count, time_info, status):
            peakVal = self.audioop.rms(in_data, 2)    
            self.peaks[deviceId] = peakVal
            if(self.activeInputDevice == deviceId):
                self.vbanSendObj.runonce(in_data)
            return None, self.pyaudio.paContinue
        return callbackFunc

    def addInputDevice(self, inDeviceId, channels, samprate):
        stream = self.p.open(format=self.p.get_format_from_width(2), channels=channels,rate=samprate, input=True,input_device_index = inDeviceId, frames_per_buffer=255, stream_callback=self.createCallback(inDeviceId))
        stream.start_stream()

    def outCallback(self, inData, frame_count, time_info, status):
        if(len(self.buffer) < self.bufferGoal - self.bufferRange):
            self.isBuffering = True
        if(self.isBuffering and len(self.buffer) >= self.bufferGoal):
            self.isBuffering = False
        while(len(self.buffer) > self.bufferGoal + self.bufferRange):
            self.buffer.pop(0)
        if(self.isBuffering):
            return b'\x00'*1024, self.pyaudio.paContinue
        return self.buffer.pop(0), self.pyaudio.paContinue


    def setVBanOutputDevice(self, outDeviceId, channels, samprate):
        if(outDeviceId == None):
            outDeviceId=self.lastDeviceId
        else:
            self.lastDeviceId=outDeviceId

        if (self.inStream != None):
            self.inStream.close()

        self.inStream = self.p.open(format = self.p.get_format_from_width(2), channels = channels, rate = samprate, output = True, output_device_index=outDeviceId, stream_callback=self.outCallback, frames_per_buffer=255)
        self.inStream.start_stream()

# This only supports output
class MobileAudioBackend(object):
    import audiostream
    def __init__(self) -> None:
        self.isBuffering = False
        self.bufferGoal = 15
        self.bufferRange = 5    
        self.inStream=None
        self.lastDeviceId=None
        self.outputStream = self.audiostream.get_output(44100, 2, 16, 255) 
        self.source = self.MySoundSource(self)
        self.source.start()

    def setVBanOutputDevice(self, outDeviceId, channels, samprate):
        pass
    
    class MySoundSource(audiostream.sources.thread.ThreadSource):
        def __init__(self, owner) -> None:
            super().__init__()
            self.owner = owner

        def get_bytes(self):
            if(len(self.owner.buffer) < self.owner.bufferGoal - self.owner.bufferRange):
                self.owner.isBuffering = True
            if(self.owner.isBuffering and len(self.owner.buffer) >= self.owner.bufferGoal):
                self.owner.isBuffering = False
            while(len(self.owner.buffer) > self.owner.bufferGoal + self.owner.bufferRange):
                self.owner.buffer.pop(0)
            if(self.owner.isBuffering):
                return b'\x00'*1024
            return self.buffer.pop(0)