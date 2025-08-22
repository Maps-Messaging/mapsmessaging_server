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

package io.mapsmessaging.utilities.filtering;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import lombok.Data;

import java.io.IOException;

@Data
public class NamespaceFilter {
  private String namespace;
  private int depth;
  private String selector;
  private boolean forcePriority;
  private ParserExecutor executor;


  public NamespaceFilter(ConfigurationProperties props) throws IOException {
    namespace = props.getProperty("namespace");
    selector = props.getProperty("filter", "");
    depth = props.getIntProperty("depth", 1);
    forcePriority = props.getBooleanProperty("forcePriority", false);
    if(selector != null && !selector.isEmpty()){
      try {
        executor = SelectorParser.compile(selector);
      } catch (ParseException e) {
        throw new IOException(e);
      }
    }
  }

  public ConfigurationProperties toConfigurationProperties(){
    ConfigurationProperties props = new ConfigurationProperties();
    props.put("namespace", namespace);
    props.put("filter", selector);
    props.put("depth", depth);
    props.put("forcePriority", forcePriority);
    return props;
  }
}
