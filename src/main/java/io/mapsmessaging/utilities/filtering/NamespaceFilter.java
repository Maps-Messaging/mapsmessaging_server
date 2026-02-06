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

package io.mapsmessaging.utilities.filtering;

import io.mapsmessaging.dto.rest.config.protocol.NamespaceFilterDTO;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import lombok.Getter;

import java.io.IOException;


@Getter
public class NamespaceFilter {

  private final NamespaceFilterDTO config;
  private ParserExecutor executor;


  public NamespaceFilter(NamespaceFilterDTO props) throws IOException {
    config = props;

    if(config.getSelector() != null && !config.getSelector().isEmpty()){
      try {
        executor = SelectorParser.compile(config.getSelector());
      } catch (ParseException e) {
        throw new IOException(e);
      }
    }
  }


}
