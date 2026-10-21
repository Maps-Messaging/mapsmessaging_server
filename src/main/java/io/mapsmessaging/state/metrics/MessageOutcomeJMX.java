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

import com.udojava.jmx.wrapper.JMXBean;
import com.udojava.jmx.wrapper.JMXBeanAttribute;
import io.mapsmessaging.utilities.admin.JMXManager;

import java.util.List;

/**
 * Registered as {@code io.mapsmessaging:type=Integration,name=MessageOutcome,source=..,reason=..,category=..}.
 * Never unregistered: the counters are process-wide and outlive any one adapter.
 */
@JMXBean(description = "Messages dropped as a genuine failure or filtered out by design, per source and reason")
public class MessageOutcomeJMX {

  private final MessageOutcomeStats.Source source;
  private final String reason;
  private final MessageOutcomeStats.Category category;

  MessageOutcomeJMX(MessageOutcomeStats.Source source, String reason, MessageOutcomeStats.Category category) {
    this.source = source;
    this.reason = reason;
    this.category = category;
    JMXManager.getInstance().register(this, List.of(
        "type=Integration",
        "name=MessageOutcome",
        "source=" + source.name(),
        "reason=" + reason,
        "category=" + category.name()));
  }

  @JMXBeanAttribute(name = "Count", description = "Messages with this outcome since startup")
  public long getCount() {
    return MessageOutcomeStats.getCount(source, reason, category);
  }
}
