# Retro LAN Server
<p align="center">
  <img src="./res/logo.png" alt="logo">
<p>
Share your local game server over the internet and connect with others.

## Concept

ReLServer can be run in two modes:

- **CLIENT** - when you want to connect to other people's servers. ReLServer creates a "phantom" server on your LAN that will listen for requests from your application and proxy to the real server over the internet.
- **SERVER** - when you want to share your server. ReLServer will manage p2p connections with the corresponding clients and proxy them to the real server.
- **HUB** - public peer-to-peer connection exchange server. Receives the public IP address of a peer connection and notifies clients and servers of new connections. (STUN server concept)

ReLServer can be run on another device that connected to the local network when running on the target device is not possible.
ReLServer in SERVER mode by default proxies connections to localhost:appPort. You can specify a device with a running server in the local network for proxy connections to its port.


## How to run

### Windows/Mac/Linux
Download a jar file from the Releases tab and run in terminal:
```bash
java -jar relserver.jar -mode=client -appId=appIdFromCatalog
#or
#localServerIp - address of the real server in the local network
java -jar relserver.jar -mode=server -appId=appIdFromCatalog -localServerIp=192.168.0.123
```

### Android
Download a jar file from the Releases tab. Install Termux (or another terminal app). Install Java. Open terminal and run ReLServer. 
```bash
#Install Java
pkg install openjdk-17

java -jar relserver.jar -mode=server -appId=appIdFromCatalog
#or
java -jar relserver.jar -mode=client -appId=appIdFromCatalog
```
**Coming soon:** Install app from Play Market


### Another devices
If the terminal application and Java are not supported on the device, you can run ReLServer on a supported device and connect both of them to the local network.


## FAQ

**How to create a private server?**

- You need to generate a unique -appId value (For example: private24532) 
- All participants must specify it when starting ReLServer in the -appId parameter 
along with the application port. (For example: -appId=private24532 -appPort=7893)

**How to play a game that is not on the list?**
- To do this, you need to download Wireshark and start scanning the local network
- Find out the IP address of the device on which the game will be launched
- Search for LAN servers in the game
- In Wireshark, see to what address and port the UDP packet was sent from the device IP (For example: 255.255.255.255:7893)
- Launch ReLServer on several devices, specify the port and game ID in the launch parameters (For example: -appPort=7893 -appId=glmc3fn)
- Check the connection and the ability to play over the Internet
- If it works, add the game to the list of supported ones. Make PR to src/main/resources/applications.json