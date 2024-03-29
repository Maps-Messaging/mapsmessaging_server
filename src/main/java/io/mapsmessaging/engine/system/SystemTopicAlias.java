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

package io.mapsmessaging.engine.system;

import io.mapsmessaging.api.message.Message;

import java.io.IOException;

public class SystemTopicAlias extends SystemTopic {

  private final SystemTopic actual;

  public SystemTopicAlias(String name, SystemTopic topic) throws IOException {
    super(name);
    actual = topic;
  }

  @Override
  public String getSchemaUUID() {
    return actual.getSchemaUUID();
  }


  @Override
  protected Message generateMessage() {
    return actual.generateMessage();
  }

  @Override
  public boolean hasUpdates() {
    return false;
  }
}
