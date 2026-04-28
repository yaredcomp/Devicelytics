package com.monitoring.devicemontoring.common;

import java.io.Serializable;

/**
 * Data transfer object representing a running system process.
 *
 * <p>This class contains key metrics for an individual process, including its ID, name, resource
 * consumption (CPU/Memory), and current execution state.
 *
 * @author Devicelytics Team
 */
public class ProcessInfo implements Serializable {

  private static final long serialVersionUID = 1L;

  /** The unique Process ID (PID). */
  public int pid;

  /** The executable name of the process. */
  public String name;

  /** Percentage of CPU utilization by this process. */
  public double cpuPercent;

  /** Total physical memory (RSS) consumed by this process in bytes. */
  public long memUsed;

  /** The current execution state (e.g., RUNNING, SLEEPING, STOPPED). */
  public String status;

  /**
   * Constructs a new ProcessInfo object with performance metrics.
   *
   * @param pid the unique process identifier
   * @param name the name of the process executable
   * @param cpuPercent the current CPU usage percentage
   * @param memUsed total physical memory consumed in bytes
   * @param status the current state of the process
   */
  public ProcessInfo(int pid, String name, double cpuPercent, long memUsed, String status) {
    this.pid = pid;
    this.name = name;
    this.cpuPercent = cpuPercent;
    this.memUsed = memUsed;
    this.status = status;
  }

  /**
   * Gets the Process ID.
   *
   * @return the pid
   */
  public int getPid() {
    return pid;
  }

  /**
   * Gets the process name.
   *
   * @return the executable name string
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the CPU usage percentage.
   *
   * @return usage percentage (0.0 - 100.0)
   */
  public double getCpuPercent() {
    return cpuPercent;
  }

  /**
   * Gets the memory consumption.
   *
   * @return memory used in bytes
   */
  public long getMemUsed() {
    return memUsed;
  }

  /**
   * Gets the process state.
   *
   * @return status string
   */
  public String getStatus() {
    return status;
  }
}
