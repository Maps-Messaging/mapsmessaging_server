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

package org.maps.messaging.engine.resources;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.Iterator;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.messaging.api.message.Message;
import org.maps.utilities.streams.RandomAccessFileObjectReader;
import org.maps.utilities.streams.RandomAccessFileObjectWriter;

public class FileResource extends MapBasedResource {

  private static final int REPORT_DELAY = 10000;

  private final Logger logger;
  private final RandomAccessFile randomAccessWriteFile;
  private final RandomAccessFile randomAccessReadFile;
  private final RandomAccessFileObjectWriter writer;
  private final RandomAccessFileObjectReader reader;

  public FileResource(String name, String mapped) throws IOException {
    super(name, mapped);
    logger = LoggerFactory.getLogger("FileResource:"+name+"_"+mapped);
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
    tmpName += "data.bin";
    randomAccessWriteFile = new RandomAccessFile(tmpName, "rw");
    randomAccessReadFile = new RandomAccessFile(tmpName, "rw");
    writer = new RandomAccessFileObjectWriter(randomAccessWriteFile);
    reader = new RandomAccessFileObjectReader(randomAccessReadFile);
    if (randomAccessReadFile.length() != 0) {
      reload();
      randomAccessWriteFile.seek(randomAccessReadFile.getFilePointer());
    }
  }

  private void reload() throws IOException {
    long pos = 0;
    long eof = randomAccessReadFile.length();
    long report = System.currentTimeMillis() + REPORT_DELAY;
    while (pos != eof) {
      Message message = new Message(reader);
      index.put(message.getIdentifier(), new MessageCache(message, pos));
      pos = randomAccessReadFile.getFilePointer();
      if(logger.isInfoEnabled() && report < System.currentTimeMillis()){
        float percent = (float)eof - pos;
        percent = percent/eof * 100.0f;
        logger.log(LogMessages.FILE_RELOAD_PERCENT, percent);
        report = System.currentTimeMillis() + REPORT_DELAY;
      }
    }
  }

  @Override
  public void stop() throws IOException {
    super.stop();
    try {
      randomAccessWriteFile.close();
      randomAccessReadFile.close();
    } catch (IOException e) {
      logger.log(LogMessages.FILE_FAILED_TO_CLOSE, e);
    }
  }

  @Override
  public void delete() throws IOException {
    super.delete();
    try {
      randomAccessWriteFile.close();
      randomAccessReadFile.close();
      File file = new File(getName());
      Files.delete(file.toPath());
    } catch (IOException e) {
      logger.log(LogMessages.FILE_FAILED_TO_DELETE, e);
    }
  }

  @Override
  public void add(Message message) throws IOException {
    super.add(message);

    long pos = randomAccessWriteFile.getFilePointer();
    index.put(message.getIdentifier(), new MessageCache(message, pos));
    randomAccessWriteFile.seek(pos);
    message.write(writer);
    index.put(message.getIdentifier(), new MessageCache(message, pos));
  }

  @Override
  public Message get(long key) throws IOException {
    Message message = null;
    if (key >= 0) {
      MessageCache cache = index.get(key);
      if (cache != null) {
        message = cache.getMessageSoftReference().get();
        if (message == null) {
          randomAccessReadFile.seek(cache.getFilePosition());
          message = new Message(reader);
          cache.update(message);
        }
      }
    }
    return message;
  }

  @Override
  public void remove(long key) throws IOException {
    MessageCache cache = index.remove(key);
    if (cache != null) {
      randomAccessReadFile.seek(cache.getFilePosition());
      randomAccessReadFile.writeLong(-1);
      cache.getMessageSoftReference().clear();
    }
  }
  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public Iterator<Long> getIterator() {
    return null;
  }

}
