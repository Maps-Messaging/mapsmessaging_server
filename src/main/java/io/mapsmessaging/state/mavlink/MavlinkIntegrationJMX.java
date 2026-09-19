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

package io.mapsmessaging.state.mavlink;

import com.udojava.jmx.wrapper.JMXBean;
import com.udojava.jmx.wrapper.JMXBeanAttribute;
import io.mapsmessaging.utilities.admin.JMXManager;

import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.util.List;

/**
 * Registered under {@code io.mapsmessaging:type=Integration,name=Mavlink,source="<source>"}.
 * The source includes the configured MAVLink source name and topic so multiple MAVLink state
 * inputs can expose metrics concurrently without colliding on one global MBean name.
 */
@JMXBean(description = "MAVLink ingest integration metrics")
public class MavlinkIntegrationJMX {

  private final MavlinkTwinUpdater updater;
  private final String source;
  private final ObjectInstance mbean;

  MavlinkIntegrationJMX(MavlinkTwinUpdater updater, String source) {
    this.updater = updater;
    this.source = source == null || source.isBlank() ? "mavlink" : source;
    this.mbean = JMXManager.getInstance().register(
        this,
        List.of(
            "type=Integration",
            "name=Mavlink",
            "source=" + ObjectName.quote(this.source)));
  }

  void close() {
    JMXManager.getInstance().unregister(mbean);
  }

  @JMXBeanAttribute(name = "Source", description = "Configured MAVLink source identity (name|topic)")
  public String getSource() {
    return source;
  }

  @JMXBeanAttribute(name = "Messages Processed Count", description = "Total MAVLink packets processed into twin state updates")
  public long getMessagesProcessedCount() {
    return updater.getMessagesProcessedCount();
  }

  @JMXBeanAttribute(name = "Twins Created Count", description = "Total new twins created from MAVLink sources")
  public long getTwinsCreatedCount() {
    return updater.getTwinsCreatedCount();
  }

  @JMXBeanAttribute(name = "Classification Override Count", description = "Twins created with a per-asset mavlink.knownSources[].cotClassification override")
  public long getClassificationOverrideCount() {
    return updater.getClassificationOverrideCount();
  }
}
