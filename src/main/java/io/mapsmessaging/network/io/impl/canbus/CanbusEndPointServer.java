package io.mapsmessaging.network.io.impl.canbus;

import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.CanbusConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.AcceptHandler;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.impl.Selector;
import io.mapsmessaging.network.io.impl.serial.SerialEndPointServer;
import io.mapsmessaging.network.protocol.ProtocolFactory;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class CanbusEndPointServer extends EndPointServer {
  private final EndPointManagerJMX managerMBean;
  private final CanbusConfigDTO canbusConfig;

  public CanbusEndPointServer(AcceptHandler acceptHandler, EndPointURL url, EndPointServerConfigDTO config, EndPointManagerJMX managerMBean) {
    super(acceptHandler, url, config);
    this.managerMBean = managerMBean;
    canbusConfig = (CanbusConfigDTO)config.getEndPointConfig();
    SimpleTaskScheduler.getInstance().schedule(this::createEndPoint, 1, TimeUnit.SECONDS);
  }

  @Override
  public void handleNewEndPoint(EndPoint endPoint) throws IOException {
    ProtocolImplFactory boundProtocolFactory = null;
    String protocols= getConfig().getProtocols();
    for(ProtocolImplFactory protocol: ProtocolFactory.getProtocolServiceList()){
      if(protocol.matches(protocols)){
        boundProtocolFactory = protocol;
        break;
      }
    }
    if(boundProtocolFactory != null){
      boundProtocolFactory.create(endPoint, ((CanbusEndPoint)endPoint).getInterfaceInformation());
    }
  }

  @Override
  public String getName() {
    return canbusConfig.getDeviceName();
  }

  @Override
  public void register() {
    // Nothing to register here
  }

  @Override
  public void deregister() {
    // Nothing to deregister here
  }

  @Override
  public void handleCloseEndPoint(EndPoint endPoint) {
    //
  }

  @Override
  public void start() throws IOException {
    // start
  }

  @Override
  protected Logger createLogger(String url) {
    return LoggerFactory.getLogger(SerialEndPointServer.class);
  }

  @Override
  public void close() {

  }

  @Override
  public void selected(Selectable selectable, Selector selector, int selection) {
    // not required
  }

  public void createEndPoint(){
    try {
      handleNewEndPoint(new CanbusEndPoint(canbusConfig, this, managerMBean.getTypePath()));
    } catch (IOException e) {
      // log this
    }
  }


}