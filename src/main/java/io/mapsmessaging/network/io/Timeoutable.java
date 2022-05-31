package io.mapsmessaging.network.io;

import java.io.Closeable;

public interface Timeoutable extends Closeable {

  default long getTimeOut(){
    return 0;
  }

}
