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

import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.serializer.MapDBSerializer;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class DBResource extends Resource {

  private final Map<Long, MessageCache> index;
  private  BTreeMap<Long, Message> diskMap;
  private final DB dataStore;
  private final String fileName;

  public DBResource(String name, String mapped) {
    super(name, mapped);
    index = new LinkedHashMap<>();
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
    fileName = tmpName;
    dataStore = DBMaker.fileDB(fileName)
        .fileMmapEnable()
        .closeOnJvmShutdown()
        .cleanerHackEnable()
        .checksumHeaderBypass()
        .make();
    diskMap = dataStore
            .treeMap(name, Serializer.LONG, new MapDBSerializer<>(Message.class))
            .createOrOpen();
    for(Long value:diskMap.getKeys()){
      index.put(value, new MessageCache());
    }
  }

  @Override
  public boolean isEmpty(){
    return index.isEmpty();
  }

  @Override
  public Iterator<Long> getIterator(){
    return diskMap.getKeys().iterator();
  }

  public synchronized void flush(){
    for(MessageCache messageCache:index.values()){
      messageCache.messageSoftReference.clear();
    }
    index.clear();
  }

  @Override
  public synchronized void stop() {
    dataStore.commit();
    dataStore.close();
    flush();
  }

  @Override
  public synchronized void delete() throws IOException {
    close();
    File tmp = new File(fileName);
    Files.delete(tmp.toPath());
  }

  @Override
  public synchronized void close() {
    if (!isClosed) {
      super.close();
      dataStore.close();
    }
  }

  @Override
  public synchronized void add(Message message) throws IOException {
    super.add(message);
    diskMap.put(message.getIdentifier(), message);
    index.put(message.getIdentifier(), new MessageCache(message));
  }

  @Override
  public synchronized Message get(long key) {
    Message message = null;
    if (key >= 0) {
      MessageCache cache = index.get(key);
      if (cache != null) {
        message = cache.messageSoftReference.get();
      }
      if (message == null) {
        message = diskMap.get(key);
        if(message != null) {
          if (cache == null) {
            cache = new MessageCache();
            index.put(key, cache);
          }
          cache.update(message);
        }
      }
    }
    return message;
  }

  @Override
  public synchronized void remove(long key) throws IOException {
    super.remove(key);
    diskMap.remove(key);
    MessageCache cache = index.remove(key);
    if (cache != null) {
      cache.messageSoftReference.clear();
    }
  }

  @Override
  public synchronized long size() {
    return diskMap.size();
  }

  private static final class MessageCache {

    private SoftReference<Message> messageSoftReference;

    MessageCache() {
      messageSoftReference = new SoftReference<>(null);
    }

    MessageCache(Message msg) {
      update(msg);
    }

    public void update(Message msg) {
      messageSoftReference = new SoftReference<>(msg);
    }
  }
}
