package org.maps.network.io;

import java.io.IOException;
import java.util.concurrent.atomic.LongAdder;
import org.maps.network.EndPointURL;
import org.maps.network.NetworkConfig;

public abstract class EndPointServerStatus {


  private final LongAdder totalPacketsSent;
  private final LongAdder totalPacketsRead;

  private final LongAdder totalBytesSent;
  private final LongAdder totalBytesRead;
  protected final EndPointURL url;

  public EndPointServerStatus(EndPointURL url){
    this.url = url;
    totalPacketsSent = new LongAdder();
    totalPacketsRead = new LongAdder();
    totalBytesSent = new LongAdder();
    totalBytesRead = new LongAdder();
  }

  public EndPointURL getUrl() {
    return url;
  }

  public abstract NetworkConfig getConfig();

  public abstract void handleNewEndPoint(EndPoint endPoint) throws IOException;

  public abstract void handleCloseEndPoint(EndPoint endPoint);

  public long getTotalPacketsRead(){
    return totalPacketsRead.sum();
  }

  public long getTotalPacketsSent(){
    return totalPacketsSent.sum();
  }

  public long getTotalBytesSent(){
    return totalBytesSent.sum();
  }

  public long getTotalBytesRead(){
    return totalBytesRead.sum();
  }

  public void incrementPacketsSent(){
    totalPacketsSent.increment();
  }

  public void incrementPacketsRead(){
    totalPacketsRead.increment();
  }

  public void updateBytesSent(int count){
    totalBytesSent.add(count);
  }

  public void updateBytesRead(int count){
    totalBytesRead.add(count);
  }


}
