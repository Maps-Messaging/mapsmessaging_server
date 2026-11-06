/*
 *
 *  Copyright [ 2026 ] Ralf Himmelein and Claude
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

package io.mapsmessaging.state.adapter.cot;

import com.udojava.jmx.wrapper.JMXBean;
import com.udojava.jmx.wrapper.JMXBeanAttribute;
import io.mapsmessaging.utilities.admin.JMXManager;

import javax.management.ObjectInstance;
import java.util.List;

/**
 * Registered under {@code io.mapsmessaging:type=Integration,name=CotIngest} - same
 * JMXManager/JMXBeanWrapper convention as {@code MtiStatusAdapterJMX}.
 */
@JMXBean(description = "CoT edge-ingest bridge integration metrics")
public class CotIngestAdapterJMX {

  private final CotIngestAdapter adapter;
  private final ObjectInstance mbean;

  CotIngestAdapterJMX(CotIngestAdapter adapter) {
    this.adapter = adapter;
    this.mbean = JMXManager.getInstance().register(this, List.of("type=Integration", "name=CotIngest"));
  }

  void close() {
    JMXManager.getInstance().unregister(mbean);
  }

  @JMXBeanAttribute(name = "Routed Count", description = "Total inbound edge CoT events successfully routed to TwinManager")
  public long getRoutedCount() {
    return adapter.getRoutedCount();
  }

  @JMXBeanAttribute(name = "Dropped Count", description = "Total inbound edge CoT events dropped (no usable uid)")
  public long getDroppedCount() {
    return adapter.getDroppedCount();
  }

  @JMXBeanAttribute(name = "Last Message Age Millis", description = "Milliseconds since the last edge CoT event, -1 if none received yet")
  public long getLastMessageAgeMillis() {
    return adapter.getLastMessageAgeMillis();
  }
}
