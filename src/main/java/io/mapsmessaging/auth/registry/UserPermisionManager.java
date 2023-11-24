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

package io.mapsmessaging.auth.registry;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

public class UserPermisionManager implements Closeable {

  private final ConcurrentMap<UUID, Quotas> store;
  private final DB db;

  public UserPermisionManager(String fileName) {
    db = DBMaker.fileDB(fileName)
        .checksumStoreEnable()
        .fileChannelEnable()
        .fileMmapEnableIfSupported()
        .fileMmapPreclearDisable()
        .closeOnJvmShutdown()
        .make();

    db.getStore().fileLoad();
    store = db.hashMap(UserPermisionManager.class.getName(), new UUIDSerializer(), new Quotas()).createOrOpen();
  }


  public void add(Quotas userDetails) {
    store.put(userDetails.getUuid(), userDetails);
  }

  public void update(Quotas userDetails) {
    store.put(userDetails.getUuid(), userDetails);
  }

  public void delete(UUID uuid) {
    store.remove(uuid);
  }

  @Override
  public void close() throws IOException {
    db.close();
  }

  public Quotas get(UUID userId) {
    return store.get(userId);
  }
}
