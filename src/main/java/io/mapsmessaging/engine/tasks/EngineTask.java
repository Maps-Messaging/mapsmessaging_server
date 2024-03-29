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
