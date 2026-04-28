package com.monitoring.devicemontoring.server.core;

import com.monitoring.devicemontoring.common.DeviceStatus;
import com.monitoring.devicemontoring.common.IMonitorClient;
import com.monitoring.devicemontoring.common.IMonitorServer;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the centralized monitoring server.
 *
 * <p>This class manages remote client registrations, receives telemetry data pushes, and provides a
 * bridge for the UI to send commands back to remote machines via RMI.
 *
 * @author Devicelytics Team
 */
public class MonitorServerImpl extends UnicastRemoteObject implements IMonitorServer {

  private static final Logger logger = LoggerFactory.getLogger(MonitorServerImpl.class);

  /** Thread-safe map of active client connections: Hostname -> RMI Stub. */
  private final Map<String, IMonitorClient> connectedClients = new ConcurrentHashMap<>();

  /** Callback for notifying the UI when a client status update is received. */
  private Consumer<DeviceStatus> onStatusUpdate;

  /** Callback for notifying the UI when a client disconnects. */
  private Consumer<String> onClientDisconnect;

  /**
   * Initializes the server implementation.
   *
   * @throws RemoteException if the RMI export fails
   */
  public MonitorServerImpl() throws RemoteException {
    super();
  }

  /**
   * Registers a new client and notifies the UI.
   *
   * @param hostname the hostname of the registering client
   * @param clientStub the RMI interface for the remote client
   * @throws RemoteException on communication failure
   */
  @Override
  public void registerClient(String hostname, IMonitorClient clientStub) throws RemoteException {
    connectedClients.put(hostname, clientStub);
    logger.info("Client registered: {}", hostname);

    DeviceStatus placeholder = new DeviceStatus();
    placeholder.hostname = hostname;
    placeholder.ipAddress = "Connected...";
    placeholder.osName = "Waiting for data...";

    if (onStatusUpdate != null) {
      onStatusUpdate.accept(placeholder);
    }
  }

  /**
   * Removes a client from the active registry.
   *
   * @param hostname the hostname of the client to remove
   * @throws RemoteException on communication failure
   */
  @Override
  public void unregisterClient(String hostname) throws RemoteException {
    connectedClients.remove(hostname);
    logger.info("Client unregistered: {}", hostname);

    if (onClientDisconnect != null) {
      onClientDisconnect.accept(hostname);
    }
  }

  /** Heartbeat method reserved for future use. */
  @Override
  public void keepAlive(String hostname) throws RemoteException {
    // No-op for now, client pushes status periodically which acts as heartbeat
  }

  /**
   * Receives a telemetry push from a remote client.
   *
   * @param status the status snapshot from the client
   * @throws RemoteException on communication failure
   */
  @Override
  public void pushStatus(DeviceStatus status) throws RemoteException {
    if (status == null) {
      return;
    }
    if (onStatusUpdate != null) {
      onStatusUpdate.accept(status);
    }
  }

  /**
   * Responds to discovery requests from clients on the network.
   *
   * @return a status string confirming server activity
   * @throws RemoteException on communication failure
   */
  @Override
  public String discoverServer() throws RemoteException {
    return "MonitorServer-Active";
  }

  /**
   * Dispatches a remote command to a specific connected client.
   *
   * @param hostname the target device hostname
   * @param command the command identifier (e.g., "shell", "ping")
   * @param args the command string or arguments
   * @return the result returned by the client
   */
  public String sendCommand(String hostname, String command, String args) {
    IMonitorClient client = connectedClients.get(hostname);
    if (client == null) {
      return "Error: Client " + hostname + " not found.";
    }
    try {
      return client.executeRemoteCommand(command, args);
    } catch (RemoteException e) {
      logger.error("Failed to send command to {}", hostname, e);
      return "Error: " + e.getMessage();
    }
  }

  /**
   * Gets a view of all currently connected clients.
   *
   * @return map of hostname to client stubs
   */
  public Map<String, IMonitorClient> getConnectedClients() {
    return connectedClients;
  }

  /**
   * Registers UI listeners for client events.
   *
   * @param onStatusUpdate callback for status pushes
   * @param onClientDisconnect callback for client removals
   */
  public void setListeners(
      Consumer<DeviceStatus> onStatusUpdate, Consumer<String> onClientDisconnect) {
    this.onStatusUpdate = onStatusUpdate;
    this.onClientDisconnect = onClientDisconnect;
  }
}
