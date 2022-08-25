package io.mapsmessaging.network.protocol.impl.coap.subscriptions;

import static io.mapsmessaging.network.protocol.impl.coap.packet.options.Constants.OBSERVE;

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Observe;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Option;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.OptionSet;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Getter;
import lombok.Setter;

public class Context {

  @Getter
  private final String path;

  @Getter
  private final BasePacket request;

  @Getter
  @Setter
  private SubscribedEventManager subscribedEventManager;

  @Getter
  private final boolean observe;

  @Getter
  private final AtomicLong observeId;


  public Context(String path, BasePacket request){
    observeId = new AtomicLong(2);
    this.path = path;
    this.request = request;
    OptionSet optionSet = request.getOptions();
    if(optionSet.hasOption(OBSERVE)){
      Option observeOption = optionSet.getOption(OBSERVE);
      observe = ((Observe)observeOption).register();
    }
    else {
      observe = false;
    }
  }

}
