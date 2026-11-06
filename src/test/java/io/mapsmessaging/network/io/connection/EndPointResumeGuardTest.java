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

package io.mapsmessaging.network.io.connection;

import io.mapsmessaging.network.io.connection.state.State;
import io.mapsmessaging.network.io.connection.state.StateMonitor;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class EndPointResumeGuardTest {

  @ParameterizedTest
  @CsvSource({"true,true,1", "true,false,0", "false,true,0", "false,false,0"})
  void resume_clears_pause_and_schedules_only_running_connections(
      boolean paused, boolean running, int expectedStarts) throws Exception {
    EndPointConnection connection = mock(EndPointConnection.class, CALLS_REAL_METHODS);
    AtomicBoolean pausedFlag = new AtomicBoolean(paused);
    StateMonitor monitor = mock(StateMonitor.class);
    set(connection, "paused", pausedFlag);
    set(connection, "running", new AtomicBoolean(running));
    set(connection, "stateMonitor", monitor);
    doNothing().when(connection).scheduleState(any(State.class));

    connection.resume();
    connection.resume();

    assertFalse(pausedFlag.get());
    verify(monitor, times(expectedStarts)).start();
    verify(connection, times(expectedStarts)).scheduleState(any(State.class));
  }

  private static void set(EndPointConnection connection, String name, Object value) throws Exception {
    Field field = EndPointConnection.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(connection, value);
  }
}
