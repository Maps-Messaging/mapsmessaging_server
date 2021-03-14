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

import org.maps.messaging.engine.selector.ParseException;
import org.maps.messaging.engine.selector.operators.IdentifierResolver;
import org.maps.utilities.service.Service;

import java.util.List;

public interface SelectorParser extends Service {

  SelectorParser createInstance(List<String> arguments) throws ParseException;

  Object parse(IdentifierResolver resolver);

}
