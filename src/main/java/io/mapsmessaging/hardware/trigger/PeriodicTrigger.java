/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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

package io.mapsmessaging.hardware.trigger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PeriodicTrigger extends Trigger {

  private final ScheduledExecutorService executorService;
  private final long periodInSeconds;

  public PeriodicTrigger(long periodInSeconds) {
    super();
    this.executorService = Executors.newScheduledThreadPool(1);
    this.periodInSeconds = periodInSeconds;
  }

  public void start() {
    executorService.scheduleAtFixedRate(() -> {
      runActions();
    }, 0, periodInSeconds, TimeUnit.SECONDS);
  }

  public void stop() {
    executorService.shutdown();
  }
}
