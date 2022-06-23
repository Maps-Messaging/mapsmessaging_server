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

package io.mapsmessaging.engine.destination.subscription;

import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.builders.BrowserSubscriptionBuilder;
import io.mapsmessaging.engine.destination.subscription.builders.QueueSubscriptionBuilder;
import io.mapsmessaging.engine.destination.subscription.builders.SchemaSubscriptionBuilder;
import io.mapsmessaging.engine.destination.subscription.builders.SharedSubscriptionBuilder;
import io.mapsmessaging.engine.destination.subscription.builders.StandardSubscriptionBuilder;
import io.mapsmessaging.engine.destination.subscription.impl.DestinationSubscription;
import java.io.IOException;

public class SubscriptionFactory {

  private static final SubscriptionFactory instance = new SubscriptionFactory();

  public static SubscriptionFactory getInstance(){
    return instance;
  }

  public SubscriptionBuilder getBuilder(DestinationImpl destination, SubscriptionContext context, boolean isPersist) throws IOException {
    if(context.getDestinationMode().equals(DestinationMode.SCHEMA)){
      return new SchemaSubscriptionBuilder(destination, context);
    }
    if (context.isSharedSubscription()) {
      return new SharedSubscriptionBuilder(destination, context);
    }
    if(destination.getResourceType().isQueue()){ // We have Temporary Queue or a Queue
      return new QueueSubscriptionBuilder(destination, context);
    }
    return new StandardSubscriptionBuilder(destination, context, isPersist);
  }

  public SubscriptionBuilder getBrowserBuilder(DestinationImpl destination, SubscriptionContext context, DestinationSubscription parent) throws IOException {
    return new BrowserSubscriptionBuilder(destination, context, parent);
  }

  private SubscriptionFactory(){}
}
