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

package io.mapsmessaging.api.message;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.schemas.config.SchemaResource;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.selector.IdentifierResolver;
import io.mapsmessaging.selector.operators.ParserExecutor;

import java.util.List;

@SuppressWarnings("java:S6548") // yes it is a singleton
public class Filter {

  private static class Holder {
    static final Filter INSTANCE = new Filter();
  }

  public static Filter getInstance() {
    return Holder.INSTANCE;
  }

  public boolean filterMessage(ParserExecutor selector, Message message, DestinationImpl destination) {
    if (selector == null) {
      return true;
    }
    if (message != null) {
      String lookup = getSchemaId(message, destination);
      Resolver resolver = new Resolver(getResolver(lookup, message), message);
      return selector.evaluate(resolver);
    }
    return false;
  }

  private String getSchemaId(Message message, DestinationImpl destination) {
    String lookup = message.getSchemaId();
    if (lookup == null) {
      lookup = destination.getSchema().getUniqueId();
    }
    return lookup;
  }

  public static IdentifierResolver getTopicResolver(String topicName, Message message) {
    List<SchemaResource> list = SchemaManager.getInstance().getSchemaByContext(topicName);
    return getResolver(list.stream().findFirst().get().getDefaultVersion().getUniqueId(), message);
  }

  public static IdentifierResolver getResolver(String lookup, Message message) {
    MessageFormatter formatter = SchemaManager.getInstance().getMessageFormatter(lookup);
    if (formatter != null) {
      return formatter.parse(message.getOpaqueData());
    }
    return null;
  }

  private Filter() {
  }

  private static final class Resolver implements IdentifierResolver {

    private final IdentifierResolver formatResolver;
    private final Message message;

    public Resolver(IdentifierResolver formatResolver, Message message) {
      this.formatResolver = formatResolver;
      this.message = message;
    }

    @Override
    public Object get(String s) {
      Object val = message.get(s);
      if(val instanceof TypedData){
        val = ((TypedData)val).getData();
      }
      if (val == null && formatResolver != null) {
        val = formatResolver.get(s);
      }
      return val;
    }

    @Override
    public byte[] getOpaqueData() {
      return IdentifierResolver.super.getOpaqueData();
    }
  }
}
