/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.engine.destination.subscription;

import io.mapsmessaging.api.features.*;
import io.mapsmessaging.utilities.PersistentObject;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.BitSet;
import java.util.Objects;

@ToString
public class SubscriptionContext  extends PersistentObject implements Comparable<SubscriptionContext> {

  private static final int NO_LOCAL_MESSAGES = 0;
  private static final int RETAIN_AS_PUBLISH = 1;
  private static final int ALLOW_OVERLAP = 2;
  private static final int BROWSER_FLAG = 3;

  @Getter
  private String destinationName;

  @Getter
  @Setter
  private BitSet flags;

  @Getter
  private String rootPath;

  @Getter
  @Setter
  private ClientAcknowledgement acknowledgementController;

  @Getter
  private String sharedName;

  @Getter
  @Setter
  private String selector;

  @Getter
  private String alias;

  @Getter
  @Setter
  private long subscriptionId;

  @Getter
  @Setter
  private int maxAtRest;

  @Getter
  @Setter
  private int receiveMaximum;

  @Getter
  @Setter
  private RetainHandler retainHandler;

  @Getter
  @Setter
  private QualityOfService qualityOfService;

  @Getter
  @Setter
  private CreditHandler creditHandler;

  @Getter
  @Setter
  private DestinationMode destinationMode;

  //
  // Server Only flag
  //
  @Getter
  @Setter
  private boolean replaced;

  public SubscriptionContext() {
    maxAtRest =0;
  }

  public SubscriptionContext(String destinationName) {
    this.destinationName = destinationName;
    maxAtRest =0;
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
    maxAtRest =0;
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

  public SubscriptionContext(InputStream inputStream) throws IOException {
    destinationName = readString(inputStream);
    alias = readString(inputStream);
    sharedName = readString(inputStream);
    selector = readString(inputStream);
    rootPath = readString(inputStream);

    acknowledgementController = ClientAcknowledgement.getInstance( readInt(inputStream));
    destinationMode = DestinationMode.getInstance(readInt(inputStream));
    retainHandler = RetainHandler.getInstance(readInt(inputStream));
    qualityOfService = QualityOfService.getInstance(readInt(inputStream));
    creditHandler = CreditHandler.getInstance(readInt(inputStream));

    subscriptionId = readLong(inputStream);
    receiveMaximum = readInt(inputStream);
    maxAtRest = readInt(inputStream);
    flags = BitSet.valueOf(readByteArray(inputStream));
  }


  public void save(OutputStream outputStream) throws IOException {
    writeString(outputStream, destinationName);
    writeString(outputStream, alias);
    writeString(outputStream, sharedName);
    writeString(outputStream, selector);
    writeString(outputStream, rootPath);

    writeInt(outputStream, acknowledgementController.getValue());
    writeInt(outputStream, destinationMode.getId());
    writeInt(outputStream, retainHandler.getHandler());
    writeInt(outputStream, qualityOfService.getLevel());
    writeInt(outputStream, creditHandler.getValue());

    writeLong(outputStream, subscriptionId);
    writeInt(outputStream, receiveMaximum);
    writeInt(outputStream, maxAtRest);
    writeByteArray(outputStream, getFlags().toByteArray());
  }

  public SubscriptionContext setRootPath(String rootPath) {
    this.rootPath = Objects.requireNonNullElse(rootPath, "");
    if (rootPath.length() > 1 && !rootPath.endsWith("/")) {
      this.rootPath = this.rootPath + File.separator;
    }
    return this;
  }

  public void setDestinationName(String destinationName) {
    if (alias.equals(destinationName)) {
      alias = destinationName;
    }
    this.destinationName = destinationName;
  }

  public void setAlias(String alias) {
    this.alias = Objects.requireNonNullElseGet(alias, this::getCorrectedPath);
  }

  public void setNoLocalMessages(boolean noLocalMessages) {
    flags.set(NO_LOCAL_MESSAGES, noLocalMessages);
  }

  public void setSharedName(String sharedName) {
    this.sharedName = sharedName;
  }

  public void setAllowOverlap(boolean allowOverlap) {
    flags.set(ALLOW_OVERLAP, allowOverlap);
  }

  public void setBrowserFlag(boolean isBrowser) {
    flags.set(BROWSER_FLAG, isBrowser);
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

  public void setRetainAsPublish(boolean flag) {
    setFlag(RETAIN_AS_PUBLISH, flag);
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


  private void setFlag(int index, boolean flag){
    flags.set(index, flag);
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
