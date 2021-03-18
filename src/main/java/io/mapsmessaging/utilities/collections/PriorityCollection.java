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

package io.mapsmessaging.utilities.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PriorityCollection<T> implements Collection<T> {

  final List<Queue<T>> priorityStructure;
  final int prioritySize;
  final PriorityFactory<T> priorityFactory;
  protected final AtomicLong entryCount;

  public PriorityCollection(int priorityBound, @Nullable PriorityFactory<T> factory) {
    if (priorityBound <= 0) {
      throw new IllegalArgumentException("Priorities must be greater than 0");
    }
    priorityStructure = new ArrayList<>();
    prioritySize = priorityBound;
    priorityFactory = factory;
    for (int x = 0; x < priorityBound; x++) {
      priorityStructure.add(new LinkedList<>());
    }
    entryCount=new AtomicLong(0);
  }

  public PriorityCollection(Queue<T>[] priorityQueues, @Nullable PriorityFactory<T> factory) {
    if (priorityQueues == null || priorityQueues.length == 0) {
      throw new IllegalArgumentException("Priority Queues must have 1 or more entries");
    }
    priorityStructure = new ArrayList<>();
    prioritySize = priorityQueues.length;
    priorityFactory = factory;
    priorityStructure.addAll(Arrays.asList(priorityQueues).subList(0, prioritySize));
    entryCount=new AtomicLong(0);
  }

  public void close() throws Exception {
    for (Queue<T> queues : priorityStructure) {
      queues.clear();
      if (queues instanceof AutoCloseable) {
        ((AutoCloseable) queues).close();
      }
    }
    entryCount.set(0);
  }


  public Queue<T> flatten (Queue<T> flattenQueue){
    for (Queue<T> queue : priorityStructure) {
      flattenQueue.addAll(queue);
    }
    return flattenQueue;
  }

  public String toString(){
    StringBuilder sb = new StringBuilder("Count:");
    sb.append(entryCount).append(",");
    for (Queue<T> queue : priorityStructure) {
      if(!queue.isEmpty()) {
        sb.append(queue.toString()).append("\n");
      }
    }
    return sb.toString();
  }

  public List<Queue<T>> getPriorityStructure() {
    return priorityStructure;
  }

  public boolean isEmpty() {
    return entryCount.get() == 0;
  }

  @Override
  public boolean contains(Object o) {
    if(entryCount.get() != 0) {
      for (Queue<T> ts : priorityStructure) {
        if (ts.contains(o)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public @NonNull @NotNull Iterator<T> iterator() {
    return new PriorityCollectionIterator();
  }

  @Override
  public Object[] toArray() {
    ArrayList<Object> response = new ArrayList<>();
    for (int x = prioritySize - 1; x >= 0; x--) {
      if (!priorityStructure.get(x).isEmpty()) {
        response.addAll(priorityStructure.get(x));
      }
    }
    return response.toArray();
  }

  public int size() {
    int size = 0;
    for (Queue<T> ts : priorityStructure) {
      if (!ts.isEmpty()) {
        size += ts.size();
      }
    }
    return size;
  }

  @Override
  public boolean add(T t) {
    return push(t);
  }

  public boolean add(T t, int priority) {
    return push(t, priority);
  }

  @Override
  public boolean remove(Object obj) {
    T entry = (T) obj;
    boolean result = false;
    if (priorityFactory != null) {
      int priority = priorityFactory.getPriority(entry);
      result = priorityStructure.get(priority).remove(entry);
    } else {
      for (Queue<T> queue : priorityStructure) {
        if (queue.remove(entry)) {
          result = true;
          break;
        }
      }
    }
    if(result){
      entryCount.decrementAndGet();
    }
    return result;
  }

  @Override
  public boolean addAll(Collection<? extends T> rhsCollection) {
    if(rhsCollection instanceof PriorityCollection){
      // Copy, but maintain the priority
      PriorityCollection<T> priorityRhs = (PriorityCollection<T>)rhsCollection;
      for(int x=0;x<priorityStructure.size();x++){
        Queue<T> lhs = priorityStructure.get(x);
        lhs.addAll(priorityRhs.priorityStructure.get(x));
      }
    }
    else {
      for (T entry : rhsCollection) {
        if (!add(entry)) {
          entryCount.set(size());
          return false;
        }
      }
    }
    entryCount.set(size());
    return true;
  }

  public boolean addAll(Collection<? extends T> c, int priority) {
    for (T entry : c) {
      if (!add(entry, priority)) {
        entryCount.set(size());
        return false;
      }
    }
    entryCount.set(size());
    return true;
  }

  @Override
  public void clear() {
    for (Queue<T> ts : priorityStructure) {
      ts.clear();
    }
    entryCount.set(0);
  }

  @Override
  public <T1> T1[] toArray(T1[] a) {
    Iterator<?> itr = iterator();
    for (int x = 0; x < a.length && itr.hasNext(); x++) {
      a[x] = (T1) itr.next();
    }
    return a;
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    for (Object l : c) {
      if (!contains(l)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    boolean result = true;
    for (Object l : c) {
      if (!remove(l)) {
        result = false;
      }
    }
    return result;
  }

  @Override
  public boolean retainAll(@NotNull Collection<?> c) {
    Iterator<?> itr = iterator();
    boolean changed = false;
    while(itr.hasNext()){
      Object val = itr.next();
      if(!c.contains(val)){
        itr.remove();
        changed = true;
      }
    }
    return changed;
  }

  boolean push(T entry) {
    if (entry == null) {
      throw new IllegalArgumentException("NULL not accepted");
    }
    if (priorityFactory == null) {
      throw new UnsupportedOperationException("No priority factory configured, unable to anonymously add entry");
    }
    int priority = priorityFactory.getPriority(entry);
    return push(entry, priority);
  }

  boolean push(T entry, int priority) {
    if (priority < 0 || priority > prioritySize) {
      throw new IllegalArgumentException("Supplied priority outside of defined bounds");
    }
    boolean ret = priorityStructure.get(priority).add(entry);
    if(ret){
      entryCount.incrementAndGet();
    }
    return ret;
  }
  
  private class PriorityCollectionIterator implements Iterator<T> {

    private final List<Iterator<T>> iterators;
    private Iterator<T> active;

    public PriorityCollectionIterator() {
      iterators = new ArrayList<>();
      for (int x = prioritySize - 1; x >= 0; x--) {
        iterators.add(priorityStructure.get(x).iterator());
      }
      active = null;
    }

    @Override
    public boolean hasNext() {
      if (!iterators.isEmpty()) {
        if (iterators.get(0).hasNext()) {
          return true;
        }
        //
        // Top iterator is empty, remove it and check the next one
        //
        iterators.remove(0);
        return hasNext();
      }
      return false;
    }

    @Override
    public T next() {
      if (!iterators.isEmpty()) {
        if (iterators.get(0).hasNext()) {
          active = iterators.get(0);
          return active.next();
        } else {
          iterators.remove(0);
          return next();
        }
      }
      throw new NoSuchElementException();
    }

    @Override
    public void remove() {
      if(active != null){
        active.remove();
      }
      else {
        throw new NoSuchElementException();
      }
    }
  }

  protected void recalculateSize(){
    entryCount.set(size());
  }
}
