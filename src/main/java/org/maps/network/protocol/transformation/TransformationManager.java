/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.network.protocol.transformation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import org.maps.network.protocol.ProtocolMessageTransformation;
import org.maps.utilities.service.Service;
import org.maps.utilities.service.ServiceManager;

public class TransformationManager implements ServiceManager {

  private static final TransformationManager instance = new TransformationManager();
  public static TransformationManager getInstance(){
    return instance;
  }

  private final ServiceLoader<ProtocolMessageTransformation> transformations;

  private TransformationManager(){
    transformations = ServiceLoader.load(ProtocolMessageTransformation.class);
  }

  public ProtocolMessageTransformation getTransformation(String protocol, String user){
    for(ProtocolMessageTransformation transformation:transformations){
      if(transformation.getName().equalsIgnoreCase(user)){
        return transformation;
      }
    }
    return null;
  }


  @Override
  public Iterator<Service> getServices() {
    List<Service> service = new ArrayList<>();
    for(ProtocolMessageTransformation transformation:transformations){
      service.add(transformation);
    }
    return service.listIterator();
  }
}
