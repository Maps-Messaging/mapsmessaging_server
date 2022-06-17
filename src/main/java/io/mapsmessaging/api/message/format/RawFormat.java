/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */
package io.mapsmessaging.api.message.format;

import io.mapsmessaging.selector.IdentifierResolver;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import java.io.IOException;

public class RawFormat implements Format{

  @Override
  public String getName() {
    return "RAW";
  }

  @Override
  public String getDescription() {
    return "Processes byte[] payloads";
  }

  @Override
  public boolean isValid(byte[] payload) {
    return true;
  }

  @Override
  public IdentifierResolver getResolver(byte[] payload) throws IOException {
    return s -> null;
  }

  @Override
  public Format getInstance(ConfigurationProperties properties) {
    return this;
  }
}
