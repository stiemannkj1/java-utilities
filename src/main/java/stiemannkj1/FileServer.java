/**
 * Copyright 2026 Kyle Stiemann
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package stiemannkj1;

import static java.net.HttpURLConnection.HTTP_BAD_METHOD;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.Executors;

/**
 * FileServer.java is a Java file server and receiver which depends solely on JDK 11+.
 *
 * <pre>{@code
 * # Download via jshell:
 * printf 'import java.net.http.*;HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("https://raw.githubusercontent.com/stiemannkj1/java-utilities/refs/heads/master/src/main/java/stiemannkj1/FileServer.java")).build(),HttpResponse.BodyHandlers.ofFile(Path.of(System.getProperty("user.home")+"/Downloads/FileServer.java")));' | jshell -
 * # Or download via curl:
 * # curl -o ~/Downloads/FileServer.java 'https://raw.githubusercontent.com/stiemannkj1/java-utilities/refs/heads/master/src/main/java/stiemannkj1/FileServer.java'
 * # Run from source:
 * java ~/Downloads/FileServer.java
 * }</pre>
 */
public final class FileServer {

  private static final int DEFAULT_PORT = 8080;
  private static final String USAGE =
      "\nStandalone file server with upload and download endpoints.\n\n"
          + "--port|-p\n"
          + "\tThe port to serve files on. Defaults to "
          + DEFAULT_PORT
          + ".\n\n"
          + "--location|-l\n"
          + "\tThe directory to server files from and upload files to. Defaults to the working directory of the process.\n\n"
          + "--help|-h\n"
          + "\tPrint this usage information.\n\n"
          + "Example usage:\n\n"
          + "# Start server from JAR:\n"
          + "java -jar stiemannkj1.jar stiemannkj1.FileServer\n"
          + "# Start server from source:\n"
          + "java stiemannkj1/FileServer.java\n"
          + "# Download file:\n"
          + "curl -o filename.txt http://localhost:"
          + DEFAULT_PORT
          + "/download/filename.txt\n"
          + "# Upload file:\n"
          + "curl -T filename.txt http://localhost:"
          + DEFAULT_PORT
          + "/upload/filename.txt\n";

  public static void main(String[] args) {
    System.exit(startFileServer(args));
  }

  private static int startFileServer(String[] args) {
    int port = DEFAULT_PORT;
    File location = new File(System.getProperty("user.dir")).getAbsoluteFile();

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--port":
          // fallthrough;
        case "-p":
          i++;

          if (i >= args.length) {
            System.err.printf("ERROR: Port value is required with %s.\n", args[i - 1]);
            System.err.printf(USAGE);
            return 1;
          }

          String portString = args[i];
          try {
            port = Integer.parseInt(portString);
          } catch (NumberFormatException e) {
            System.err.printf(
                "ERROR: Failed to parse %s as an integer port.\n%s", portString, USAGE);
            return 1;
          }

          break;
        case "--location":
          // fallthrough;
        case "-l":
          i++;

          if (i >= args.length) {
            System.err.printf("ERROR: Location value is required with %s.\n", args[i - 1]);
            System.err.printf(USAGE);
            return 1;
          }

          location = new File(args[i]).getAbsoluteFile();

          break;
        case "--help":
          // fallthrough;
        case "-h":
          System.out.printf(USAGE);
          return 0;
        default:
          System.err.printf("ERROR: Unexpected argument: %s.\n%s", args[i], USAGE);
          return 1;
      }
    }

    location = location.toPath().normalize().toAbsolutePath().toFile();

    if (!location.exists()) {
      System.err.printf("ERROR: %s does not exist.\n%s", location.getAbsolutePath(), USAGE);
      return 1;
    }

    if (!location.isDirectory()) {
      System.err.printf("ERROR: %s is not a directory.\n%s", location.getAbsolutePath(), USAGE);
      return 1;
    }

    if (!location.canRead() || !location.canExecute()) {
      System.err.printf("ERROR: %s is not accessible.\n%s", location.getAbsolutePath(), USAGE);
      return 1;
    }

    boolean writable = location.canWrite();

    HttpServer server;

    try {
      server = HttpServer.create(new InetSocketAddress(port), 0);
    } catch (IOException e) {
      e.printStackTrace(System.err);
      return 1;
    }

    Path locationPath = location.toPath();

    if (writable) {
      server.createContext(UploadHandler.PATH, new UploadHandler(locationPath));
    } else {
      System.err.printf(
          "WARN: %s is not writeable. Disabling %s functionality.\n",
          UploadHandler.PATH, location.getAbsolutePath());
    }

    server.createContext(DownloadHandler.PATH, new DownloadHandler(locationPath));

    server.setExecutor(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));

    System.out.printf(
        "Serving and receiving files under \"%s\" at the following URLs:\n", locationPath);
    System.out.printf("\nhttp://localhost:%d/download/\n", port);
    System.out.printf("http://localhost:%d/upload/\n", port);

    Enumeration<NetworkInterface> ifaces = Collections.emptyEnumeration();

    try {
      ifaces = NetworkInterface.getNetworkInterfaces();
    } catch (SocketException e) {
      System.err.printf("WARN: Failed to list all available IP addresses.\n");
      e.printStackTrace(System.err);
    }

    while (ifaces.hasMoreElements()) {
      NetworkInterface iface = ifaces.nextElement();

      boolean printUrl = false;

      try {
        printUrl = iface.isUp() && !iface.isLoopback();
      } catch (SocketException e) {
        // ignore
      }

      if (!printUrl) {
        continue;
      }

      Enumeration<InetAddress> addrsIter = iface.getInetAddresses();

      while (addrsIter.hasMoreElements()) {
        InetAddress addr = addrsIter.nextElement();

        boolean ipv6 = addr instanceof Inet6Address;

        if (!(addr instanceof Inet4Address || ipv6)) {
          continue;
        }

        String addrStr = addr.getHostAddress();

        if (ipv6) {
          if (addrStr.contains("%")) {
            continue;
          }

          System.out.printf("\nhttp://[%s]:%d/download/\n", addrStr, port);
          System.out.printf("http://[%s]:%d/upload/\n", addrStr, port);
        } else {
          System.out.printf("\nhttp://%s:%d/download/\n", addrStr, port);
          System.out.printf("http://%s:%d/upload/\n", addrStr, port);
        }
      }
    }

    System.out.printf("\n");

    server.start();

    try {
      Thread.currentThread().join();
    } catch (InterruptedException e) {
      // ignore
    }
    return 0;
  }

  public static final class DownloadHandler implements HttpHandler {

    public static final String PATH = "/download/";
    public static final String METHOD = "GET";

    private final Path location;

    public DownloadHandler(Path location) {
      this.location = location;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

      ThreadUnsafeStorage threadLocals = THREAD_LOCALS.get();

      String remoteAddr = exchange.getRemoteAddress().getAddress().getHostAddress();

      if (!METHOD.equals(exchange.getRequestMethod())) {
        sendErrorResponse(
            exchange,
            HTTP_BAD_METHOD,
            remoteAddr,
            threadLocals
                .sb()
                .append(exchange.getRequestMethod())
                .append(" not allowed for: ")
                .append(PATH)
                .append(". Use ")
                .append(METHOD)
                .append(".\n")
                .toString());
        return;
      }

      Path toDownload =
          Paths.get(stripPrefix(exchange.getRequestURI().getPath(), PATH))
              .toAbsolutePath()
              .normalize();

      System.out.printf(
          threadLocals
              .sb()
              .append("Serving \"")
              .append(toDownload)
              .append("\" to ")
              .append(remoteAddr)
              .append(".\n")
              .toString());

      // Prevent path traversal outside the server's context dir.
      if (!toDownload.startsWith(location)) {
        sendErrorResponse(
            exchange,
            HTTP_BAD_REQUEST,
            remoteAddr,
            threadLocals
                .sb()
                .append("Requested file \"")
                .append(toDownload)
                .append("\" is outside FileServer's context directory of \"")
                .append(location)
                .append("\".\n")
                .toString());
        return;
      }

      if (!Files.exists(toDownload) || Files.isDirectory(toDownload)) {
        sendErrorResponse(
            exchange,
            HTTP_NOT_FOUND,
            remoteAddr,
            threadLocals
                .sb()
                .append("File not found: \"")
                .append(toDownload)
                .append("\".\n")
                .toString());
        return;
      }

      exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
      exchange
          .getResponseHeaders()
          .set(
              "Content-Disposition",
              threadLocals
                  .sb()
                  .append("attachment; filename=\"")
                  .append(toDownload.toFile().getName())
                  .append("\"")
                  .toString());
      exchange.sendResponseHeaders(200, Files.size(toDownload));

      try (OutputStream os = exchange.getResponseBody();
          InputStream is = Files.newInputStream(toDownload)) {
        copy(is, os, threadLocals.buffer());
      }

      System.out.printf(
          threadLocals
              .sb()
              .append("Successfully served \"")
              .append(toDownload)
              .append("\" to ")
              .append(remoteAddr)
              .append(".\n")
              .toString());
      // TODO trailer header of sha512 after reading the file.
    }
  }

  public static final class UploadHandler implements HttpHandler {

    public static final String PATH = "/upload/";
    public static final String METHOD = "PUT";

    private final Path location;

    public UploadHandler(Path location) {
      this.location = location;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

      ThreadUnsafeStorage threadLocals = THREAD_LOCALS.get();

      String remoteAddr = exchange.getRemoteAddress().getAddress().getHostAddress();

      if (!METHOD.equals(exchange.getRequestMethod())) {
        sendErrorResponse(
            exchange,
            HTTP_BAD_METHOD,
            remoteAddr,
            threadLocals
                .sb()
                .append(exchange.getRequestMethod())
                .append(" not allowed for: ")
                .append(PATH)
                .append(". Use ")
                .append(METHOD)
                .append(".\n")
                .toString());
        return;
      }

      Path toUpload =
          Paths.get(stripPrefix(exchange.getRequestURI().getPath(), PATH))
              .toAbsolutePath()
              .normalize();

      System.out.printf(
          threadLocals
              .sb()
              .append("Receiving \"")
              .append(toUpload)
              .append("\" from ")
              .append(remoteAddr)
              .append(".\n")
              .toString());

      // Prevent path traversal outside the server's context dir.
      if (!toUpload.startsWith(location)) {
        sendErrorResponse(
            exchange,
            HTTP_BAD_REQUEST,
            remoteAddr,
            threadLocals
                .sb()
                .append("Requested upload location \"")
                .append(toUpload)
                .append("\" is outside FileServer's context directory of \"")
                .append(location)
                .append("\".\n")
                .toString());
        return;
      }

      if (Files.exists(toUpload)) {
        sendErrorResponse(
            exchange,
            HTTP_CONFLICT,
            remoteAddr,
            threadLocals
                .sb()
                .append("\"")
                .append(toUpload)
                .append("\" already exists.\n")
                .toString());
        return;
      }

      try (InputStream is = exchange.getRequestBody();
          OutputStream os = Files.newOutputStream(toUpload)) {
        copy(is, os, threadLocals.buffer());
      }

      // TODO check optional sha512 after uploading file (and delete if it doesn't match).
      sendResponse(
          exchange,
          HTTP_CREATED,
          threadLocals
              .sb()
              .append("Successfully uploaded file to: \"")
              .append(toUpload)
              .append("\".\n")
              .toString());

      System.out.printf(
          threadLocals
              .sb()
              .append("Successfully received \"")
              .append(toUpload)
              .append("\" from ")
              .append(remoteAddr)
              .append(".\n")
              .toString());
    }
  }

  private static void sendResponse(HttpExchange exchange, int code, String message)
      throws IOException {
    byte[] response = message.getBytes();
    exchange.sendResponseHeaders(code, response.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(response);
    }
  }

  private static void sendErrorResponse(
      HttpExchange exchange, int code, String remoteAddr, String message) throws IOException {

    if (199 < code && code < 300) {
      throw new AssertionError(code + " is not an HTTP error code.");
    }

    sendResponse(exchange, code, message);
    System.err.printf("WARN: request from %s failed. %s", remoteAddr, message);
  }

  private static String stripPrefix(String str, String prefix) {

    if (str.startsWith(prefix)) {
      return str.substring(prefix.length());
    }

    return str;
  }

  private static long copy(InputStream is, OutputStream os, byte[] buffer) throws IOException {

    long total = 0;
    int read;

    while ((read = is.read(buffer)) >= 0) {
      os.write(buffer, 0, read);
      total += read;
    }

    return total;
  }

  private static final class ThreadUnsafeStorage {
    private StringBuilder sb;
    private byte[] buffer;

    private StringBuilder sb() {

      if (sb == null) {
        sb = new StringBuilder();
      }

      sb.setLength(0);
      return sb;
    }

    private byte[] buffer() {

      if (buffer == null) {
        buffer = new byte[8 * 1024];
      }

      return buffer;
    }
  }

  private static final ThreadLocal<ThreadUnsafeStorage> THREAD_LOCALS =
      ThreadLocal.withInitial(ThreadUnsafeStorage::new);
}
