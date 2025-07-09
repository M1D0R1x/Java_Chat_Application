# Java Chat Application

This is a simple Java-based chat application that allows **two users** to connect and chat with each other over a local network. The application consists of two components:

- `Server.java`: The server-side component that manages client connections.
- `Client.java`: The client-side component that enables individual users to connect to the server and communicate.

## Getting Started

### Prerequisites

- Java JDK 8 or higher
- Any IDE (e.g., IntelliJ, Eclipse) or terminal with Java configured
- Local network setup (for client-server communication)

### Running the Application

1. **Clone the repository**:
   ```bash
   git clone https://github.com/M1D0R1x/Java_Chat_Application.git
   cd Java_Chat_Application
   ```
2. **Run the Server**:

   First, you need to run the Server.java file to start the chat server. This can be done in an IDE or via the terminal.
   ```bash
   javac Server.java
   java Server

<!-- Blank line added here for separation -->

   The server will automatically fetch your local IP address and start listening for client connections.

3. **Run the Client**:

   After the server is running, you can run the Client.java file to connect a client to the server.
   ```bash	
   javac Client.java
   java Client

<!-- Blank line added here for separation -->

   By default, the client will attempt to connect to the server using the local IP. The application allows two users to connect and chat at the same time.

## Running the Chat Application on Two Different Systems

This section explains how to run the **Server** on one machine and the **Client** on another, allowing them to communicate over the same Wi-Fi network.

### Prerequisites:
- Both machines must be connected to the **same Wi-Fi network**.
- Ensure that the **port number** used in both the server and client code is the same.

### Steps for Server (Machine 1):
1. Open the **Server** application code.
2. Find the local IP address of the machine:
   - Open **Command Prompt** and type the following command:
     ```bash
     ipconfig
     ```
   - Look for the **IPv4 Address** under the active network (e.g., `192.168.x.x` or `172.x.x.x`). This is the IP address your friend (the client) will need.
3. Start the **Server** application by running the program. It will now wait for the client to connect.

### Steps for Client (Machine 2):
1. Obtain the IP address of the server machine (provided by your friend in the previous step).
2. Open the **Client** application code.
3. Modify the following line in the client code:
   ```java
   socket = new Socket("SERVER_IP_ADDRESS", PORT_NUMBER);

<!-- Blank line added here for separation -->

   - Replace SERVER_IP_ADDRESS with the actual IP address of the server machine (e.g., 192.168.1.5), and ensure the PORT_NUMBER is the same as used in the server.

<!-- Blank line added here for separation -->


4. Run the Client application. It will attempt to connect to the server using the provided IP address and port.


## Features

   - Automatic local IP detection

   - Two users can connect to the server and chat simultaneously

   - Simple text-based communication

## Future Enhancements

   - Add support for multiple users to connect to the chat

   - Add encryption for secure communication
   - 
