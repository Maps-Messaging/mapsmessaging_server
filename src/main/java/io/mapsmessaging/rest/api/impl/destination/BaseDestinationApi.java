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

package io.mapsmessaging.rest.api.impl.destination;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.rest.api.impl.BaseRestApi;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BaseDestinationApi extends BaseRestApi {

  protected static final String RESOURCE = "destinations";

  protected DestinationImpl lookup(String name)
      throws ExecutionException, InterruptedException, TimeoutException {
    if (name.startsWith("$")) {
      return null;
    }
    DestinationImpl destinationImpl = MessageDaemon.getInstance().getDestinationManager().find(name).get(60, TimeUnit.SECONDS);
    if (destinationImpl == null && !name.startsWith("/")) {
      name = "/" + name;
      destinationImpl = MessageDaemon.getInstance().getDestinationManager().find(name).get(60, TimeUnit.SECONDS);
    }
    return destinationImpl;
  }
}
