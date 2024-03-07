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

public class ExitRunner extends Thread {
  private final Path pidFilePath;
  private final WatchService watchService;

  ExitRunner(File pidFile) throws IOException {
    this.pidFilePath = pidFile.toPath().toAbsolutePath();
    this.watchService = FileSystems.getDefault().newWatchService();
    pidFilePath.getParent().register(watchService, StandardWatchEventKinds.ENTRY_DELETE);
    super.start();
  }

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
