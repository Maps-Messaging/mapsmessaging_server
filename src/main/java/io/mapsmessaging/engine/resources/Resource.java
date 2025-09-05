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

package io.mapsmessaging.engine.resources;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.storage.Statistics;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;

public interface Resource extends AutoCloseable {

  @Override
  void close() throws IOException;

  void add(Message message) throws IOException;

  void keepOnly(List<Long> validKeys) throws IOException;

  void checkLoaded();

  long getNextIdentifier();

  void remove(long key) throws IOException;

  void delete() throws IOException;

  boolean isEmpty();

  Message get(long key) throws IOException;

  boolean contains(Long id) throws IOException;

  List<Long> getKeys() throws IOException;

  long size() throws IOException;

  @Nullable Statistics getStatistics();

  @SneakyThrows
  default <T> T getFromFuture(Future<T> future) {
    return future.get();
  }

  String getName();

  boolean isPersistent();

  ResourceProperties getResourceProperties();
}
