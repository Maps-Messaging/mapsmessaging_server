package io.mapsmessaging.network.io.impl.canbus;

import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.CanbusConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.AcceptHandler;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.impl.Selector;
import io.mapsmessaging.network.io.impl.serial.SerialEndPointServer;
import io.mapsmessaging.network.protocol.ProtocolFactory;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.protocol.Protocol;

public class CanbusEndPointServer extends EndPointServer {
  private final EndPointManagerJMX managerMBean;
  private final CanbusConfigDTO canbusConfig;
  private final Object lifecycleLock = new Object();
  private boolean started;
  private int cleanupCount;
  private boolean discoveryClosed;
  private long generation;
  private ScheduledFuture<?> retryTask;
  private CanbusEndPoint currentEndPoint;

  public CanbusEndPointServer(AcceptHandler acceptHandler, EndPointURL url, EndPointServerConfigDTO config, EndPointManagerJMX managerMBean) {
    super(acceptHandler, url, config);
    this.managerMBean = managerMBean;
    canbusConfig = (CanbusConfigDTO)config.getEndPointConfig();
  }

  @Override
  public void handleNewEndPoint(EndPoint endPoint) throws IOException {
    synchronized (lifecycleLock) {
      if (!started || currentEndPoint != endPoint) {
        return;
      }
      if (activeEndPoints.containsKey(endPoint.getId())) {
        return;
      }
      registerNewEndPoint(endPoint);
      bindProtocol((CanbusEndPoint) endPoint);
    }
  }

  protected void bindProtocol(CanbusEndPoint endPoint) throws IOException {
    String protocols = getConfig().getProtocols();
    for (ProtocolImplFactory protocol : ProtocolFactory.getProtocolServiceList()) {
      if (protocol.matches(protocols)) {
        protocol.create(endPoint, endPoint.getInterfaceInformation());
        return;
      }
    }
    throw new IOException("No protocol implementation available for " + protocols);
  }

  @Override
  public String getName() {
    return canbusConfig.getDeviceName();
  }

  @Override
  public void register() {
    // Nothing to register here
  }

  @Override
  public void deregister() {
    // Nothing to deregister here
  }

  @Override
  public void handleCloseEndPoint(EndPoint endPoint) {
    synchronized (lifecycleLock) {
      super.handleCloseEndPoint(endPoint);
      if (currentEndPoint != endPoint) {
        return;
      }
      currentEndPoint = null;
      cleanupCount++;
    }
    try {
      closeEndPoint((CanbusEndPoint) endPoint);
    } catch (IOException | RuntimeException failure) {
      logger.log(ServerLogMessages.END_POINT_CONNECTION_CLOSE_EXCEPTION, failure);
    } finally {
      synchronized (lifecycleLock) {
        cleanupCount--;
        lifecycleLock.notifyAll();
        scheduleRetryIfStarted();
      }
    }
  }

  @Override
  public void start() throws IOException {
    long startGeneration;
    synchronized (lifecycleLock) {
      while (cleanupCount != 0) {
        try {
          lifecycleLock.wait();
        } catch (InterruptedException exception) {
          Thread.currentThread().interrupt();
          throw new IOException("Interrupted while waiting for CAN bus cleanup", exception);
        }
      }
      if (started) {
        return;
      }
      if (discoveryClosed) {
        io.mapsmessaging.MessageDaemon.getInstance().getSubSystemManager().getDiscoveryManager().register(this);
        discoveryClosed = false;
      }
      started = true;
      startGeneration = ++generation;
    }
    attemptStart(startGeneration);
  }

  @Override
  protected Logger createLogger(String url) {
    return LoggerFactory.getLogger(SerialEndPointServer.class);
  }

  @Override
  public void close() throws IOException {
    CanbusEndPoint endpoint;
    synchronized (lifecycleLock) {
      if (!started && (discoveryClosed || cleanupCount != 0)) {
        return;
      }
      started = false;
      generation++;
      if (retryTask != null) {
        retryTask.cancel(false);
        retryTask = null;
      }
      endpoint = currentEndPoint;
      currentEndPoint = null;
      activeEndPoints.clear();
      cleanupCount++;
    }
    try {
      try {
        closeEndPoint(endpoint);
      } finally {
        super.close();
      }
    } finally {
      synchronized (lifecycleLock) {
        discoveryClosed = true;
        cleanupCount--;
        lifecycleLock.notifyAll();
      }
    }
  }

  @Override
  public void selected(Selectable selectable, Selector selector, int selection) {
    // not required
  }

  public void createEndPoint() {
    long currentGeneration;
    synchronized (lifecycleLock) {
      currentGeneration = generation;
    }
    attemptStart(currentGeneration);
  }

  protected CanbusEndPoint newEndPoint() throws IOException {
    return new CanbusEndPoint(canbusConfig, this, managerMBean.getTypePath());
  }

  protected ScheduledFuture<?> scheduleRetry(Runnable task) {
    return SimpleTaskScheduler.getInstance().schedule(task, 5, TimeUnit.SECONDS);
  }

  private void attemptStart(long expectedGeneration) {
    CanbusEndPoint failedEndpoint = null;
    synchronized (lifecycleLock) {
      if (!started || generation != expectedGeneration || currentEndPoint != null || cleanupCount != 0) {
        return;
      }
      retryTask = null;
      try {
        CanbusEndPoint endpoint = newEndPoint();
        currentEndPoint = endpoint;
        endpoint.setCloseHandler(() -> handleCloseEndPoint(endpoint));
        handleNewEndPoint(endpoint);
        return;
      } catch (IOException | RuntimeException failure) {
        failedEndpoint = currentEndPoint;
        currentEndPoint = null;
        if (failedEndpoint != null) {
          activeEndPoints.remove(failedEndpoint.getId());
        }
        logger.log(ServerLogMessages.END_POINT_CONNECTION_FAILED, failure, getName());
        cleanupCount++;
      }
    }
    try {
      closeEndPoint(failedEndpoint);
    } catch (IOException | RuntimeException failure) {
      logger.log(ServerLogMessages.END_POINT_CONNECTION_CLOSE_EXCEPTION, failure);
    } finally {
      synchronized (lifecycleLock) {
        cleanupCount--;
        lifecycleLock.notifyAll();
        if (generation == expectedGeneration) {
          scheduleRetryIfStarted();
        }
      }
    }
  }

  private void scheduleRetryIfStarted() {
    boolean serial = canbusConfig.getSerialConfig() != null
        && canbusConfig.getSerialConfig().getSerialDevice() != null;
    if (started && cleanupCount == 0 && !serial && currentEndPoint == null && retryTask == null) {
      long retryGeneration = generation;
      retryTask = scheduleRetry(() -> attemptStart(retryGeneration));
    }
  }

  private void closeEndPoint(CanbusEndPoint endpoint) throws IOException {
    if (endpoint == null) {
      return;
    }
    Exception failure = null;
    Protocol protocol = endpoint.getBoundProtocol();
    try {
      if (protocol != null) {
        protocol.close();
      }
    } catch (IOException | RuntimeException exception) {
      failure = exception;
    }
    try {
      endpoint.close();
    } catch (IOException | RuntimeException exception) {
      if (failure == null) {
        failure = exception;
      } else {
        failure.addSuppressed(exception);
      }
    }
    if (failure instanceof IOException ioException) {
      throw ioException;
    }
    if (failure instanceof RuntimeException runtimeException) {
      throw runtimeException;
    }
  }
}
