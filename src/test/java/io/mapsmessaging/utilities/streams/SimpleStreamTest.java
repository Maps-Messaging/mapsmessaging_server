/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.utilities.streams;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SimpleStreamTest {

  byte byteVal = 0b1010;
  short shortVal = 0xff;
  int intVal = -10101;
  long longVal = 0xff00ff00ff00ffL;
  float floatVal = 129.234F;
  double doubleVal = 2123312.31231;

  String nullString = null;
  String emptyString = "";
  String string = "this is a string";

  byte[] nullArray = null;
  byte[] emptyArray = {};
  byte[] byteArray = {0b11111, 0b0, 0b1010101};

  @Test
  public void memoryStreamTest() throws IOException{
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
    StreamObjectWriter sow = new StreamObjectWriter(byteArrayOutputStream);
    populateStream(sow);
    byteArrayOutputStream.flush();

    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    StreamObjectReader sor = new StreamObjectReader(byteArrayInputStream);
    checkStream(sor);
  }

  @Test
  public void testRandomAccess() throws Exception {
    RandomAccessFile randomAccessFile = new RandomAccessFile("tmp", "rw");
    RandomAccessFileObjectWriter rafow = new RandomAccessFileObjectWriter(randomAccessFile);
    populateStream(rafow);
    randomAccessFile.seek(0);
    RandomAccessFileObjectReader rafor = new RandomAccessFileObjectReader(randomAccessFile);
    checkStream(rafor);
  }

  @Test
  public void testByteBuffer() throws Exception{
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    BufferObjectWriter bow = new BufferObjectWriter(buffer);
    populateStream(bow);
    buffer.flip();
    checkStream(new BufferObjectReader(buffer));
  }

  @Test
  public void testRandomAccessToStream() throws Exception {
    RandomAccessFile randomAccessFile = new RandomAccessFile("tmp_check", "rw");
    RandomAccessFileObjectWriter rafow = new RandomAccessFileObjectWriter(randomAccessFile);
    populateStream(rafow);
    randomAccessFile.seek(0);

    byte[] buffer = new byte[(int)randomAccessFile.length()];
    randomAccessFile.read(buffer);

    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
    StreamObjectReader sor = new StreamObjectReader(byteArrayInputStream);
    checkStream(sor);
  }

  @Test
  public void testStreamToRandomAccess() throws Exception {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
    StreamObjectWriter sow = new StreamObjectWriter(byteArrayOutputStream);
    populateStream(sow);
    byteArrayOutputStream.flush();

    RandomAccessFile randomAccessFile = new RandomAccessFile("tmp_check", "rw");
    randomAccessFile.seek(0);
    randomAccessFile.write(byteArrayOutputStream.toByteArray());
    randomAccessFile.seek(0);
    RandomAccessFileObjectReader rafor = new RandomAccessFileObjectReader(randomAccessFile);
    checkStream(rafor);

  }


  @Test
  public void testBufferToStream() throws Exception {
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    BufferObjectWriter bow = new BufferObjectWriter(buffer);
    populateStream(bow);
    buffer.flip();

    byte[] byteBuffer = new byte[buffer.limit()];
    buffer.get(byteBuffer);
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteBuffer);
    StreamObjectReader sor = new StreamObjectReader(byteArrayInputStream);
    checkStream(sor);
  }

  @Test
  public void testStreamToBuffer() throws Exception {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
    StreamObjectWriter sow = new StreamObjectWriter(byteArrayOutputStream);
    populateStream(sow);
    byteArrayOutputStream.flush();
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    buffer.put(byteArrayOutputStream.toByteArray());
    buffer.flip();
    checkStream(new BufferObjectReader(buffer));
  }

  private void populateStream(ObjectWriter sow) throws IOException {
    sow.write(byteVal);
    sow.write(shortVal);
    sow.write(intVal);
    sow.write(longVal);
    sow.write(floatVal);
    sow.write(doubleVal);
    sow.write(nullString);
    sow.write(emptyString);
    sow.write(string);
    sow.write(nullArray);
    sow.write(emptyArray);
    sow.write(byteArray);

  }

  private void checkStream(ObjectReader sor) throws IOException {
    Assertions.assertEquals(byteVal, sor.readByte());
    Assertions.assertEquals(shortVal, sor.readShort());
    Assertions.assertEquals(intVal, sor.readInt());
    Assertions.assertEquals(longVal, sor.readLong());
    Assertions.assertEquals(floatVal, sor.readFloat());
    Assertions.assertEquals(doubleVal, sor.readDouble());
    Assertions.assertEquals(nullString, sor.readString());
    Assertions.assertEquals(emptyString, sor.readString());
    Assertions.assertEquals(string, sor.readString());
    Assertions.assertNull(sor.readByteArray());
    Assertions.assertEquals(0, sor.readByteArray().length);
    byte[] tmp = sor.readByteArray();
    for(int x=0;x<tmp.length;x++){
      Assertions.assertEquals(byteArray[x], tmp[x]);
    }
  }
}
