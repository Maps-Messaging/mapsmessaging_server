/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.consul.ecwid;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.ConsulRawClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import io.mapsmessaging.consul.ConsulServerApi;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.rest.RestApiServerManager;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.mapsmessaging.logging.ServerLogMessages.CONSUL_CLIENT_LOG;
import static io.mapsmessaging.logging.ServerLogMessages.CONSUL_KEY_VALUE_MANAGER;

public class EcwidConsulManager extends ConsulServerApi {

  private final Logger logger = LoggerFactory.getLogger(EcwidConsulManager.class);

  private final ConsulClient client;

  public EcwidConsulManager(String name) throws IOException {
    super(name);
    try {
      logger.log(CONSUL_CLIENT_LOG, "Creating client", consulConfiguration);
      client = createClient();
      logger.log(CONSUL_CLIENT_LOG, "Created client", consulConfiguration);
      logger.log(ServerLogMessages.CONSUL_STARTUP);
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  private ConsulClient createClient() throws MalformedURLException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    List<Header> defaultHeaders = new ArrayList<>();
    if (consulConfiguration.getConsulToken() != null) {
      defaultHeaders.add(new BasicHeader("X-Consul-Token", consulConfiguration.getConsulToken()));
    }

    // Trust all certificates - NOT recommended for production
    SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, (certificate, authType) -> true).build();

    HttpClient httpClient = HttpClients.custom()
        .setDefaultHeaders(defaultHeaders)
        .setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE))
        .build();

    URL url = new URL(consulConfiguration.getConsulUrl());
    int port = url.getPort();

    if (port == -1) {
      if (url.getProtocol().equalsIgnoreCase("https")) {
        port = 443;
      } else {
        port = 8500;
      }
    }
    ConsulRawClient rawClient = new ConsulRawClient(url.getProtocol() + "://" + url.getHost(), port, httpClient);

    return new ConsulClient(rawClient);
  }

  @Override
  public void stop() {
    super.stop();
  }

  @Override
  protected void pingService() {

  }

  @Override
  public void register(Map<String, String> meta) {

  }

  @Override
  public void register(EndPointServer endPointServer) {

  }

  @Override
  public void register(RestApiServerManager restApiServerManager) {

  }

  @Override
  public List<String> getKeys(String key) throws IOException {
    String keyName = validateKey(key);
    logger.log(CONSUL_KEY_VALUE_MANAGER, "getKeys", keyName, "");
    Response<List<String>> response = client.getKVKeysOnly(keyName);
    List<String> list = response.getValue();
    if (list == null) {
      list = new ArrayList<>();
    }
    logger.log(CONSUL_KEY_VALUE_MANAGER, "getKeys", keyName, list);
    return list;
  }

  @Override
  public String getValue(String key) throws IOException {
    String keyName = validateKey(key);
    logger.log(CONSUL_KEY_VALUE_MANAGER, "GetValues", keyName, "");
    Response<GetValue> response = client.getKVValue(keyName);
    GetValue getValue = response.getValue();
    String value = getValue.getDecodedValue();
    logger.log(CONSUL_KEY_VALUE_MANAGER, "GetValues", keyName, value);
    return value;
  }

  @Override
  public void putValue(String key, String value) {
    String keyName = validateKey(key);
    logger.log(CONSUL_KEY_VALUE_MANAGER, "putValue", keyName, value);
    client.setKVValue(keyName, value);
  }

  @Override
  public void deleteKey(String key) {
    String keyName = validateKey(key);
    logger.log(CONSUL_KEY_VALUE_MANAGER, "deleteKey", keyName, "");
    client.deleteKVValue(keyName);
  }
}
