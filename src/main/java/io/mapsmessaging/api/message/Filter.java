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
      Format format = destination.getSchema().getFormat();
      try {
        IdentifierResolver formatResolver = format.getResolver(message.getOpaqueData());
        return selector.evaluate(new Resolver(formatResolver, message));
      } catch (IOException e) {
        e.printStackTrace(); // Log this and move on...
      }
    }
    return false;
  }

  private Filter(){}

  private static final class Resolver implements IdentifierResolver{
    private final IdentifierResolver formatResolver;
    private final Message message;

    public Resolver(IdentifierResolver formatResolver, Message message){
      this.formatResolver = formatResolver;
      this.message = message;
    }

    @Override
    public Object get(String s) {
      Object val = message.get(s);
      if(val == null){
        val = formatResolver.get(s);
      }
      return val;
    }

    @Override
    public byte[] getOpaqueData() {
      return IdentifierResolver.super.getOpaqueData();
    }
  }
}
