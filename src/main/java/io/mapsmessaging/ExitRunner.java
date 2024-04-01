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

import org.tanukisoftware.wrapper.WrapperManager;

import java.io.File;

import java.nio.file.*;
import java.io.IOException;

/**
 * This class extends the Thread class and is used to monitor a specified file for deletion events.
 * When the file is deleted, the WrapperManager.stop(1) method is called to stop the application.
 * The class has a constructor that takes a File object representing the path of the file to monitor.
 * The run() method is overridden to implement the logic for monitoring the file using a WatchService.
 * The run() method continuously checks for deletion events on the file and stops the application if the file is deleted.
 */
public class ExitRunner extends Thread {
  private final Path pidFilePath;
  private final WatchService watchService;

  /**
   * Constructor for the ExitRunner class.
   *
   * @param pidFile The file to monitor for deletion events.
   * @throws IOException If an I/O error occurs.
   */
  ExitRunner(File pidFile) throws IOException {
    this.pidFilePath = pidFile.toPath().toAbsolutePath();
    this.watchService = FileSystems.getDefault().newWatchService();
    pidFilePath.getParent().register(watchService, StandardWatchEventKinds.ENTRY_DELETE);
    super.start();
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

        if (kind == StandardWatchEventKinds.ENTRY_DELETE && fileName.equals(pidFilePath.getFileName())) {
          WrapperManager.stop(1);
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
