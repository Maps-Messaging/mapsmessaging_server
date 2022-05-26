package io.mapsmessaging.network.io.impl.udp.session;

import java.io.Closeable;
import lombok.Getter;
import lombok.Setter;

public class UDPSessionState<T extends Closeable> {
  @Getter
  @Setter
  private String clientIdentifier;

  private T context;

  @Getter
  private long timeout;


  public UDPSessionState(T context){
    this.context = context;
    timeout = System.currentTimeMillis();
  }

  public void updateTimeout(){
    timeout = System.currentTimeMillis();
  }

  public T getContext(){
    return context;
  }

  public void setContext(T context){
    this.context = context;
  }

}
