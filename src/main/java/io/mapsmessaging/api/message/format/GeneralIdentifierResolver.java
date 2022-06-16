package io.mapsmessaging.api.message.format;

import io.mapsmessaging.api.message.format.walker.Resolver;
import io.mapsmessaging.api.message.format.walker.StructureWalker;
import io.mapsmessaging.selector.IdentifierResolver;
import java.util.ArrayList;
import java.util.List;

public class GeneralIdentifierResolver implements IdentifierResolver {

  private final Resolver resolver;

  public GeneralIdentifierResolver(Resolver resolver){
    this.resolver = resolver;
  }

  @Override
  public Object get(String s) {
    List<String> keys = new ArrayList<>();

    if(s.contains(".")){
      keys.addAll(List.of(s.split("\\.")));
    }
    else{
      keys.add(s);
    }
    return StructureWalker.locateObject(resolver, keys);
  }

  @Override
  public byte[] getOpaqueData() {
    return IdentifierResolver.super.getOpaqueData();
  }
}