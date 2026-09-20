package io.mapsmessaging.network.discovery;

import io.mapsmessaging.network.monitor.NetworkInterfaceMonitor;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NetworkAddressHelperTest {

  @Test
  void hostnameListIsTrimmedAndLoopbackAddressesAreExcluded() throws Exception {
    NetworkInterfaceMonitor monitor = mock(NetworkInterfaceMonitor.class);
    InetAddress loopback = InetAddress.getByName("127.0.0.1");
    when(monitor.getIpAddressByName("alpha")).thenReturn(List.of(loopback));
    when(monitor.getIpAddressByName("beta")).thenReturn(List.of(loopback));

    try (MockedStatic<NetworkInterfaceMonitor> mocked =
             mockStatic(NetworkInterfaceMonitor.class)) {
      mocked.when(NetworkInterfaceMonitor::getInstance).thenReturn(monitor);

      assertTrue(new NetworkAddressHelper().getAddresses(" alpha , beta ").isEmpty());
    }

    verify(monitor).getIpAddressByName("alpha");
    verify(monitor).getIpAddressByName("beta");
  }

  @Test
  void nullHostnameUsesCurrentAddressSnapshot() throws Exception {
    NetworkInterfaceMonitor monitor = mock(NetworkInterfaceMonitor.class);
    when(monitor.getCurrentIpAddresses())
        .thenReturn(List.of(InetAddress.getByName("127.0.0.1")));

    try (MockedStatic<NetworkInterfaceMonitor> mocked =
             mockStatic(NetworkInterfaceMonitor.class)) {
      mocked.when(NetworkInterfaceMonitor::getInstance).thenReturn(monitor);

      assertTrue(new NetworkAddressHelper().getAddresses(null).isEmpty());
    }

    verify(monitor).getCurrentIpAddresses();
  }

  @Test
  void ipv6ClassificationRejectsSiteLocalAndAcceptsGlobalAddress() throws Exception {
    NetworkAddressHelper helper = new NetworkAddressHelper();
    Method method = NetworkAddressHelper.class.getDeclaredMethod(
        "isGloballyRoutableIPv6Address", InetAddress.class);
    method.setAccessible(true);

    Inet6Address global = (Inet6Address) InetAddress.getByName("2001:db8::1");
    Inet6Address siteLocal = (Inet6Address) InetAddress.getByName("fec0::1");

    assertEquals(true, method.invoke(helper, global));
    assertEquals(false, method.invoke(helper, siteLocal));
    assertEquals(false, method.invoke(helper, InetAddress.getByName("192.0.2.1")));
  }
}
