package io.mapsmessaging.network.io.impl.canbus;

import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.AcceptHandler;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.EndPointServerFactory;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;

public class CanbusEndPointServerFactory implements EndPointServerFactory {


  @Override
  public EndPointServer instance(
      EndPointURL url,
      SelectorLoadManager selector,
      AcceptHandler acceptHandler,
      EndPointServerConfigDTO endPointServerConfig,
      EndPointManagerJMX managerMBean) {
    return new CanbusEndPointServer(acceptHandler, url, endPointServerConfig, managerMBean);
  }

  @Override
  public String getName() {
    return "canbus";
  }

  @Override
  public String getDescription() {
    return "CanbusEnd Point Server Factory";
  }

  @Override
  public boolean active() {
    return true;
  }

}
