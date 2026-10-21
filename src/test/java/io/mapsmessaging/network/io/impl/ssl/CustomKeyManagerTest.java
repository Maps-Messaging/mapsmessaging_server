package io.mapsmessaging.network.io.impl.ssl;

import org.junit.jupiter.api.Test;

import javax.net.ssl.X509ExtendedKeyManager;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomKeyManagerTest {

  @Test
  void configuredAliasIsAlwaysSelectedForClientAndServer() {
    X509ExtendedKeyManager delegate = mock(X509ExtendedKeyManager.class);
    CustomKeyManager manager = new CustomKeyManager(delegate, "maps-key");

    assertEquals("maps-key", manager.chooseClientAlias(new String[]{"RSA"}, null, null));
    assertEquals("maps-key", manager.chooseServerAlias("RSA", null, null));
    assertEquals("maps-key", manager.chooseEngineClientAlias(new String[]{"RSA"}, null, null));
    assertEquals("maps-key", manager.chooseEngineServerAlias("RSA", null, null));
    assertArrayEquals(new String[]{"maps-key"}, manager.getServerAliases("RSA", null));
    assertArrayEquals(new String[0], manager.getClientAliases("RSA", null));
  }

  @Test
  void certificateChainAndPrivateKeyDelegateUsingRequestedAlias() {
    X509ExtendedKeyManager delegate = mock(X509ExtendedKeyManager.class);
    X509Certificate certificate = mock(X509Certificate.class);
    PrivateKey key = mock(PrivateKey.class);
    when(delegate.getCertificateChain("maps-key"))
        .thenReturn(new X509Certificate[]{certificate});
    when(delegate.getPrivateKey("maps-key")).thenReturn(key);

    CustomKeyManager manager = new CustomKeyManager(delegate, "selected");

    assertArrayEquals(
        new X509Certificate[]{certificate},
        manager.getCertificateChain("maps-key")
    );
    assertSame(key, manager.getPrivateKey("maps-key"));
  }
}
