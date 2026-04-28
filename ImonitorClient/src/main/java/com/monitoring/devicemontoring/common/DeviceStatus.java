package com.monitoring.devicemontoring.common;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a comprehensive snapshot of a device's runtime status.
 *
 * <p>This class encapsulates various system metrics including hardware specifications, performance
 * statistics, security information, and active process data. It is designed to be serialized across
 * RMI for centralized monitoring and analytical processing.
 *
 * @author Devicelytics Team
 * @version 3.0
 */
public class DeviceStatus implements Serializable {

  private static final long serialVersionUID = 3L;

  /** The network hostname of the device. */
  public String hostname;

  /** The primary IPv4 address of the device. */
  public String ipAddress;

  /** The MAC address of the primary network interface. */
  public String macAddress;

  /** The BIOS or Hardware Universally Unique Identifier (UUID). */
  public String uuid;

  /** The system serial number. */
  public String serialNumber;

  /** The family name of the operating system (e.g., Windows, Linux). */
  public String osName;

  /** The specific version or build number of the operating system. */
  public String osVersion;

  /** The total system uptime in seconds. */
  public long uptimeSecs;

  /** The epoch timestamp (in seconds) of the last system boot. */
  public long lastBootTime;

  /** The descriptive name of the CPU model. */
  public String cpuModel;

  /** Current system-wide CPU load percentage (0.0 to 100.0). */
  public double cpuLoad;

  /** Current CPU temperature in degrees Celsius, if available. */
  public double cpuTemp;

  /** Number of physical CPU cores. */
  public int physicalCores;

  /** Number of logical processors (threads). */
  public int logicalProcessors;

  /** System load averages for the last 1, 5, and 15 minutes. */
  public double[] loadAverage;

  /** Total number of context switches since boot. */
  public long contextSwitches;

  /** Total number of hardware interrupts since boot. */
  public long interrupts;

  /** Total physical RAM in bytes. */
  public long totalRam;

  /** Currently available physical RAM in bytes. */
  public long availableRam;

  /** List of detected Graphics Processing Units (GPUs) and their status. */
  public List<GPUInfo> gpus;

  /** Current network transmission speed in bytes per second. */
  public long netTxSpeed;

  /** Current network reception speed in bytes per second. */
  public long netRxSpeed;

  /** The type of active network connection (e.g., Wi-Fi, Ethernet). */
  public String networkType;

  /** Round-trip time latency to the monitoring server in milliseconds. */
  public double latencyMs;

  /** Total storage capacity across all mounted file systems in bytes. */
  public long totalStorage;

  /** Total used storage space in bytes. */
  public long usedStorage;

  /** Simplified health status from S.M.A.R.T. monitoring. */
  public String diskSmartStatus;

  /** Current battery level percentage (0-100), or -1 if no battery is detected. */
  public int batteryPercentage = -1;

  /** List of usernames currently logged into the system. */
  public List<String> activeUsers;

  /** Total number of open network ports currently listening. */
  public int openPortsCount;

  /** List of top resource-consuming processes. */
  public List<ProcessInfo> topProcesses;

  /** Total number of active processes. */
  public int numberOfProcesses;

  /** Total number of active threads across all processes. */
  public int numberOfThreads;

  /**
   * Returns the current CPU load percentage.
   *
   * @return a double representing CPU load (0.0 - 100.0)
   */
  public double getCpuLoad() {
    return cpuLoad;
  }

  /**
   * Returns the current CPU temperature.
   *
   * @return temperature in Celsius
   */
  public double getCpuTemp() {
    return cpuTemp;
  }

  /**
   * Returns the hostname of the device.
   *
   * @return the hostname string
   */
  public String getHostname() {
    return hostname;
  }

  /**
   * Returns the primary IP address of the device.
   *
   * @return the IP address string
   */
  public String getIpAddress() {
    return ipAddress;
  }

  /**
   * Calculates the current RAM usage percentage.
   *
   * @return a double representing RAM usage (0.0 - 100.0)
   */
  public double getRamUsage() {
    if (totalRam == 0) {
      return 0;
    }
    return ((double) (totalRam - availableRam) / totalRam) * 100;
  }

  /**
   * Provides a human-readable string of current RAM usage (e.g., "4.2 / 16.0 GB").
   *
   * @return a formatted string representing RAM usage
   */
  public String getFormattedRamUsage() {
    double usedGb = (totalRam - availableRam) / (1024.0 * 1024.0 * 1024.0);
    double totalGb = totalRam / (1024.0 * 1024.0 * 1024.0);
    return String.format("%.1f / %.1f GB", usedGb, totalGb);
  }
}
