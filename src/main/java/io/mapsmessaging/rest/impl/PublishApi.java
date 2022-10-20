package io.mapsmessaging.rest.impl;

import static spark.Spark.get;

import io.mapsmessaging.rest.RestApi;
import io.mapsmessaging.rest.transformation.JsonTransformation;
import spark.Request;
import spark.Response;

public class PublishApi implements RestApi {

  public void initialise() {
    get("/destination/*", (req, res) -> publish(req, res), new JsonTransformation());
  }

  private String publish(Request request, Response response) {
    String destination ="";
    for(String part:request.splat()){
      destination += "/"+part;
    }
    System.err.println("Publishing to "+destination);
    return destination;
  }
}
