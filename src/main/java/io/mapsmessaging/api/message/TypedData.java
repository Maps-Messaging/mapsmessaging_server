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

package io.mapsmessaging.api.message;

import io.mapsmessaging.storage.impl.streams.ObjectReader;
import io.mapsmessaging.storage.impl.streams.ObjectWriter;
import lombok.ToString;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@ToString(callSuper = true)

public class TypedData {

  private final TYPE type;
  private final Object data;

  public TypedData(Object data) {
    TYPE testType = getType(data);
    if (testType == TYPE.UNKNOWN) {
      this.data = data.toString();
      type = TYPE.STRING;
    } else {
      this.data = data;
      type = testType;
    }
  }

  public TypedData(ObjectReader reader) throws IOException {
    type = TYPE.valueOf(reader.readByte());
    data = readType(type, reader);
  }

  public void write(ObjectWriter writer) throws IOException {
    writer.write(type.value);
    switch (type) {
      case BOOLEAN:
        if (((boolean) data)) {
          writer.write((byte) 1);
        } else {
          writer.write((byte) 0);
        }
        break;

      case BYTE:
        writer.write((byte) data);
        break;

      case SHORT:
        writer.write((short) data);
        break;

      case INT:
        writer.write((int) data);
        break;

      case LONG:
        writer.write((long) data);
        break;

      case FLOAT:
        writer.write((float) data);
        break;

      case DOUBLE:
        writer.write((double) data);
        break;

      case STRING:
        writer.write((String) data);
        break;

      case CHAR:
        writer.write((char) data);
        break;

      case BYTE_ARRAY:
        writer.write((byte[]) data);
        break;

      case SHORT_ARRAY:
        writer.write((short[]) data);
        break;

      case INT_ARRAY:
        writer.write((int[]) data);
        break;

      case LONG_ARRAY:
        writer.write((long[]) data);
        break;

      case FLOAT_ARRAY:
        writer.write((float[]) data);
        break;

      case DOUBLE_ARRAY:
        writer.write((double[]) data);
        break;

      case STRING_ARRAY:
        writer.write((String[]) data);
        break;

      case CHAR_ARRAY:
        writer.write((char[]) data);
        break;

      case TYPED_MAP:
        break;

      default:
        throw new IOException("Unknown type detected");
    }
  }

  public TYPE getType() {
    return type;
  }

  public Object getData() {
    return data;
  }

  public enum TYPE {
    BYTE(0),
    SHORT(1),
    INT(2),
    LONG(3),
    FLOAT(4),
    DOUBLE(5),
    STRING(6),
    CHAR(7),
    BOOLEAN(8),
    BYTE_ARRAY(20),
    SHORT_ARRAY(21),
    INT_ARRAY(21),
    LONG_ARRAY(22),
    FLOAT_ARRAY(23),
    DOUBLE_ARRAY(24),
    STRING_ARRAY(25),
    CHAR_ARRAY(26),
    TYPED_MAP(30),
    UNKNOWN(1000);

    private static final Map<Byte, TYPE> map = new HashMap<>();

    static {
      for (TYPE type : TYPE.values()) {
        map.put(type.value, type);
      }
    }

    byte value;

    TYPE(int val) {
      value = (byte) val;
    }

    public static TYPE valueOf(int type) {
      return map.get((byte) type);
    }
  }

  @java.lang.SuppressWarnings("squid:S3776")
  private static TYPE getType(Object data) {
    if (data instanceof Byte) {
      return TYPE.BYTE;
    }
    if (data instanceof Short) {
      return TYPE.SHORT;
    }
    if (data instanceof Integer) {
      return TYPE.INT;
    }
    if (data instanceof Long) {
      return TYPE.LONG;
    }
    if (data instanceof Float) {
      return TYPE.FLOAT;
    }
    if (data instanceof Double) {
      return TYPE.DOUBLE;
    }
    if (data instanceof Boolean) {
      return TYPE.BOOLEAN;
    }
    if (data instanceof String) {
      return TYPE.STRING;
    }
    if (data instanceof Character) {
      return TYPE.CHAR;
    }
    if (data instanceof byte[]) {
      return TYPE.BYTE_ARRAY;
    }
    if (data instanceof short[]) {
      return TYPE.SHORT_ARRAY;
    }
    if (data instanceof int[]) {
      return TYPE.INT_ARRAY;
    }
    if (data instanceof long[]) {
      return TYPE.LONG_ARRAY;
    }
    if (data instanceof float[]) {
      return TYPE.FLOAT_ARRAY;
    }
    if (data instanceof double[]) {
      return TYPE.DOUBLE_ARRAY;
    }
    if (data instanceof String[]) {
      return TYPE.STRING_ARRAY;
    }
    if (data instanceof char[]) {
      return TYPE.CHAR_ARRAY;
    }
    if (data instanceof Map) {
      return TYPE.TYPED_MAP;
    }
    return TYPE.UNKNOWN;
  }

  private static Object readType(TYPE type, ObjectReader reader) throws IOException {
    switch (type) {
      case BOOLEAN:
        return (reader.readByte() == 1);

      case BYTE:
        return reader.readByte();

      case SHORT:
        return reader.readShort();

      case INT:
        return reader.readInt();

      case LONG:
        return reader.readLong();

      case FLOAT:
        return reader.readFloat();

      case DOUBLE:
        return reader.readDouble();

      case STRING:
        return reader.readString();

      case CHAR:
        return reader.readChar();

      case BYTE_ARRAY:
        return reader.readByteArray();

      case SHORT_ARRAY:
        return reader.readShortArray();

      case INT_ARRAY:
        return reader.readIntArray();

      case LONG_ARRAY:
        return reader.readLongArray();

      case FLOAT_ARRAY:
        return reader.readFloatArray();

      case DOUBLE_ARRAY:
        return reader.readDoubleArray();

      case STRING_ARRAY:
        return reader.readStringArray();

      case CHAR_ARRAY:
        return reader.readCharArray();

      case TYPED_MAP:
      default:
        return null;
    }
  }
}
