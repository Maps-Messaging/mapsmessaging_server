/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

import io.mapsmessaging.auth.priviliges.PrivilegeSerializer;
import io.mapsmessaging.auth.priviliges.SessionPrivileges;
import io.mapsmessaging.auth.registry.mapping.GroupIdSerializer;
import io.mapsmessaging.auth.registry.mapping.UserIdSerializer;
import io.mapsmessaging.security.access.mapping.GroupIdMap;
import io.mapsmessaging.security.access.mapping.UserIdMap;
import lombok.Getter;
import org.mapdb.DB;
import org.mapdb.DBException;
import org.mapdb.DBMaker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public class AuthDbStoreManager implements AutoCloseable {

  private static final String OLD_DB_FILE_NAME = ".auth.db";
  private static final String NEW_DB_FILE_NAME = ".auth_tx.db";

  private static final String USER_ID_MAP_NAME = "userIdMap";
  private static final String GROUP_ID_MAP_NAME = "groupIdMap";
  private static final String PRIVILEGES_MAP_NAME = UserPermisionManager.class.getName();

  private final DB dbStore;

  @Getter
  private final Map<UUID, UserIdMap> userMapSet;

  @Getter
  private final Map<UUID, GroupIdMap> groupMapSet;

  @Getter
  private final Map<UUID, SessionPrivileges> sessionPrivilegesMap;

  public AuthDbStoreManager(String securityDirectory) throws IOException {
    Path securityPath = Path.of(securityDirectory);
    Files.createDirectories(securityPath);

    Path oldPath = securityPath.resolve(OLD_DB_FILE_NAME);
    Path newPath = securityPath.resolve(NEW_DB_FILE_NAME);

    dbStore = openStore(oldPath, newPath);

    userMapSet = dbStore.hashMap(USER_ID_MAP_NAME, new UUIDSerializer(), new UserIdSerializer()).createOrOpen();
    groupMapSet = dbStore.hashMap(GROUP_ID_MAP_NAME, new UUIDSerializer(), new GroupIdSerializer()).createOrOpen();
    sessionPrivilegesMap = dbStore.hashMap(PRIVILEGES_MAP_NAME, new UUIDSerializer(), new PrivilegeSerializer()).createOrOpen();

    AuthDbValidator.validate(userMapSet, groupMapSet);
  }

  public void commit() {
    dbStore.commit();
  }

  @Override
  public void close() {
    dbStore.close();
  }

  private DB openStore(Path oldPath, Path newPath) throws IOException {
    boolean hasNew = Files.exists(newPath);
    boolean hasOld = Files.exists(oldPath);

    if (hasNew) {
      if (hasOld) {
        deleteQuietly(oldPath);
      }
      return openNew(newPath);
    }

    if (hasOld) {
      migrateOldToNew(oldPath, newPath);
      return openNew(newPath);
    }

    return openNew(newPath);
  }

  private void migrateOldToNew(Path oldPath, Path newPath) throws IOException {
    DB oldDb = null;
    DB newDb = null;

    try {
      oldDb = openOldWithRecovery(oldPath);
      newDb = openNew(newPath);

      copyAllMaps(oldDb, newDb);

      newDb.commit();
    }
    catch (Throwable failure) {
      safeClose(newDb);
      safeClose(oldDb);
      deleteQuietly(newPath);
      throw new IOException("Failed migrating auth DB from " + oldPath + " to " + newPath, failure);
    }

    safeClose(newDb);
    safeClose(oldDb);

    deleteQuietly(oldPath);
  }

  private void copyAllMaps(DB oldDb, DB newDb) throws IOException {
    Map<UUID, UserIdMap> oldUserMap = oldDb.hashMap(USER_ID_MAP_NAME, new UUIDSerializer(), new UserIdSerializer()).createOrOpen();
    Map<UUID, GroupIdMap> oldGroupMap = oldDb.hashMap(GROUP_ID_MAP_NAME, new UUIDSerializer(), new GroupIdSerializer()).createOrOpen();
    Map<UUID, SessionPrivileges> oldPrivilegesMap = oldDb.hashMap(PRIVILEGES_MAP_NAME, new UUIDSerializer(), new PrivilegeSerializer()).createOrOpen();

    AuthDbValidator.validate(oldUserMap, oldGroupMap);

    Map<UUID, UserIdMap> newUserMap = newDb.hashMap(USER_ID_MAP_NAME, new UUIDSerializer(), new UserIdSerializer()).createOrOpen();
    Map<UUID, GroupIdMap> newGroupMap = newDb.hashMap(GROUP_ID_MAP_NAME, new UUIDSerializer(), new GroupIdSerializer()).createOrOpen();
    Map<UUID, SessionPrivileges> newPrivilegesMap = newDb.hashMap(PRIVILEGES_MAP_NAME, new UUIDSerializer(), new PrivilegeSerializer()).createOrOpen();

    newUserMap.clear();
    newGroupMap.clear();
    newPrivilegesMap.clear();

    copyMap(oldUserMap, newUserMap);
    copyMap(oldGroupMap, newGroupMap);
    copyMap(oldPrivilegesMap, newPrivilegesMap);
  }

  private <K, V> void copyMap(Map<K, V> source, Map<K, V> target) {
    for (Map.Entry<K, V> entry : source.entrySet()) {
      K key = entry.getKey();
      V value = entry.getValue();
      if (key == null || value == null) {
        continue;
      }
      target.put(key, value);
    }
  }

  private DB openNew(Path path) {
    return DBMaker.fileDB(path.toFile())
        .fileChannelEnable()
        .transactionEnable()
        .fileLockDisable()
        .closeOnJvmShutdown()
        .make();
  }

  private DB openOldWithRecovery(Path path) {
    try {
      return openOld(path, false);
    }
    catch (DBException.DataCorruption corruption) {
      String message = corruption.getMessage();
      if (message != null && message.contains("Header checksum broken")) {
        return openOld(path, true);
      }
      throw corruption;
    }
  }

  private DB openOld(Path path, boolean bypassHeaderChecksum) {
    DBMaker.Maker maker = DBMaker.fileDB(path.toFile())
        .checksumStoreEnable()
        .cleanerHackEnable()
        .fileChannelEnable()
        .fileMmapEnableIfSupported()
        .fileMmapPreclearDisable()
        .fileLockDisable()
        .closeOnJvmShutdown();

    if (bypassHeaderChecksum) {
      maker = maker.checksumHeaderBypass();
    }

    return maker.make();
  }

  private void deleteQuietly(Path path) {
    try {
      Files.deleteIfExists(path);
    }
    catch (IOException ignored) {
    }
  }

  private void safeClose(DB db) {
    try {
      if (db != null) {
        db.close();
      }
    }
    catch (Throwable ignored) {
    }
  }
}
