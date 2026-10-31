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
  void current_onNewInstance_returnsNull() throws IOException {
    RetainManager retainManager = null;
    try {
      retainManager = new RetainManager(false, tempDirectory.toString());

      Long currentIdFirst = retainManager.current();
      Assertions.assertNull(currentIdFirst);

      Long currentIdSecond = retainManager.current();
      Assertions.assertNull(currentIdSecond);
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

      Long oldIdFirst = retainManager.replace(10L);
      Assertions.assertNull(oldIdFirst);
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
      Assertions.assertNull(second.current());
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

      Long current = retainManager.current();
      Assertions.assertNull(current);
    } finally {
      if (retainManager != null) {
        retainManager.close();
      }
    }
  }

  @Test
  void replace_acceptsSignedLongBoundaries() throws IOException {
    RetainManager manager = new RetainManager(false, tempDirectory.toString());
    try {
      Assertions.assertNull(manager.replace(Long.MIN_VALUE));
      Assertions.assertEquals(Long.MIN_VALUE, manager.current());
      Assertions.assertEquals(Long.MIN_VALUE, manager.replace(-2L));
      Assertions.assertEquals(-2L, manager.replace(-1L));
      Assertions.assertEquals(-1L, manager.current());
      Assertions.assertEquals(-1L, manager.replace(0L));
      Assertions.assertEquals(0L, manager.current());
      Assertions.assertEquals(0L, manager.replace(null));
      Assertions.assertNull(manager.current());
    } finally {
      manager.close();
    }
  }

  @Test
  void replace_withValidIds_setsAndOverwritesRetainId() throws IOException {
    RetainManager retainManager = null;
    try {
      retainManager = new RetainManager(false, null);

      Long oldFirst = retainManager.replace(10L);
      Assertions.assertNull(oldFirst);
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
  void minusOneIsAnActualRetainedIdentifierUntilExplicitlyCleared() throws IOException {
    RetainManager manager = new RetainManager(false, null);
    try {
      Assertions.assertNull(manager.replace(-1L));
      Assertions.assertEquals(-1L, manager.current());
      Assertions.assertEquals(-1L, manager.replace(null));
      Assertions.assertNull(manager.current());
    } finally {
      manager.close();
    }
  }

  @Test
  void replace_acceptsZeroAndLargePositiveIds() throws IOException {
    RetainManager retainManager = null;
    try {
      retainManager = new RetainManager(false, tempDirectory.toString());

      Assertions.assertNull(retainManager.replace(0L));
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

      Assertions.assertNull(retainManager.current());
      Assertions.assertNull(retainManager.replace(0L));
      Assertions.assertEquals(0L, retainManager.current());
    } finally {
      if (retainManager != null) {
        retainManager.close();
      }
    }
  }

  @Test
  void clearingRetainResetsCachedIdentifierAndAllowsSubsequentReplacement() throws IOException {
    RetainManager manager = new RetainManager(false, null);
    try {
      Assertions.assertNull(manager.replace(42L));
      Assertions.assertEquals(42L, manager.current());
      Assertions.assertEquals(42L, manager.replace(null));
      Assertions.assertNull(manager.current());
      Assertions.assertNull(manager.replace(null));
      Assertions.assertNull(manager.current());
      Assertions.assertNull(manager.replace(43L));
      Assertions.assertEquals(43L, manager.current());
    } finally {
      manager.close();
    }
  }

  @Test
  void clearingPersistentRetainSurvivesRestart() throws IOException {
    Path path = tempDirectory.resolve("cleared-persistent");
    RetainManager original = new RetainManager(true, path.toString());
    try {
      original.replace(77L);
      Assertions.assertEquals(77L, original.current());
      Assertions.assertEquals(77L, original.replace(null));
      Assertions.assertNull(original.current());
    } finally {
      original.close();
    }

    RetainManager reopened = new RetainManager(true, path.toString());
    try {
      Assertions.assertNull(reopened.current());
    } finally {
      reopened.close();
    }
  }

  @Test
  void negativeRetainValuesPersistAcrossReopen() throws IOException {
    Path path = tempDirectory.resolve("negative-retained");
    RetainManager original = new RetainManager(true, path.toString());
    try {
      Assertions.assertNull(original.replace(-1L));
      Assertions.assertEquals(-1L, original.current());
    } finally {
      original.close();
    }
    RetainManager reopened = new RetainManager(true, path.toString());
    try {
      Assertions.assertEquals(-1L, reopened.current());
      Assertions.assertEquals(-1L, reopened.replace(Long.MIN_VALUE));
      Assertions.assertEquals(Long.MIN_VALUE, reopened.current());
    } finally {
      reopened.close();
    }
    RetainManager again = new RetainManager(true, path.toString());
    try {
      Assertions.assertEquals(Long.MIN_VALUE, again.current());
    } finally {
      again.close();
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
