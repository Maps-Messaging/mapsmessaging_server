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

import io.mapsmessaging.utilities.service.Service;
import io.mapsmessaging.utilities.service.ServiceManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public class FormatManager implements ServiceManager {
  private static final FormatManager instance = new FormatManager();

  public static FormatManager getInstance(){
    return instance;
  }

  private final ServiceLoader<Format> formatServiceLoader;

  public Format getFormat(String name){
    for(Format format:formatServiceLoader){
      if(format.getName().equalsIgnoreCase(name)){
        return format;
      }
    }
    return new RawFormat();
  }

  @Override
  public Iterator<Service> getServices() {
    List<Service> service = new ArrayList<>();
    for(Format parser:formatServiceLoader){
      service.add(parser);
    }
    return service.listIterator();
  }

  private FormatManager(){
    formatServiceLoader = ServiceLoader.load(Format.class);
  }

}
