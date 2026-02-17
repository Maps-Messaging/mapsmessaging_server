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

package io.mapsmessaging.engine.transformers;

import io.mapsmessaging.api.transformers.InterServerTransformation;
import io.mapsmessaging.api.transformers.ProtocolTransformationWrapper;
import io.mapsmessaging.config.transformer.TransformationConfigFactory;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.transformer.TransformationConfigDTO;
import io.mapsmessaging.network.protocol.transformation.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import io.mapsmessaging.utilities.service.Service;
import io.mapsmessaging.utilities.service.ServiceManager;

import java.util.*;

@SuppressWarnings("java:S6548") // yes it is a singleton
public class TransformerManager implements ServiceManager {

  private static class Holder {
    static final TransformerManager INSTANCE = new TransformerManager();
  }
  public static TransformerManager getInstance() {
    return Holder.INSTANCE;
  }

  private final Map<String, Service> transformerMap;

  public InterServerTransformation get(ConfigurationProperties props) {
    if(props == null || !props.containsKey("name")) return null;
    String transformer = props.getProperty("name").toLowerCase();
    TransformationConfigDTO dto = TransformationConfigFactory.loadSingle(props);
    if(dto != null){
      return  get(dto);
    }
    else{
      ProtocolMessageTransformation protocolMessageTransformation = TransformationManager.getInstance().getTransformation(transformer);
      if(protocolMessageTransformation != null){
        return new ProtocolTransformationWrapper(protocolMessageTransformation);
      }
    }
    return null;
  }

  public InterServerTransformation get(TransformationConfigDTO dto) {
    if(dto != null){
      InterServerTransformation t = (InterServerTransformation) transformerMap.get(dto.getType().getWireName());
      return  t.build(dto);
    }
    return null;
  }

  public List<InterServerTransformation> buildList(List<Map<String, Object>> list) {
    List<InterServerTransformation> interServerTransformation = new ArrayList<>();
    if (list != null && !list.isEmpty()) {
      for(Map<String, Object> obj: list) {
        interServerTransformation.add(TransformerManager.getInstance().get(new ConfigurationProperties(obj)));
      }
    }
    return interServerTransformation;
  }

  private TransformerManager() {
    ServiceLoader<InterServerTransformation> transformerServiceLoader = ServiceLoader.load(InterServerTransformation.class);
    transformerMap = new LinkedHashMap<>();
    for (InterServerTransformation transformer : transformerServiceLoader) {
      transformerMap.put(transformer.getName().toLowerCase(), transformer);
    }
  }

  @Override
  public Iterator<Service> getServices() {
    return transformerMap.values().iterator();
  }
}
