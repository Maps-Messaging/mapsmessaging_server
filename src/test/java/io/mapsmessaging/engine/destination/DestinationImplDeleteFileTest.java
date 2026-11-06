/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.engine.destination;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DestinationImplDeleteFileTest {

  @TempDir
  Path tempDir;

  @Test
  void deleteFileRemovesDirectoryContainingFiles() throws Exception {
    Path destination = Files.createDirectory(tempDir.resolve("destination"));
    Files.writeString(destination.resolve("one.bin"), "one");
    Files.writeString(destination.resolve("two.bin"), "two");

    DestinationImpl.deleteFile(destination.toFile(), 0);

    assertFalse(Files.exists(destination));
  }

  @Test
  void deleteFileRemovesEmptyChildDirectoryBeforeParent() throws Exception {
    Path destination = Files.createDirectory(tempDir.resolve("destination"));
    Path child = Files.createDirectory(destination.resolve("child"));
    Files.writeString(destination.resolve("message.bin"), "message");

    DestinationImpl.deleteFile(destination.toFile(), 0);

    assertFalse(Files.exists(child));
    assertFalse(Files.exists(destination));
  }
  @Test
  void deleteFileRemovesNestedDirectoryContents() throws Exception {
    Path destination = Files.createDirectory(tempDir.resolve("destination-nested"));
    Path child = Files.createDirectory(destination.resolve("child"));
    Files.writeString(child.resolve("nested.bin"), "nested");

    DestinationImpl.deleteFile(destination.toFile(), 0);

    assertFalse(Files.exists(child.resolve("nested.bin")));
    assertFalse(Files.exists(child));
    assertFalse(Files.exists(destination));
  }

  @Test
  void deleteFileDoesNotFollowSymbolicDirectoryLinks() throws Exception {
    Path outside = Files.createDirectory(tempDir.resolve("outside"));
    Path protectedFile = Files.writeString(outside.resolve("keep.bin"), "keep");
    Path destination = Files.createDirectory(tempDir.resolve("destination-link"));
    Path link = destination.resolve("external");

    try {
      Files.createSymbolicLink(link, outside);
    } catch (UnsupportedOperationException | java.io.IOException | SecurityException exception) {
      Assumptions.assumeTrue(false, "Symbolic links unavailable: " + exception.getMessage());
    }

    DestinationImpl.deleteFile(destination.toFile(), 0);

    assertTrue(Files.exists(protectedFile));
    assertTrue(Files.exists(outside));
    assertFalse(Files.exists(link));
    assertFalse(Files.exists(destination));
  }

}
