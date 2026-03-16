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
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorConfigDTO;
import io.mapsmessaging.dto.rest.config.aggregator.AggregatorInputConfigDTO;
import io.mapsmessaging.dto.rest.config.aggregator.WindowCloseMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AggregatorConfig extends AggregatorConfigDTO implements Config {

  public AggregatorConfig(ConfigurationProperties props) {
    this.name = props.getProperty("name", "");
    this.enabled = props.getBooleanProperty("enabled", true);
    this.outputTopic = props.getProperty("outputTopic", "");

    this.windowCloseMode = loadWindowCloseMode(props);

    this.windowDurationMs = props.getLongProperty("windowDurationMs", 0L);
    this.timeoutMs = props.getLongProperty("timeoutMs", 0L);

    this.maxEventsPerTopic = props.getIntProperty("maxEventsPerTopic", 1);

    this.inputs = unpackInputs(props);
    this.outputTransformers = TransformationConfigFactory.loadChain(props.get("outputTransformers"));
  }

  private WindowCloseMode loadWindowCloseMode(ConfigurationProperties props) {
    String value = props.getProperty("windowCloseMode", WindowCloseMode.ALL_INPUTS_OR_TIMEOUT.name());
    if (value == null || value.isBlank()) {
      return WindowCloseMode.ALL_INPUTS_OR_TIMEOUT;
    }
    return WindowCloseMode.valueOf(value.trim().toUpperCase());
  }

  @SuppressWarnings("unchecked")
  private List<AggregatorInputConfigDTO> unpackInputs(ConfigurationProperties props) {
    List<AggregatorInputConfigDTO> inputList = new ArrayList<>();

    Object inputsObject = props.get("inputs");
    if (inputsObject instanceof List) {
      for (Object entry : (List<?>) inputsObject) {
        if (entry instanceof ConfigurationProperties inputProperties) {
          inputList.add(new AggregatorInputConfig(inputProperties));
        } else if (entry instanceof Map) {
          inputList.add(new AggregatorInputConfig(new ConfigurationProperties((Map<String, Object>) entry)));
        }
      }
    } else if (inputsObject instanceof ConfigurationProperties inputProperties) {
      inputList.add(new AggregatorInputConfig(inputProperties));
    } else if (inputsObject instanceof Map) {
      inputList.add(new AggregatorInputConfig(new ConfigurationProperties((Map<String, Object>) inputsObject)));
    }

    return inputList;
  }

  @Override
  public ConfigurationProperties toConfigurationProperties() {
    ConfigurationProperties configurationProperties = new ConfigurationProperties();

    configurationProperties.put("name", name);
    configurationProperties.put("enabled", enabled);
    configurationProperties.put("outputTopic", outputTopic);
    configurationProperties.put("windowCloseMode", windowCloseMode.name());

    configurationProperties.put("windowDurationMs", windowDurationMs);
    configurationProperties.put("timeoutMs", timeoutMs);

    configurationProperties.put("maxEventsPerTopic", maxEventsPerTopic);

    List<ConfigurationProperties> inputsList = new ArrayList<>();
    if (inputs != null) {
      for (AggregatorInputConfigDTO inputConfigDTO : inputs) {
        inputsList.add(((AggregatorInputConfig) inputConfigDTO).toConfigurationProperties());
      }
    }
    configurationProperties.put("inputs", inputsList);

    if (outputTransformers != null) {
      configurationProperties.put("outputTransformers", outputTransformers);
    }

    return configurationProperties;
  }

  @Override
  public boolean update(BaseConfigDTO update) {
    boolean hasChanged = false;

    if (update instanceof AggregatorConfigDTO config) {
      if (!safeEquals(this.name, config.getName())) {
        this.name = config.getName();
        hasChanged = true;
      }

      if (this.enabled != config.isEnabled()) {
        this.enabled = config.isEnabled();
        hasChanged = true;
      }

      if (!safeEquals(this.outputTopic, config.getOutputTopic())) {
        this.outputTopic = config.getOutputTopic();
        hasChanged = true;
      }

      if (!Objects.equals(this.windowCloseMode, config.getWindowCloseMode())) {
        this.windowCloseMode = config.getWindowCloseMode();
        hasChanged = true;
      }

      if (this.windowDurationMs != config.getWindowDurationMs()) {
        this.windowDurationMs = config.getWindowDurationMs();
        hasChanged = true;
      }

      if (this.timeoutMs != config.getTimeoutMs()) {
        this.timeoutMs = config.getTimeoutMs();
        hasChanged = true;
      }

      if (this.maxEventsPerTopic != config.getMaxEventsPerTopic()) {
        this.maxEventsPerTopic = config.getMaxEventsPerTopic();
        hasChanged = true;
      }

      if (!Objects.equals(this.outputTransformers, config.getOutputTransformers())) {
        this.outputTransformers = config.getOutputTransformers();
        hasChanged = true;
      }

      if (this.inputs == null && config.getInputs() != null) {
        this.inputs = config.getInputs();
        hasChanged = true;
      } else if (this.inputs != null && config.getInputs() == null) {
        this.inputs = null;
        hasChanged = true;
      } else if (this.inputs != null) {
        if (this.inputs.size() != config.getInputs().size()) {
          this.inputs = config.getInputs();
          hasChanged = true;
        } else {
          for (int index = 0; index < this.inputs.size(); index++) {
            if (!Objects.equals(this.inputs.get(index), config.getInputs().get(index))) {
              this.inputs.set(index, config.getInputs().get(index));
              hasChanged = true;
            }
          }
        }
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
}
