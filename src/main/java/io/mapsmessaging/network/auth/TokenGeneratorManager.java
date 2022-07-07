package io.mapsmessaging.network.auth;

import io.mapsmessaging.utilities.service.Service;
import io.mapsmessaging.utilities.service.ServiceManager;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;

public class TokenGeneratorManager implements ServiceManager {

  public static TokenGeneratorManager getInstance() {
    return instance;
  }

  private static final TokenGeneratorManager instance = new TokenGeneratorManager();

  private final Map<String, Service> tokenGeneratorMap;

  public TokenGenerator get(String tokenGenerator) {
    return (TokenGenerator) tokenGeneratorMap.get(tokenGenerator);
  }

  private TokenGeneratorManager() {
    ServiceLoader<TokenGenerator> transformerServiceLoader = ServiceLoader.load(TokenGenerator.class);
    tokenGeneratorMap = new LinkedHashMap<>();
    for (TokenGenerator tokenGenerator : transformerServiceLoader) {
      tokenGeneratorMap.put(tokenGenerator.getName(), tokenGenerator);
    }
  }

  @Override
  public Iterator<Service> getServices() {
    return tokenGeneratorMap.values().iterator();
  }
}
