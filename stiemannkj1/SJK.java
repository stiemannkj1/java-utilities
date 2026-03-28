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
import static stiemannkj1.SJK.Check.isBlank;
import static stiemannkj1.SJK.Check.isEmpty;
import static stiemannkj1.SJK.Check.isNotNull;
import static stiemannkj1.SJK.FileServer.FileHandler.Type.DOWNLOAD;
import static stiemannkj1.SJK.FileServer.FileHandler.Type.UPLOAD;
import static stiemannkj1.SJK.IO.DEFAULT_ATOMIC_ATTEMPTS;
import static stiemannkj1.SJK.IO.DEFAULT_BUF_SIZE;
import static stiemannkj1.SJK.IO.DEFAULT_MAX_FOLLOW_SYMLINKS;
import static stiemannkj1.SJK.IO.Deleter.DELETER;
import static stiemannkj1.SJK.IO.Operation.CREATE_DIR;
import static stiemannkj1.SJK.IO.Operation.CREATE_FILE;
import static stiemannkj1.SJK.IO.Operation.RENAME_TO;
import static stiemannkj1.SJK.IO.copy;
import static stiemannkj1.SJK.IO.deleteRecursively;
import static stiemannkj1.SJK.IO.handleNonRegularFiles;
import static stiemannkj1.SJK.IO.mkdirs;
import static stiemannkj1.SJK.IO.write;
import static stiemannkj1.SJK.Strings.decapitalize;
import static stiemannkj1.SJK.Strings.stripPrefix;
import static stiemannkj1.SJK.ThreadUnsafeStorage.THREAD_LOCALS;
import static stiemannkj1.SJK.net.downloadFile;
import static stiemannkj1.SJK.net.isOk;
import static stiemannkj1.SJK.net.uploadFile;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import stiemannkj1.SJK.IO.AtomicFile;

/**
 * SJK is a "Software Joy Kit" for Java. It is a single Java file which depends solely on JDK 8 to
 * supplement the JDK.
 *
 * <pre>{@code
 * # Download via jshell:
 * printf 'import java.net.http.*;HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("https://raw.githubusercontent.com/stiemannkj1/java-utilities/refs/heads/master/stiemannkj1/SJK.java")).build(),HttpResponse.BodyHandlers.ofFile(Path.of(System.getProperty("user.home")+"/Downloads/SJK.java")));' | jshell -
 * # Or download via curl:
 * # curl -o ~/Downloads/SJK.java 'https://raw.githubusercontent.com/stiemannkj1/java-utilities/refs/heads/master/stiemannkj1/SJK.java'
 * # Run from source:
 * java ~/Downloads/SJK.java
 * }</pre>
 */
public final class SJK {

  public static final String JAVA_HOME;
  public static final String JAVA_EXE;
  public static final int JAVA_MAJOR_VERSION;
  public static final boolean JAVA_9_AND_UP;

  private static final String SJK_DEBUG_PORT = "SJK_DEBUG_PORT";
  private static final String SJK_DEBUG_PORT_USAGE =
      SJK_DEBUG_PORT
          + "\n"
          + "\tThe port to use for remote debugging when this command starts a child Java"
          + " process.\n\n";

  static {
    JAVA_HOME = require.nonBlank(System.getProperty("java.home"));
    String javaBin = JAVA_HOME + File.separator + "bin" + File.separator + "java";

    if (System.getProperty("os.name").toLowerCase().contains("win")) {
      javaBin += ".exe";
    }

    JAVA_EXE = javaBin;

    String javaSpecVersion = System.getProperty("java.specification.version");
    String legacyJavaVersion = "1.";

    if (javaSpecVersion.startsWith(legacyJavaVersion)) {
      javaSpecVersion = javaSpecVersion.substring(legacyJavaVersion.length());
    }

    int javaMajorVersion;

    try {
      javaMajorVersion = Integer.parseInt(javaSpecVersion);
    } catch (NumberFormatException e) {
      javaMajorVersion = 8;
    }

    JAVA_MAJOR_VERSION = javaMajorVersion;
    JAVA_9_AND_UP = JAVA_MAJOR_VERSION > 8;
  }

  /** Throws a checked {@link Exception} as if it were a {@link RuntimeException}. */
  @SuppressWarnings("unchecked")
  public static <E extends Throwable> E sneaky(Throwable e) throws E {
    throw (E) e;
  }

  public static <T extends Enum<T>> IllegalStateException unhandledCase(T unhandledEnumValue) {
    throw new IllegalStateException("Unexpected case: " + unhandledEnumValue.name());
  }

  public static <C extends Collection<String>> C addJavaDebugArgIfSpecified(
      C args, boolean suspend) {

    String debugPort = System.getenv(SJK_DEBUG_PORT);

    if (isEmpty(debugPort)) {
      return args;
    }

    String debugArg = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=";

    if (suspend) {
      debugArg += "y";
    } else {
      debugArg += "n";
    }

    debugArg += ",address=";

    if (JAVA_9_AND_UP) {
      debugArg += "*:";
    }

    debugArg += debugPort;
    args.add(debugArg);

    return args;
  }

  public static void main(String[] args) {
    System.exit(runMainClass(args));
  }

  private static int runMainClass(String[] args) {

    Map<String, Method> tools = new TreeMap<>();

    for (Class<?> nested : SJK.class.getDeclaredClasses()) {
      try {
        Method method = nested.getDeclaredMethod("main", String[].class);

        if ((method.getModifiers() & Modifier.STATIC) == 0
            || !void.class.equals(method.getReturnType())) {
          continue;
        }

        tools.put(decapitalize(nested.getSimpleName()), method);
      } catch (ReflectiveOperationException e) {
        // no_op;
      }
    }

    String requestedTool = null;
    Method mainMethod = null;

    if (args.length >= 1) {
      requestedTool = args[0];
      mainMethod = tools.get(requestedTool);
    }

    if (mainMethod == null) {

      StringBuilder sb = new StringBuilder();

      if (requestedTool != null) {
        sb.append("Invalid tool requested: ").append(requestedTool).append("\n\n");
      }

      sb.append("Provide one of the following tools as the first argument:\n\n");

      for (String tool : tools.keySet()) {
        sb.append("\t").append(tool).append("\n");
      }

      System.out.println(sb);

      return requestedTool != null ? 1 : 0;
    }

    String[] newArgs = new String[args.length - 1];
    System.arraycopy(args, 1, newArgs, 0, newArgs.length);

    try {
      mainMethod.invoke(null, (Object) newArgs);
    } catch (InvocationTargetException e) {
      throw sneaky(e.getCause());
    } catch (ReflectiveOperationException e) {
      throw sneaky(e);
    }

    return 0;
  }

  public static final class flags {
    public static boolean printIfMissingValue(
        String flagTitle, int i, String[] args, String usage, PrintStream err) {

      if (i + 1 < args.length || isBlank(args[i + 1])) {
        return false;
      }

      err.printf("ERROR: %s value is required with %s.\n", flagTitle, args[i]);
      err.printf(usage);
      return true;
    }

    private flags() {}
  }

  public static final class Strings {
    public static String stripPrefix(String str, String prefix) {

      if (str.startsWith(prefix)) {
        return str.substring(prefix.length());
      }

      return str;
    }

    public static String decapitalize(String str) {
      if (isEmpty(str)) {
        return str;
      }

      return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    public static int parseInt(String possibleInt, int defaultValue) {
      try {
        return Integer.parseInt(possibleInt);
      } catch (NumberFormatException e) {
        return defaultValue;
      }
    }

    public static Builder builder() {
      return new Builder();
    }

    public static final class Builder {
      private final StringBuilder sb;

      private Builder() {
        this.sb = new StringBuilder();
      }

      public StringBuilder get() {
        return sb;
      }

      public StringBuilder reset() {
        sb.setLength(0);
        return sb;
      }
    }

    private Strings() {}
  }

  public static final class Check {

    public static boolean isNotNull(Object obj) {
      return obj != null;
    }

    public static boolean isNull(Object obj) {
      return obj == null;
    }

    public static <T> boolean isEmpty(T[] array) {
      return isNull(array) || array.length == 0;
    }

    public static <T> boolean isEmpty(Collection<T> collection) {
      return isNull(collection) || collection.isEmpty();
    }

    public static <T> boolean isEmpty(Iterator<T> iter) {
      return isNull(iter) || !iter.hasNext();
    }

    public static <K, V> boolean isEmpty(Map<K, V> map) {
      return isNull(map) || map.isEmpty();
    }

    public static <T extends CharSequence> boolean isEmpty(T str) {
      return isNull(str) || str.length() == 0;
    }

    public static boolean isBlank(String str) {
      return isEmpty(str) || str.isBlank();
    }

    private Check() {}
  }

  public static final class require {

    public static boolean isTrue(boolean condition) {
      if (!condition) {
        throw new IllegalStateException("condition must be true");
      }

      return condition;
    }

    public static <T> T isNull(T t) {
      if (isNotNull(t)) {
        throw new IllegalStateException("value must be null");
      }

      return t;
    }

    public static <T> T nonNull(T t) {
      if (Check.isNull(t)) {
        throw new IllegalStateException("value must not be null");
      }

      return t;
    }

    public static <T extends CharSequence> T nonEmpty(T str) {
      if (nonNull(str).length() == 0) {
        throw new IllegalStateException("string must not be empty");
      }

      return str;
    }

    public static String nonBlank(String str) {
      if (nonEmpty(str).isBlank()) {
        throw new IllegalStateException("string must not be blank");
      }

      return str;
    }

    public static <T> T[] nonEmpty(T[] array) {
      if (nonNull(array).length == 0) {
        throw new IllegalStateException("array must not be empty");
      }

      return array;
    }

    public static <T> Collection<T> nonEmpty(Collection<T> collection) {
      if (nonNull(collection).isEmpty()) {
        throw new IllegalStateException("collection must not be empty");
      }

      return collection;
    }

    public static <T> Iterator<T> nonEmpty(Iterator<T> iter) {
      if (!nonNull(iter).hasNext()) {
        throw new IllegalStateException("iterator must not be empty");
      }

      return iter;
    }

    public static <K, V> Map<K, V> nonEmpty(Map<K, V> map) {
      if (nonNull(map).isEmpty()) {
        throw new IllegalStateException("map must not be empty");
      }

      return map;
    }

    public static int positive(int value) {
      if (value <= 0) {
        throw new IllegalStateException("value must be positive but was " + value);
      }

      return value;
    }

    public static File dir(File possibleDir) {
      if (!possibleDir.isDirectory()) {
        throw new IllegalStateException(
            "file " + possibleDir.getAbsolutePath() + " must be a directory");
      }

      return possibleDir;
    }

    public static Path dir(Path possibleDir) {
      dir(possibleDir.toFile());
      return possibleDir;
    }

    public static File file(File possibleFile) {
      if (!possibleFile.isFile()) {
        throw new IllegalStateException(
            "file " + possibleFile.getAbsolutePath() + " must be a file");
      }

      return possibleFile;
    }

    public static Path file(Path possibleFile) {
      dir(possibleFile.toFile());
      return possibleFile;
    }

    public static File exists(File file) {
      if (!file.exists()) {
        throw new IllegalStateException("file " + file.getAbsolutePath() + " must exist");
      }

      return file;
    }

    public static Path exists(Path path) {
      exists(path.toFile());
      return path;
    }

    private require() {}
  }

  public static final class BusyWait {

    private static final long DEFAULT_SLEEP_MS = 250;

    public static boolean untilCondition(BooleanSupplier condition, long timeoutMs, long sleepMs)
        throws InterruptedException {

      boolean result;
      long startNs = System.nanoTime();
      long timeoutNs = TimeUnit.MILLISECONDS.toNanos(timeoutMs);

      while (!(result = condition.getAsBoolean()) || (System.nanoTime() - startNs) > timeoutNs) {
        Thread.sleep(sleepMs);
      }

      return result;
    }

    public static boolean until(BooleanSupplier condition, long timeoutMs, long sleepMs) {
      try {
        return untilCondition(condition, timeoutMs, sleepMs);
      } catch (InterruptedException e) {
        throw sneaky(e);
      }
    }

    public static boolean until(BooleanSupplier condition, long timeoutMs) {
      return until(condition, timeoutMs, DEFAULT_SLEEP_MS);
    }

    public static boolean until(BooleanSupplier condition, Duration timeout, Duration sleep) {
      return until(condition, timeout.toMillis(), sleep.toMillis());
    }

    public static boolean until(BooleanSupplier condition, Duration timeout) {
      return until(condition, timeout.toMillis(), DEFAULT_SLEEP_MS);
    }

    private BusyWait() {}
  }

  public static final class Testing {

    private static final AtomicBoolean isTesting = new AtomicBoolean(false);

    public static boolean isTesting() {
      return isTesting.get();
    }

    private final AssertionError failures;
    private final StringBuilder sb;

    public Testing() {
      this.failures = new AssertionError();
      this.sb = new StringBuilder();
      isTesting.set(true);
    }

    public StringBuilder sb() {
      sb.setLength(0);
      return sb;
    }

    public void fail(String message) {
      failures.addSuppressed(new AssertionError(message));
    }

    public void assertTrue(boolean bool, String message) {
      if (bool) {
        return;
      }

      failures.addSuppressed(new AssertionError(message));
    }

    public void assertTrue(boolean bool) {
      assertTrue(bool, "Expected true, but was false.");
    }

    public void assertEquals(Object o1, Object o2) {

      if (Objects.equals(o1, o2)) {
        return;
      }

      failures.addSuppressed(
          new AssertionError(
              sb().append("Expected: <[\n")
                  .append(o1)
                  .append("\n]> but was <[\n")
                  .append(o2)
                  .append("\n]>")));
    }

    public void assertNotEquals(Object o1, Object o2) {

      if (!Objects.equals(o1, o2)) {
        return;
      }

      failures.addSuppressed(
          new AssertionError(
              sb().append("Expected not equals: <[\n")
                  .append(o1)
                  .append("\n]> but was <[\n")
                  .append(o2)
                  .append("\n]>")));
    }

    public void throwIfFailed() {
      if (failures.getSuppressed().length > 0) {
        throw failures;
      }

      System.out.println("Tests passed.");
    }
  }

  /**
   * IO and file operation utilities.
   *
   * <p>Many file utilities have a {@code atomicAttempts} parameter. This indicates that the method
   * can perform the action atomically. If a value greater than 0 is passed, the method will attempt
   * to create a temporary uniquely/randomly named file or directory a maximum of {@code
   * atomicAttempts} times. If the method succeeds in creating the temporary file or directory, it
   * will perform the requested operation on that temporary file or directory and then atomically
   * rename that file or directory to the original intended name. Atomic operations prevent code
   * from leaving the filesystem in an inconsistent state. File writes, recursive deletions,
   * recursive copies, and more can fail in the middle of operations leaving partially written
   * files, partially deleted directories, partially copied directories, or other problematic
   * states. If those inconsistent states can harm the system or other processes or programs, atomic
   * operations should be used.
   */
  public static final class IO {
    public static final int DEFAULT_BUF_SIZE = 8 * 1024;
    public static final int DEFAULT_MAX_FOLLOW_SYMLINKS = 1;
    public static final int DEFAULT_ATOMIC_ATTEMPTS = 16;

    public static final class SimpleError extends IOException {
      public SimpleError(String message) {
        super(message);
      }

      public SimpleError(String message, Throwable cause) {
        super(message, cause);
      }

      public SimpleError(Throwable cause) {
        super(cause);
      }

      @Override
      public synchronized Throwable fillInStackTrace() {
        // Avoid costly stacktrace creation.
        return this;
      }
    }

    public static long copy(InputStream is, OutputStream os, byte[] buf) throws IOException {

      long total = 0;
      int read;

      while ((read = is.read(buf)) >= 0) {
        os.write(buf, 0, read);
        total += read;
      }

      return total;
    }

    public static long copy(Path file, OutputStream os, byte[] buf) throws IOException {
      return copy(file.toFile(), os, buf);
    }

    public static long copy(File file, OutputStream os, byte[] buf) throws IOException {
      try (InputStream is = new FileInputStream(file)) {
        return copy(is, os, buf);
      }
    }

    public static long copy(
        CharSequence str,
        CharsetEncoder encoder,
        int off,
        int len,
        WritableByteChannel channel,
        ByteBuffer buf)
        throws IOException {

      require.isTrue(buf.capacity() >= 4);

      long total = 0;
      encoder.reset();

      CharBuffer chars = CharBuffer.wrap(str);
      chars.position(off);
      chars.limit(len);

      while (chars.hasRemaining()) {

        CoderResult result = encoder.encode(chars, buf, true);

        if (result.isError()) {
          result.throwException();
        }

        buf.reset();
        total += channel.write(buf);
      }

      CoderResult result = encoder.flush(buf);

      if (result.isError()) {
        result.throwException();
      }

      return total;
    }

    public static long write(
        CharSequence str,
        CharsetEncoder encoder,
        int off,
        int len,
        File file,
        ByteBuffer buf,
        int atomicAttempts)
        throws IOException {
      try (AtomicFile atomicFile = AtomicFile.orStandardFile(CREATE_FILE, file, atomicAttempts);
          FileChannel channel =
              FileChannel.open(atomicFile.file.toPath(), StandardOpenOption.WRITE)) {
        return copy(str, encoder, off, len, channel, buf);
      }
    }

    public static long write(byte[] buf, int off, int len, File file, int atomicAttempts)
        throws IOException {
      try (AtomicFile atomicFile = AtomicFile.orStandardFile(CREATE_FILE, file, atomicAttempts);
          OutputStream os = new FileOutputStream(atomicFile.file)) {
        os.write(buf, off, len);
      }

      return len - off;
    }

    public static long write(InputStream is, File file, byte[] buf, int atomicAttempts)
        throws IOException {
      try (AtomicFile atomicFile = AtomicFile.orStandardFile(CREATE_FILE, file, atomicAttempts);
          OutputStream os = new FileOutputStream(atomicFile.file)) {
        return copy(is, os, buf);
      }
    }

    public static final class AtomicFile implements Closeable {

      public final File file;
      private final File finalFile;

      private static AtomicFile orStandardFile(Operation op, File file, int atomicAttempts)
          throws IOException {
        if (atomicAttempts > 0) {
          return create(op, file, atomicAttempts);
        }

        return new AtomicFile(file, null);
      }

      public static AtomicFile createFile(File file, int atomicAttempts) throws IOException {
        return create(CREATE_FILE, file, atomicAttempts);
      }

      public static AtomicFile createDir(File file, int atomicAttempts) throws IOException {
        return create(CREATE_DIR, file, atomicAttempts);
      }

      private static AtomicFile create(Operation op, File file, int atomicAttempts)
          throws IOException {

        require.isTrue(op == CREATE_FILE || op == CREATE_DIR);

        return new AtomicFile(
            createTempFile(file.getParentFile(), file.getName(), atomicAttempts).toFile(),
            require.nonNull(file));
      }

      private AtomicFile(File file, File finalFile) {
        this.file = require.nonNull(file);
        this.finalFile = finalFile; // Allow null.
      }

      @Override
      public void close() throws IOException {
        if (finalFile != null) {
          Files.move(file.toPath(), finalFile.toPath());
        }
      }
    }

    static final class Deleter extends SimpleFileVisitor<Path> {

      static final Deleter DELETER = new Deleter();

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException ioe) throws IOException {

        try {
          Files.delete(dir);
        } catch (IOException e) {
          if (ioe != null) {
            e.addSuppressed(ioe);
          }
          throw e;
        }

        if (ioe != null) {
          throw ioe;
        }

        return FileVisitResult.CONTINUE;
      }
    }

    public static boolean deleteRecursively(Path dir, int atomicAttempts) {
      try {
        Path path =
            atomicAttempts > 0
                ? randomFile(RENAME_TO, dir, dir.toFile().getName(), atomicAttempts)
                : dir;
        Files.walkFileTree(path, DELETER);
        return true;
      } catch (IOException e) {
        return false;
      }
    }

    private static final class Copier extends SimpleFileVisitor<Path> {

      private final Path src;
      private final Path dst;

      private Copier(Path src, Path dst) {
        this.src = src;
        this.dst = dst;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Path relative = src.relativize(file);
        Files.copy(file, dst.resolve(relative));
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
          throws IOException {
        return visitFile(dir, attrs);
      }
    }

    public static boolean copyRecursively(Path src, Path dst, int atomicAttempts) {
      try (AtomicFile atomicDir =
          AtomicFile.orStandardFile(CREATE_DIR, dst.toFile(), atomicAttempts)) {
        Files.walkFileTree(src, new Copier(src, atomicDir.file.toPath()));
        return true;
      } catch (IOException e) {
        return false;
      }
    }

    enum Operation {
      RENAME_TO,
      CREATE_DIR,
      CREATE_FILE
    }

    public static Path createTempFile(Path parent, String prefix, int attempts) throws IOException {
      return randomFile(CREATE_FILE, parent, prefix, attempts);
    }

    public static Path createTempFile(File parent, String prefix, int attempts) throws IOException {
      return randomFile(CREATE_FILE, parent, prefix, attempts);
    }

    public static Path createTempDir(Path parent, String prefix, int attempts) throws IOException {
      return randomFile(CREATE_DIR, parent, prefix, attempts);
    }

    public static Path createTempDir(File parent, String prefix, int attempts) throws IOException {
      return randomFile(CREATE_DIR, parent, prefix, attempts);
    }

    private static Path randomFile(Operation op, Path sourceOrParent, String prefix, int attempts)
        throws IOException {
      return randomFile(op, sourceOrParent.toFile(), prefix, attempts);
    }

    private static Path randomFile(Operation op, File sourceOrParent, String prefix, int attempts)
        throws IOException {

      require.nonNull(op);
      require.positive(attempts);
      require.nonNull(sourceOrParent);
      require.nonEmpty(prefix);

      int idx;

      if ((idx = prefix.indexOf(File.separator)) > -1) {
        throw new IllegalStateException(
            "Prefix \""
                + prefix
                + "\" contained file separator \""
                + File.separator
                + "\" at: "
                + idx);
      }

      Strings.Builder sb = new Strings.Builder();

      for (int i = 0; i < attempts; i++) {
        try {
          String fileName =
              sb.reset()
                  .append(".")
                  .append(prefix)
                  .append("_")
                  .append(UUID.randomUUID())
                  .toString();
          Path path;
          switch (op) {
            case RENAME_TO:
              path = new File(sourceOrParent.getParent(), fileName).toPath();
              Files.move(sourceOrParent.toPath(), path);
              return path;
            case CREATE_DIR:
              path = new File(sourceOrParent, fileName).toPath();
              Files.createDirectory(new File(sourceOrParent, fileName).toPath());
              return path;
            case CREATE_FILE:
              path = new File(sourceOrParent, fileName).toPath();
              Files.createFile(new File(sourceOrParent, fileName).toPath());
              return path;
            default:
              throw unhandledCase(op);
          }
        } catch (FileAlreadyExistsException ignored) {
        }
      }

      return null;
    }

    public static File mkdirs(File parent, String children) {
      File dir = new File(parent, children);
      boolean ignored = dir.mkdirs();
      return dir.getAbsoluteFile();
    }

    public static Path followSymbolicLinks(Path file, int maxFollows) throws IOException {

      require.positive(maxFollows);

      Path initialFile = file;

      int i = 0;
      for (; Files.isSymbolicLink(file) && i < maxFollows; i++) {
        file = Files.readSymbolicLink(file);
      }

      if (i >= maxFollows) {
        throw new IO.SimpleError(
            "Failed to follow all symbolic links from "
                + initialFile.toAbsolutePath()
                + " path referenced more than "
                + maxFollows
                + " links.");
      }

      return file;
    }

    static Path handleNonRegularFiles(
        Path path,
        BasicFileAttributes attrs,
        Path requiredParent,
        boolean ignoreNonRegularFiles,
        boolean allowExternalSymlinks,
        int maxFollowSymLinks)
        throws IOException {

      if (attrs.isSymbolicLink()) {

        if (ignoreNonRegularFiles) {
          return null;
        }

        path = followSymbolicLinks(path, maxFollowSymLinks);

        if (!allowExternalSymlinks && !path.startsWith(requiredParent)) {
          throw new IO.SimpleError(
              "Symlink "
                  + path.toFile().getAbsolutePath()
                  + " targets file outside of required parent dir "
                  + requiredParent.toFile().getAbsolutePath());
        }
      }

      if (!attrs.isRegularFile() && ignoreNonRegularFiles) {
        return null;
      }

      return path;
    }

    private IO() {}
  }

  public static final class Javac {

    private static final String USAGE =
        "\nTool for compiling .java files using javac.\n\n"
            + "Example usage:\n\n"
            + "java SJK.java javac source/ output/\n"
            + "\nFlags:\n"
            + "--classpath|-cp\n"
            + "\tThe optional compile classpath to use when compiling the sources.\n\n"
            + "--help|-h\n"
            + "\tPrint this usage information.\n\n";

    public static void main(String[] args) {
      System.exit(compile(args));
    }

    public static int compile(String[] args) {
      PrintStream out = System.out;
      PrintStream err = System.err;
      File sourceDir = null;
      File outputDir = null;
      String classpath = null;

      for (int i = 0; i < args.length; i++) {
        switch (args[i]) {
          case "--help":
          // fallthrough;
          case "-h":
            out.printf(USAGE);
            return 0;
          case "--classpath":
          // fallthrough;
          case "-cp":
            if (flags.printIfMissingValue("Classpath", i, args, USAGE, err)) {
              return 1;
            }

            i++;

            classpath = args[i];

            break;
          default:
            if (sourceDir == null) {
              sourceDir = new File(args[i]);
            } else if (outputDir == null) {
              outputDir = new File(args[i]);
            } else {
              err.printf("ERROR: Unexpected argument: %s.\n%s", args[i], USAGE);
              return 1;
            }
        }
      }

      if (sourceDir == null) {
        err.printf("ERROR: source directory is required.\n%s", USAGE);
        return 1;
      }

      if (outputDir == null) {
        err.printf("ERROR: output directory is required.\n%s", USAGE);
        return 1;
      }

      try {
        compile(sourceDir, outputDir, classpath, false, false, 1, err);
      } catch (IOException e) {
        e.printStackTrace(err);
        return 1;
      }

      return 0;
    }

    public static void compile(
        File dirToCompile,
        File outputDir,
        List<String> classpath,
        boolean ignoreNonRegularFiles,
        boolean allowExternalSymlinks,
        int maxFollowSymLinks,
        PrintStream err)
        throws IOException {
      compile(
          dirToCompile,
          outputDir,
          String.join(File.pathSeparator, classpath),
          ignoreNonRegularFiles,
          allowExternalSymlinks,
          maxFollowSymLinks,
          err);
    }

    private static void compile(
        File dirToCompileFile,
        File outputDirFile,
        String classpath,
        boolean ignoreNonRegularFiles,
        boolean allowExternalSymlinks,
        int maxFollowSymLinks,
        PrintStream err)
        throws IOException {

      List<File> javaFiles = new ArrayList<>();
      Path sourceDir = require.dir(dirToCompileFile).getAbsoluteFile().toPath();
      Path outputDir = require.dir(outputDirFile).getAbsoluteFile().toPath();

      SimpleFileVisitor<Path> visitor =
          new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                throws IOException {

              if ((dir =
                      handleNonRegularFiles(
                          dir,
                          attrs,
                          sourceDir,
                          ignoreNonRegularFiles,
                          allowExternalSymlinks,
                          maxFollowSymLinks))
                  == null) {
                return FileVisitResult.CONTINUE;
              }

              Path targetDir = outputDir.resolve(sourceDir.relativize(dir));

              if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
              }

              if (!Files.isDirectory(targetDir)) {
                throw new IO.SimpleError(targetDir + " should be a directory but is not.");
              }

              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {

              if ((file =
                      handleNonRegularFiles(
                          file,
                          attrs,
                          sourceDir,
                          ignoreNonRegularFiles,
                          allowExternalSymlinks,
                          maxFollowSymLinks))
                  == null) {
                return FileVisitResult.CONTINUE;
              }

              if (file.toString().endsWith(".java")) {
                javaFiles.add(file.toFile());
              } else {
                Path targetFile = outputDir.resolve(sourceDir.relativize(file));
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
              }

              return FileVisitResult.CONTINUE;
            }
          };

      Files.walkFileTree(sourceDir, visitor);

      if (javaFiles.isEmpty()) {
        err.printf(
            "WARN: no .java source files found. Non .java files have been copied to" + " %s.\n",
            outputDir);
        return;
      }

      boolean succeeded = false;
      JavaCompiler compiler = require.nonNull(ToolProvider.getSystemJavaCompiler());

      DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

      try (StandardJavaFileManager fm = compiler.getStandardFileManager(diagnostics, null, null)) {
        Iterable<? extends JavaFileObject> units = fm.getJavaFileObjectsFromFiles(javaFiles);

        List<String> args = new ArrayList<>();
        args.add("-sourcepath");
        args.add(sourceDir.toString());
        args.add("-d");
        args.add(outputDir.toString());

        if (classpath != null) {
          args.add("-cp");
          args.add(classpath);
        }

        if (compiler.getTask(null, fm, diagnostics, args, null, units).call()) {
          succeeded = true;
        }
      }

      for (Diagnostic<? extends JavaFileObject> diag : diagnostics.getDiagnostics()) {
        err.format(
            "[%s] Line %d in %s: %s\n",
            diag.getKind(),
            diag.getLineNumber(),
            diag.getSource().getName(),
            diag.getMessage(null));
      }

      if (succeeded) {
        // TODO perform atomic copy to make full compilation appear atomic.
        return;
      }

      throw new IO.SimpleError("Failed to compile files. See diagnostics.");
    }

    private Javac() {}
  }

  public static final class Packager {

    private static final String USAGE =
        "\nPackaging tool for different archive types.\n\n"
            + "Example usage:\n\n"
            + "java SJK.java packager --zip source/ output.zip\n"
            + "\nFlags:\n"
            + "--zip|-z\n"
            + "\tArchives the source directory into a zip file.\n\n"
            + "--help|-h\n"
            + "\tPrint this usage information.\n\n";

    public static void main(final String[] args) {
      System.exit(packageUp(args));
    }

    public enum Type {
      ZIP
    }

    private static int packageUp(String[] args) {
      PrintStream out = System.out;
      PrintStream err = System.err;
      File sourceDir = null;
      File outputArchive = null;

      Type type = null;

      for (int i = 0; i < args.length; i++) {
        switch (args[i]) {
          case "--zip":
          // fallthrough;
          case "-z":
            type = Type.ZIP;
            break;
          case "--help":
          // fallthrough;
          case "-h":
            out.printf(USAGE);
            return 0;
          default:
            if (sourceDir == null) {
              sourceDir = new File(args[i]);
            } else if (outputArchive == null) {
              outputArchive = new File(args[i]);
            } else {
              err.printf("ERROR: Unexpected argument: %s.\n%s", args[i], USAGE);
              return 1;
            }
        }
      }

      if (type == null) {
        err.printf("ERROR: Package type is required.\n%s", USAGE);
        return 1;
      }

      if (sourceDir == null) {
        err.printf("ERROR: source directory is required.\n%s", USAGE);
        return 1;
      }

      if (outputArchive == null) {
        err.printf("ERROR: output archive is required.\n%s", USAGE);
        return 1;
      }

      try {
        switch (type) {
          case ZIP:
            zip(sourceDir, outputArchive);
            return 0;
          // TODO case TAR:
          // TODO case TAR_GZ:
          default:
            throw unhandledCase(type);
        }
      } catch (final IOException e) {
        throw sneaky(e);
      }
    }

    public static Path zip(File dirToZip, File outputZipFile) throws IOException {
      return zip(
          dirToZip,
          outputZipFile,
          false,
          false,
          DEFAULT_MAX_FOLLOW_SYMLINKS,
          DEFAULT_ATOMIC_ATTEMPTS);
    }

    public static Path zip(
        File dirToZip,
        File outputZipFile,
        boolean ignoreNonRegularFiles,
        boolean allowExternalSymlinks,
        int maxFollowSymLinks,
        int atomicAttempts)
        throws IOException {

      Path sourceDir = require.dir(require.nonNull(dirToZip).toPath()).toAbsolutePath();
      Path zipFilePath = require.nonNull(outputZipFile).toPath().toAbsolutePath();
      File zipFile = zipFilePath.toFile();
      zipFilePath = zipFilePath.toFile().exists() ? require.file(zipFilePath) : zipFilePath;
      byte[] buf = new byte[8 * 1024 * 1024];
      Strings.Builder sb = new Strings.Builder();

      try (AtomicFile zip = AtomicFile.orStandardFile(CREATE_FILE, zipFile, atomicAttempts);
          OutputStream fos = new FileOutputStream(zip.file, false);
          ZipOutputStream zos = new ZipOutputStream(fos)) {

        zos.setLevel(Deflater.BEST_SPEED);

        SimpleFileVisitor<Path> zipper =
            new SimpleFileVisitor<>() {
              @Override
              public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                  throws IOException {

                if ((file =
                        handleNonRegularFiles(
                            file,
                            attrs,
                            sourceDir,
                            ignoreNonRegularFiles,
                            allowExternalSymlinks,
                            maxFollowSymLinks))
                    == null) {
                  return FileVisitResult.CONTINUE;
                }

                Path relativePath = sourceDir.relativize(file);
                ZipEntry zipEntry = new ZipEntry(relativePath.toString());
                zos.putNextEntry(zipEntry);
                copy(file, zos, buf);
                zos.closeEntry();

                return FileVisitResult.CONTINUE;
              }

              @Override
              public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                  throws IOException {

                if (sourceDir.equals(dir)) {
                  return FileVisitResult.CONTINUE;
                }

                if ((dir =
                        handleNonRegularFiles(
                            dir,
                            attrs,
                            sourceDir,
                            ignoreNonRegularFiles,
                            allowExternalSymlinks,
                            maxFollowSymLinks))
                    == null) {
                  return FileVisitResult.CONTINUE;
                }

                Path relativePath = sourceDir.relativize(dir);
                ZipEntry zipEntry =
                    new ZipEntry(sb.reset().append(relativePath).append("/").toString());
                zos.putNextEntry(zipEntry);
                zos.closeEntry();

                return FileVisitResult.CONTINUE;
              }
            };
        Files.walkFileTree(sourceDir, zipper);
      }

      return zipFilePath;
    }

    private Packager() {}
  }

  public static final class ContentType {
    public static final String HEADER = "Content-Type";
    public static final String OCTET_STREAM = "application/octet-stream";
    public static final String TEXT = "text/plain";

    private ContentType() {}
  }

  public static final class net {

    private static final String USAGE =
        "\n"
            + "HTTP client tool for uploading/downloading files.\n\n"
            + "Example usage:\n\n"
            + "# Download file:\n"
            + "java SJK.java net --download https://stiemannkj1.gitlab.com/output.txt output.txt\n"
            + "# Upload file:\n"
            + "java SJK.java net --upload source.txt https://stiemannkj1.gitlab.com/source.txt\n"
            + "\n"
            + "Flags:\n"
            + "--download|-d\n"
            + "\tDownloads a file from the provided URL.\n\n"
            + "--upload|-u\n"
            + "\tUploads the source file to the provided URL.\n\n"
            + "--help|-h\n"
            + "\tPrint this usage information.\n\n";

    public static void main(final String[] args) {
      System.exit(cli(args));
    }

    public enum Type {
      DOWNLOAD,
      UPLOAD
    }

    private static int cli(String[] args) {
      PrintStream out = System.out;
      PrintStream err = System.err;
      String source = null;
      String output = null;

      Type type = null;

      for (int i = 0; i < args.length; i++) {
        switch (args[i]) {
          case "--download":
          // fallthrough;
          case "-d":
            type = Type.DOWNLOAD;
            break;
          case "--upload":
          // fallthrough;
          case "-u":
            type = Type.UPLOAD;
            break;
          case "--help":
          // fallthrough;
          case "-h":
            out.printf(USAGE);
            return 0;
          default:
            if (source == null) {
              source = args[i];
            } else if (output == null) {
              output = args[i];
            } else {
              err.printf("ERROR: Unexpected argument: %s.\n%s", args[i], USAGE);
              return 1;
            }
        }
      }

      if (type == null) {
        err.printf("ERROR: Type is required.\n%s", USAGE);
        return 1;
      }

      if (isBlank(source)) {
        err.printf("ERROR: source is required.\n%s", USAGE);
        return 1;
      }

      if (isBlank(output)) {
        err.printf("ERROR: output is required.\n%s", USAGE);
        return 1;
      }

      int responseCode;
      byte[] buf = new byte[8 * 1024];
      switch (type) {
        case DOWNLOAD:
          responseCode = downloadFile(source, new File(output), buf, err);
          break;
        case UPLOAD:
          responseCode = uploadFile(new File(output), source, buf, err);
          break;
        default:
          err.printf("ERROR: Unexpected type: %s.\n%s", type, USAGE);
          return 1;
      }

      if (!isOk(responseCode)) {
        err.printf(
            "ERROR: %d failed to %s %s to %s.\n",
            responseCode, type.name().toLowerCase(Locale.ENGLISH), source, output);
        return 1;
      }

      return 0;
    }

    public static boolean isOk(int responseCode) {
      return 199 < responseCode && responseCode < 300;
    }

    public static int get(String urlStr) {
      try {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        return conn.getResponseCode();
      } catch (IOException e) {
        return -1;
      }
    }

    public static int uploadFile(File toUpload, String urlStr, byte[] buf, PrintStream err) {

      try {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setDoOutput(true);
        conn.setRequestMethod("PUT");
        conn.setRequestProperty(ContentType.HEADER, ContentType.OCTET_STREAM);
        conn.setRequestProperty("Content-Length", String.valueOf(toUpload.length()));

        try (FileInputStream is = new FileInputStream(toUpload);
            OutputStream os = conn.getOutputStream()) {
          copy(is, os, buf);
        }

        return conn.getResponseCode();
      } catch (IOException e) {
        e.printStackTrace(err);
        return -1;
      }
    }

    public static int downloadFile(
        String urlStr, File downloadLocation, byte[] buf, PrintStream err) {

      try {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        int responseCode = conn.getResponseCode();

        if (responseCode != HttpURLConnection.HTTP_OK) {
          return responseCode;
        }

        try (InputStream is = conn.getInputStream()) {
          write(is, downloadLocation, buf, DEFAULT_ATOMIC_ATTEMPTS);
        }

        return responseCode;
      } catch (IOException e) {
        e.printStackTrace(err);
        return -1;
      }
    }

    private net() {}
  }

  /**
   * @see SJK
   * @see #USAGE
   *     <pre>{@code
   * java ~/Downloads/SJK.java fileServer
   * }</pre>
   */
  public static final class FileServer {

    private static final int DEFAULT_PORT = 8080;
    private static final String USAGE =
        "\n"
            + "Standalone file server that handles file upload and download over HTTP. The"
            + " server is designed for development and testing and may not be suitable for"
            + " production use cases.\n\n"
            + "Example usage:\n\n"
            + "# Start server from JAR:\n"
            + "java -jar stiemannkj1.jar fileServer --port 8080 --location somewhere/\n"
            + "# Start server from source:\n"
            + "java SJK.java fileServer --port 8080 --location somewhere/\n"
            + "# Download file:\n"
            + "curl -o filename.txt http://localhost:"
            + DEFAULT_PORT
            + "/download/filename.txt\n"
            + "# Upload file:\n"
            + "curl -T filename.txt http://localhost:"
            + DEFAULT_PORT
            + "/upload/filename.txt\n"
            + "\nFlags:\n"
            + "--port|-p\n"
            + "\tThe optional port to serve files on. Defaults to "
            + DEFAULT_PORT
            + ".\n\n"
            + "--location|-l\n"
            + "\tThe optional directory to server files from and upload files to."
            + " Defaults to the working directory of the process.\n\n"
            + "--help|-h\n"
            + "\tPrint this usage information.\n\n";

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
            if (flags.printIfMissingValue("Port", i, args, USAGE, err)) {
              return 1;
            }

            i++;

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
            if (flags.printIfMissingValue("Location", i, args, USAGE, err)) {
              return 1;
            }

            i++;

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

      Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));

      try {
        Thread.currentThread().join();
      } catch (InterruptedException e) {
        e.printStackTrace(err);
        return 1;
      }

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
        err.printf(
            "ERROR: threadPoolSize must be greater 0, but was: %d\n%s", threadPoolSize, USAGE);
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

      server.createContext(
          "/ping",
          exchange -> {
            exchange.getResponseHeaders().set(ContentType.HEADER, ContentType.TEXT);

            sendResponse(exchange, HTTP_CREATED, "pong\n");
          });

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

    private static void sendErrorResponse(
        HttpExchange exchange, int code, String remoteAddr, String message, PrintStream err)
        throws IOException {

      if (isOk(code)) {
        throw new AssertionError(code + " is not an HTTP error code.");
      }

      sendResponse(exchange, code, message);
      err.printf("WARN: request from %s failed. %s", remoteAddr, message);
    }

    private static void sendResponse(HttpExchange exchange, int code, String message)
        throws IOException {
      byte[] response = message.getBytes();
      exchange.sendResponseHeaders(code, response.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(response);
      }
    }

    public static final class FileHandler implements HttpHandler {

      public enum Type {
        DOWNLOAD,
        UPLOAD
      }

      private final Type type;
      private final String endpoint;
      private final String method;
      private final Path location;
      private final PrintStream out;
      private final PrintStream err;

      private FileHandler(
          FileHandler.Type type,
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
          Type type,
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
                  .toString(),
              err);
          return;
        }

        Path toDownload =
            new File(location.toFile(), stripPrefix(exchange.getRequestURI().getPath(), endpoint))
                .getAbsoluteFile()
                .toPath()
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
                  .toString(),
              err);
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
                  .toString(),
              err);
          return;
        }

        exchange.getResponseHeaders().set(ContentType.HEADER, ContentType.OCTET_STREAM);
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
          copy(is, os, threadLocals.buf());
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
                  .toString(),
              err);
          return;
        }

        Path toUpload =
            new File(location.toFile(), stripPrefix(exchange.getRequestURI().getPath(), endpoint))
                .getAbsoluteFile()
                .toPath()
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
                  .toString(),
              err);
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
                  .toString(),
              err);
          return;
        }

        try (InputStream is = exchange.getRequestBody();
            OutputStream os = Files.newOutputStream(toUpload)) {
          copy(is, os, threadLocals.buf());
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
            throw unhandledCase(type);
        }
      }
    }

    private FileServer() {}
  }

  static final class ThreadUnsafeStorage {
    private Strings.Builder sb;
    private byte[] buf;

    public StringBuilder sb() {

      if (sb == null) {
        sb = new Strings.Builder();
      }

      return sb.reset();
    }

    public byte[] buf() {

      if (buf == null) {
        buf = new byte[DEFAULT_BUF_SIZE];
      }

      return buf;
    }

    static final ThreadLocal<ThreadUnsafeStorage> THREAD_LOCALS =
        ThreadLocal.withInitial(ThreadUnsafeStorage::new);
  }

  public static final class FileServerTest {

    public static final int TEST_PORT;

    private static final int DEFAULT_TEST_PORT = 35294;
    private static final String SJK_TEST_PORT = "SJK_TEST_PORT";
    public static final String SJK_TEST_PORT_USAGE =
        SJK_TEST_PORT
            + "\n"
            + "\tThe port to use for testing when this command starts a test server."
            + " Defaults to "
            + DEFAULT_TEST_PORT
            + ".\n";

    static {
      int testPort = 35294;
      String testPortString = System.getenv(SJK_TEST_PORT);

      if (!isBlank(testPortString)) {
        testPort = Integer.parseInt(testPortString);
      }

      TEST_PORT = testPort;
    }

    public static void main(String[] args) throws IOException, InterruptedException {

      System.out.println(SJK_DEBUG_PORT_USAGE);
      System.out.println(SJK_TEST_PORT_USAGE);

      Testing test = new Testing();

      for (boolean useLocationArg : new boolean[] {true, false}) {

        // Verify that this test and server can run from source and has zero dependencies.
        String sourcePath = System.getProperty("jdk.launcher.sourcefile");
        File sourceFile;

        if (sourcePath != null) {
          sourceFile = new File(sourcePath);
        } else {
          String projectRoot = System.getProperty("user.dir");
          sourceFile = new File(projectRoot, "src/main/java/stiemannkj1/SJK.java");

          if (!sourceFile.isFile()) {
            sourceFile = new File(projectRoot, "stiemannkj1/SJK.java");
          }

          if (!sourceFile.isFile()) {
            sourceFile = new File(projectRoot, "SJK.java");
          }
        }

        Path parentTempDir =
            Files.createTempDirectory(FileServerTest.class.getTypeName()).toAbsolutePath();
        Runtime.getRuntime()
            .addShutdownHook(new Thread(() -> deleteRecursively(parentTempDir, -1)));

        Path tempDir = mkdirs(parentTempDir.toFile(), "work").toPath();
        Path javaDir = mkdirs(parentTempDir.toFile(), "java").toPath();
        Path serverDir = mkdirs(parentTempDir.toFile(), "server").toPath();

        Path sjkSource =
            Files.copy(sourceFile.toPath(), new File(javaDir.toFile(), "SJK.java").toPath());

        List<String> childArgs = new ArrayList<>();
        childArgs.add(JAVA_EXE);
        addJavaDebugArgIfSpecified(childArgs, true);
        childArgs.add(sjkSource.toFile().getAbsolutePath());
        childArgs.add("fileServer");
        childArgs.add("--port");
        childArgs.add(Integer.toString(TEST_PORT));

        if (useLocationArg) {
          childArgs.add("--location");
          childArgs.add(serverDir.toFile().getAbsolutePath());
        }

        ProcessBuilder pb = new ProcessBuilder().command(childArgs).inheritIO();

        if (!useLocationArg) {
          pb.directory(serverDir.toFile());
        }

        Process serverProc = pb.start();

        Runtime.getRuntime().addShutdownHook(new Thread(serverProc::destroyForcibly));

        int timeoutMs = 15_000;
        test.assertTrue(
            BusyWait.until(
                () -> net.isOk(net.get("http://localhost:" + TEST_PORT + "/ping")), timeoutMs),
            "Failed to ping within " + timeoutMs + "ms timeout.");

        byte[] buf = new byte[DEFAULT_BUF_SIZE];

        // Test upload.
        Path fooTxt = Files.writeString(new File(tempDir.toFile(), "foo.txt").toPath(), "foo\nbar");

        if (!isOk(
            uploadFile(
                fooTxt.toFile(),
                "http://localhost:" + TEST_PORT + "/upload/foo.txt",
                buf,
                System.err))) {
          test.fail("Failed to upload " + fooTxt);
        }

        test.assertEquals(
            "foo\nbar", Files.readString(new File(serverDir.toFile(), "foo.txt").toPath()));

        // Test download of uploaded file.
        Path downloadedFooTxt = new File(tempDir.toFile(), "downloadedFoo.txt").toPath();

        if (!isOk(
            downloadFile(
                "http://localhost:" + TEST_PORT + "/download/foo.txt",
                downloadedFooTxt.toFile(),
                buf,
                System.err))) {
          test.fail("Failed to download " + fooTxt);
        }

        test.assertEquals("foo\nbar", Files.readString(downloadedFooTxt));

        // Test upload.
        Path barTxt = Files.writeString(new File(serverDir.toFile(), "bar.txt").toPath(), "baz");
        Path downloadedBarTxt = new File(tempDir.toFile(), "downloadedBar.txt").toPath();

        if (!isOk(
            downloadFile(
                "http://localhost:" + TEST_PORT + "/download/bar.txt",
                downloadedBarTxt.toFile(),
                buf,
                System.err))) {
          test.fail("Failed to download " + barTxt);
        }

        test.assertEquals("baz", Files.readString(downloadedBarTxt));

        // Test malicious download path traversal.
        File barTxtOutsideServerDir = new File(parentTempDir.toFile(), "bar.txt");
        Files.copy(barTxt, barTxtOutsideServerDir.toPath());

        if (HTTP_BAD_REQUEST
            != downloadFile(
                "http://localhost:" + TEST_PORT + "/download/../bar.txt",
                downloadedBarTxt.toFile(),
                buf,
                System.err)) {
          test.fail(
              "Malicious request escaped server context dir of "
                  + serverDir
                  + " to download "
                  + barTxtOutsideServerDir.getAbsolutePath());
        }

        // Test malicious upload path traversal.
        File fooTxtOutsideServerDir = new File(parentTempDir.toFile(), "foo.txt");

        if (HTTP_BAD_REQUEST
            != uploadFile(
                fooTxt.toFile(),
                "http://localhost:" + TEST_PORT + "/upload/../foo.txt",
                buf,
                System.err)) {
          test.fail(
              "Malicious request escaped server context dir of "
                  + serverDir
                  + " to upload "
                  + fooTxtOutsideServerDir.getAbsolutePath());
        }

        if (fooTxtOutsideServerDir.exists()) {
          test.fail(
              "Malicious upload appeared to fail, but "
                  + fooTxtOutsideServerDir.getAbsolutePath()
                  + " was uploaded outside "
                  + serverDir);
        }

        serverProc.destroy();

        int timeoutMins = 1;
        if (!serverProc.waitFor(timeoutMins, TimeUnit.MINUTES)) {
          test.fail(
              FileServer.class.getSimpleName()
                  + " failed to exit within "
                  + timeoutMins
                  + " minutes.");
        }
        // Unix returns error code 143 for processes killed via SIGTERM.
        else if (serverProc.exitValue() != 143) {
          test.fail(
              FileServer.class.getSimpleName()
                  + " exited with error code: "
                  + serverProc.exitValue());
        }
      }

      test.throwIfFailed();
    }

    private FileServerTest() {}
  }

  private SJK() {}
}
