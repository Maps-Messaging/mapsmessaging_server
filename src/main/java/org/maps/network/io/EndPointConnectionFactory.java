package org.maps.network.io;

import java.io.IOException;
import java.util.List;
import org.maps.network.EndPointURL;
import org.maps.network.admin.EndPointConnectionJMX;
import org.maps.network.io.impl.SelectorLoadManager;
import org.maps.utilities.service.Service;

public abstract class EndPointConnectionFactory  implements Service {

  public abstract EndPoint connect(EndPointURL url,
      SelectorLoadManager selector,
      EndPointServerStatus endPointServerStatus,
      List<String> jmxPath)
      throws IOException;


  public long generateID() {
    return Constants.getNextId();
  }

}
