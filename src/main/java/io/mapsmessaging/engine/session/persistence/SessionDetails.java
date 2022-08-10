package io.mapsmessaging.engine.session.persistence;

import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class SessionDetails {

  @Getter
  @Setter
  private String sessionName;

  @Getter
  @Setter
  private String uniqueId;

  @Getter
  @Setter
  private List<SubscriptionContext> subscriptionContextList = new ArrayList<>();

  @Getter
  @Setter
  private WillData willDetails;

  public SessionDetails() {
  }

  public SessionDetails(String sessionName, String uniqueId) {
    this.sessionName = sessionName;
    this.uniqueId = uniqueId;
  }
}
