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

package io.mapsmessaging.utilities;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PersistentObjectTest {

  private final TestPersistentObject persistentObject = new TestPersistentObject();

  @Test
  void integerAndLong_roundTripSignedBoundaryValues() throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    persistentObject.writeInteger(output, Integer.MIN_VALUE);
    persistentObject.writeInteger(output, Integer.MAX_VALUE);
    persistentObject.writeLongValue(output, Long.MIN_VALUE);
    persistentObject.writeLongValue(output, Long.MAX_VALUE);

    ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
    assertEquals(Integer.MIN_VALUE, persistentObject.readInteger(input));
    assertEquals(Integer.MAX_VALUE, persistentObject.readInteger(input));
    assertEquals(Long.MIN_VALUE, persistentObject.readLongValue(input));
    assertEquals(Long.MAX_VALUE, persistentObject.readLongValue(input));
  }

  @Test
  void string_roundTripsAsciiAndEmptyValues() throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    persistentObject.writeText(output, "maps-messaging");
    persistentObject.writeText(output, "");

    ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
    assertEquals("maps-messaging", persistentObject.readText(input));
    assertEquals("", persistentObject.readText(input));
  }

  @Test
  void nullString_readsAsEmptyString() throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    persistentObject.writeText(output, null);

    assertEquals("", persistentObject.readText(new ByteArrayInputStream(output.toByteArray())));
  }

  @Test
  void byteArray_roundTripsDataAndNull() throws IOException {
    byte[] expected = {0, 1, -1, 127, -128};
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    persistentObject.writeBytes(output, expected);
    persistentObject.writeBytes(output, null);

    ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
    assertArrayEquals(expected, persistentObject.readBytes(input));
    assertNull(persistentObject.readBytes(input));
  }

  private static class TestPersistentObject extends PersistentObject {

    void writeInteger(ByteArrayOutputStream output, int value) throws IOException {
      writeInt(output, value);
    }

    int readInteger(ByteArrayInputStream input) throws IOException {
      return readInt(input);
    }

    void writeLongValue(ByteArrayOutputStream output, long value) throws IOException {
      writeLong(output, value);
    }

    long readLongValue(ByteArrayInputStream input) throws IOException {
      return readLong(input);
    }

    void writeText(ByteArrayOutputStream output, String value) throws IOException {
      writeString(output, value);
    }

    String readText(ByteArrayInputStream input) throws IOException {
      return readString(input);
    }

    void writeBytes(ByteArrayOutputStream output, byte[] value) throws IOException {
      writeByteArray(output, value);
    }

    byte[] readBytes(ByteArrayInputStream input) throws IOException {
      return readByteArray(input);
    }
  }
}
