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

package io.mapsmessaging.hardware.trigger;

import io.mapsmessaging.configuration.ConfigurationProperties;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PeriodicTrigger extends Trigger {

  private final ScheduledExecutorService executorService;
  private final long periodInMilliseconds;

  public PeriodicTrigger(){
    executorService = null;
    periodInMilliseconds = -1;
  }

  public PeriodicTrigger(long periodInMilliseconds) {
    super();
    this.executorService = Executors.newScheduledThreadPool(1);
    this.periodInMilliseconds = periodInMilliseconds;
  }

  @Override
  public Trigger build(ConfigurationProperties properties) throws IOException {
    return new PeriodicTrigger(properties.getLongProperty("interval", 60000));
  }


  public void start() {
    executorService.scheduleAtFixedRate(() -> {
      runActions();
    }, 0, periodInMilliseconds, TimeUnit.MILLISECONDS);
  }

  public void stop() {
    executorService.shutdown();
  }

  @Override
  public String getName() {
    return "periodic";
  }

  @Override
  public String getDescription() {
    return "Triggers device read periodically";
  }
}
