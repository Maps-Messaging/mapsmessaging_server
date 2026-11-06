package io.mapsmessaging.network.discovery;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NetworkAddressHelperBranchCoverageTest {

  @Test
  void deprecatedClassifierOnlyAppliesToIpv6AndRecognizesMarkers() throws Exception {
    NetworkAddressHelper helper = new NetworkAddressHelper();
    Method method = NetworkAddressHelper.class.getDeclaredMethod(
        "isDeprecatedIPv6Address", InetAddress.class);
    method.setAccessible(true);

    assertEquals(false, method.invoke(helper, InetAddress.getByName("192.0.2.1")));

    Inet6Address deprecated = mock(Inet6Address.class);
    when(deprecated.getHostAddress()).thenReturn("2001:db8::1%deprecated");
    assertEquals(true, method.invoke(helper, deprecated));

    Inet6Address temporary = mock(Inet6Address.class);
    when(temporary.getHostAddress()).thenReturn("2001:db8::2%temporary");
    assertEquals(true, method.invoke(helper, temporary));

    Inet6Address normal = mock(Inet6Address.class);
    when(normal.getHostAddress()).thenReturn("2001:db8::3");
    assertEquals(false, method.invoke(helper, normal));
  }

  @Test
  void primaryAddressSelectionReturnsNullForEmptyCandidateList() throws Exception {
    NetworkAddressHelper helper = new NetworkAddressHelper();
    Method method = NetworkAddressHelper.class.getDeclaredMethod(
        "selectPrimaryAddress", List.class);
    method.setAccessible(true);

    assertNull(method.invoke(helper, List.of()));
  }
}