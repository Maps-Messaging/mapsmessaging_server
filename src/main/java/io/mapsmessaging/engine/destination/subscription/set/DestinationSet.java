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

package io.mapsmessaging.engine.destination.subscription.set;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class DestinationSet implements Set<DestinationImpl> {

  @Getter
  private final SubscriptionContext context;
  private final Map<String, DestinationImpl> matching;

  public DestinationSet(SubscriptionContext context, Map<String, DestinationImpl> destinationMap) {
    this.context = context;
    matching = new LinkedHashMap<>(destinationMap);
  }

  public static boolean matches(String wildcard, String destinationName) {
    if (wildcard == null || destinationName == null) {
      return false;
    }
    if (!wildcard.startsWith("$") && destinationName.startsWith("$")) {
      return false; // Can not match $ topics with a wildcard
    }
    String[] nameList = destinationName.split("/", -1);
    String[] subList = wildcard.split("/", -1);

    for (int levelIndex = 0; levelIndex < subList.length; levelIndex++) {
      String level = subList[levelIndex];
      if (level.equals("#")) {
        return levelIndex == subList.length - 1;
      }
      if (levelIndex >= nameList.length || level.contains("#")) {
        return false;
      }
      if (!level.equals("+") && !level.equals(nameList[levelIndex])) {
        return false;
      }
    }
    return nameList.length == subList.length;
  }

  @Override
  public int size() {
    return matching.size();
  }

  @Override
  public boolean isEmpty() {
    return matching.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    if (o instanceof String) {
      return matching.containsKey(o);
    } else if (o instanceof DestinationImpl) {
      return matching.containsValue(o);
    }
    return false;
  }

  @Override
  public @NonNull @NotNull Iterator<DestinationImpl> iterator() {
    return matching.values().iterator();
  }

  @Override
  public void forEach(Consumer<? super DestinationImpl> action) {
    for (DestinationImpl destinationImpl : matching.values()) {
      action.accept(destinationImpl);
    }
  }

  @Override
  public Object @NotNull [] toArray() {
    return matching.values().toArray(new Object[0]);
  }

  @Override
  public <T> T @NotNull [] toArray(T @NotNull [] a) {
    return matching.values().toArray(a);
  }

  public boolean interest(String destinationName) {
    return matches(context, destinationName);
  }

  @Override
  public boolean add(DestinationImpl destination) {
    if (matches(context, destination.getFullyQualifiedNamespace())) {
      matching.put(destination.getFullyQualifiedNamespace(), destination);
      return true;
    }
    return false;
  }

  @Override
  public boolean remove(Object o) {
    if (o instanceof String) {
      return matching.remove(o) != null;
    } else if (o instanceof DestinationImpl destination) {
      return matching.remove(destination.getFullyQualifiedNamespace()) != null;
    }
    return false;
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    boolean result = true;
    for (Object o : c) {
      if (!contains(o)) {
        result = false;
        break;
      }
    }
    return result;
  }

  @Override
  public boolean addAll(Collection<? extends DestinationImpl> c) {
    boolean result = false;
    for (DestinationImpl destinationImpl : c) {
      if (add(destinationImpl)) {
        result = true;
      }
    }
    return result;
  }

  @Override
  public boolean retainAll(@NonNull @NotNull Collection<?> c) {
    return false;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    boolean result = false;
    for (Object o : c) {
      if (remove(o)) {
        result = true;
      }
    }
    return result;
  }

  @Override
  public boolean removeIf(@NotNull Predicate<? super DestinationImpl> filter) {
    for (DestinationImpl destinationImpl : matching.values()) {
      if (filter.test(destinationImpl)) {
        remove(destinationImpl);
        return true;
      }
    }
    return false;
  }

  @Override
  public void clear() {
    matching.clear();
  }

  public static boolean matches(SubscriptionContext context, String destinationName) {
    if (context.containsWildcard()) {
      return matches(context.getFilter(), destinationName);
    } else {
      return context.getFilter().equals(destinationName);
    }
  }
}
