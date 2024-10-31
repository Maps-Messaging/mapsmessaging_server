/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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

package io.mapsmessaging.auth.registry.mapping;

import io.mapsmessaging.security.access.mapping.GroupIdMap;
import java.io.IOException;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

public class GroupIdSerializer implements Serializer<GroupIdMap> {

  @Override
  public void serialize(@NotNull DataOutput2 dataOutput2, @NotNull GroupIdMap groupId) throws IOException {
    dataOutput2.writeLong(groupId.getAuthId().getMostSignificantBits());
    dataOutput2.writeLong(groupId.getAuthId().getLeastSignificantBits());
    dataOutput2.writeUTF(groupId.getAuthDomain());
    dataOutput2.writeUTF(groupId.getGroupName());
  }

  @Override
  public GroupIdMap deserialize(@NotNull DataInput2 dataInput2, int i) throws IOException {
    java.util.UUID id = new UUID(dataInput2.readLong(), dataInput2.readLong());
    String auth = dataInput2.readUTF();
    String group = dataInput2.readUTF();
    return new GroupIdMap(id, group, auth);
  }
}
