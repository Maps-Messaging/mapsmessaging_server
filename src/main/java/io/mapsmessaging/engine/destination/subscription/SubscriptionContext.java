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

import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.CreditHandler;
import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.features.RetainHandler;
import io.mapsmessaging.engine.serializer.MapSerializable;
import io.mapsmessaging.storage.impl.streams.ObjectReader;
import io.mapsmessaging.storage.impl.streams.ObjectWriter;
import java.io.File;
import java.io.IOException;
import java.util.BitSet;
import java.util.Objects;
import lombok.Getter;
import lombok.ToString;

@ToString
public class SubscriptionContext implements Comparable<SubscriptionContext>, MapSerializable {

  private static final int NO_LOCAL_MESSAGES = 0;
  private static final int RETAIN_AS_PUBLISH = 1;
  private static final int ALLOW_OVERLAP = 2;
  private static final int BROWSER_FLAG = 3;

  @Getter
  private String destinationName;
  @Getter
  private BitSet flags;
  @Getter
  private String rootPath;
  @Getter
  private ClientAcknowledgement acknowledgementController;
  @Getter
  private String sharedName;
  @Getter
  private String selector;
  @Getter
  private String alias;
  @Getter
  private long subscriptionId;
  @Getter
  private int receiveMaximum;
  @Getter
  private RetainHandler retainHandler;
  @Getter
  private QualityOfService qualityOfService;
  @Getter
  private CreditHandler creditHandler;
  @Getter
  private DestinationMode destinationMode;

  //
  // Server Only flag
  //
  @Getter
  private boolean replaced;

  public SubscriptionContext() {
  }

  public SubscriptionContext(String destinationName) {
    this.destinationName = destinationName;
    alias = destinationName; // Make the Alias the same as the destination. In some protocols this can be overridden
    flags = new BitSet(8);
    receiveMaximum = 1;
    rootPath = "";
    creditHandler = CreditHandler.AUTO;
    retainHandler = RetainHandler.SEND_ALWAYS;
    qualityOfService = QualityOfService.AT_MOST_ONCE;
    acknowledgementController = ClientAcknowledgement.AUTO;
    parseName();
  }

  public SubscriptionContext(SubscriptionContext rhs, String destinationName, String alias) {
    this.destinationName = destinationName;
    this.alias = alias;
    acknowledgementController = rhs.acknowledgementController;
    sharedName = rhs.sharedName;
    selector = rhs.selector;
    subscriptionId = rhs.subscriptionId;
    receiveMaximum = rhs.receiveMaximum;
    retainHandler = rhs.retainHandler;
    qualityOfService = rhs.qualityOfService;
    flags = BitSet.valueOf(rhs.flags.toByteArray());
    parseName();
  }

  public SubscriptionContext(ObjectReader reader) throws IOException {
    read(reader);
  }

  public void read(ObjectReader reader) throws IOException {
    retainHandler = RetainHandler.getInstance(reader.readByte());
    creditHandler = CreditHandler.getInstance(reader.readByte());
    qualityOfService = QualityOfService.getInstance(reader.readByte());
    acknowledgementController = ClientAcknowledgement.getInstance(reader.readByte());

    subscriptionId = reader.readLong();

    destinationName = reader.readString();
    sharedName = reader.readString();
    selector = reader.readString();
    alias = reader.readString();
    flags = BitSet.valueOf(reader.readByteArray());
    rootPath = reader.readString();

    if (alias == null) {
      alias = destinationName;
    }
    parseName();
  }

  public void write(ObjectWriter writer) throws IOException {
    writer.write((byte) retainHandler.getHandler());
    writer.write((byte) creditHandler.getValue());
    writer.write((byte) qualityOfService.getLevel());
    writer.write((byte) acknowledgementController.getValue());
    writer.write(subscriptionId);

    writer.write(destinationMode.getNamespace() + destinationName);
    writer.write(sharedName);
    writer.write(selector);
    writer.write(alias);
    writer.write(flags.toByteArray());
    writer.write(rootPath);
  }

  public SubscriptionContext setRootPath(String rootPath) {
    this.rootPath = Objects.requireNonNullElse(rootPath, "");
    if (rootPath.length() > 1 && !rootPath.endsWith("/")) {
      this.rootPath = this.rootPath + File.separator;
    }
    return this;
  }

  public SubscriptionContext setDestinationName(String destinationName) {
    if (alias.equals(destinationName)) {
      alias = destinationName;
    }
    this.destinationName = destinationName;
    return this;
  }

  public SubscriptionContext setQualityOfService(QualityOfService qos) {
    this.qualityOfService = qos;
    return this;
  }

  public SubscriptionContext setRetainAsPublish(boolean retainAsPublish) {
    flags.set(RETAIN_AS_PUBLISH, retainAsPublish);
    return this;
  }

  public SubscriptionContext setRetainHandler(RetainHandler retainHandler) {
    this.retainHandler = retainHandler;
    return this;
  }

  public SubscriptionContext setAcknowledgementController(ClientAcknowledgement clientAcknowledgement) {
    this.acknowledgementController = clientAcknowledgement;
    return this;
  }

  public SubscriptionContext setSelector(String selector) {
    this.selector = selector;
    return this;
  }

  public SubscriptionContext setSubscriptionId(long subscriptionId) {
    this.subscriptionId = subscriptionId;
    return this;
  }

  public SubscriptionContext setReceiveMaximum(int receiveMaximum) {
    this.receiveMaximum = receiveMaximum;
    return this;
  }

  public SubscriptionContext setAlias(String alias) {
    this.alias = Objects.requireNonNullElseGet(alias, this::getCorrectedPath);
    return this;
  }

  public SubscriptionContext setReplaced(boolean flag) {
    replaced = flag;
    return this;
  }

  public SubscriptionContext setNoLocalMessages(boolean noLocalMessages) {
    flags.set(NO_LOCAL_MESSAGES, noLocalMessages);
    return this;
  }

  public SubscriptionContext setSharedName(String sharedName) {
    this.sharedName = sharedName;
    return this;
  }

  public SubscriptionContext setAllowOverlap(boolean allowOverlap) {
    flags.set(ALLOW_OVERLAP, allowOverlap);
    return this;
  }

  public SubscriptionContext setBrowserFlag(boolean isBrowser) {
    flags.set(BROWSER_FLAG, isBrowser);
    return this;
  }

  public SubscriptionContext setCreditHandler(CreditHandler creditHandler) {
    this.creditHandler = creditHandler;
    return this;
  }

  public boolean isSharedSubscription() {
    return (sharedName != null && sharedName.length() > 0);
  }

  public boolean containsWildcard() {
    return destinationName.contains("#") || destinationName.contains("+");
  }

  public String getFilter() {
    return getCorrectedPath();
  }

  public boolean isRetainAsPublish() {
    return flags.get(RETAIN_AS_PUBLISH);
  }

  public boolean noLocalMessages() {
    return flags.get(NO_LOCAL_MESSAGES);
  }

  public boolean allowOverlap() {
    return flags.get(ALLOW_OVERLAP);
  }

  public boolean isBrowser() {
    return flags.get(BROWSER_FLAG);
  }


  private String getCorrectedPath() {
    String lookup = rootPath + destinationName;
    return lookup.replace("//", "/");
  }

  @Override
  public int compareTo(SubscriptionContext lhs) {
    return lhs.qualityOfService.getLevel() - qualityOfService.getLevel();
  }

  @Override
  public boolean equals(Object lhs) {
    if (lhs instanceof SubscriptionContext) {
      return ((SubscriptionContext) lhs).qualityOfService == qualityOfService;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  private void parseName() {
    if (destinationName.startsWith(DestinationMode.SCHEMA.getNamespace())) {
      destinationMode = DestinationMode.SCHEMA;
      destinationName = destinationName.substring(DestinationMode.SCHEMA.getNamespace().length());
    } else if (destinationName.startsWith(DestinationMode.METRICS.getNamespace())) {
      destinationMode = DestinationMode.METRICS;
      destinationName = destinationName.substring(DestinationMode.METRICS.getNamespace().length());
    } else {
      destinationMode = DestinationMode.NORMAL;
    }
  }

}
