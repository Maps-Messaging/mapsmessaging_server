/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.coap;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.eclipse.californium.elements.util.SslContextUtil;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuiteSelector;
import org.eclipse.californium.scandium.dtls.cipher.DefaultCipherSuiteSelector;
import org.eclipse.californium.scandium.dtls.x509.SingleCertificateProvider;
import org.eclipse.californium.scandium.dtls.x509.StaticNewAdvancedCertificateVerifier;

public class DtlsTest {

  private static final Configuration.DefinitionsProvider DEFAULTS = new Configuration.DefinitionsProvider() {

    @Override
    public void applyDefinitions(Configuration config) {
      config.set(DtlsConfig.DTLS_CONNECTION_ID_LENGTH, 0);
      config.set(DtlsConfig.DTLS_RECEIVER_THREAD_COUNT, 2);
      config.set(DtlsConfig.DTLS_CONNECTOR_THREAD_COUNT, 2);
    }

  };

  public static void main(String[] args) throws GeneralSecurityException, IOException {
    Configuration configuration = Configuration.createWithFile(Configuration.DEFAULT_FILE, "DTLS example client", DEFAULTS);
    DtlsConnectorConfig.Builder config = new DtlsConnectorConfig.Builder(configuration);
    config.setAddress(new InetSocketAddress(0)); // Use ephemeral port

    DtlsConnectorConfig.Builder builder = DtlsConnectorConfig.builder(configuration);

    SslContextUtil.Credentials clientCredentials = SslContextUtil.loadCredentials("my-keystore.jks", "server", "password".toCharArray(), "password".toCharArray());
    Certificate[] trustedCertificates = SslContextUtil.loadTrustedCertificates("my-truststore.jks", "ca", "password".toCharArray());

    builder.setCertificateIdentityProvider(new SingleCertificateProvider(clientCredentials.getPrivateKey(), clientCredentials.getCertificateChain(),
        CertificateType.RAW_PUBLIC_KEY, CertificateType.X_509));
    builder.setAdvancedCertificateVerifier(StaticNewAdvancedCertificateVerifier.builder()
        .setTrustedCertificates(trustedCertificates).setTrustAllRPKs().build());

    CipherSuiteSelector selector = new DefaultCipherSuiteSelector();
    builder.setCipherSuiteSelector(selector);
    // Initialize DTLS connector
    DTLSConnector dtlsConnector = new DTLSConnector(builder.build());

    // Create CoAP endpoint with DTLS connector
    CoapEndpoint.Builder endpointBuilder = new CoapEndpoint.Builder();
    endpointBuilder.setConnector(dtlsConnector);
    CoapEndpoint coapEndpoint = endpointBuilder.build();

    // Create CoAP client
    CoapClient client = new CoapClient("coap://localhost:5684/resource");
    client.setEndpoint(coapEndpoint);

    try {
      // Perform a CoAP request
      for (int x = 0; x < 100; x++) {
        System.err.println(client.put(("this is a payload" + x).getBytes(), 0).getResponseText());
        Thread.sleep(1000);
        System.err.println(client.get().getResponseText());
        Thread.sleep(1000);
      }
    } catch (ConnectorException | IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}

