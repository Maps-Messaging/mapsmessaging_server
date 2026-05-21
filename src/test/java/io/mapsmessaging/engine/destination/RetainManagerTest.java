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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class RetainManagerTest {

  @TempDir
  Path tempDirectory;

  @Test
  void current_onNewInstance_returnsMinusOne() throws IOException {
    RetainManager retainManager = null;
    try {
      retainManager = new RetainManager(false, tempDirectory.toString());

      long currentIdFirst = retainManager.current();
      Assertions.assertEquals(-1L, currentIdFirst);

      long currentIdSecond = retainManager.current();
      Assertions.assertEquals(-1L, currentIdSecond);
    } finally {
      if (retainManager != null) {
        retainManager.close();
      }
    }
  }

  @Test
  void replace_withValidIds_setsAndOverwritesRetainId_happyPath() throws IOException {
    RetainManager retainManager = null;
    try {
      retainManager = new RetainManager(false, null);

      long oldIdFirst = retainManager.replace(10L);
      Assertions.assertEquals(-1L, oldIdFirst);
      Assertions.assertEquals(10L, retainManager.current());

      long oldIdSecond = retainManager.replace(20L);
      Assertions.assertEquals(10L, oldIdSecond);
      Assertions.assertEquals(20L, retainManager.current());
    } finally {
      if (retainManager != null) {
        retainManager.close();
      }
    }
  }

  @Test
  void replace_doesNotLoseOldValueEvenAfterCurrentCaches() throws IOException {
    RetainManager retainManager = null;
    try {
      retainManager = new RetainManager(false, tempDirectory.toString());

      retainManager.replace(55L);

      long cached = retainManager.current();
      Assertions.assertEquals(55L, cached);

      long oldId = retainManager.replace(66L);
      Assertions.assertEquals(55L, oldId);
      Assertions.assertEquals(66L, retainManager.current());
    } finally {
      if (retainManager != null) {
        retainManager.close();
      }
    }
  }

  @Test
  void nonPersistent_doesNotSurviveRestart() throws IOException {
    Path path = tempDirectory.resolve("nonpersistent");
    Files.createDirectories(path);

    RetainManager first = null;
    RetainManager second = null;
    try {
      first = new RetainManager(false, path.toString());
      first.replace(99L);
      Assertions.assertEquals(99L, first.current());
    } finally {
      if (first != null) {
        first.close();
      }
    }

    try {
      second = new RetainManager(false, path.toString());
      Assertions.assertEquals(-1L, second.current());
    } finally {
      if (second != null) {
        second.close();
      }
    }
  }

  @Test
  void persistent_survivesRestart() throws IOException {
    Path path = tempDirectory.resolve("persistent");
    Files.createDirectories(path);

    RetainManager first = null;
    RetainManager second = null;
    try {
      first = new RetainManager(true, path.toString());
      first.replace(77L);
      Assertions.assertEquals(77L, first.current());
    } finally {
      if (first != null) {
        first.close();
      }
    }

    try {
      second = new RetainManager(true, path.toString());
      Assertions.assertEquals(77L, second.current());
    } finally {
      if (second != null) {
        second.close();
      }
    }
  }

  @Test
  void persistent_createsDirectoryIfMissing() throws IOException {
    Path missingPath = tempDirectory.resolve("willBeCreated");
    Assertions.assertFalse(Files.exists(missingPath));

    RetainManager retainManager = null;
    try {
      retainManager = new RetainManager(true, missingPath.toString());

      Assertions.assertTrue(Files.exists(missingPath));
      Assertions.assertTrue(Files.isDirectory(missingPath));

      long current = retainManager.current();
      Assertions.assertEquals(-1L, current);
    } finally {
      if (retainManager != null) {
        retainManager.close();
      }
    }
  }

  @Test
  void replace_rejectsNegativeValuesLessThanMinusOne() throws IOException {
    RetainManager retainManager = null;
    try {
      retainManager = new RetainManager(false, tempDirectory.toString());
      RetainManager retainManager2 = retainManager;
      Assertions.assertThrows(IllegalArgumentException.class, () -> retainManager2.replace(-2L));
      Assertions.assertThrows(IllegalArgumentException.class, () -> retainManager2.replace(Long.MIN_VALUE));
    } finally {
      if (retainManager != null) {
        retainManager.close();
      }
    }
  }

  @Test
  void replace_withValidIds_setsAndOverwritesRetainId() throws IOException {
    RetainManager retainManager = null;
    try {
      retainManager = new RetainManager(false, null);

      long oldFirst = retainManager.replace(10L);
      Assertions.assertEquals(-1L, oldFirst);
      Assertions.assertEquals(10L, retainManager.current());

      long oldSecond = retainManager.replace(20L);
      Assertions.assertEquals(10L, oldSecond);
      Assertions.assertEquals(20L, retainManager.current());
    } finally {
      if (retainManager != null) {
        retainManager.close();
      }
    }
  }

  @Test
  void replace_rejectsMinusOne() throws IOException {
    RetainManager retainManager = null;
    try {
      retainManager = new RetainManager(false, null);
      RetainManager retainManager2 = retainManager;
      Assertions.assertThrows(IllegalArgumentException.class, () -> retainManager2.replace(-2L));
    } finally {
      if (retainManager != null) {
        retainManager.close();
      }
    }
  }

  @Test
  void replace_acceptsZeroAndLargePositiveIds() throws IOException {
    RetainManager retainManager = null;
    try {
      retainManager = new RetainManager(false, tempDirectory.toString());

      Assertions.assertEquals(-1L, retainManager.replace(0L));
      Assertions.assertEquals(0L, retainManager.current());

      long large = Long.MAX_VALUE;
      Assertions.assertEquals(0L, retainManager.replace(large));
      Assertions.assertEquals(large, retainManager.current());
    } finally {
      if (retainManager != null) {
        retainManager.close();
      }
    }
  }


  @Test
  void nonPersistent_nullPath_isAllowed() throws IOException {
    RetainManager retainManager = null;
    try {
      retainManager = new RetainManager(false, null);

      Assertions.assertEquals(-1L, retainManager.current());
      Assertions.assertEquals(-1L, retainManager.replace(0L));
      Assertions.assertEquals(0L, retainManager.current());
    } finally {
      if (retainManager != null) {
        retainManager.close();
      }
    }
  }

  @Test
  void close_canBeCalledAfterUse() throws IOException {
    RetainManager retainManager = new RetainManager(false, tempDirectory.toString());
    retainManager.replace(1L);
    retainManager.current();
    retainManager.close();
  }
}
