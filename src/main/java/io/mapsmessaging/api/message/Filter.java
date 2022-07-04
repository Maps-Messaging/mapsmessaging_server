package io.mapsmessaging.api.message;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.selector.IdentifierResolver;
import io.mapsmessaging.selector.operators.ParserExecutor;
import java.util.UUID;

public class Filter {

  private static final Filter instance = new Filter();

  public static Filter getInstance(){
    return instance;
  }

  public boolean filterMessage(ParserExecutor selector, Message message, DestinationImpl destination) {
    if(selector == null) return true;
    if(message != null) {
      UUID lookup = getSchemaId(message, destination);
      Resolver resolver = new Resolver(getResolver(lookup, message), message);
      return selector.evaluate(resolver);
    }
    return false;
  }

  private UUID getSchemaId(Message message, DestinationImpl destination){
    UUID lookup = message.getSchemaId();
    if(lookup == null){
      lookup = destination.getSchema().getUniqueId();
    }
    return lookup;
  }

  private IdentifierResolver getResolver(UUID lookup, Message message){
    MessageFormatter formatter = SchemaManager.getInstance().getMessageFormatter(lookup);
    if(formatter != null) {
      return formatter.parse(message.getOpaqueData());
    }
    return null;
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
      if(val == null && formatResolver != null){
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
