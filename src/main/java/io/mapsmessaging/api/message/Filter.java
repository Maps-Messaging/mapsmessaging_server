package io.mapsmessaging.api.message;


import io.mapsmessaging.api.message.format.Format;
import io.mapsmessaging.api.message.format.FormatManager;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.selector.IdentifierResolver;
import io.mapsmessaging.selector.operators.ParserExecutor;
import java.io.IOException;

public class Filter {

  private static final Filter instance = new Filter();

  public static Filter getInstance(){
    return instance;
  }

  public boolean filterMessage(ParserExecutor selector, Message message, DestinationImpl destination) {
    if(selector == null) return true;
    if(message != null) {
      Format format = FormatManager.getInstance().getFormat(destination.getFormatType());
      try {
        IdentifierResolver formatResolver = format.getResolver(message.getOpaqueData());
        return  (selector.evaluate((IdentifierResolver) s -> {
          Object val = message.get(s);
          if(val == null){
            val = formatResolver.get(s);
          }
          return val;
        }));
      } catch (IOException e) {
        e.printStackTrace(); // Log this and move on...
      }
    }
    return false;
  }

  private Filter(){}
}
