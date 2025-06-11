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

import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

import java.io.IOException;
import java.util.UUID;

public class UUIDSerializer implements Serializer<UUID> {

  @Override
  public void serialize(@NotNull DataOutput2 dataOutput2, @NotNull UUID uuid) throws IOException {
    dataOutput2.writeLong(uuid.getMostSignificantBits());
    dataOutput2.writeLong(uuid.getLeastSignificantBits());
  }

  @Override
  public UUID deserialize(@NotNull DataInput2 dataInput2, int i) throws IOException {
    return new UUID(dataInput2.readLong(), dataInput2.readLong());
  }
}