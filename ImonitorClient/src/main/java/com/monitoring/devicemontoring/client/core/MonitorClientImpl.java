package com.monitoring.devicemontoring.client.core;

import com.monitoring.devicemontoring.common.DeviceStatus;
import com.monitoring.devicemontoring.common.GPUInfo;
import com.monitoring.devicemontoring.common.IMonitorClient;
import com.monitoring.devicemontoring.common.IMonitorServer;
import com.monitoring.devicemontoring.common.ProcessInfo;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OSProcess;
import oshi.software.os.OSSession;
import oshi.software.os.OperatingSystem;

/**
 * Implementation of the monitoring client that gathers system telemetry.
 *
 * <p>This class uses the OSHI library to collect detailed hardware and software metrics from the
 * host machine. It exposes these metrics via RMI and supports remote command execution.
 *
 * @author Devicelytics Team
 */
public class MonitorClientImpl extends UnicastRemoteObject implements IMonitorClient {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = LoggerFactory.getLogger(MonitorClientImpl.class);

  private final HardwareAbstractionLayer hardware;
  private final OperatingSystem os;
  private long[] prevCpuTicks;
  private long prevNetRx = 0;
  private long prevNetTx = 0;
  private long prevNetTime = 0;

  /**
   * Initializes the monitoring client and its hardware/software abstractions.
   *
   * @throws Exception if system information cannot be accessed
   */
  public MonitorClientImpl() throws Exception {
    SystemInfo systemInfo = new SystemInfo();
    this.hardware = systemInfo.getHardware();
    this.os = systemInfo.getOperatingSystem();
    this.prevCpuTicks = hardware.getProcessor().getSystemCpuLoadTicks();
    this.prevNetTime = System.currentTimeMillis();

    // Initialize network counters
    for (NetworkIF net : hardware.getNetworkIFs()) {
      net.updateAttributes();
      prevNetRx += net.getBytesRecv();
      prevNetTx += net.getBytesSent();
    }
  }

  /**
   * Gathers a comprehensive snapshot of system metrics.
   *
   * @return a populated {@link DeviceStatus} object
   */
  @Override
  public DeviceStatus getCurrentStatus() {
    DeviceStatus status = new DeviceStatus();
    CentralProcessor cpu = hardware.getProcessor();
    GlobalMemory mem = hardware.getMemory();

    try {
      // 1. Basic Identification
      status.hostname = getHostname();
      status.ipAddress = getActiveIp();
      status.macAddress = getMacAddress(); // Added MAC Address
      status.uuid = hardware.getComputerSystem().getHardwareUUID();
      status.serialNumber = hardware.getComputerSystem().getSerialNumber(); // Added Serial Number
      status.osName = os.getFamily();
      status.osVersion = os.getVersionInfo().getVersion();
      status.uptimeSecs = os.getSystemUptime();
      status.lastBootTime = System.currentTimeMillis() / 1000 - status.uptimeSecs; // Added Last Boot Time

      // 2. CPU Performance
      status.cpuModel = cpu.getProcessorIdentifier().getName();
      status.cpuLoad = cpu.getSystemCpuLoadBetweenTicks(prevCpuTicks) * 100;
      prevCpuTicks = cpu.getSystemCpuLoadTicks();
      status.physicalCores = cpu.getPhysicalProcessorCount(); // Added Physical Cores
      status.logicalProcessors = cpu.getLogicalProcessorCount(); // Added Logical Processors
      status.loadAverage = cpu.getSystemLoadAverage(3);

      try {
        status.cpuTemp = hardware.getSensors().getCpuTemperature();
      } catch (Exception e) {
        status.cpuTemp = 0;
      }

      // 3. Memory & GPU
      status.totalRam = mem.getTotal();
      status.availableRam = mem.getAvailable();
      status.gpus =
          hardware.getGraphicsCards().stream()
              .map(g -> new GPUInfo(g.getName(), g.getVendor(), g.getVRam()))
              .collect(Collectors.toCollection(ArrayList::new));

      // 4. Network, Storage, Battery
      calculateNetworkSpeed(status);
      status.networkType = detectNetworkType(); // Added Network Type
      calculateStorage(status);
      status.batteryPercentage =
          hardware.getPowerSources().isEmpty()
              ? -1
              : (int) (hardware.getPowerSources().get(0).getRemainingCapacityPercent() * 100);

      // 5. Security & Processes
      status.activeUsers =
          os.getSessions().stream().map(OSSession::getUserName).distinct().collect(Collectors.toList());
      status.numberOfProcesses = os.getProcessCount();
      status.numberOfThreads = os.getThreadCount();

      // Top 5 Resource Intensive Processes
      List<OSProcess> procs =
          os.getProcesses(
              OperatingSystem.ProcessFiltering.ALL_PROCESSES, OperatingSystem.ProcessSorting.CPU_DESC, 5);
      status.topProcesses =
          procs.stream()
              .map(
                  p ->
                      new ProcessInfo(
                          p.getProcessID(),
                          p.getName(),
                          100d * (p.getKernelTime() + p.getUserTime()) / Math.max(1, p.getUpTime()),
                          p.getResidentSetSize(),
                          p.getState().toString()))
              .collect(Collectors.toList());

    } catch (Exception e) {
      logger.error("Error during telemetry collection", e);
    }
    return status;
  }

  /**
   * Calculates instantaneous network transmission and reception speeds.
   *
   * @param status the status object to populate with network speeds
   */
  private void calculateNetworkSpeed(DeviceStatus status) {
    long currentRx = 0;
    long currentTx = 0;
    for (NetworkIF net : hardware.getNetworkIFs()) {
      net.updateAttributes();
      currentRx += net.getBytesRecv();
      currentTx += net.getBytesSent();
    }
    long now = System.currentTimeMillis();
    long timeDiff = now - prevNetTime;
    if (timeDiff > 0) {
      status.netRxSpeed = (currentRx - prevNetRx) * 1000 / timeDiff;
      status.netTxSpeed = (currentTx - prevNetTx) * 1000 / timeDiff;
    }
    prevNetRx = currentRx;
    prevNetTx = currentTx;
    prevNetTime = now;
  }

  /**
   * Gathers storage capacity and utilization data across all file systems.
   *
   * @param status the status object to populate with storage information
   */
  private void calculateStorage(DeviceStatus status) {
    long totalFs = 0;
    long usableFs = 0;
    for (oshi.software.os.OSFileStore fs : os.getFileSystem().getFileStores()) {
      totalFs += fs.getTotalSpace();
      usableFs += fs.getUsableSpace();
    }
    status.totalStorage = totalFs;
    status.usedStorage = totalFs - usableFs;
  }

  /**
   * Handles remote command requests from the server.
   *
   * @param command the command identifier
   * @param args the command string or arguments
   * @return a string result from the command execution
   */
  @Override
  public String executeRemoteCommand(String command, String args) {
    try {
      switch (command.toLowerCase()) {
        case "ping":
          return "Pong! Connection is active.";
        case "info":
          return String.format(
              "System Info:\n- OS: %s\n- CPU: %s\n- RAM: %.1f GB",
              os.getFamily(),
              hardware.getProcessor().getProcessorIdentifier().getName(),
              hardware.getMemory().getTotal() / 1073741824.0);
        case "shell":
          return executeShellCommand(args);
        default:
          return "Error: Unknown command '" + command + "'";
      }
    } catch (Exception e) {
      return "Execution failed: " + e.getMessage();
    }
  }

  /**
   * Executes a command via the system's native shell.
   *
   * @param cmd the shell command to run
   * @return the captured output or error message
   */
  private String executeShellCommand(String cmd) {
    if (cmd == null || cmd.trim().isEmpty()) {
      return "Error: Empty command.";
    }
    StringBuilder output = new StringBuilder();
    try {
      String osName = System.getProperty("os.name").toLowerCase();
      ProcessBuilder pb =
          osName.contains("win")
              ? new ProcessBuilder("cmd.exe", "/c", cmd)
              : new ProcessBuilder("/bin/sh", "-c", cmd);
      pb.redirectErrorStream(true);
      Process process = pb.start();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          output.append(line).append("\n");
        }
      }
      process.waitFor(15, TimeUnit.SECONDS);
      return output.length() == 0 ? "Done (No output)." : output.toString();
    } catch (Exception e) {
      return "Error: " + e.getMessage();
    }
  }

  /**
   * Retrieves the system's local hostname.
   *
   * @return the hostname string
   */
  @Override
  public String getHostname() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      return "Unknown-PC";
    }
  }

  /**
   * Identifies the primary active non-loopback IPv4 address.
   *
   * @return the IP address string
   */
  private String getActiveIp() {
    return hardware.getNetworkIFs().stream()
        .filter(n -> n.getIPv4addr().length > 0 && !n.getIPv4addr()[0].startsWith("127"))
        .findFirst()
        .map(n -> n.getIPv4addr()[0])
        .orElse("0.0.0.0");
  }

  /**
   * Identifies the MAC address of the primary non-loopback network interface.
   *
   * @return the MAC address string
   */
  private String getMacAddress() {
    return hardware.getNetworkIFs().stream()
        .filter(n -> !n.getMacaddr().isEmpty())
        .findFirst()
        .map(NetworkIF::getMacaddr)
        .orElse("Unknown");
  }

  /**
   * Detects the type of the primary active network interface (e.g., Wi-Fi, Ethernet).
   *
   * @return the network type string
   */
  private String detectNetworkType() {
    return hardware.getNetworkIFs().stream()
        .filter(n -> n.getIPv4addr().length > 0)
        .findFirst()
        .map(n -> n.getDisplayName().toLowerCase().contains("wi-fi") ? "Wi-Fi" : "Ethernet")
        .orElse("Unknown");
  }

  /** lightweight RMI check. */
  @Override
  public void ping() {}

  /**
   * Main entry point for the monitoring client.
   *
   * @param args optional server IP address
   */
  public static void main(String[] args) {
    String serverIp = args.length > 0 ? args[0] : "localhost";
    logger.info("Devicelytics Client starting...");

    while (true) {
      try {
        MonitorClientImpl client = new MonitorClientImpl();
        String hostname = client.getHostname();

        logger.info("Connecting to server at {}...", serverIp);
        Registry registry = LocateRegistry.getRegistry(serverIp, 1099);
        IMonitorServer server = (IMonitorServer) registry.lookup("MonitorServer");

        server.registerClient(hostname, client);
        logger.info("Successfully connected to Devicelytics Server.");

        // Continuous telemetry push loop
        while (true) {
          server.pushStatus(client.getCurrentStatus());
          Thread.sleep(5000);
        }
      } catch (java.rmi.ConnectException | java.rmi.NoSuchObjectException e) {
        logger.warn("Server at {} is currently unreachable. Retrying in 10s...", serverIp);
      } catch (Exception e) {
        logger.error("Unexpected connection error: {}. Re-attempting in 10s...", e.getMessage());
      }

      try {
        Thread.sleep(10000);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }
}
