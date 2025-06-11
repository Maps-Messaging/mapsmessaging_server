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

package io.mapsmessaging.engine.tasks;

import io.mapsmessaging.logging.ThreadContext;

import java.util.Map;
import java.util.concurrent.Callable;

public abstract class EngineTask implements Callable<Response> {

  private final Map<String, String> logContext;

  protected EngineTask() {
    logContext = ThreadContext.getContext();
  }

  // The exception is derived from the Callable interface, and we just extend it here
  @java.lang.SuppressWarnings("squid:S00112")
  public abstract Response taskCall() throws Exception;

  @Override
  public Response call() throws Exception {
    if (logContext != null) {
      ThreadContext.putAll(logContext);
    } else {
      ThreadContext.clearAll();
    }
    try {
      return taskCall();
    } finally {
      ThreadContext.clearAll();
    }
  }
}
