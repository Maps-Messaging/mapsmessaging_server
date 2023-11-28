package io.mapsmessaging.auth.priviliges;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class PublishPrivileges extends Privilege {

  public PublishPrivileges() {
    super("publish");
  }

}
