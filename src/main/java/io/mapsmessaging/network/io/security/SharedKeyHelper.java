package io.mapsmessaging.network.io.security;

import java.io.ByteArrayOutputStream;
import java.util.StringTokenizer;

public class SharedKeyHelper {

  public static byte[] convertKey(String key) {
    key = key.trim();
    if (key.startsWith("0x")) {
      return fromHex(key);
    }
    return key.getBytes();
  }

  private static byte[] fromHex(String hexStr) {
    StringTokenizer st = new StringTokenizer(hexStr, ",");
    ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
    while (st.hasMoreElements()) {
      String val = (String) st.nextElement();
      val = val.trim();
      int v = 0;
      if (val.startsWith("0x")) {
        v = Integer.parseInt(val.substring(2), 16);
      } else {
        v = Integer.parseInt(val, 10);
      }
      baos.write(v);
    }
    return baos.toByteArray();
  }

  private SharedKeyHelper() {
  }
}
