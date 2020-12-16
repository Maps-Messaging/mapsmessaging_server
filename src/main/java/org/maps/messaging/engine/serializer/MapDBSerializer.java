/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.messaging.engine.serializer;

import java.io.IOException;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.serializer.GroupSerializerObjectArray;
import org.maps.utilities.streams.DataObjectReader;
import org.maps.utilities.streams.ObjectReader;
import org.maps.utilities.streams.ObjectWriter;
import org.maps.utilities.streams.StreamObjectWriter;

public class MapDBSerializer<P extends SerializedObject> extends GroupSerializerObjectArray<P> {

  private final Class<P> classDefinition;

  public MapDBSerializer(Class<P> aClass) {
    classDefinition = aClass;
  }

  public void serialize(DataOutput2 out, P context) throws IOException {
    ObjectWriter writer = new StreamObjectWriter(out);
    context.write(writer);
  }

  public P deserialize(DataInput2 in, int available) throws IOException {
    ObjectReader reader = new DataObjectReader(in);
    try {
      return classDefinition.getConstructor(ObjectReader.class).newInstance(reader);
    } catch (Exception e) {
      throw new IOException("Unable to construct serialized object", e);
    }
  }

  @Override
  public boolean isTrusted() {
    return true;
  }
}
