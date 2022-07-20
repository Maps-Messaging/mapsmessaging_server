package io.mapsmessaging.network.protocol.impl.coap;

import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import java.util.ArrayList;
import java.util.List;

public class DuplicationManager {

  private final int nstart; // Depth of outstanding
  private final List<BasePacket> requestResponseMap;

  public DuplicationManager(int nstart){
    requestResponseMap = new ArrayList<>();
    this.nstart = nstart;
  }

  public synchronized void put(BasePacket response){
    requestResponseMap.add(response);
    while(requestResponseMap.size() > nstart){
      requestResponseMap.remove(0).getMessageId();
    }
  }

  public synchronized BasePacket getResponse(int messageId){
    return requestResponseMap.stream().filter(response -> response.getMessageId() == messageId).findFirst().orElse(null);
  }

}
