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

package io.mapsmessaging.api.features;

import lombok.Getter;

import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public enum CompressionMode {


  NONE (0, false, "None", "No compression", new NoCompression()),
  INFLATOR(1, true, "Inflator", "Java Inflater/Deflater implementation",new InflaterDeflater())
  ;

  private static final int HEADER_SIZE = 5;

  private final int index;

  @Getter
  private final boolean compresses;

  @Getter
  private final String name;

  @Getter
  private final String description;

  private final Compression compression;

  CompressionMode(int index, boolean compresses, String name, String description, Compression compression){
    this.index = index;
    this.compresses = compresses;
    this.name = name;
    this.description = description;
    this.compression = compression;
  }


  public byte[] decompress(ByteBuffer byteBuffer){
    int type = byteBuffer.get();
    Compression impl = CompressionMode.valueOf(type);
    return impl.decompress(byteBuffer);
  }

  public ByteBuffer compress(byte[] data){
    return compression.compress(data);
  }

  public static Compression valueOf(int index){
    if(index == 1){
      return INFLATOR.compression;
    }
    return NONE.compression;
  }

  private interface Compression{
    ByteBuffer compress( byte[] data);
    byte[] decompress(ByteBuffer compressed);
  }

  private static class NoCompression implements Compression{
    @Override
    public ByteBuffer compress(byte[] data) {
      return ByteBuffer.wrap(data);
    }

    @Override
    public byte[] decompress(ByteBuffer compressed) {
      return compressed.array();
    }
  }

  private static class InflaterDeflater implements Compression{

    @Override
    public ByteBuffer compress(byte[] data) {
      Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
      deflater.setInput(data);
      deflater.finish();
      byte[] tmp = new byte[data.length+10];
      int len = deflater.deflate(tmp);
      ByteBuffer packedBuffer = ByteBuffer.allocate(len+HEADER_SIZE);
      packedBuffer.put((byte)INFLATOR.index);
      packedBuffer.putInt(data.length);
      packedBuffer.put(tmp, 0, len);
      packedBuffer.flip();
      return packedBuffer;
    }

    @Override
    public byte[] decompress(ByteBuffer compressed) {
      Inflater inflater = new Inflater();
      int len = compressed.getInt();
      byte[] tmp = new byte[len];
      try {
        byte[] input = compressed.array();
        inflater.setInput(input, HEADER_SIZE, input.length - HEADER_SIZE );
        inflater.inflate(tmp);
      } catch (DataFormatException e) {
        return compressed.array();
      }
      return tmp;
    }
  }
}
