from kivy.app import App
from kivy.uix.button import Button
from pyVBAN import *
import threading
from audioBackend import *
import socket
import time

class Mybutton(Button):
    text="Click me!"
    on_press =lambda a : print("My Button")    
    
class TutorialApp(App):
    def build(self):
        #create AudioBackend
        self.audioBackend = MobileAudioBackend()

        #VBAN Socket
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM) # UDP
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.sock.bind(("0.0.0.0", 6980))

        # VBAN Receive Stuff
        self.vbanRecv = VBAN_Recv(streamName="Stream1", socket=self.sock, audioBackend=self.audioBackend, verbose=False)

        def recvFunc():
            while True:
                self.result = self.vbanRecv.runonce();
        
        self.recvThread = threading.Thread(target=recvFunc, daemon=True)
        self.recvThread.start()

        #VBAN Text Stuff
        self.vbanText = VBAN_SendText(streamName="Stream1", socket=self.sock, verbose=False)

        #Audio Backend stuff
        #audioBackend.setVBanOutputDevice(16, 2, 44100)
        def mainLoopFunc():
            # Main Loop
            counter = 0;
            while True:
                time.sleep(0.1)
                #print (len(audioBackend.buffer))
                if(counter % 10 == 0):
                    self.vbanText.send("GIMMESTREAM", "192.168.2.42", 6981)
                counter+=1
                if(counter >=100000):
                    counter = 0
        self.mainThread = threading.Thread(target=mainLoopFunc, daemon=True)
        self.mainThread.start()

        return Mybutton()

TutorialApp().run()
