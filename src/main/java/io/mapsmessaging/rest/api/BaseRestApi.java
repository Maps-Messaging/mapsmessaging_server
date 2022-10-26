package io.mapsmessaging.rest.api;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;

public class BaseRestApi {

  @Context
  protected Request request;
}
