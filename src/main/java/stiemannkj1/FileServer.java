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
import static stiemannkj1.FileServer.FileHandlerType.DOWNLOAD;
import static stiemannkj1.FileServer.FileHandlerType.UPLOAD;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
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
import java.util.Objects;
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
    PrintStream out = System.out;
    PrintStream err = System.err;
    int port = DEFAULT_PORT;
    File location = new File(System.getProperty("user.dir")).getAbsoluteFile();

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--port":
          // fallthrough;
        case "-p":
          i++;

          if (i >= args.length) {
            err.printf("ERROR: Port value is required with %s.\n", args[i - 1]);
            err.printf(USAGE);
            return 1;
          }

          String portString = args[i];
          try {
            port = Integer.parseInt(portString);
          } catch (NumberFormatException e) {
            err.printf("ERROR: Failed to parse %s as an integer port.\n%s", portString, USAGE);
            return 1;
          }

          break;
        case "--location":
          // fallthrough;
        case "-l":
          i++;

          if (i >= args.length) {
            err.printf("ERROR: Location value is required with %s.\n", args[i - 1]);
            err.printf(USAGE);
            return 1;
          }

          location = new File(args[i]).getAbsoluteFile();

          break;
        case "--help":
          // fallthrough;
        case "-h":
          out.printf(USAGE);
          return 0;
        default:
          err.printf("ERROR: Unexpected argument: %s.\n%s", args[i], USAGE);
          return 1;
      }
    }

    HttpServer server =
        startFileServer(port, location, Runtime.getRuntime().availableProcessors() * 2, out, err);

    if (server == null) {
      return 1;
    }

    try {
      Thread.currentThread().join();
    } catch (InterruptedException e) {
      // ignore
    }

    server.stop(0);
    return 0;
  }

  /**
   * Starts a file server serving and receiving files at the specified directory and port.
   *
   * @param port the port to use for the server or 0 for an ephemeral port
   * @param location the file system location to serve files from
   * @param threadPoolSize the number of threads to use for the server
   * @param out the std {@link PrintStream}
   * @param err the stderr {@link PrintStream}
   * @return the server or null if the server failed to start
   */
  public static HttpServer startFileServer(
      int port, File location, int threadPoolSize, PrintStream out, PrintStream err) {

    if (port < 0) {
      err.printf("ERROR: port must be greater than or equal to 0, but was: %d\n%s", port, USAGE);
      return null;
    }

    if (threadPoolSize <= 0) {
      err.printf("ERROR: threadPoolSize must be greater 0, but was: %d\n%s", threadPoolSize, USAGE);
      return null;
    }

    location = location.toPath().normalize().toAbsolutePath().toFile();

    if (!location.exists()) {
      err.printf("ERROR: %s does not exist.\n%s", location.getAbsolutePath(), USAGE);
      return null;
    }

    if (!location.isDirectory()) {
      err.printf("ERROR: %s is not a directory.\n%s", location.getAbsolutePath(), USAGE);
      return null;
    }

    if (!location.canRead() || !location.canExecute()) {
      err.printf("ERROR: %s is not accessible.\n%s", location.getAbsolutePath(), USAGE);
      return null;
    }

    boolean writable = location.canWrite();

    HttpServer server;

    try {
      InetSocketAddress addr = new InetSocketAddress(port);
      server = HttpServer.create(addr, 0);

      if (port == 0) {
        port = addr.getPort();
      }
    } catch (IOException e) {
      e.printStackTrace(err);
      return null;
    }

    String uploadPath = "/upload/";
    Path locationPath = location.toPath();

    if (writable) {
      FileHandler.addEndpoint(server, UPLOAD, uploadPath, "PUT", locationPath, out, err);
    } else {
      err.printf(
          "WARN: %s is not writeable. Disabling %s functionality.\n",
          location.getAbsolutePath(), UPLOAD);
    }

    String downloadPath = "/download/";
    FileHandler.addEndpoint(server, DOWNLOAD, downloadPath, "GET", locationPath, out, err);

    server.setExecutor(Executors.newFixedThreadPool(threadPoolSize));

    out.printf("Serving and receiving files under \"%s\" at the following URLs:\n", location);
    out.printf("\nhttp://localhost:%d%s\n", port, downloadPath);
    out.printf("http://localhost:%d%s\n", port, uploadPath);

    Enumeration<NetworkInterface> ifaces = Collections.emptyEnumeration();

    try {
      ifaces = NetworkInterface.getNetworkInterfaces();
    } catch (SocketException e) {
      err.printf("WARN: Failed to list all available IP addresses.\n");
      e.printStackTrace(err);
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

          out.printf("\nhttp://[%s]:%d%s\n", addrStr, port, downloadPath);
          out.printf("http://[%s]:%d%s\n", addrStr, port, uploadPath);
        } else {
          out.printf("\nhttp://%s:%d%s\n", addrStr, port, downloadPath);
          out.printf("http://%s:%d%s\n", addrStr, port, uploadPath);
        }
      }
    }

    out.printf("\n");

    server.start();

    return server;
  }

  public enum FileHandlerType {
    DOWNLOAD,
    UPLOAD
  }

  public static class FileHandler implements HttpHandler {

    private final FileHandlerType type;
    private final String endpoint;
    private final String method;
    private final Path location;
    private final PrintStream out;
    private final PrintStream err;

    private FileHandler(
        FileHandlerType type,
        String endpoint,
        String method,
        Path location,
        PrintStream out,
        PrintStream err) {
      this.type = Objects.requireNonNull(type);
      this.endpoint = Objects.requireNonNull(endpoint);
      this.method = Objects.requireNonNull(method);
      this.location = Objects.requireNonNull(location);
      this.out = Objects.requireNonNull(out);
      this.err = Objects.requireNonNull(err);
    }

    public static HttpContext addEndpoint(
        HttpServer server,
        FileHandlerType type,
        String endpoint,
        String method,
        Path location,
        PrintStream out,
        PrintStream err) {
      return server.createContext(
          endpoint, new FileHandler(type, endpoint, method, location, out, err));
    }

    private void handleFileDownload(HttpExchange exchange) throws IOException {

      ThreadUnsafeStorage threadLocals = THREAD_LOCALS.get();

      String remoteAddr = exchange.getRemoteAddress().getAddress().getHostAddress();

      if (!method.equals(exchange.getRequestMethod())) {
        sendErrorResponse(
            exchange,
            HTTP_BAD_METHOD,
            remoteAddr,
            threadLocals
                .sb()
                .append(exchange.getRequestMethod())
                .append(" not allowed for: ")
                .append(endpoint)
                .append(". Use ")
                .append(method)
                .append(".\n")
                .toString());
        return;
      }

      Path toDownload =
          Paths.get(stripPrefix(exchange.getRequestURI().getPath(), endpoint))
              .toAbsolutePath()
              .normalize();

      out.printf(
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

      out.printf(
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

    private void handleFileUpload(HttpExchange exchange) throws IOException {

      ThreadUnsafeStorage threadLocals = THREAD_LOCALS.get();

      String remoteAddr = exchange.getRemoteAddress().getAddress().getHostAddress();

      if (!method.equals(exchange.getRequestMethod())) {
        sendErrorResponse(
            exchange,
            HTTP_BAD_METHOD,
            remoteAddr,
            threadLocals
                .sb()
                .append(exchange.getRequestMethod())
                .append(" not allowed for: ")
                .append(endpoint)
                .append(". Use ")
                .append(method)
                .append(".\n")
                .toString());
        return;
      }

      Path toUpload =
          Paths.get(stripPrefix(exchange.getRequestURI().getPath(), endpoint))
              .toAbsolutePath()
              .normalize();

      out.printf(
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

      out.printf(
          threadLocals
              .sb()
              .append("Successfully received \"")
              .append(toUpload)
              .append("\" from ")
              .append(remoteAddr)
              .append(".\n")
              .toString());
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      switch (type) {
        case DOWNLOAD:
          handleFileDownload(exchange);
          return;
        case UPLOAD:
          handleFileUpload(exchange);
          return;
        default:
          throw new IllegalArgumentException(
              THREAD_LOCALS.get().sb().append("Unexpected type: ").append(type).toString());
      }
    }

    private void sendErrorResponse(
        HttpExchange exchange, int code, String remoteAddr, String message) throws IOException {

      if (199 < code && code < 300) {
        throw new AssertionError(code + " is not an HTTP error code.");
      }

      sendResponse(exchange, code, message);
      err.printf("WARN: request from %s failed. %s", remoteAddr, message);
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

    public StringBuilder sb() {

      if (sb == null) {
        sb = new StringBuilder();
      }

      sb.setLength(0);
      return sb;
    }

    public byte[] buffer() {

      if (buffer == null) {
        buffer = new byte[8 * 1024];
      }

      return buffer;
    }
  }

  private static final ThreadLocal<ThreadUnsafeStorage> THREAD_LOCALS =
      ThreadLocal.withInitial(ThreadUnsafeStorage::new);

  private FileServer() {}
}
