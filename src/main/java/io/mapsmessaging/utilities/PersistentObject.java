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

package io.mapsmessaging.utilities;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PersistentObject {

  /**
   * Writes an integer value to the specified output stream.
   *
   * @param outputStream the output stream to write the integer value to
   * @param val the integer value to be written
   * @throws IOException if an I/O error occurs while writing to the output stream
   */
  protected void writeInt(OutputStream outputStream, int val) throws IOException {
    writeBinary(outputStream, val, 4);
  }

  /**
   * Reads an integer value from the specified input stream.
   *
   * @param inputStream the input stream to read the integer value from
   * @return the integer value read from the input stream
   * @throws IOException if an I/O error occurs while reading from the input stream
   */
  protected int readInt(InputStream inputStream) throws IOException {
    return (int)readBinary(inputStream, 4);
  }

  /**
   * Writes a long value to the specified output stream.
   *
   * @param outputStream the output stream to write the long value to
   * @param val the long value to be written
   * @throws IOException if an I/O error occurs while writing to the output stream
   */
  protected void writeLong(OutputStream outputStream, long val) throws IOException {
    writeBinary(outputStream, val, 8);
  }

  /**
   * Reads a long value from the specified input stream.
   *
   * @param inputStream the input stream to read the long value from
   * @return the long value read from the input stream
   * @throws IOException if an I/O error occurs while reading from the input stream
   */
  protected long readLong(InputStream inputStream) throws IOException {
    return readBinary(inputStream, 8);
  }

  /**
   * Writes a string value to the specified output stream.
   *
   * @param outputStream the output stream to write the string value to
   * @param text the string value to be written
   * @throws IOException if an I/O error occurs while writing to the output stream
   */
  protected void writeString(OutputStream outputStream, String text) throws IOException {
    if(text == null){
      writeInt(outputStream, -1);
    }
    else{
      writeInt(outputStream, text.length());
      outputStream.write(text.getBytes());
    }
  }

  /**
   * Reads a string value from the specified input stream.
   *
   * @param inputStream the input stream to read the string value from
   * @return the string value read from the input stream
   * @throws IOException if an I/O error occurs while reading from the input stream
   */
  protected String readString(InputStream inputStream) throws IOException {
    int len = readInt(inputStream);
    if(len >=0){
      return new String(readFullBuffer(inputStream, len));
    }
    return "";
  }

  /**
   * Writes a byte array to the specified output stream.
   *
   * @param outputStream the output stream to write the byte array to
   * @param buffer the byte array to be written
   * @throws IOException if an I/O error occurs while writing to the output stream
   */
  protected void writeByteArray(OutputStream outputStream, byte[] buffer) throws IOException {
    if(buffer == null){
      writeInt(outputStream, -1);
    }
    else{
      writeInt(outputStream, buffer.length);
      outputStream.write(buffer);
    }
  }

  /**
   * Reads a byte array value from the specified input stream.
   *
   * @param inputStream the input stream to read the byte array value from
   * @return the byte array value read from the input stream, or null if the length is negative
   * @throws IOException if an I/O error occurs while reading from the input stream
   */
  protected @Nullable byte[] readByteArray(InputStream inputStream) throws IOException {
    int len = readInt(inputStream);
    if(len >=0){
      return readFullBuffer(inputStream, len);
    }
    return null;
  }

  /**
   * Reads a byte array from the specified input stream.
   *
   * @param inputStream the input stream to read the byte array from
   * @param len the length of the byte array to be read
   * @return the byte array read from the input stream
   * @throws IOException if an I/O error occurs while reading from the input stream
   */
  protected byte[] readFullBuffer(InputStream inputStream, int len) throws IOException {
    byte[] tmp = new byte[len];
    int read = 0;
    while(read < len) {
      int t = inputStream.read(tmp);
      if(t < 0)throw new IOException("EOF reached");
      read += t;
    }
    return tmp;
  }

  /**
   * Writes a binary value to the specified output stream.
   *
   * @param outputStream the output stream to write the binary value to
   * @param val the binary value to be written
   * @param size the size of the binary value in bytes
   * @throws IOException if an I/O error occurs while writing to the output stream
   */
  private void writeBinary(OutputStream outputStream, long val, int size) throws IOException {
    for (int x = 0; x < size; x++) {
      outputStream.write((byte) ((val >> (8 * (size - (x + 1)))) & 0xff));
    }
  }

  /**
   * Reads a binary value from the specified input stream.
   *
   * @param inputStream the input stream to read the binary value from
   * @param size the size of the binary value in bytes
   * @return the binary value read from the input stream
   * @throws IOException if an I/O error occurs while reading from the input stream
   */
  private long readBinary(InputStream inputStream, int size) throws IOException {
    long tmp = 0;
    for (int x = 0; x < size; x++) {
      tmp = (tmp << 8) + (inputStream.read() & 0xff);
    }
    return tmp;
  }
}
