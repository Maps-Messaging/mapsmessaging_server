package io.mapsmessaging.engine.destination.delayed;

import io.mapsmessaging.utilities.collections.NaturalOrderedLongQueue;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactory;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.Queue;


/**
 * Structure bucket containing message identifiers.
 *
 * Please note.. The structure containing the message identifiers has a Int (32 bit) value to uniquely identify them, however, time in milliseconds are longs (64 bits) this may
 * cause a single bucket containing delay values that span a roughly 24 day period ( Java uses 32 signed bits, so only 2^31 can be used )
 *
 * What this means is that when we process the bucket we need to confirm each events delay time and only if it has passed do we process it, else we simply pass over it
 */
public class DelayedBucket {

  private final NaturalOrderedLongQueue delayedMessageState;

  public DelayedBucket(long delayTime, BitSetFactory factory) {
    delayedMessageState = new NaturalOrderedLongQueue((int) delayTime, factory);
  }

  public boolean register(long identifier) {
    return delayedMessageState.offer(identifier);
  }

  public long size() {
    return delayedMessageState.size();
  }

  public boolean isEmpty() {
    return delayedMessageState.isEmpty();
  }

  @Override
  public String toString() {
    return delayedMessageState.toString();
  }

  public boolean remove(long messageIdentifier) {
    return delayedMessageState.remove(messageIdentifier);
  }

  public long peek() {
    Long val = delayedMessageState.peek();
    return val == null? -1: val;
  }

  public @NonNull @NotNull Queue<Long> getQueue() {
    return delayedMessageState;
  }
}