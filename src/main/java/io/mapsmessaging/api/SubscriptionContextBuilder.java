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

package io.mapsmessaging.api;

import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.CreditHandler;
import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.features.RetainHandler;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
public class SubscriptionContextBuilder {

  @Getter private final String name;
  @Getter private final ClientAcknowledgement acknowledgementController;
  @Getter private RetainHandler retainHandler;
  @Getter private CreditHandler creditHandler;
  @Getter private boolean noLocalMessages;
  @Getter private String sharedName;
  @Getter private String alias;
  @Getter private String selector;
  @Getter private QualityOfService qos;
  @Getter private boolean retainAsPublish;
  @Getter private boolean allowOverlap;
  @Getter private boolean isBrowser;
  @Getter private long subscriptionId;
  @Getter private int receiveMaximum;
  @Getter private DestinationMode mode;

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
    subscriptionId = 0;
    receiveMaximum = 1;
    mode = DestinationMode.NORMAL;
  }

  public SubscriptionContextBuilder setRetainHandler(@NonNull @NotNull RetainHandler retainHandler) {
    this.retainHandler = retainHandler;
    return this;
  }

  public SubscriptionContextBuilder setSharedName(@NonNull @NotNull String sharedName) {
    this.sharedName = sharedName;
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

  public SubscriptionContext build() {
    SubscriptionContext context = new SubscriptionContext(name);
    context.setAcknowledgementController(acknowledgementController)
        .setAllowOverlap(allowOverlap)
        .setNoLocalMessages(noLocalMessages)
        .setQualityOfService(qos)
        .setRetainAsPublish(retainAsPublish)
        .setSelector(selector)
        .setSharedName(sharedName)
        .setRetainHandler(retainHandler)
        .setSubscriptionId(subscriptionId)
        .setReceiveMaximum(receiveMaximum)
        .setAlias(alias)
        .setCreditHandler(creditHandler)
        .setBrowserFlag(isBrowser);
    return context;
  }
}
