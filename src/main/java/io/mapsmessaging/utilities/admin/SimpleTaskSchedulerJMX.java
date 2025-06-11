/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.utilities.admin;

import com.udojava.jmx.wrapper.JMXBean;
import com.udojava.jmx.wrapper.JMXBeanAttribute;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;

import java.util.ArrayList;
import java.util.List;

@JMXBean(description = "JMX Bean to monitor the internal task scheduler and thread pool")
public class SimpleTaskSchedulerJMX {

  public SimpleTaskSchedulerJMX(List<String> typePath) {
    List<String> scheduler = new ArrayList<>(typePath);
    scheduler.add("scheduler=scheduler");
    JMXManager.getInstance().register(this, scheduler);
  }

  @JMXBeanAttribute(name = "Total Scheduled", description = "Returns the total number of tasks that have been scheduled")
  public long getTotalScheduled() {
    return SimpleTaskScheduler.getInstance().getTotalScheduled();
  }

  @JMXBeanAttribute(name = "Total Executed", description = "Returns the total number of tasks that have been executed")
  public long getTotalExecuted() {
    return SimpleTaskScheduler.getInstance().getTotalExecuted();
  }

  @JMXBeanAttribute(name = "Queue depth", description = "Returns the current depth of tasks that need to be executed")
  public int getDepth() {
    return SimpleTaskScheduler.getInstance().getDepth();
  }
}
