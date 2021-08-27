package io.mapsmessaging.network.auth;

import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import io.mapsmessaging.utilities.service.Service;
import java.io.IOException;

public interface TokenGenerator extends Service {

  TokenGenerator getInstance(ConfigurationProperties properties);

  String generate() throws IOException;

}
