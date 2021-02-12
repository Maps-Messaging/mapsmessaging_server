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

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.Map;
import org.maps.messaging.api.message.Message;

public abstract class MapBasedResource extends Resource {

  protected final Map<Long, MessageCache> index;

  public MapBasedResource(String name, String mapped) throws IOException {
    super(name, mapped);
    index = new LinkedHashMap<>();
  }

  @Override
  public void stop() throws IOException {
    index.clear();
  }

  @Override
  public void delete() throws IOException {
    index.clear();
  }

  @Override
  public long size() {
    return index.size();
  }

  @Override
  public void remove(long key) throws IOException {
    MessageCache cache = index.remove(key);
    if (cache != null) {
      cache.getMessageSoftReference().clear();
    }
    super.remove(key);
  }

  protected static final class MessageCache {

    private final long filePosition;
    private SoftReference<Message> messageSoftReference;

    MessageCache(Message msg, long filePosition) {
      this.filePosition = filePosition;
      update(msg);
    }

    public long getFilePosition() {
      return filePosition;
    }

    public SoftReference<Message> getMessageSoftReference() {
      return messageSoftReference;
    }

    public void update(Message msg) {
      messageSoftReference = new SoftReference<>(msg);
    }
  }
}
