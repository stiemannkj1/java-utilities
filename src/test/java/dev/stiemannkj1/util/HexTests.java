package dev.stiemannkj1.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class HexTests {

  static Stream<Arguments> hexTestCases() {
    return Stream.of(
        Arguments.of("0xCAFEBABE", 0xCAFEBABE),
        Arguments.of("0xDEADBEEF", 0xDEADBEEF),
        Arguments.of("0x00000000", 0x00000000),
        Arguments.of("0x00000001", 0x00000001),
        Arguments.of("0xFFFFFFFE", 0xFFFFFFFE),
        Arguments.of("0xFFFFFFFF", 0xFFFFFFFF));
  }

  @MethodSource("hexTestCases")
  @ParameterizedTest
  void appends_hex_to_string_builder(final String expectedHexString, final int hex) {
    final StringBuilder hexString = new StringBuilder();
    Hex.appendHexString(hexString, hex);
    assertEquals(expectedHexString, hexString.toString());
  }
}
