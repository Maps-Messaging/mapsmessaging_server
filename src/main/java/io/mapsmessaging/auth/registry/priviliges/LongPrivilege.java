package io.mapsmessaging.auth.registry.priviliges;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@EqualsAndHashCode(callSuper = true)
public class LongPrivilege extends Privilege {

  private final long value;

  public LongPrivilege(String name, long value) {
    super(name);
    this.value = value;
  }
}
