package io.mapsmessaging.auth.priviliges;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
public class StringPrivilege extends Privilege {

  private final String value;

  public StringPrivilege(String name, String value) {
    super(name);
    this.value = value;
  }
}
