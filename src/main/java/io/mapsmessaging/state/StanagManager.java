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

package io.mapsmessaging.state;

import io.mapsmessaging.state.config.StanagConfig;
import io.mapsmessaging.state.mavlink.stanag.StanagStateSubscriber;
import io.mapsmessaging.utilities.Lifecycle;

import java.io.IOException;

public class StanagManager implements Lifecycle {

  private StanagStateSubscriber stanagStateSubscriber;

  public StanagManager(StanagConfig config) {
    try {
      stanagStateSubscriber = new StanagStateSubscriber(config);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void start() {
    try {
      stanagStateSubscriber.start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void stop() {
    try {
      stanagStateSubscriber.stop();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
