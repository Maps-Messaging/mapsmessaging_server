package io.mapsmessaging.network.io.impl.udp.session;

import io.mapsmessaging.network.io.Timeoutable;
import lombok.Getter;
import lombok.Setter;

public class UDPSessionState<T extends Timeoutable> {
  @Getter
  @Setter
  private String clientIdentifier;

  private T context;

  @Getter
  private long getLastAccess;


  public UDPSessionState(T context){
    this.context = context;
    getLastAccess = System.currentTimeMillis();
  }

  public void updateTimeout(){
    getLastAccess = System.currentTimeMillis();
  }

  public T getContext(){
    return context;
  }

  public void setContext(T context){
    this.context = context;
  }

}
