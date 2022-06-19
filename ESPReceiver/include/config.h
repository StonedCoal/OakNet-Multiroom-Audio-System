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