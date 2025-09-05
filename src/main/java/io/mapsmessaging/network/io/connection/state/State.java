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

package io.mapsmessaging.network.io.connection.state;

import io.mapsmessaging.network.io.connection.Constants;
import io.mapsmessaging.network.io.connection.EndPointConnection;

public abstract class State implements Runnable {

  protected final EndPointConnection endPointConnection;
  private final long timeout;

  protected State(EndPointConnection connection) {
    this.endPointConnection = connection;
    timeout = System.currentTimeMillis() + Constants.TIMEOUT;
  }

  public boolean hasTimedOut() {
    return timeout < System.currentTimeMillis();
  }

  public void run() {
    execute();
  }

  public void cancel() {
    // No Op in most cases
  }

  public abstract void execute();

  public abstract String getName();

}
