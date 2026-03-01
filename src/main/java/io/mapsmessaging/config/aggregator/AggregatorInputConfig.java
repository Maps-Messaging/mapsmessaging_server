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

package io.mapsmessaging.config.aggregator;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.config.transformer.TransformationConfigFactory;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorInputConfigDTO;
import io.mapsmessaging.engine.transformers.TransformerManager;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AggregatorInputConfig extends AggregatorInputConfigDTO implements Config {

  public AggregatorInputConfig(ConfigurationProperties props) {
    this.topicName = props.getProperty("topicName", "");
    this.selector = props.getProperty("selector", "");

    Object transformerObject = props.get("outputTransformers");
    if (transformerObject != null) {
      this.transformer = TransformationConfigFactory.loadChain(transformerObject);
    }
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties configurationProperties = new ConfigurationProperties();

    configurationProperties.put("topicName", topicName);

    if (selector != null && !selector.isEmpty()) {
      configurationProperties.put("selector", selector);
    }

    if (transformer != null) {
      configurationProperties.put("transformer", transformer);
    }

    return configurationProperties;
  }

  @Override
  public boolean update(BaseConfigDTO update) {
    boolean hasChanged = false;

    if (update instanceof AggregatorInputConfigDTO config) {
      if (!safeEquals(this.topicName, config.getTopicName())) {
        this.topicName = config.getTopicName();
        hasChanged = true;
      }

      if (!safeEquals(this.selector, config.getSelector())) {
        this.selector = config.getSelector();
        hasChanged = true;
      }

    }

    return hasChanged;
  }

  private boolean safeEquals(String left, String right) {
    if (left == null) {
      return right == null;
    }
    return left.equals(right);
  }

  private boolean safeEqualsListMap(List<Map<String, Object>> left, List<Map<String, Object>> right) {
    if (left == null) {
      return right == null;
    }
    return left.equals(right);
  }
}
