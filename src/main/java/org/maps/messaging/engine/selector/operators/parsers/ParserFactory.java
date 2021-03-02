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

package org.maps.messaging.engine.selector.operators.parsers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import org.maps.messaging.engine.selector.Identifier;
import org.maps.messaging.engine.selector.ParseException;
import org.maps.messaging.engine.selector.operators.FunctionOperator;
import org.maps.utilities.service.Service;
import org.maps.utilities.service.ServiceManager;

public class ParserFactory implements ServiceManager {

  private static final ParserFactory instance = new ParserFactory();

  public static ParserFactory getInstance(){
    return instance;
  }

  private final ServiceLoader<SelectorParser> knownParsers;

  private ParserFactory(){
    knownParsers = ServiceLoader.load(SelectorParser.class);
  }

  public FunctionOperator loadParser(Object parserName, List<String> arguments) throws ParseException{
    for(SelectorParser parser:knownParsers){
      if(parserName instanceof String) {
        if (parser.getName().equalsIgnoreCase(parserName.toString())) {
          return new ParserProxy(parser.createInstance(arguments));
        }
      }
      else if(parserName instanceof Identifier){
        // We need to lazy load on each message here
        return new IdentityLoadParser((Identifier) parserName, arguments);
      }
    }
    return null;
  }

  @Override
  public Iterator<Service> getServices() {
    List<Service> service = new ArrayList<>();
    for(SelectorParser parser:knownParsers){
      service.add(parser);
    }
    return service.listIterator();
  }
}
