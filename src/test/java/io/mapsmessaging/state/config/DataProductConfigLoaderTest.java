package io.mapsmessaging.state.config;

import io.mapsmessaging.configuration.ConfigurationProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DataProductConfigLoaderTest {

  @Test
  void singleProductParsesIdentityUriAndFlexibleMaps() {
    ConfigurationProperties product = new ConfigurationProperties();
    product.put("identifier", "camera-1");
    product.put("description", "Optical feed");
    product.put("uri", "rtsp://camera/feed");
    ConfigurationProperties type = new ConfigurationProperties();
    type.put("name", "VIDEO");
    product.put("product_type", type);
    product.put("conforms_to", Map.of("standard", "MISB"));

    DataProductConfig parsed = DataProductConfigLoader.parseProducts(product).getFirst();

    assertEquals("camera-1", parsed.getIdentifier());
    assertEquals("rtsp://camera/feed", parsed.getUri());
    assertEquals("VIDEO", parsed.getProductType().get("name"));
    assertEquals("MISB", parsed.getConformsTo().get("standard"));
  }

  @Test
  void productListIgnoresUnsupportedEntriesAndPreservesOrder() {
    ConfigurationProperties first = new ConfigurationProperties();
    first.put("identifier", "first");
    ConfigurationProperties second = new ConfigurationProperties();
    second.put("identifier", "second");

    List<DataProductConfig> parsed = DataProductConfigLoader.parseProducts(List.of(first, "ignored", second));

    assertEquals(List.of("first", "second"), parsed.stream().map(DataProductConfig::getIdentifier).toList());
  }

  @Test
  void nullAndUnsupportedValuesProduceNoProducts() {
    assertTrue(DataProductConfigLoader.parseProducts(null).isEmpty());
    assertTrue(DataProductConfigLoader.parseProducts("not-a-product").isEmpty());
  }
}