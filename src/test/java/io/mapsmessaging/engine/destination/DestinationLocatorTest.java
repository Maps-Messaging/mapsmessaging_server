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

import io.mapsmessaging.dto.rest.config.destination.DestinationConfigDTO;
import io.mapsmessaging.engine.resources.ResourceFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

class DestinationLocatorTest {

  @TempDir
  Path tempDirectory;

  @Test
  void parse_whenRootDoesNotExist_returnsNoResults() {
    DestinationLocator locator = createLocator(tempDirectory.resolve("missing"), null);

    locator.parse();

    Assertions.assertTrue(locator.getValid().isEmpty());
    Assertions.assertTrue(locator.getRejected().isEmpty());
  }

  @Test
  void parse_withoutSubdirectory_recursivelyFindsOnlyDirectoriesWithBothMarkerFiles() throws IOException {
    Path validFirst = createValidDestination(tempDirectory.resolve("first"));
    Path validNested = createValidDestination(tempDirectory.resolve("group").resolve("nested"));
    Path missingResource = Files.createDirectories(tempDirectory.resolve("missing-resource"));
    Files.createFile(missingResource.resolve("message.data"));
    Path missingData = Files.createDirectories(tempDirectory.resolve("missing-data"));
    Files.createFile(missingData.resolve(ResourceFactory.RESOURCE_FILE_NAME));

    DestinationLocator locator = createLocator(tempDirectory, null);

    locator.parse();

    Assertions.assertEquals(
        Set.of(validFirst.toAbsolutePath(), validNested.toAbsolutePath()),
        toAbsolutePaths(locator.getValid())
    );
    Assertions.assertTrue(toAbsolutePaths(locator.getRejected()).contains(missingResource.toAbsolutePath()));
    Assertions.assertTrue(toAbsolutePaths(locator.getRejected()).contains(missingData.toAbsolutePath()));
  }

  @Test
  void parse_withSubdirectory_scansThatPathBelowEachDirectChild() throws IOException {
    Path expectedFirst = createValidDestination(tempDirectory.resolve("tenant-a").resolve("destinations"));
    Path expectedSecond = createValidDestination(tempDirectory.resolve("tenant-b").resolve("destinations").resolve("nested"));
    createValidDestination(tempDirectory.resolve("tenant-a").resolve("ignored"));
    createValidDestination(tempDirectory.resolve("root-destination"));

    DestinationLocator locator = createLocator(tempDirectory, "destinations");

    locator.parse();

    Assertions.assertEquals(
        Set.of(expectedFirst.toAbsolutePath(), expectedSecond.toAbsolutePath()),
        toAbsolutePaths(locator.getValid())
    );
  }

  private DestinationLocator createLocator(Path root, String subdirectory) {
    DestinationConfigDTO config = new DestinationConfigDTO();
    config.setDirectory(root.toString());
    return new DestinationLocator(config, subdirectory);
  }

  private Path createValidDestination(Path directory) throws IOException {
    Files.createDirectories(directory);
    Files.createFile(directory.resolve("message.data"));
    Files.createFile(directory.resolve(ResourceFactory.RESOURCE_FILE_NAME));
    return directory;
  }

  private Set<Path> toAbsolutePaths(java.util.List<java.io.File> files) {
    return files.stream()
        .map(java.io.File::toPath)
        .map(Path::toAbsolutePath)
        .collect(Collectors.toSet());
  }
}
