package dev.stiemannkj1.util;

import static dev.stiemannkj1.util.SleepUtils.busyWait;
import static dev.stiemannkj1.util.StringUtils.readUtf8String;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public final class ProcessUtils {

  public static final class Result {
    public final int exitValue;
    public final File outFile;
    public final File errFile;
    private String stdout;
    private String stderr;

    private Result(final int exitValue, final File outFile, final File errFile) {
      this.exitValue = exitValue;
      this.outFile = outFile;
      this.errFile = errFile;
    }

    public String stdout() throws IOException {
      if (stdout == null) {
        stdout = readUtf8String(outFile);
      }

      return stdout;
    }

    public String stderr() throws IOException {
      if (stderr == null) {
        stderr = readUtf8String(errFile);
      }

      return stderr;
    }
  }

  public static Result runWithTimeout(
      final ProcessBuilder processBuilder, final long timeoutMs, final long shutdownTimeoutNs)
      throws IOException, InterruptedException {

    final File stdout = Files.createTempFile("cmd.", ".out").toFile();
    stdout.deleteOnExit();
    final File stderr = Files.createTempFile("cmd.", ".err").toFile();
    stderr.deleteOnExit();

    processBuilder.redirectOutput(stdout);
    processBuilder.redirectError(stderr);

    final Process process = processBuilder.start();

    busyWait(process::isAlive, timeoutMs);

    if (process.isAlive()) {
      process.destroy();
    }

    busyWait(process::isAlive, shutdownTimeoutNs);

    if (process.isAlive()) {
      process.destroyForcibly();
    }

    return new Result(process.isAlive() ? -1 : process.exitValue(), stdout, stderr);
  }

  private ProcessUtils() {}
}
