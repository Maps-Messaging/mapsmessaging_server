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

package org.maps.network.protocol.impl.stomp.frames;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class Frame {

  static final byte END_OF_FRAME = 0x00;
  static final byte END_OF_LINE = 0x0A;
  static final byte DELIMITER = ':';

  private final Map<String, String> header;
  private final Map<String, String> caseHeader;

  String receipt;
  private CompletionHandler completionHandler;

  public Frame() {
    header = new LinkedHashMap<>();
    caseHeader = new LinkedHashMap<>();
    completionHandler = null;
  }

  public String getReceipt() {
    return receipt;
  }

  public void setReceipt(String receipt) {
    this.receipt = receipt;
  }

  protected String getHeader(String key) {
    String val = header.get(key);
    if(val == null){
      String keyLookup = caseHeader.get(key.toLowerCase());
      if(keyLookup != null){
        val = header.get(keyLookup);
      }
    }
    return val;
  }

  public Map<String, String> getHeader() {
    return header;
  }

  protected void putHeader(String key, String val) {
    header.put(key, val);
    caseHeader.put(key.toLowerCase(), key);
  }

  protected String removeHeader(String key) {
    String caseKey = caseHeader.remove(key.toLowerCase());
    return header.remove(caseKey);
  }

  protected boolean headerContainsKey(String key){
    return header.containsKey(key.toLowerCase());
  }


  public abstract Frame instance();

  public CompletionHandler getCallback() {
    return completionHandler;
  }

  public void setCallback(CompletionHandler completion) {
    completionHandler = completion;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().toString());
    if (receipt != null) {
      sb.append("::").append(receipt);
    }
    return sb.toString();
  }

  public void complete() {
    CompletionHandler tmp;
    synchronized (this) {
      tmp = completionHandler;
      completionHandler = null;
    }
    if (tmp != null) {
      tmp.run();
    }
  }

  public String getHeaderAsString() {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> entry : header.entrySet()) {
      sb.append(entry.getKey()).append(":").append(entry.getValue()).append(",");
    }
    if(receipt != null){
      sb.append("Receipt:").append(receipt);
    }
    return sb.toString();
  }
}
