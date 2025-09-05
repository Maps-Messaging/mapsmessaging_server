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

package io.mapsmessaging.engine.system.impl.clients;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.engine.system.SystemTopic;
import io.mapsmessaging.network.io.EndPoint;

import java.io.IOException;

public class Maximum extends SystemTopic {

  private long max;

  public Maximum() throws IOException {
    super("$SYS/broker/clients/maximum");
    max = 0;
  }

  @Override
  public String getSchemaUUID() {
    return SchemaManager.DEFAULT_NUMERIC_STRING_SCHEMA;
  }


  @Override
  protected Message generateMessage() {
    return getMessage(("" + max).getBytes());
  }

  @Override
  public boolean hasUpdates() {
    long current = EndPoint.totalConnections.sum() - EndPoint.totalDisconnections.sum();
    if (current > max) {
      max = current;
      return true;
    }
    return false;
  }

  @Override
  public String[] aliases() {
    return new String[]{"$SYS/clients/maximum"};
  }
}
