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

package io.mapsmessaging.engine.transformers;

import io.mapsmessaging.api.transformers.Transformer;
import io.mapsmessaging.utilities.service.Service;
import io.mapsmessaging.utilities.service.ServiceManager;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;

public class TransformerManager implements ServiceManager {

  public static TransformerManager getInstance(){
    return instance;
  }
  private static final TransformerManager instance;
  static {
    instance =new TransformerManager();
  }

  private final Map<String, Service> transformerMap;

  public Transformer get(String transformer){
    return (Transformer) transformerMap.get(transformer);
  }

  private TransformerManager(){
    ServiceLoader<Transformer> transformerServiceLoader = ServiceLoader.load(Transformer.class);
    transformerMap = new LinkedHashMap<>();
    for(Transformer transformer:transformerServiceLoader){
      transformerMap.put(transformer.getName(), transformer);
    }
  }

  @Override
  public Iterator<Service> getServices() {
    return  transformerMap.values().iterator();
  }
}
