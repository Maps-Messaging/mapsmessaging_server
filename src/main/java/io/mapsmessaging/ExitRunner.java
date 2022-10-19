package io.mapsmessaging;

import java.io.File;
import java.util.concurrent.locks.LockSupport;
import org.tanukisoftware.wrapper.WrapperManager;

public class ExitRunner extends Thread {

  private final File pidFile;

  ExitRunner(File pidFile) {
    this.pidFile = pidFile;
    super.start();
  }

  @Override
  public void run() {
    while (pidFile.exists()) {
      LockSupport.parkNanos(1000000);
    }
    WrapperManager.stop(1);
  }
}