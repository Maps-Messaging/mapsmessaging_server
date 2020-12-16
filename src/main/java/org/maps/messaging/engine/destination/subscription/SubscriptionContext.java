/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.messaging.engine.destination.subscription;

import java.io.IOException;
import java.util.BitSet;
import org.maps.messaging.api.features.ClientAcknowledgement;
import org.maps.messaging.api.features.CreditHandler;
import org.maps.messaging.api.features.QualityOfService;
import org.maps.messaging.api.features.RetainHandler;
import org.maps.messaging.engine.serializer.SerializedObject;
import org.maps.utilities.streams.ObjectReader;
import org.maps.utilities.streams.ObjectWriter;

public class SubscriptionContext implements Comparable<SubscriptionContext>, SerializedObject {

  private static final int NO_LOCAL_MESSAGES = 0;
  private static final int RETAIN_AS_PUBLISH = 1;
  private static final int ALLOW_OVERLAP = 2;
  private static final int BROWSER_FLAG = 3;

  private final String destinationName;
  private final BitSet flags;
  private ClientAcknowledgement acknowledgement;
  private String sharedName;
  private String selector;
  private String alias;
  private long subscriptionId;
  private int receiveMaximum;
  private RetainHandler retainHandler;
  private QualityOfService qos;
  private CreditHandler creditHandler;

  //
  // Server Only flag
  //
  private boolean replaced;

  public SubscriptionContext(String destinationName) {
    this.destinationName = destinationName;
    alias = destinationName; // Make the Alias the same as the destination. In some protocols this can be overridden
    flags = new BitSet(8);
    receiveMaximum = 1;
  }

  public SubscriptionContext(SubscriptionContext rhs, String destinationName, String alias) {
    this.destinationName = destinationName;
    this.alias = alias;
    acknowledgement = rhs.acknowledgement;
    sharedName = rhs.sharedName;
    selector = rhs.selector;
    subscriptionId = rhs.subscriptionId;
    receiveMaximum = rhs.receiveMaximum;
    retainHandler = rhs.retainHandler;
    qos = rhs.qos;
    flags = BitSet.valueOf(rhs.flags.toByteArray());
  }

  public SubscriptionContext(ObjectReader reader) throws IOException {
    retainHandler = RetainHandler.getInstance(reader.readByte());
    creditHandler = CreditHandler.getInstance(reader.readByte());
    qos = QualityOfService.getInstance(reader.readByte());
    acknowledgement = ClientAcknowledgement.getInstance(reader.readByte());
    subscriptionId = reader.readLong();

    destinationName = reader.readString();
    sharedName = reader.readString();
    selector = reader.readString();
    alias = reader.readString();
    flags = BitSet.valueOf(reader.readByteArray());

    if (alias == null) {
      alias = destinationName;
    }
  }

  public void write(ObjectWriter writer) throws IOException {
    writer.write((byte)retainHandler.getHandler());
    writer.write((byte) creditHandler.getValue());
    writer.write((byte) qos.getLevel());
    writer.write((byte) acknowledgement.getValue());
    writer.write(subscriptionId);

    writer.write(destinationName);
    writer.write(sharedName);
    writer.write(selector);
    writer.write(alias);
    writer.write(flags.toByteArray());
  }

  public boolean containsWildcard() {
    return destinationName.contains("#") || destinationName.contains("+");
  }

  public QualityOfService getQualityOfService() {
    return qos;
  }

  public void setQualityOfService(QualityOfService qos) {
    this.qos = qos;
  }

  public boolean isRetainAsPublish() {
    return flags.get(RETAIN_AS_PUBLISH);
  }

  public void setRetainAsPublish(boolean retainAsPublish) {
    flags.set(RETAIN_AS_PUBLISH, retainAsPublish);
  }

  public boolean isSharedSubscription() {
    return (sharedName != null && sharedName.length() > 0);
  }

  public String getSharedSubscriptionName() {
    return sharedName;
  }

  public RetainHandler getRetainHandler() {
    return retainHandler;
  }

  public void setRetainHandler(RetainHandler retainHandler) {
    this.retainHandler = retainHandler;
  }

  public boolean noLocalMessages() {
    return flags.get(NO_LOCAL_MESSAGES);
  }

  public String getFilter() {
    return destinationName;
  }

  public ClientAcknowledgement getAcknowledgementController() {
    return acknowledgement;
  }

  public void setAcknowledgementController(ClientAcknowledgement clientAcknowledgement) {
    this.acknowledgement = clientAcknowledgement;
  }

  public boolean allowOverlap() {
    return flags.get(ALLOW_OVERLAP);
  }

  public boolean isBrowser() {
    return flags.get(BROWSER_FLAG);
  }

  public String getSelector() {
    return selector;
  }

  public void setSelector(String selector) {
    this.selector = selector;
  }

  public long getSubscriptionId() {
    return subscriptionId;
  }

  public void setSubscriptionId(long subscriptionId) {
    this.subscriptionId = subscriptionId;
  }

  public int getReceiveMaximum() {
    return receiveMaximum;
  }

  public void setReceiveMaximum(int receiveMaximum) {
    this.receiveMaximum = receiveMaximum;
  }

  public String getAlias() {
    return alias;
  }

  public void setAlias(String alias) {
    if (alias == null) {
      this.alias = destinationName;
    } else {
      this.alias = alias;
    }
  }

  public boolean getReplaced() {
    return replaced;
  }

  public void setReplaced(boolean flag) {
    replaced = flag;
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

  public CreditHandler getCreditHandler() {
    return creditHandler;
  }

  public void setCreditHandler(CreditHandler creditHandler) {
    this.creditHandler = creditHandler;
  }

  @Override
  public int compareTo(SubscriptionContext lhs) {
    return lhs.qos.getLevel() - qos.getLevel();
  }

  @Override
  public boolean equals(Object lhs) {
    if (lhs instanceof SubscriptionContext) {
      return ((SubscriptionContext) lhs).qos == qos;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("SubscriptionContext:");
    sb.append(" Destination:").append(destinationName);
    sb.append(", alias:").append(alias);
    sb.append(", QOS").append(qos);
    sb.append(", shareName:").append(sharedName);
    sb.append(", Selector:").append(selector);
    sb.append(", subscriptionId:").append(subscriptionId);
    sb.append(", receiveMax:").append(receiveMaximum);
    sb.append(", retainHandler:").append(retainHandler);
    sb.append(", flags:").append(flags.toString());
    sb.append(", ClientAck:").append(acknowledgement.toString());
    sb.append(", CreditHandler:").append(creditHandler.getName());
    sb.append(", isBrowser:").append(isBrowser());
    return sb.toString();
  }
}
