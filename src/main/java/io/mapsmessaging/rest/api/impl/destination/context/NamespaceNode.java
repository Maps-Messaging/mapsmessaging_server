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

package io.mapsmessaging.rest.api.impl.destination.context;

import lombok.Getter;

import java.util.*;

public class NamespaceNode {

  @Getter
  private final NamespaceNode parent;

  @Getter
  private final String segment;

  @Getter
  private long lastUpdateMillis;

  private Map<String, NamespaceNode> foldersByName;
  private Map<String, Type> destinationsByName;

  private List<Entry> entriesSorted;

  public NamespaceNode(NamespaceNode parent, String segment, long lastUpdateMillis) {
    this.parent = parent;
    this.segment = segment;
    this.lastUpdateMillis = lastUpdateMillis;
  }

  public NamespaceNode getChild(String name) {
    if (foldersByName == null) {
      return null;
    }
    return foldersByName.get(name);
  }

  public NamespaceNode getOrCreateChild(String name, long now) {
    return getOrCreateFolder(name, now);
  }

  public NamespaceNode getOrCreateFolder(String name, long now) {
    if (foldersByName == null) {
      foldersByName = new HashMap<>();
    }

    NamespaceNode existing = foldersByName.get(name);
    if (existing != null) {
      return existing;
    }

    NamespaceNode created = new NamespaceNode(this, name, now);
    foldersByName.put(name, created);
    invalidate(now);
    return created;
  }

  public void addDestination(String name, Type type, long now) {
    if (type == null || type == Type.FOLDER) {
      return;
    }

    if (destinationsByName == null) {
      destinationsByName = new HashMap<>();
    }

    Type existing = destinationsByName.get(name);
    if (existing == type) {
      return;
    }

    destinationsByName.put(name, type);
    invalidate(now);
  }

  public List<Entry> pageEntries(int pageNo, int pageSize) {
    List<Entry> sorted = getEntriesSorted();

    if (pageNo < 0 || pageSize <= 0) {
      return Collections.emptyList();
    }

    int startIndex = pageNo * pageSize;
    if (startIndex >= sorted.size()) {
      return Collections.emptyList();
    }

    int endIndex = Math.min(startIndex + pageSize, sorted.size());
    return sorted.subList(startIndex, endIndex);
  }

  public int getEntryCount() {
    return getEntriesSorted().size();
  }

  private List<Entry> getEntriesSorted() {
    if (entriesSorted != null) {
      return entriesSorted;
    }

    List<Entry> list = new ArrayList<>();

    if (foldersByName != null && !foldersByName.isEmpty()) {
      for (NamespaceNode folder : foldersByName.values()) {
        list.add(new Entry(
            folder.getSegment(),
            folder.getFullPath(),
            Type.FOLDER,
            folder.getEntryCount()
        ));
      }
    }

    if (destinationsByName != null && !destinationsByName.isEmpty()) {
      for (Map.Entry<String, Type> destination : destinationsByName.entrySet()) {
        String childName = destination.getKey();
        Type type = destination.getValue();
        list.add(new Entry(
            childName,
            getChildPath(childName),
            type,
            0
        ));
      }
    }

    list.sort(Comparator
        .comparing(Entry::getName)
        .thenComparing(Entry::getDestinationType));

    entriesSorted = Collections.unmodifiableList(list);
    return entriesSorted;
  }

  private boolean hasChildren() {
    return foldersByName != null && !foldersByName.isEmpty();
  }

  public String getFullPath() {
    Deque<String> segments = new ArrayDeque<>();
    NamespaceNode current = this;

    while (current != null && current.parent != null) {
      segments.addFirst(current.segment);
      current = current.parent;
    }

    if (segments.isEmpty()) {
      return "";
    }

    boolean absolute = segments.peekFirst().isEmpty();
    StringBuilder path = new StringBuilder();

    if (absolute) {
      path.append('/');
      segments.removeFirst();
    }

    Iterator<String> it = segments.iterator();
    while (it.hasNext()) {
      path.append(it.next());
      if (it.hasNext()) {
        path.append('/');
      }
    }

    if (path.isEmpty() && absolute) {
      return "/";
    }

    return path.toString();
  }

  private String getChildPath(String childName) {
    String base = getFullPath();
    if (base.isEmpty()) {
      return childName;
    }
    if (base.equals("/")) {
      return "/" + childName;
    }
    return base + "/" + childName;
  }

  private void invalidate(long now) {
    entriesSorted = null;
    touch(now);
  }

  private void touch(long now) {
    lastUpdateMillis = Math.max(lastUpdateMillis, now);
    if (parent != null) {
      parent.touch(now);
    }
  }
}
