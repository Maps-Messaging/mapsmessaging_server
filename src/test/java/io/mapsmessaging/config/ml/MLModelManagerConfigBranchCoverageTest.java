package io.mapsmessaging.config.ml;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MLModelManagerConfigBranchCoverageTest {

  @Test
  void fileStoreSingleEventStreamAutoRefreshAndLlmAreParsed() throws Exception {
    ConfigurationProperties event = new ConfigurationProperties();
    event.put("id", "stream-1");
    event.put("topicFilter", "/sensor/#");
    event.put("maxTrainEvents", 123);

    ConfigurationProperties auto = new ConfigurationProperties();
    auto.put("enabled", true);
    auto.put("intervalMinutes", 15);

    ConfigurationProperties llm = new ConfigurationProperties();
    llm.put("api_token", "token");
    llm.put("model", "model");

    ConfigurationProperties root = baseStore("file");
    root.put("preloadModels", "a,b");
    root.put("eventStreams", event);
    root.put("autoRefresh", auto);
    root.put("llm", llm);

    MLModelManagerConfig config = create(root);

    assertEquals(List.of("a", "b"), config.getPreloadModels());
    assertEquals(1, config.getEventStreams().size());
    assertTrue(config.getAutoRefresh().isEnabled());
    assertEquals("model", config.getLlmConfig().getModel());
    assertEquals("/tmp/models", config.getModelStore().getConfig().getFile().getPath());
  }

  @Test
  void nexusAndMapsStoresParseCredentialBlocks() throws Exception {
    MLModelManagerConfig nexus = create(baseStore("nexus"));
    MLModelManagerConfig maps = create(baseStore("maps"));

    assertEquals("user", nexus.getModelStore().getConfig().getNexus().getUser());
    assertEquals("user", maps.getModelStore().getConfig().getMaps().getUser());
  }

  @Test
  void s3StoreParsesAndSerializesBucketDetails() throws Exception {
    MLModelManagerConfig config = create(baseStore("s3"));

    ConfigurationProperties packed = config.toConfigurationProperties();

    assertEquals("bucket", config.getModelStore().getConfig().getS3().getBucket());
    assertTrue(packed.containsKey("modelStore"));
  }

  @Test
  void unrelatedUpdateIsRejected() {
    assertFalse(new MLModelManagerConfig().update(new BaseConfigDTO()));
  }

  private static ConfigurationProperties baseStore(String type) {
    ConfigurationProperties root = new ConfigurationProperties();
    ConfigurationProperties modelStore = new ConfigurationProperties();
    ConfigurationProperties config = new ConfigurationProperties();
    modelStore.put("type", type);

    ConfigurationProperties specific = new ConfigurationProperties();
    switch (type) {
      case "file" -> {
        specific.put("path", "/tmp/models");
        config.put("file", specific);
      }
      case "nexus", "maps" -> {
        specific.put("url", "https://example");
        specific.put("user", "user");
        specific.put("password", "pass");
        config.put(type, specific);
      }
      case "s3" -> {
        specific.put("region", "eu-west-1");
        specific.put("accessKey", "access");
        specific.put("secretKey", "secret");
        specific.put("bucket", "bucket");
        specific.put("endpoint", "https://s3.example");
        specific.put("prefix", "models/");
        config.put("s3", specific);
      }
      default -> throw new IllegalArgumentException(type);
    }
    modelStore.put("config", config);
    root.put("modelStore", modelStore);
    return root;
  }

  private static MLModelManagerConfig create(ConfigurationProperties properties) throws Exception {
    Constructor<MLModelManagerConfig> constructor =
        MLModelManagerConfig.class.getDeclaredConstructor(ConfigurationProperties.class);
    constructor.setAccessible(true);
    return constructor.newInstance(properties);
  }
}