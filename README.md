# Devicelytics: Enterprise System Monitoring & Analytics

**Devicelytics** is a robust, distributed system monitoring solution designed to provide real-time analytical insights into a fleet of devices. It consists of a lightweight Client agent and a centralized JavaFX-based Server Dashboard.

## 🚀 Key Features

### 📊 Real-time Analytical Dashboard

- **Performance Tracking**: Monitor CPU load averages (1/5/15m), RAM usage, and network throughput across all connected devices.
- **Hardware Insights**: Detailed views for CPU models, thermal sensors, storage health, and battery status.
- **Process Monitoring**: Track the top 5 resource-intensive processes on every machine.

### 🛡️ Security & Compliance

- **Active User Tracking**: See who is currently logged into any device in your network.
- **Unique Identification**: Persistent tracking of devices using BIOS/Hardware UUIDs, regardless of IP changes.
- **Network Visibility**: Identify connection types (Wi-Fi vs. Ethernet) and MAC addresses.

### 🎮 Remote Management (Command Center)

- **Terminal Access**: Execute secure shell commands directly on remote clients (Windows CMD / Linux Bash).
- **Quick Diagnostics**: One-click "Ping Tests" and "System Info" reports.
- **Live Console**: Monospaced terminal UI with auto-scrolling and timestamped command history.

## 🛠️ Technology Stack

- **Java 11/17**: Core language for both Client and Server.
- **OSHI (Operating System and Hardware Information)**: High-performance library for native metric collection.
- **Java RMI (Remote Method Invocation)**: Efficient, low-latency communication protocol between agents and the server.
- **JavaFX**: Modern, responsive desktop UI for the administrative dashboard.
- **Ikonli**: Font-awesome integration for a professional look and feel.

## 📁 Project Structure

- `ImonitorClient`: Lightweight agent that runs on target machines.
- `ImonitorServer`: Centralized hub that collects data and provides the management UI.
- `common`: Shared RMI interfaces and data models (DeviceStatus, GPUInfo, etc.).

## 📖 How to Run

### Server

1. Run `ServerApp.java`.
2. The server will start an RMI registry on port `1099` and begin broadcasting its presence on the local network (port `8888`).

### Client

1. Run `MonitorClientImpl.java`.
2. The client will automatically discover the server on the network (or you can provide the IP as an argument).
3. If the server goes down, the client will gracefully log a retry message and attempt to reconnect every 10 seconds.

## 📸 Screenshots

Here are some screenshots of the application in action:

1. ![Screenshot 1](screenshots/1.png)
2. ![Screenshot 2](screenshots/2.png)
3. ![Screenshot 3](screenshots/3.png)
4. ![Screenshot 4](screenshots/4.png)
5. ![Screenshot 5](screenshots/5.png)

---

_Developed for Proactive System Maintenance and Analytical Oversight._
