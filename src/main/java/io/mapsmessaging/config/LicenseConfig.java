package io.mapsmessaging.config;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.IOException;

@Getter
@NoArgsConstructor
public class LicenseConfig implements ConfigManager {

  private String clientName;
  private String clientSecret;

  public static LicenseConfig getInstance() {
    return ConfigurationManager.getInstance().getConfiguration(LicenseConfig.class);
  }

  private LicenseConfig(ConfigurationProperties properties) {
    clientName = properties.getProperty("name", "");
    clientSecret = properties.getProperty("secret", "");
  }

  @Override
  public ConfigManager load(FeatureManager featureManager) {
    return new LicenseConfig(ConfigurationManager.getInstance().getProperties(getName()));
  }

  @Override
  public void save() throws IOException {

  }

  @Override
  public String getName() {
    return "License";
  }
}
