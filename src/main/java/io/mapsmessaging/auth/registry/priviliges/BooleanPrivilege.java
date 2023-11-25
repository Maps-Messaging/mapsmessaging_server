package io.mapsmessaging.auth.registry.priviliges;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@EqualsAndHashCode(callSuper = true)
public class BooleanPrivilege extends Privilege {

  private final boolean value;

  public BooleanPrivilege(String name, boolean value) {
    super(name);
    this.value = value;
  }
}
