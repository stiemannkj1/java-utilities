package dev.stiemannkj1.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
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

  @Test
  void test_starts_with_empty() {
    assertTrue(StringUtils.startsWith("", "", 0));
    assertTrue(StringUtils.startsWith("foo", "", 0));
  }

  @CsvSource({
    "false,'',bar",
    "false,foo,b",
    "false,foo,ba",
    "false,foo,bar",
    "false,foo,barb",
    "true,foo,f",
    "true,foo,fo",
    "true,foo,foo",
    "false,foo,oof",
    "false,foo,ofo",
    "false,foo,ooo",
    "false,foo,fff",
    "false,foo,foob",
  })
  @ParameterizedTest
  void test_starts_with(final boolean expectedResult, final String string, final String prefix) {
    assertFalse(prefix.isEmpty());
    for (int i = 0; i < prefix.length(); i++) {
      assertEquals(expectedResult, StringUtils.startsWith(string, prefix, i));
    }
  }
}
