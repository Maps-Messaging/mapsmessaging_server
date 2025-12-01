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

package io.mapsmessaging.engine.destination.subscription;

import io.mapsmessaging.api.auth.DestinationAuthorisationCheck;
import io.mapsmessaging.api.features.*;
import io.mapsmessaging.dto.rest.session.SubscriptionContextDTO;
import io.mapsmessaging.utilities.PersistentObject;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.BitSet;
import java.util.Objects;

@Getter
@ToString
public class SubscriptionContext  extends PersistentObject implements Comparable<SubscriptionContext> {

  private static final int NO_LOCAL_MESSAGES = 0;
  private static final int RETAIN_AS_PUBLISH = 1;
  private static final int ALLOW_OVERLAP = 2;
  private static final int BROWSER_FLAG = 3;
  private static final int SYNC_FLAG = 4;

  @Getter
  @Setter
  private long allocatedId;

  private String destinationName;

  @Setter
  private BitSet flags;

  private String rootPath;

  @Setter
  private ClientAcknowledgement acknowledgementController;

  @Setter
  private String sharedName;

  @Setter
  private String selector;

  private String alias;

  @Setter
  private long subscriptionId;

  @Setter
  private int maxAtRest;

  @Setter
  private int receiveMaximum;

  @Setter
  private RetainHandler retainHandler;

  @Setter
  private QualityOfService qualityOfService;

  @Setter
  private CreditHandler creditHandler;

  @Setter
  private DestinationMode destinationMode;

  @Setter
  private DestinationAuthorisationCheck authCheck;

  //
  // Server Only flag
  //
  @Setter
  private boolean replaced;

  public SubscriptionContext() {
    maxAtRest =0;
  }

  public SubscriptionContext(String destinationName) {
    this.allocatedId = -1;
    this.destinationName = destinationName;
    maxAtRest =0;
    subscriptionId = 0;
    alias = destinationName; // Make the Alias the same as the destination. In some protocols this can be overridden
    flags = new BitSet(8);
    receiveMaximum = 1;
    rootPath = "";
    creditHandler = CreditHandler.AUTO;
    retainHandler = RetainHandler.SEND_ALWAYS;
    qualityOfService = QualityOfService.AT_MOST_ONCE;
    acknowledgementController = ClientAcknowledgement.AUTO;
    authCheck = null;
    parseName();
  }

  public SubscriptionContext(SubscriptionContext rhs, String destinationName, String alias) {
    this.allocatedId = rhs.allocatedId;
    this.destinationName = destinationName;
    this.alias = alias;
    subscriptionId = 0;
    maxAtRest =0;
    acknowledgementController = rhs.acknowledgementController;
    sharedName = rhs.sharedName;
    selector = rhs.selector;
    subscriptionId = rhs.subscriptionId;
    receiveMaximum = rhs.receiveMaximum;
    retainHandler = rhs.retainHandler;
    qualityOfService = rhs.qualityOfService;
    flags = BitSet.valueOf(rhs.flags.toByteArray());
    authCheck = null;
    parseName();
  }

  public SubscriptionContext(InputStream inputStream, long sessionId) throws IOException {
    this.allocatedId = sessionId;
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
    authCheck = null;
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

  public void setDestinationName(String destinationName) {
    if (alias.equals(destinationName)) {
      alias = destinationName;
    }
    this.destinationName = destinationName;
  }

  public String getKey(){
    String key = getCorrectedPath();
    key = destinationMode.getNamespace()+key;
    return key.replace("//", "/");

  }

  public String getFilter() {
    return getCorrectedPath();
  }

  public void setAlias(String alias) {
    this.alias = Objects.requireNonNullElseGet(alias, this::getCorrectedPath);
  }

  public boolean isSharedSubscription() {
    return (sharedName != null && !sharedName.isEmpty());
  }

  public boolean containsWildcard() {
    return destinationName.contains("#") || destinationName.contains("+");
  }


  public void setNoLocalMessages(boolean noLocalMessages) {
    flags.set(NO_LOCAL_MESSAGES, noLocalMessages);
  }

  public void setAllowOverlap(boolean allowOverlap) {
    flags.set(ALLOW_OVERLAP, allowOverlap);
  }

  public void setBrowserFlag(boolean isBrowser) {
    flags.set(BROWSER_FLAG, isBrowser);
  }

  public void setRetainAsPublish(boolean flag) {
    setFlag(RETAIN_AS_PUBLISH, flag);
  }

  public void setSync(boolean flag) {
    setFlag(SYNC_FLAG, flag);
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

  public boolean isRetainAsPublish() {
    return flags.get(RETAIN_AS_PUBLISH);
  }

  public boolean isSync() {return flags.get(SYNC_FLAG);}

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
    } else {
      destinationMode = DestinationMode.NORMAL;
    }
  }

  private void setFlag(int index, boolean flag){
    flags.set(index, flag);
  }

  private String getCorrectedPath() {
    String lookup = rootPath + destinationName;
    return lookup.replace("//", "/");
  }

  public SubscriptionContextDTO getDetails() {
    SubscriptionContextDTO dto = new SubscriptionContextDTO();
    dto.setSubscriptionId(subscriptionId);
    dto.setAlias(alias);
    dto.setSharedName(sharedName);
    dto.setSelector(selector);
    dto.setReceiveMaximum(receiveMaximum);
    dto.setMaxAtRest(maxAtRest);
    dto.setDestinationName(getCorrectedPath());

    dto.setQualityOfService(qualityOfService.name());
    dto.setAcknowledgementController(acknowledgementController.name());
    dto.setDestinationMode(destinationMode.getName());
    dto.setCreditHandler(creditHandler.getName());
    dto.setRetainHandler(retainHandler.name());

    dto.setNoLocalMessages(noLocalMessages());
    dto.setAllowOverlap(allowOverlap());
    dto.setSync(isSync());
    dto.setBrowser(isBrowser());
    dto.setRetainAsPublish(isRetainAsPublish());
    return dto;
  }
}
