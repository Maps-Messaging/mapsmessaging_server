/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.messaging.api;

import org.jetbrains.annotations.NotNull;
import org.maps.messaging.api.features.ClientAcknowledgement;
import org.maps.messaging.api.features.CreditHandler;
import org.maps.messaging.api.features.QualityOfService;
import org.maps.messaging.api.features.RetainHandler;
import org.maps.messaging.engine.destination.subscription.SubscriptionContext;

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
  private long subscriptionId;
  private int receiveMaximum;

  public SubscriptionContextBuilder(@NotNull String name, @NotNull ClientAcknowledgement acknowledgementController) {
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
    subscriptionId = 0;
    receiveMaximum = 1;
  }

  public SubscriptionContextBuilder setRetainHandler(@NotNull RetainHandler retainHandler) {
    this.retainHandler = retainHandler;
    return this;
  }

  public SubscriptionContextBuilder setSharedName(@NotNull String sharedName) {
    this.sharedName = sharedName;
    return this;
  }

  public SubscriptionContextBuilder setSelector(@NotNull String selector) {
    this.selector = selector;
    return this;
  }

  public SubscriptionContextBuilder setQos(@NotNull QualityOfService qos) {
    this.qos = qos;
    return this;
  }

  public SubscriptionContextBuilder setAlias(@NotNull String alias) {
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
    context.setBrowserFlag(isBrowser);
    return context;
  }
}
