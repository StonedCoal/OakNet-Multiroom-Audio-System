#include <Arduino.h>
#include <WiFi.h>
#include <AsyncUDP.h>
#include "buffer.h"
#include <endian.h>
#include <driver/i2s.h>


#include "config.h"

#ifndef CONFIG_INCLUDED
  #define CONFIG_INCLUDED
  
  #define WIFI_SSID "WIFI_SSID"
  #define WIFI_PASSWORD "WIFI_PASS"
  #define WIFI_HOSTNAME "oaknet-audio-client"

  #define BROADCAST_ADDRESS IPAddress(192, 168, 2, 255)
  #define LOCAL_PORT 6981
  #define REMOTE_PORT 6980
  #define CLIENT_NAME "client name"
  // Due the small RAM of the ESP the buffer is limited to 60 Packets -> 75KiB RAM usage
  // We only have 160KiB dynamic RAM...
  #define MAX_BUFFER_SIZE 60
  #define BUFFER_GOAL 50
  #define BUFFER_GOAL_TIGHT 1
  #define BUFFER_TIGHT_CYCLUS 8192
  
  #define DMA_BUFFER_SIZE 64
  #define I2S_SCK_PIN 26  // SerialClock (SCK)
  #define I2S_WS_PIN 25   // WordSelect (WS)
  #define I2S_DATA_PIN 33 // Data

  #define PACKET_SIZE 1280

#endif // !CONFIG_INCLUDED

AsyncUDP udp;

Buffer* buffer = nullptr;

void initWiFi(){
  WiFi.setHostname(WIFI_HOSTNAME);
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  
  //Serial.print("Connecting to WiFi ..");
  
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
  }
}
const i2s_port_t I2S_PORT = I2S_NUM_0;

void initI2S(){
  esp_err_t err;

  // The I2S config as per the example
  const i2s_config_t i2s_config = {
      .mode = i2s_mode_t(I2S_MODE_MASTER | I2S_MODE_TX), 
      .sample_rate = 44100,
      .bits_per_sample = I2S_BITS_PER_SAMPLE_16BIT, 
      .channel_format = I2S_CHANNEL_FMT_RIGHT_LEFT, 
      .communication_format = I2S_COMM_FORMAT_STAND_I2S,
      .intr_alloc_flags = ESP_INTR_FLAG_LEVEL1, 
      .dma_buf_count = 4,
      .dma_buf_len = DMA_BUFFER_SIZE,                          
  };

  // The pin config as per the setup
  const i2s_pin_config_t pin_config = {
      .bck_io_num = I2S_SCK_PIN,   // Serial Clock (SCK)
      .ws_io_num = I2S_WS_PIN,    // Word Select (WS)
      .data_out_num = I2S_DATA_PIN, // Data
      .data_in_num = I2S_PIN_NO_CHANGE   // Serial Data (SD)
  };

  // Configuring the I2S driver and pins.
  // This function must be called before any I2S driver read/write operations.
  err = i2s_driver_install(I2S_PORT, &i2s_config, 0, NULL);
  if (err != ESP_OK) {
    //Serial.printf("Failed installing driver: %d\n", err);
    while (true);
  }
  err = i2s_set_pin(I2S_PORT, &pin_config);
  if (err != ESP_OK) {
    //Serial.printf("Failed setting pin: %d\n", err);
    while (true);
  }
  //Serial.println("I2S driver installed.");
}

void setup() {
  Serial.begin(9600);
  buffer = new Buffer(MAX_BUFFER_SIZE*PACKET_SIZE);
  initWiFi();
  if(udp.listen(6981)){
    udp.onPacket([](AsyncUDPPacket packet) {
      //Serial.print("got packet\n");
    
      int pos = 0;

      // parse command
      char command[12];
      memcpy(command, packet.data(), 11);
      command[11] = 0;
      pos += 11;
      //Serial.printf("read command: %s\n", command);
      // command: TAKIESTREAM
      if(strcmp(command, "TAKIESTREAM") == 0){
        // parse name
        char name[33];
        memcpy(name, packet.data() + pos, 32);
        name[32] = 0;
        pos += 32;
        // parse packet id
        int packetID = be64toh(*(uint64_t*) (packet.data() + pos));
        pos += 8;
        // copy audiodata
        //byte* audioData = (byte*) malloc(PACKET_SIZE);
        //memcpy(audioData, packet.data()+pos, PACKET_SIZE);
        size_t ignored;
        buffer->putBunch((packet.data()+pos), PACKET_SIZE);
        
        //Serial.printf("got Data\n");
      }
      
     });
  }
  initI2S();
}

unsigned long lastHandshake = 0;
byte* lastProcessedPacket = (byte*) malloc(PACKET_SIZE);

bool buffering = true;
int bufferSizeMedian = 0;
byte bufferSizes[BUFFER_TIGHT_CYCLUS];
int bufferSizesCount = 0;
int lastTimestamp=0;
void loop() {
  // send Handshake
  if(millis() - lastHandshake > 1000){
    byte response[11+32+1+1+2+2];
    memset(response, 0, 11+32+1+1+2+2);
    memcpy(response, "GIMMESTREAM", 11);
    memcpy(response+11, CLIENT_NAME, sizeof(CLIENT_NAME));
    response[11+32] = 100;
    response[11+32+1] = 100;
    response[11+32+1+1+1] = bufferSizeMedian;
    response[11+32+1+1+2+1] = BUFFER_GOAL;

    udp.broadcastTo(response, 11+32+1+1+2+2, 6980);
    
    //Serial.printf("Sent handshake\n");
    lastHandshake = millis();
  }

  // audio stuff

  size_t ignored;

  if(!buffering && lastTimestamp <= 0){
    int sum = 0;
    for(int i=0; i<bufferSizesCount; i++){
      sum+=bufferSizes[i];
    }
      bufferSizeMedian = sum/bufferSizesCount;
      bufferSizesCount = 0;

      Serial.printf("Current Buffer Size: %d | Goal: %d | Tight Range: %d \n", bufferSizeMedian, BUFFER_GOAL, BUFFER_GOAL_TIGHT);

      int deviation = bufferSizeMedian - BUFFER_GOAL;
      if(deviation < 0){
        deviation = -deviation;
      }
      int divisor = deviation-BUFFER_GOAL_TIGHT;
      if(divisor < 1){
        divisor = 1;
      }
      lastTimestamp = BUFFER_TIGHT_CYCLUS / divisor;
      if(bufferSizeMedian > BUFFER_GOAL + BUFFER_GOAL_TIGHT){
        buffer->popBunch(lastProcessedPacket, PACKET_SIZE);
        Serial.println("Tight Adjustment removed a Frame");
      }else if(bufferSizeMedian < BUFFER_GOAL - BUFFER_GOAL_TIGHT){
        memset(lastProcessedPacket, 0, PACKET_SIZE);
        i2s_write(I2S_PORT, lastProcessedPacket, PACKET_SIZE, &ignored, portMAX_DELAY );
        Serial.println("Tight Adjustment added a Frame");
      }
  }else if(!buffering){
    lastTimestamp--;
  }
  
  if(buffering && buffer->getCapacity() > PACKET_SIZE * BUFFER_GOAL){
    buffering = false;
  } 
  if(!buffering && buffer->getCapacity() < PACKET_SIZE){
    buffering = true;
  }
  if(buffering){
    memset(lastProcessedPacket, 0, DMA_BUFFER_SIZE);
  }
  else{
    bufferSizes[bufferSizesCount] = (buffer->getCapacity()/PACKET_SIZE);
    bufferSizesCount++;
    buffer->popBunch(lastProcessedPacket, DMA_BUFFER_SIZE);
  }
    
  //Serial.printf("Sending Data\n");
  i2s_write(I2S_PORT, lastProcessedPacket, DMA_BUFFER_SIZE, &ignored, portMAX_DELAY );
  //Serial.printf("Sent data");
}