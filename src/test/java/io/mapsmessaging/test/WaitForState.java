/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class WaitForState {

  public static void waitFor(long time, TimeUnit timeUnit, Condition condition) throws IOException {
    long timeout = System.currentTimeMillis() + timeUnit.toMillis(time);
    while(timeout > System.currentTimeMillis() && !condition.complete()){
      LockSupport.parkNanos(100000000);
    }
  }

  public static void wait(long time, TimeUnit timeUnit) {
    long timeout = System.currentTimeMillis() + timeUnit.toMillis(time);
    while(timeout > System.currentTimeMillis()){
      LockSupport.parkNanos(100000000);
    }
  }

}
