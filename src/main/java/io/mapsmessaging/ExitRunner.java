/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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

import io.mapsmessaging.utilities.PidFileManager;
import java.io.IOException;
import java.nio.file.*;
import org.tanukisoftware.wrapper.WrapperManager;

/**
 * This class extends the Thread class and is used to monitor a specified file for deletion events.
 * When the file is deleted, the WrapperManager.stop(1) method is called to stop the application.
 * The class has a constructor that takes a File object representing the path of the file to monitor.
 * The run() method is overridden to implement the logic for monitoring the file using a WatchService.
 * The run() method continuously checks for deletion events on the file and stops the application if the file is deleted.
 */
public class ExitRunner extends Thread {
  private final PidFileManager pidFileManager;
  private final WatchService watchService;
  private final Path pidFilePath;

  private int exitCode = 0;

  /**
   * Constructor for the ExitRunner class.
   *
   * @param pidFileManager The file to monitor for deletion events.
   * @throws IOException If an I/O error occurs.
   */
  ExitRunner(PidFileManager pidFileManager) throws IOException {
    this.pidFileManager = pidFileManager;
    this.watchService = FileSystems.getDefault().newWatchService();
    pidFilePath = pidFileManager.getPidFile().toPath().toAbsolutePath();
    pidFilePath.getParent().register(watchService, StandardWatchEventKinds.ENTRY_DELETE);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> deletePidFile(2)));
    super.start();
  }

  public void deletePidFile(int exitCode)  {
    if(!pidFileManager.deletePidFile()){
      System.err.println("Failed to delete PID file");
    }
    else{
      this.exitCode = exitCode;
    }
  }

  /**
   * The method is responsible for continuously monitoring a specified file for deletion events using a WatchService.
   * If a deletion event occurs and the deleted file matches the specified file, the WrapperManager.stop(1) method is
   * called to stop the application.
   * The method also handles InterruptedException and restores the interrupted status of the thread.
   * The method uses a loop to continuously check for deletion events and resets the WatchKey if it becomes invalid.
   */
  @Override
  public void run() {
    while (!Thread.interrupted()) {
      WatchKey key;
      if(!pidFileManager.exists()){
        WrapperManager.stop(exitCode);
        return;
      }
      try {
        key = watchService.take(); // Blocks here until an event occurs
      } catch (InterruptedException x) {
        Thread.currentThread().interrupt(); // Restore interrupted status
        return;
      }

      for (WatchEvent<?> event : key.pollEvents()) {
        WatchEvent.Kind<?> kind = event.kind();

        WatchEvent<Path> ev = (WatchEvent<Path>) event;
        Path fileName = ev.context();

        fileName = fileName.toAbsolutePath();
        if (kind == StandardWatchEventKinds.ENTRY_DELETE && fileName.equals(pidFilePath)) {
          WrapperManager.stop(exitCode);
          return;
        }
      }

      boolean valid = key.reset();
      if (!valid) {
        break;
      }
    }
  }
}
