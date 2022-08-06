package io.mapsmessaging.utilities.queue;

import io.mapsmessaging.utilities.collections.NaturalOrderedLongQueue;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentQueue {

  private final AtomicInteger indexGenerator;
  private final ThreadLocalQueue[] localQueue;
  private final ThreadLocal<Integer> threadIndex;

  public ConcurrentQueue() {
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
    Queue<Long> result = new NaturalOrderedLongQueue(0, new BitSetFactoryImpl(8192));
    for (ThreadLocalQueue threadLocalQueue : localQueue) {
      Queue<Long> tQueue = threadLocalQueue.getAndClear();
      result.addAll(tQueue);
    }
    return result;
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

  public static void main(String[] args) throws InterruptedException {
    ConcurrentQueue queue = new ConcurrentQueue();
    for (int x = 0; x < 64; x++) {
      Thread t = new Thread(new Runnable() {
        long counter = 0;

        @Override
        public void run() {
          for (long x = 0; x < 10_000_000L; x++) {
            queue.add(counter++);
          }
        }
      });
      t.start();
    }
    while (true) {
      TimeUnit.SECONDS.sleep(1);
      Queue<Long> current = queue.getAndClear();
      System.err.println("Size::" + current);
    }
  }
}
