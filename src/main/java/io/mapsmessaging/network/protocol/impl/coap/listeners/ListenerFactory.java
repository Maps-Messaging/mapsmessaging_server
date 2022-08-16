package io.mapsmessaging.network.protocol.impl.coap.listeners;

import io.mapsmessaging.network.protocol.impl.coap.packet.PacketFactory;

public class ListenerFactory {

  private final Listener[] listeners;


  public Listener getListener(int id){
    if(id >=0 && id< listeners.length) {
      return listeners[id];
    }
    return null;
  }


  public ListenerFactory(){
    listeners = new Listener[8];
    for(int x=0;x<listeners.length;x++){
      listeners[x] = new EmptyListener();
    }
    listeners[PacketFactory.DELETE] = new DeleteListener();
    listeners[PacketFactory.EMPTY] = new EmptyListener();
    listeners[PacketFactory.FETCH] = new FetchListener();
    listeners[PacketFactory.GET] = new GetListener();
    listeners[PacketFactory.IPATCH] = new IPatchListener();
    listeners[PacketFactory.PATCH] = new PatchListener();
    listeners[PacketFactory.POST] = new PostListener();
    listeners[PacketFactory.PUT] = new PutListener();
  }
}
