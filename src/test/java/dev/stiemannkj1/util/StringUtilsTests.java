package dev.stiemannkj1.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class StringUtilsTests {

  static Stream<Arguments> hexTestCases() {
    return Stream.of(
        Arguments.of("CAFEBABE", 0xCAFEBABE),
        Arguments.of("DEADBEEF", 0xDEADBEEF),
        Arguments.of("00000000", 0x00000000),
        Arguments.of("00000001", 0x00000001),
        Arguments.of("FFFFFFFE", 0xFFFFFFFE),
        Arguments.of("FFFFFFFF", 0xFFFFFFFF));
  }

  @MethodSource("hexTestCases")
  @ParameterizedTest
  void appends_hex_to_string_builder(final String expectedHexString, final int hex) {
    final StringBuilder hexString = new StringBuilder();
    StringUtils.appendHexString(hexString, hex, 4);
    assertEquals(expectedHexString, hexString.toString());
  }
}
