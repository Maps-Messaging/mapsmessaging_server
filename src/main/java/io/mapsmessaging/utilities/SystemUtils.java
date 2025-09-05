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

package io.mapsmessaging.utilities;

import com.sun.management.OperatingSystemMXBean;
import lombok.Getter;

import java.lang.management.ManagementFactory;

@SuppressWarnings("java:S6548") // yes it is a singleton
public class SystemUtils {


  private static class Holder {
    static final SystemUtils INSTANCE = new SystemUtils();
  }

  public static SystemUtils getInstance() {
    return Holder.INSTANCE;
  }


  private final OperatingSystemMXBean osMXBean;

  @Getter
  private long lastCpuTime;

  @Getter
  private float cpuPercentage;

  private long lastProcessed;

  private SystemUtils() {
    osMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    lastProcessed = System.currentTimeMillis();
    lastCpuTime = osMXBean.getProcessCpuTime()/1_000_000;
  }

  public long getCpuTime() {
    long now = System.currentTimeMillis();
    long timeAvailable = now - lastProcessed;
    if(timeAvailable <=0 ) timeAvailable = 1;
    long totalTime = timeAvailable * Runtime.getRuntime().availableProcessors();

    long current = osMXBean.getProcessCpuTime()/ 1_000_000;
    long usedTime = current - lastCpuTime;

    int percent  =(int)( (usedTime * 1000)/totalTime);
    if(percent < 0){
      percent = 0;
    }
    cpuPercentage = percent/10f;
    lastProcessed = now;
    lastCpuTime = current;

    return current;
  }
}
