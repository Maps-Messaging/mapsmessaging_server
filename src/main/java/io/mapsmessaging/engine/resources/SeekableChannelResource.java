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

package io.mapsmessaging.engine.resources;


import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.logging.LogMessages;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.utilities.streams.BufferObjectReader;
import io.mapsmessaging.utilities.streams.BufferObjectWriter;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class SeekableChannelResource extends MapBasedResource {
  private static final long REPORT_DELAY = 10000;
  private final Logger logger;
  private final SeekableByteChannel readChannel;
  private final BufferObjectReader reader;

  private final SeekableByteChannel writeChannel;
  private final BufferObjectWriter writer;
  private final ByteBuffer lengthBuffer;
  private final ByteBuffer writeBuffer;
  private final ByteBuffer readBuffer;

  public SeekableChannelResource(String name, String mapped) throws IOException {
    super(name, mapped);
    logger = LoggerFactory.getLogger("SeekableChannel:" + name + "_" + mapped);
    String tmpName = name;
    if (File.separatorChar == '/') {
      while (tmpName.indexOf('\\') != -1) {
        tmpName = tmpName.replace("\\", File.separator);
      }
    } else {
      while (tmpName.indexOf('/') != -1) {
        tmpName = tmpName.replace("/", File.separator);
      }
    }
    lengthBuffer = ByteBuffer.allocate(4);
    tmpName += "data.bin";
    File file = new File(tmpName);
    long length = 0;
    if (file.exists()) {
      length = file.length();
    }
    writeBuffer = ByteBuffer.allocateDirect(1024 * 1024);
    readBuffer = ByteBuffer.allocateDirect(1024 * 1024);

    writeChannel = Files.newByteChannel(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    readChannel = Files.newByteChannel(file.toPath(),  StandardOpenOption.READ);
    reader = new BufferObjectReader(readBuffer);
    writer = new BufferObjectWriter(writeBuffer);
    reload(length);
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  private void reload(long eof) throws IOException {
    long pos = 0;
    long report = System.currentTimeMillis() + REPORT_DELAY;
    while (pos != eof) {
      Message message = reloadMessage(pos);
      pos = readChannel.position();
      if(message != null) {
        index.put(message.getIdentifier(), new MessageCache(message, pos));
        if (logger.isInfoEnabled() && report < System.currentTimeMillis()) {
          float percent = (float) eof - pos;
          percent = percent / eof * 100.0f;
          logger.log(LogMessages.FILE_RELOAD_PERCENT, percent);
          report = System.currentTimeMillis() + REPORT_DELAY;
        }
      }
    }
  }

  @Override
  public void delete() throws IOException {
    readChannel.close();
    writeChannel.close();
  }

  @Override
  public void add(Message message) throws IOException {
    super.add(message);
    index.put(message.getIdentifier(), new MessageCache(message, writeChannel.position()));
    writeBuffer.clear();
    writeBuffer.position(4); // Skip the first 4 bytes so we can set the full length of the buffer
    message.write(writer);
    int len = writeBuffer.position() - 4;
    writeBuffer.putInt(0, len);
    writeBuffer.flip();
    writeChannel.write(writeBuffer);
  }

  @Override
  public void remove(long key) throws IOException {
    MessageCache cache = index.remove(key);
    if (cache != null) {
      cache.getMessageSoftReference().clear();
      long eof = writeChannel.position();
      lengthBuffer.clear();
      readChannel.position(cache.getFilePosition());
      readChannel.read(lengthBuffer);
      int len = lengthBuffer.getInt(0);
      len = len * -1;
      lengthBuffer.putInt(0, len);
      writeChannel.position(cache.getFilePosition()-4);
      lengthBuffer.flip();
      writeChannel.write(lengthBuffer);
      writeChannel.position(eof);
      lengthBuffer.clear();
    }
  }

  @Override
  public Message get(long key) throws IOException {
    Message message = null;
    if (key >= 0) {
      MessageCache cache = index.get(key);
      if (cache != null) {
        message = cache.getMessageSoftReference().get();
        if (message == null) {
          message = reloadMessage(cache.getFilePosition());
          cache.update(message);
        }
      }
    }
    return message;
  }


  private Message reloadMessage(long filePosition) throws IOException {
    readChannel.position(filePosition);
    lengthBuffer.clear();
    readChannel.read(lengthBuffer);
    int len = lengthBuffer.getInt(0);
    if(len > 0){
      readBuffer.limit(len);
      readChannel.read(readBuffer);
      readBuffer.flip();
      Message message = new Message(reader);
      readBuffer.clear();
      return message;
    }
    else{
      readChannel.position( readChannel.position() + (len * -1)); // skip
      return null;
    }
  }

  @Override
  public void close() throws IOException {
    super.close();
    readChannel.close();
    writeChannel.close();

  }
}
