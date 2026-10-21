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

package io.mapsmessaging.state.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Counts messages that did not make it into the picture, split into genuine failures (malformed,
 * rejected, conversion failure, unexpected loss) and messages filtered out by design (IC26 KPI
 * clarification Q17: only failures count towards the drop/rejection rate; filtering is reported
 * separately). One {@link MessageOutcomeJMX} bean is registered per (source, reason) on first use.
 */
public final class MessageOutcomeStats {

  public enum Category {
    FAILURE,
    FILTERED
  }

  /** Where a message was dropped or filtered. */
  public enum Source {
    MAVLINK,
    COT_INGEST,
    MTI,
    TAK_PUBLISHER,
    TAK_SOCKET
  }

  private record Key(Source source, String reason, Category category) {
  }

  private static final Map<Key, LongAdder> COUNTS = new ConcurrentHashMap<>();
  private static final Map<Key, MessageOutcomeJMX> BEANS = new ConcurrentHashMap<>();

  private MessageOutcomeStats() {
  }

  public static void failure(Source source, String reason) {
    record(source, reason, Category.FAILURE);
  }

  public static void filtered(Source source, String reason) {
    record(source, reason, Category.FILTERED);
  }

  public static long getCount(Source source, String reason, Category category) {
    LongAdder adder = COUNTS.get(new Key(source, reason, category));
    return adder == null ? 0L : adder.sum();
  }

  private static void record(Source source, String reason, Category category) {
    Key key = new Key(source, reason, category);
    COUNTS.computeIfAbsent(key, ignored -> new LongAdder()).increment();
    BEANS.computeIfAbsent(key, ignored -> new MessageOutcomeJMX(source, reason, category));
  }
}
