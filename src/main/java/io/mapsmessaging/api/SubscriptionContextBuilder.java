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

package io.mapsmessaging.api;

import io.mapsmessaging.api.features.*;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@Getter
public class SubscriptionContextBuilder {

  private final String name;
  private final ClientAcknowledgement acknowledgementController;
  private RetainHandler retainHandler;
  private CreditHandler creditHandler;
  private boolean noLocalMessages;
  private String sharedName;
  private String alias;
  private String selector;
  private QualityOfService qos;
  private boolean retainAsPublish;
  private boolean allowOverlap;
  private boolean isBrowser;
  private boolean sync;
  private long subscriptionId;
  private int receiveMaximum;
  private DestinationMode mode;


  public SubscriptionContextBuilder(@NonNull @NotNull String name, @NonNull @NotNull ClientAcknowledgement acknowledgementController) {
    this.name = name;
    this.acknowledgementController = acknowledgementController;
    sharedName = null;
    selector = null;
    alias = null;
    noLocalMessages = false;
    retainHandler = RetainHandler.SEND_ALWAYS;
    qos = QualityOfService.AT_MOST_ONCE;
    creditHandler = CreditHandler.AUTO;
    allowOverlap = false;
    retainAsPublish = false;
    isBrowser = false;
    sync = false;
    subscriptionId = 0;
    receiveMaximum = 1;
    mode = DestinationMode.NORMAL;
  }

  public SubscriptionContextBuilder setRetainHandler(@NonNull @NotNull RetainHandler retainHandler) {
    this.retainHandler = retainHandler;
    return this;
  }

  public SubscriptionContextBuilder setSharedName(@NonNull @NotNull String sharedName) {
    this.sharedName = (sharedName.isEmpty()) ? null : sharedName;
    return this;
  }

  public SubscriptionContextBuilder setMode(@NonNull @NotNull DestinationMode mode) {
    this.mode = mode;
    return this;
  }

  public SubscriptionContextBuilder setSelector(@NonNull @NotNull String selector) {
    this.selector = selector;
    return this;
  }

  public SubscriptionContextBuilder setQos(@NonNull @NotNull QualityOfService qos) {
    this.qos = qos;
    return this;
  }

  public SubscriptionContextBuilder setAlias(@NonNull @NotNull String alias) {
    this.alias = alias;
    return this;
  }

  public SubscriptionContextBuilder setNoLocalMessages(boolean noLocalMessages) {
    this.noLocalMessages = noLocalMessages;
    return this;
  }

  public SubscriptionContextBuilder setBrowserFlag(boolean isBrowser) {
    this.isBrowser = isBrowser;
    return this;
  }

  public SubscriptionContextBuilder setRetainAsPublish(boolean retainAsPublish) {
    this.retainAsPublish = retainAsPublish;
    return this;
  }

  public SubscriptionContextBuilder setAllowOverlap(boolean allowOverlap) {
    this.allowOverlap = allowOverlap;
    return this;
  }

  public SubscriptionContextBuilder setSubscriptionId(long subscriptionId) {
    this.subscriptionId = subscriptionId;
    return this;
  }

  public SubscriptionContextBuilder setReceiveMaximum(int receiveMaximum) {
    this.receiveMaximum = receiveMaximum;
    return this;
  }

  public SubscriptionContextBuilder setCreditHandler(CreditHandler creditHandler) {
    this.creditHandler = creditHandler;
    return this;
  }

  public SubscriptionContextBuilder setSync(boolean sync) {
    this.sync = sync;
    return this;
  }

  public SubscriptionContext build() {
    SubscriptionContext context = new SubscriptionContext(name);
    context.setAcknowledgementController(acknowledgementController);
    context.setAllowOverlap(allowOverlap);
    context.setNoLocalMessages(noLocalMessages);
    context.setQualityOfService(qos);
    context.setRetainAsPublish(retainAsPublish);
    context.setSelector(selector);
    context.setSharedName(sharedName);
    context.setRetainHandler(retainHandler);
    context.setSubscriptionId(subscriptionId);
    context.setReceiveMaximum(receiveMaximum);
    context.setAlias(alias);
    context.setCreditHandler(creditHandler);
    context.setSync(sync);
    context.setBrowserFlag(isBrowser);
    return context;
  }
}
