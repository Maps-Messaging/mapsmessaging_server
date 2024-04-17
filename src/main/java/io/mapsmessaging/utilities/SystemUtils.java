/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.utilities;

import com.sun.management.OperatingSystemMXBean;

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

  private SystemUtils() {
    osMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
  }

  public long getCpuTime() {
    long processCpuTime = osMXBean.getProcessCpuTime();
    return processCpuTime / 1_000_000;
  }
}
