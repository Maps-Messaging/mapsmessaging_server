package io.mapsmessaging;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.locks.LockSupport;
import org.tanukisoftware.wrapper.WrapperListener;
import org.tanukisoftware.wrapper.WrapperManager;

public class ServerRunner implements WrapperListener {

  private static final String PID_FILE = "pid";
  private static ExitRunner exitRunner;
  private static ServerRunner serverRunner;

  // Start the application.  If the JVM was launched from the native
  //  Wrapper then the application will wait for the native Wrapper to
  //  call the application's start method.  Otherwise, the start method
  //  will be called immediately.
  public static void main(String[] args) throws IOException {
    File pidFile = new File(PID_FILE);

    if (pidFile.exists()) {
      try {
        java.nio.file.Files.delete(Paths.get(PID_FILE));
      } catch (IOException e) {
        LockSupport.parkNanos(10000000);
      }
    }
    try {
      if (pidFile.createNewFile()) {
        pidFile.deleteOnExit();
      }
    } catch (IOException e) {
      // can ignore this exception
    }
    serverRunner  = new ServerRunner(args);
    exitRunner = new ExitRunner(pidFile);
  }

  public ServerRunner(String[] args){
    WrapperManager.start(this, args);
  }

  @Override
  public Integer start(String[] strings) {
    try {
      return  MessageDaemon.getInstance().start(strings);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public int stop(int i) {
    return  MessageDaemon.getInstance().stop(i);
  }

  @Override
  public void controlEvent(int event) {
    if (!((event == WrapperManager.WRAPPER_CTRL_LOGOFF_EVENT) && (WrapperManager.isLaunchedAsService()))) {
      WrapperManager.stop(0);
    }
  }
}
