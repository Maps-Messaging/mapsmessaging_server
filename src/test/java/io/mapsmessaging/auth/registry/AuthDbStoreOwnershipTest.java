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

package io.mapsmessaging.auth.registry;

import io.mapsmessaging.auth.registry.mapping.UserIdSerializer;
import io.mapsmessaging.security.access.mapping.UserIdMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthDbStoreOwnershipTest {
  @TempDir Path directory;

  @Test
  void successful_construction_keeps_store_open_until_owner_close() throws Exception {
    DB db = DBMaker.memoryDB().make();
    DBMaker.Maker allocator = mock(DBMaker.Maker.class, RETURNS_SELF);
    when(allocator.make()).thenReturn(db);
    try (var factory = mockStatic(DBMaker.class)) {
      factory.when(() -> DBMaker.fileDB(any(File.class))).thenReturn(allocator);
      try (AuthDbStoreManager owner = new AuthDbStoreManager(directory.toString())) {
        assertFalse(db.isClosed());
        assertTrue(owner.getUserMapSet().isEmpty());
      }
      assertTrue(db.isClosed());
    } finally {
      if (!db.isClosed()) db.close();
    }
  }

  @Test
  void failed_validation_closes_the_store_that_cannot_be_transferred_to_an_owner() {
    DB db = DBMaker.memoryDB().make();
    UUID key = UUID.randomUUID();
    var users = db.hashMap("userIdMap", new UUIDSerializer(), new UserIdSerializer()).createOrOpen();
    users.put(key, new UserIdMap(UUID.randomUUID(), "user", "domain"));
    DBMaker.Maker allocator = mock(DBMaker.Maker.class, RETURNS_SELF);
    when(allocator.make()).thenReturn(db);
    try (var factory = mockStatic(DBMaker.class)) {
      factory.when(() -> DBMaker.fileDB(any(File.class))).thenReturn(allocator);
      IOException error = assertThrows(IOException.class, () -> new AuthDbStoreManager(directory.toString()));
      assertTrue(error.getMessage().contains("mismatch"));
      assertTrue(db.isClosed());
    } finally {
      if (!db.isClosed()) db.close();
    }
  }
}
