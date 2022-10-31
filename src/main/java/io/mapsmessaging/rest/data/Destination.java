package io.mapsmessaging.rest.data;

import lombok.Getter;

public class Destination {

  @Getter
  private final String name;


  public Destination(String name){
    this.name = name;
  }
}
