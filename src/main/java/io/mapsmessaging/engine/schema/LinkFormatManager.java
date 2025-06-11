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

package io.mapsmessaging.engine.schema;

import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.operators.ParserExecutor;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class LinkFormatManager {

  private static final LinkFormatManager instance;

  public static LinkFormatManager getInstance() {
    return instance;
  }

  static {
    instance = new LinkFormatManager();
  }

  public String buildLinkFormatString(String filter, List<LinkFormat> linkFormatList) {
    ParserExecutor parser = build(filter);
    StringBuilder sb = new StringBuilder();
    AtomicBoolean first = new AtomicBoolean(true);
    linkFormatList.stream().filter(linkFormat -> select(linkFormat, parser)).forEach(linkFormat -> {
      if (!first.get()) {
        sb.append(",");
      }
      first.set(false);
      sb.append(linkFormat.pack());
    });
    return sb.toString();
  }

  private boolean select(LinkFormat linkFormat, ParserExecutor parser) {
    if (parser != null && !parser.evaluate(linkFormat)) {
      return false;
    }
    return !linkFormat.getPath().toLowerCase().startsWith("$sys") &&
        !linkFormat.getPath().equalsIgnoreCase(".well-known/core") &&
        linkFormat.getInterfaceDescription() != null;
  }

  private ParserExecutor build(String filter) {
    if (filter != null && !filter.isEmpty()) {
      try {
        return io.mapsmessaging.selector.SelectorParser.compile(filter);
      } catch (ParseException e) {
        // Ignore this
      }
    }
    return null;
  }

}
