package io.mapsmessaging.rest.api.impl;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.rest.api.BaseRestApi;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BaseDestinationApi extends BaseRestApi {


  protected DestinationImpl lookup(String name) throws ExecutionException, InterruptedException, TimeoutException {
    DestinationImpl destinationImpl = MessageDaemon.getInstance().getDestinationManager().find(name).get(60, TimeUnit.SECONDS);
    if(destinationImpl == null && !name.startsWith("/")){
      name = "/"+name;
      destinationImpl = MessageDaemon.getInstance().getDestinationManager().find(name).get(60, TimeUnit.SECONDS);
    }
    return destinationImpl;
  }

}
