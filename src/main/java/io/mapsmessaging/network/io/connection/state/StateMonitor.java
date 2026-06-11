package io.mapsmessaging.network.io.connection.state;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;

import java.io.IOException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class StateMonitor implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(StateMonitor.class);

  private static final long MAXIMUM_STATE_DURATION = 40000;

  private final Object lifecycleLock;
  private final EndPointConnection connection;

  private ScheduledFuture<?> future;
  private boolean closed;

  private long lastChangeTime;
  private State lastState;

  public StateMonitor(EndPointConnection connection) {
    this.connection = connection;
    this.lifecycleLock = new Object();
    logger.log(ServerLogMessages.STATE_MONITOR_STARTED, connection);
  }

  public void start() {
    synchronized (lifecycleLock) {
      if (closed) {
        return;
      }

      if (future != null && !future.isCancelled() && !future.isDone()) {
        return;
      }

      future = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(this, 60, 10, TimeUnit.SECONDS);
    }
  }

  public void stop() {
    synchronized (lifecycleLock) {
      cancelFuture();
    }
  }

  public void close() {
    synchronized (lifecycleLock) {
      if (closed) {
        return;
      }

      closed = true;
      cancelFuture();
      lastState = null;
    }

    logger.log(ServerLogMessages.STATE_MONITOR_CLOSED, connection);
  }

  private void cancelFuture() {
    if (future != null) {
      future.cancel(false);
      future = null;
    }
  }

  private void checkState() {
    if (!isRunning()) {
      return;
    }

    if (!connection.isRunning() || connection.isPaused()) {
      return;
    }

    State state = connection.getState();
    if (state == null) {
      return;
    }

    if (lastState == null) {
      lastChangeTime = System.currentTimeMillis();
      lastState = state;
      return;
    }

    if (state.getName() != null &&
        (state.getName().equals("Established") ||
            state.getName().equals("Holding"))) {
      lastState = state;
      return;
    }

    if (lastState != state) {
      lastChangeTime = System.currentTimeMillis();
      lastState = state;
      return;
    }

    long duration = System.currentTimeMillis() - lastChangeTime;
    if (duration <= MAXIMUM_STATE_DURATION) {
      return;
    }

    closeStaleEndpoint(state, duration);
    lastState = state;
  }

  private boolean isRunning() {
    synchronized (lifecycleLock) {
      return !closed && future != null;
    }
  }

  private void closeStaleEndpoint(State state, long duration) {
    logger.log(
        ServerLogMessages.STATE_MONITOR_CLOSING_STALE_ENDPOINT,
        connection.getEndPoint(),
        state.getName(),
        duration);

    if (connection.getEndPoint() != null) {
      try {
        connection.getEndPoint().close();
      } catch (IOException exception) {
        logger.log(ServerLogMessages.STATE_MONITOR_ENDPOINT_CLOSE_EXCEPTION, connection.getEndPoint(), exception);
      }
    }

    connection.handleCloseEndPoint(connection.getEndPoint());
  }

  @Override
  public void run() {
    checkState();
  }
}