/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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
import lombok.Getter;
import lombok.ToString;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@ToString(callSuper = true)

@Getter
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
    INT_ARRAY(22),
    LONG_ARRAY(23),
    FLOAT_ARRAY(24),
    DOUBLE_ARRAY(25),
    STRING_ARRAY(26),
    CHAR_ARRAY(27),
    TYPED_MAP(30),
    UNKNOWN(1000);

    private static final Map<Byte, TYPE> map = new HashMap<>();

    static {
      for (TYPE type : TYPE.values()) {
        map.put(type.value, type);
      }
    }

    final byte value;

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
    return switch (type) {
      case BOOLEAN -> (reader.readByte() == 1);
      case BYTE -> reader.readByte();
      case SHORT -> reader.readShort();
      case INT -> reader.readInt();
      case LONG -> reader.readLong();
      case FLOAT -> reader.readFloat();
      case DOUBLE -> reader.readDouble();
      case STRING -> reader.readString();
      case CHAR -> reader.readChar();
      case BYTE_ARRAY -> reader.readByteArray();
      case SHORT_ARRAY -> reader.readShortArray();
      case INT_ARRAY -> reader.readIntArray();
      case LONG_ARRAY -> reader.readLongArray();
      case FLOAT_ARRAY -> reader.readFloatArray();
      case DOUBLE_ARRAY -> reader.readDoubleArray();
      case STRING_ARRAY -> reader.readStringArray();
      case CHAR_ARRAY -> reader.readCharArray();
      default -> null;
    };
  }
}
