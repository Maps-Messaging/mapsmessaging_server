/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 */

package io.mapsmessaging.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DownloadScriptsHttpsTest {
  @TempDir static Path certificateDirectory;
  @TempDir Path workingDirectory;
  static Path certificate;
  static SSLContext sslContext;
  static String curl;

  @BeforeAll
  static void prepare_https_certificate() throws Exception {
    curl = command("bash", "-c", "command -v curl").trim();
    Path keytool = Path.of(System.getProperty("java.home"), "bin", "keytool");
    Path keystorePath = certificateDirectory.resolve("server.p12");
    certificate = certificateDirectory.resolve("server.pem");
    command(keytool.toString(), "-genkeypair", "-alias", "server", "-keyalg", "RSA",
        "-keystore", keystorePath.toString(), "-storepass", "changeit", "-dname", "CN=localhost",
        "-ext", "SAN=dns:localhost", "-validity", "1", "-storetype", "PKCS12");
    command(keytool.toString(), "-exportcert", "-rfc", "-alias", "server", "-keystore",
        keystorePath.toString(), "-storepass", "changeit", "-file", certificate.toString());
    KeyStore keystore = KeyStore.getInstance("PKCS12");
    try (var input = Files.newInputStream(keystorePath)) {
      keystore.load(input, "changeit".toCharArray());
    }
    KeyManagerFactory keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagers.init(keystore, "changeit".toCharArray());
    sslContext = SSLContext.getInstance("TLS");
    sslContext.init(keyManagers.getKeyManagers(), null, null);
  }

  @ParameterizedTest
  @CsvSource({"metadata,https", "extension,https", "smile,https", "pack,https",
      "metadata,http", "extension,http", "smile,http", "pack,http",
      "metadata,ftp", "extension,ftp", "smile,ftp", "pack,ftp",
      "smile,404", "smile,500"})
  void downloads_follow_https_redirects_but_reject_other_protocols(String location, String scheme)
      throws Exception {
    AtomicInteger insecureRequests = new AtomicInteger();
    AtomicInteger securePayloads = new AtomicInteger();
    HttpServer httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    httpServer.createContext("/", exchange -> {
      insecureRequests.incrementAndGet();
      byte[] content = payload(exchange.getRequestURI().getPath());
      exchange.sendResponseHeaders(200, content.length);
      try (var output = exchange.getResponseBody()) { output.write(content); }
    });
    HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress("localhost", 0), 0);
    httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
    String origin = "https://localhost:" + httpsServer.getAddress().getPort();
    httpsServer.createContext("/", exchange -> {
      String path = exchange.getRequestURI().getPath();
      if (path.startsWith("/redirect/")) {
        boolean targeted = location.equals("metadata") == path.endsWith(".xml");
        String destination = origin;
        if (targeted && scheme.equals("http")) {
          destination = "http://localhost:" + httpServer.getAddress().getPort();
        } else if (targeted && scheme.equals("ftp")) {
          destination = "ftp://localhost:" + httpServer.getAddress().getPort();
        }
        exchange.getResponseHeaders().add("Location", destination + "/payload/" + path.substring(10));
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
      } else {
        securePayloads.incrementAndGet();
        boolean httpError = scheme.equals("404") || scheme.equals("500");
        byte[] content = httpError ? "<html>Download failed</html>".getBytes(StandardCharsets.UTF_8) : payload(path);
        exchange.sendResponseHeaders(httpError ? Integer.parseInt(scheme) : 200, content.length);
        try (var output = exchange.getResponseBody()) { output.write(content); }
      }
    });
    httpServer.start();
    httpsServer.start();
    try {
      Path bin = Files.createDirectory(workingDirectory.resolve("bin"));
      // Only substitute the endpoint and CA; the real curl evaluates production protocol options.
      String wrapper = """
          #!/bin/bash
          args=()
          for argument in "$@"; do
            if [[ "$argument" == https://* ]]; then
              name="${argument##*/}"
              name="${name//%/percent}"
              args+=("$TEST_ORIGIN/redirect/$name")
            else
              args+=("$argument")
            fi
          done
          if [[ "${0##*/}" == wget ]]; then
            wget_binary=$(PATH="$TEST_SYSTEM_PATH" command -v wget)
            "$wget_binary" --no-proxy --ca-certificate="$TEST_CERTIFICATE" --timeout=1 --tries=1 "${args[@]}"
          else
            "$TEST_CURL" --noproxy '*' --cacert "$TEST_CERTIFICATE" --max-time 1 --retry-max-time 1 "${args[@]}"
          fi
          result=$?
          echo "$result" >> "$TEST_RESULTS"
          exit "$result"
          """;
      executable(bin.resolve("curl"), wrapper);
      executable(bin.resolve("wget"), wrapper);
      executable(bin.resolve("pack"), "#!/bin/bash\necho invoked >> \"$TEST_PACK_MARKER\"\n");
      executable(bin.resolve("docker"), "#!/bin/bash\nexit 0\n");
      Files.writeString(workingDirectory.resolve("pom.xml"), "<version>1.2.3</version>\n");
      String script = switch (location) {
        case "metadata", "extension" -> "download-extensions.sh";
        case "smile" -> "download-smile.sh";
        default -> "pack.sh";
      };
      Path results = workingDirectory.resolve("results.txt");
      ProcessBuilder builder = new ProcessBuilder("bash",
          Path.of("src/main/scripts", script).toAbsolutePath().toString());
      builder.directory(workingDirectory.toFile());
      builder.redirectErrorStream(true);
      builder.redirectOutput(workingDirectory.resolve("output.txt").toFile());
      builder.environment().put("PATH", bin + ":" + System.getenv("PATH"));
      builder.environment().put("MAPS_HOME", workingDirectory.toString());
      builder.environment().put("TEST_ORIGIN", origin);
      builder.environment().put("TEST_CURL", curl);
      builder.environment().put("TEST_SYSTEM_PATH", System.getenv("PATH"));
      builder.environment().put("TEST_CERTIFICATE", certificate.toString());
      builder.environment().put("TEST_RESULTS", results.toString());
      Path packMarker = workingDirectory.resolve("pack-invoked.txt");
      builder.environment().put("TEST_PACK_MARKER", packMarker.toString());
      Process process = builder.start();
      try {
        assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Download script timed out");
        String output = Files.readString(workingDirectory.resolve("output.txt"));
        var statuses = Files.readAllLines(results);
        if (scheme.equals("https")) {
          assertEquals(0, process.exitValue(), output);
          assertTrue(statuses.stream().allMatch("0"::equals), output);
          assertTrue(securePayloads.get() > 0, "HTTPS redirect must reach the artifact");
          if (location.equals("extension") || location.equals("metadata")) {
            assertEquals("jar payload", Files.readString(workingDirectory.resolve("aws-sns-extension-1.0.0-SNAPSHOT.jar")));
          } else if (location.equals("smile")) {
            assertEquals("jar payload", Files.readString(workingDirectory.resolve("lib/smile-core-4.3.0.jar")));
          }
        } else {
          String expectedCurlStatus = scheme.equals("404") || scheme.equals("500") ? "22" : "1";
          assertTrue(statuses.stream().anyMatch(expectedCurlStatus::equals),
              "Unsafe redirect or HTTP error must fail in the downloader: " + output);
          assertEquals(0, insecureRequests.get(), "Redirect must not contact HTTP destination");
          assertTrue(process.exitValue() != 0, "Script must propagate download failure: " + output);
          assertFalse(Files.exists(packMarker), "Failed download must not invoke pack on a stale jar");
          assertFalse(output.contains("All Smile JARs downloaded"), "Failed download must not report success");
          if (scheme.equals("404") || scheme.equals("500")) {
            assertFalse(Files.exists(workingDirectory.resolve("lib/smile-core-4.3.0.jar")),
                "HTTP error response must not be saved as a jar");
          }
        }
      } finally {
        process.destroyForcibly();
      }
    } finally {
      httpsServer.stop(0);
      httpServer.stop(0);
    }
  }

  private static byte[] payload(String path) {
    String content = path.endsWith(".xml")
        ? "<metadata><snapshotVersion><extension>jar</extension><value>1.0.0-20261013.000000-1</value></snapshotVersion></metadata>"
        : "jar payload";
    return content.getBytes(StandardCharsets.UTF_8);
  }

  private static void executable(Path file, String content) throws Exception {
    Files.writeString(file, content);
    assertTrue(file.toFile().setExecutable(true));
  }

  private static String command(String... arguments) throws Exception {
    Process process = new ProcessBuilder(arguments).redirectErrorStream(true).start();
    try {
      assertTrue(process.waitFor(20, TimeUnit.SECONDS), "Tool timed out");
      String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      assertEquals(0, process.exitValue(), output);
      return output;
    } finally {
      process.destroyForcibly();
    }
  }
}
