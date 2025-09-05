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

package io.mapsmessaging.auth.priviliges;

import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PrivilegeSerializer implements Serializer<SessionPrivileges> {

  @Override
  public void serialize(@NotNull DataOutput2 dataOutput2, @NotNull SessionPrivileges userDetails) throws IOException {
    dataOutput2.writeUTF(userDetails.getUsername());
    dataOutput2.writeLong(userDetails.getUniqueId().getMostSignificantBits());
    dataOutput2.writeLong(userDetails.getUniqueId().getLeastSignificantBits());
    int count = userDetails.getPriviliges().size();
    dataOutput2.writeInt(count);
    for (Privilege p : userDetails.getPriviliges()) {
      dataOutput2.writeUTF(p.getName());
      if (p instanceof BooleanPrivilege) {
        dataOutput2.writeInt(0);
        dataOutput2.writeBoolean(((BooleanPrivilege) p).isValue());
      } else if (p instanceof LongPrivilege) {
        dataOutput2.writeInt(1);
        dataOutput2.writeLong(((LongPrivilege) p).getValue());
      } else if (p instanceof StringPrivilege) {
        dataOutput2.writeInt(2);
        dataOutput2.writeUTF(((StringPrivilege) p).getValue());
      }
    }
  }

  @Override
  public SessionPrivileges deserialize(@NotNull DataInput2 dataInput2, int i) throws IOException {
    List<Privilege> list = new ArrayList<>();
    String user = dataInput2.readUTF();
    java.util.UUID id = new UUID(dataInput2.readLong(), dataInput2.readLong());
    int count = dataInput2.readInt();
    for (int x = 0; x < count; x++) {
      String name = dataInput2.readUTF();
      int type = dataInput2.readInt();
      if (type == 0) {
        list.add(new BooleanPrivilege(name, dataInput2.readBoolean()));
      } else if (type == 1) {
        list.add(new LongPrivilege(name, dataInput2.readLong()));
      } else if (type == 2) {
        list.add(new StringPrivilege(name, dataInput2.readUTF()));
      }
    }
    return new SessionPrivileges(id, user, list);
  }
}
