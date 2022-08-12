package io.mapsmessaging.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PersistentObject {

  protected void writeInt(OutputStream outputStream, int val) throws IOException {
    writeBinary(outputStream, val, 4);
  }

  protected int readInt(InputStream inputStream) throws IOException {
    return (int)readBinary(inputStream, 4);
  }

  protected void writeLong(OutputStream outputStream, long val) throws IOException {
    writeBinary(outputStream, val, 8);
  }


  protected long readLong(InputStream inputStream) throws IOException {
    return readBinary(inputStream, 8);
  }


  protected void writeString(OutputStream outpuStream, String text) throws IOException {
    if(text == null){
      writeInt(outpuStream, -1);
    }
    else{
      writeInt(outpuStream, text.length());
      outpuStream.write(text.getBytes());
    }
  }

  protected String readString(InputStream inputStream) throws IOException {
    int len = readInt(inputStream);
    if(len >=0){
      return new String(readFullBuffer(inputStream, len));
    }
    return null;
  }

  protected void writeByteArray(OutputStream outputStream, byte[] buffer) throws IOException {
    if(buffer == null){
      writeInt(outputStream, -1);
    }
    else{
      writeInt(outputStream, buffer.length);;
      outputStream.write(buffer);
    }
  }

  protected byte[] readByteArray(InputStream inputStream) throws IOException {
    int len = readInt(inputStream);
    if(len >=0){
      return readFullBuffer(inputStream, len);
    }
    return null;
  }

  private byte[] readFullBuffer(InputStream inputStream, int len) throws IOException {
    byte[] tmp = new byte[len];
    int read = 0;
    while(read < len) {
      int t = inputStream.read(tmp);
      if(t < 0)throw new IOException("EOF reached");
      read += t;
    }
    return tmp;
  }

  private void writeBinary(OutputStream outputStream, long val, int size) throws IOException {
    for (int x = 0; x < size; x++) {
      outputStream.write((byte) ((val >> (8 * (size - (x + 1)))) & 0xff));
    }
  }

  private long readBinary(InputStream inputStream, int size) throws IOException {
    long tmp = 0;
    for (int x = 0; x < size; x++) {
      tmp = (tmp << 8) + (inputStream.read() & 0xff);
    }
    return tmp;
  }
}
