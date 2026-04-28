package com.monitoring.devicemontoring.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Server-side remote interface for the Devicelytics monitoring system.
 *
 * <p>This interface defines the methods that the monitoring server exposes to connected clients. It
 * facilitates client registration, telemetry data ingestion, and heartbeat management.
 *
 * @author Devicelytics Team
 */
public interface IMonitorServer extends Remote {

  /**
   * Registers a client instance with the server to enable bidirectional communication.
   *
   * @param hostname the unique hostname of the client device
   * @param clientStub the RMI stub used by the server to send commands back to the client
   * @throws RemoteException if a communication error occurs during the RMI call
   */
  void registerClient(String hostname, IMonitorClient clientStub) throws RemoteException;

  /**
   * Unregisters a client from the server's active monitoring list.
   *
   * @param hostname the hostname of the client to disconnect
   * @throws RemoteException if a communication error occurs during the RMI call
   */
  void unregisterClient(String hostname) throws RemoteException;

  /**
   * Receives a heartbeat signal from a client to verify its online status.
   *
   * @param hostname the hostname of the client sending the signal
   * @throws RemoteException if a communication error occurs during the RMI call
   */
  void keepAlive(String hostname) throws RemoteException;

  /**
   * Accepts a telemetry data push from a client.
   *
   * <p>This method allows clients to actively update their status on the server, ensuring real-time
   * visibility in the dashboard.
   *
   * @param status a {@link DeviceStatus} object containing the latest system metrics
   * @throws RemoteException if a communication error occurs during the RMI call
   */
  void pushStatus(DeviceStatus status) throws RemoteException;

  /**
   * Responds to discovery requests from clients on the network.
   *
   * @return a server identifier or status confirmation string
   * @throws RemoteException if a communication error occurs during the RMI call
   */
  String discoverServer() throws RemoteException;
}
