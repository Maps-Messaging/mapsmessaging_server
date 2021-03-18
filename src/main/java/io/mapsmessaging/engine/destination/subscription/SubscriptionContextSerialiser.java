/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.engine.destination.subscription;

import io.mapsmessaging.utilities.streams.DataObjectReader;
import io.mapsmessaging.utilities.streams.ObjectReader;
import io.mapsmessaging.utilities.streams.ObjectWriter;
import io.mapsmessaging.utilities.streams.StreamObjectWriter;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.serializer.GroupSerializerObjectArray;

public class SubscriptionContextSerialiser extends GroupSerializerObjectArray<SubscriptionContext> {

  public SubscriptionContextSerialiser() {
    // This is used via a class for
  }

  public void serialize(@NotNull DataOutput2 out, SubscriptionContext context) throws IOException {
    ObjectWriter writer = new StreamObjectWriter(out);
    context.write(writer);
  }

  public SubscriptionContext deserialize(@NotNull DataInput2 in, int available) throws IOException {
    ObjectReader reader = new DataObjectReader(in);
    return new SubscriptionContext(reader);
  }

  @Override
  public boolean isTrusted() {
    return true;
  }
}
