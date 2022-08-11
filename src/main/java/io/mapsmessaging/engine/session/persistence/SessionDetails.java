package io.mapsmessaging.engine.session.persistence;

import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

  public Map<String, SubscriptionContext> getSubscriptionContextMap(){
    Map<String, SubscriptionContext> map = new LinkedHashMap<>();
    for(SubscriptionContext context:subscriptionContextList){
      map.put(context.getAlias(), context); // Pre-populate with persistent data
    }
    return map;
  }

  public void clearSubscriptions() {
    subscriptionContextList.clear();
  }
}
