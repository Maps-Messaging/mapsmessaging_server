/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging;

import io.mapsmessaging.utilities.SystemProperties;
import org.tanukisoftware.wrapper.WrapperListener;
import org.tanukisoftware.wrapper.WrapperManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.locks.LockSupport;

/**
 * This is the ServerRunner class which implements the WrapperListener interface.
 * It is responsible for starting and stopping the application.
 * The main method is used to start the application and the start method is called by the native Wrapper.
 * The stop method is called to stop the application.
 * The controlEvent method is used to handle control events.
 */
public class ServerRunner implements WrapperListener {

  private static String PID_FILE = "pid";
  private static ExitRunner exitRunner;
  private static ServerRunner serverRunner;

  /**
   * The main method of the ServerRunner class.
   *
   * This method is the entry point of the application. If the JVM was launched from the native
   *  Wrapper then the application will wait for the native Wrapper to call the application's start method.
   *  Otherwise, the start method will be called immediately.
   *
   *  It performs the following steps:
   * 1. Retrieves the directory path from the MAPS_HOME system property using the SystemProperties class.
   * 2. Constructs the path for the PID file by appending the directory path and the PID_FILE constant.
   * 3. Deletes the PID file if it already exists.
   * 4. Creates a new PID file.
   * 5. Initializes the serverRunner object with the provided command line arguments.
   * 6. Creates and starts the exitRunner thread with the PID file.
   *
   * @param args The command line arguments passed to the application.
   * @throws IOException If an I/O error occurs while deleting or creating the PID file.
   */
  public static void main(String[] args) throws IOException {
    String directoryPath = SystemProperties.getInstance().locateProperty("MAPS_HOME", "");
    if (!directoryPath.isEmpty()) {
      PID_FILE = directoryPath + File.separator + PID_FILE;
      PID_FILE = PID_FILE.replace("//", "/");
    }
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

  /**
   * The constructor for the ServerRunner class.
   * It takes an array of arguments as input and uses the WrapperManager class to start the server.
   */
  public ServerRunner(String[] args){
    WrapperManager.start(this, args);
  }

  /**
   * This method starts the MessageDaemon instance.
   *
   * @param strings An array of strings.
   * @return An Integer value.
   * @throws RuntimeException If an IOException occurs.
   */
  @Override
  public Integer start(String[] strings) {
    try {
      return  MessageDaemon.getInstance().start(strings);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * This method stops the MessageDaemon instance.
   *
   * @param i The integer parameter.
   * @return The result of stopping the MessageDaemon instance.
   */
  @Override
  public int stop(int i) {
    return  MessageDaemon.getInstance().stop(i);
  }

  /**
   * Handles Tanuki Wrapper control events.
   * It takes an integer parameter named event and does the following:
   * - Checks if the event is not WrapperManager.WRAPPER_CTRL_LOGOFF_EVENT and if the application is not launched as a service.
   * - If the above condition is true, it stops the WrapperManager with an exit code of 0.
   */
  @Override
  public void controlEvent(int event) {
    if (!((event == WrapperManager.WRAPPER_CTRL_LOGOFF_EVENT) && (WrapperManager.isLaunchedAsService()))) {
      WrapperManager.stop(0);
    }
  }
}
