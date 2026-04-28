package com.monitoring.devicemontoring.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote client interface for the Devicelytics monitoring system.
 *
 * <p>This interface defines the methods that a monitoring client must expose to the server.
 * It allows the server to pull system status, measure connectivity, and execute
 * diagnostic commands remotely.
 *
 * @author Devicelytics Team
 */
public interface IMonitorClient extends Remote {

  /**
   * Retrieves a real-time snapshot of the device's system metrics.
   *
   * @return a {@link DeviceStatus} object containing current system information
   * @throws RemoteException if a communication error occurs during the RMI call
   */
  DeviceStatus getCurrentStatus() throws RemoteException;

  /**
   * Retrieves the configured network hostname of the client device.
   *
   * @return the hostname string as defined on the client system
   * @throws RemoteException if a communication error occurs during the RMI call
   */
  String getHostname() throws RemoteException;

  /**
   * Performs a lightweight heart-beat check to measure reachability and round-trip latency.
   *
   * @throws RemoteException if the client is unreachable or a communication error occurs
   */
  void ping() throws RemoteException;

  /**
   * Executes a remote diagnostic or system command on the client machine.
   *
   * @param command the type of command to execute (e.g., "ping", "shell", "info")
   * @param args the arguments or command string to be processed by the client's shell
   * @return a string containing the standard output or error message from the command execution
   * @throws RemoteException if a communication error occurs during the RMI call
   */
  String executeRemoteCommand(String command, String args) throws RemoteException;
}
