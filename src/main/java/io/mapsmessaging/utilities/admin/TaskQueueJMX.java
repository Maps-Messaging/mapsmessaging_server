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
import io.mapsmessaging.utilities.admin.HealthStatus.LEVEL;
import io.mapsmessaging.utilities.threads.tasks.TaskScheduler;

import javax.management.ObjectInstance;
import java.util.ArrayList;
import java.util.List;

@JMXBean(description = "Task Queue monitor")
public class TaskQueueJMX implements HealthMonitor {

  private final TaskScheduler taskQueue;
  private final ObjectInstance instance;

  //<editor-fold desc="Life cycle functions">
  public TaskQueueJMX(TaskScheduler taskQueue, List<String> typePath) {
    this.taskQueue = taskQueue;
    List<String> scheduler = new ArrayList<>(typePath);
    instance = JMXManager.getInstance().register(this, scheduler);
  }

  public void close() {
    JMXManager.getInstance().unregister(instance);
  }
  //</editor-fold>

  //<editor-fold desc="JMX Bean implementation">
  @JMXBeanAttribute(name = "Outstanding Tasks", description = "Returns the current number of tasks pending execution")
  public long getOutstanding() {
    return taskQueue.getOutstanding();
  }

  @JMXBeanAttribute(name = "Total Tasks Queued", description = "Returns the total number of tasks that have been queued")
  public long getTotalTasksQueued() {
    return taskQueue.getTotalTasksQueued();
  }

  @JMXBeanAttribute(name = "Maximum outstanding", description = "Returns maximum number of tasks that have been waiting for execution")
  public long getMaxOutstanding() {
    return taskQueue.getMaxOutstanding();
  }

  @JMXBeanAttribute(name = "Thread offload count", description = "Returns the number of times the task queue has been offloaded to a dedicated thread")
  public long getOffloadCount() {
    return taskQueue.getOffloadCount();
  }

  @Override
  @JMXBeanAttribute(name = "Health status", description = "Returns the current HealthStatus for this task queue")
  public HealthStatus checkHealth() {
    return new HealthStatus("TaskQueue", LEVEL.INFO, "Task Queue seems ok", instance.getObjectName().toString());
  }
  //</editor-fold>

}
