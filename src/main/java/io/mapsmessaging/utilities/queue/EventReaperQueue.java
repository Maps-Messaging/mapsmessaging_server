package io.mapsmessaging.utilities.queue;

import io.mapsmessaging.utilities.collections.NaturalOrderedLongQueue;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class EventReaperQueue {

  private final AtomicInteger indexGenerator;
  private final ThreadLocalQueue[] localQueue;
  private final ThreadLocal<Integer> threadIndex;

  public EventReaperQueue() {
    indexGenerator = new AtomicInteger(0);
    threadIndex = new ThreadLocal<>();
    int numberOfCPUs = Runtime.getRuntime().availableProcessors();
    localQueue = new ThreadLocalQueue[numberOfCPUs];
    for (int x = 0; x < numberOfCPUs; x++) {
      localQueue[x] = new ThreadLocalQueue();
    }
  }

  public void close() {
    for (ThreadLocalQueue threadLocalQueue : localQueue) {
      threadLocalQueue.getAndClear();
    }
  }

  public void add(long id) {
    Integer index = threadIndex.get();
    if (index == null) {
      index = indexGenerator.incrementAndGet() % localQueue.length;
    }
    localQueue[index].add(id);
  }

  public Queue<Long> getAndClear() {
    if (Arrays.stream(localQueue).anyMatch(threadLocalQueue -> !threadLocalQueue.queue.isEmpty())) {
      Queue<Long> result = new NaturalOrderedLongQueue(0, new BitSetFactoryImpl(8192));
      for (ThreadLocalQueue threadLocalQueue : localQueue) {
        Queue<Long> tQueue = threadLocalQueue.getAndClear();
        result.addAll(tQueue);
      }
      return result;
    }
    return new LinkedList<>();
  }

  private static final class ThreadLocalQueue {

    private Queue<Long> queue;

    public ThreadLocalQueue() {
      queue = new NaturalOrderedLongQueue(0, new BitSetFactoryImpl(8192));
    }

    public synchronized void add(long entry) {
      queue.offer(entry);
    }

    public synchronized Queue<Long> getAndClear() {
      Queue<Long> copy = queue;
      queue = new NaturalOrderedLongQueue(0, new BitSetFactoryImpl(8192));
      return copy;
    }
  }
}
