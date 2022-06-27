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
package io.mapsmessaging.engine.schema;

import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.selector.IdentifierResolver;
import java.io.IOException;

public class Format  {

  private final MessageFormatter formatter;

  public Format(MessageFormatter formatter){
    this.formatter = formatter;
  }

  public IdentifierResolver getResolver(byte[] payload) throws IOException{
    return formatter.parse(payload);
  }

}
