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

package io.mapsmessaging.auth.registry.mapping;

import io.mapsmessaging.security.access.mapping.IdMap;
import io.mapsmessaging.security.access.mapping.MapParser;
import io.mapsmessaging.security.access.mapping.store.MapStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class IdDbStore<T extends IdMap> implements MapStore<T> {

  private final Map<UUID, T> store;

  public IdDbStore(Map<UUID, T> store) {
    this.store = store;
  }

  @Override
  public List<T> load(MapParser<T> parser) {
    return new ArrayList<>(store.values());
  }

  @Override
  public void save(List<T> entries, MapParser<T> parser) {
    store.clear();
    for (T entry : entries)
      store.put(entry.getAuthId(), entry);
  }
}
