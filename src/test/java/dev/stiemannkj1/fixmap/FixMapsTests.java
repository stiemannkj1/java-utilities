/*
  Copyright 2023 Kyle J. Stiemann

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/
package dev.stiemannkj1.fixmap;

import static dev.stiemannkj1.fixmap.FixMaps.binarySearchArrayPrefixMap;
import static dev.stiemannkj1.fixmap.FixMaps.binarySearchArraySuffixMap;
import static dev.stiemannkj1.fixmap.FixMaps.limitedCharArrayTriePrefixMap;
import static dev.stiemannkj1.fixmap.FixMaps.limitedCharArrayTrieSuffixMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.stiemannkj1.fixmap.FixMaps.ImmutablePrefixMap;
import dev.stiemannkj1.fixmap.FixMaps.ImmutableSuffixMap;
import dev.stiemannkj1.util.Pair;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

final class FixMapsTests {

  // TODO test unicode

  interface PrefixMapsTests {

    @CsvSource(
        value = {
          "null,false,null",
          ",false,null",
          "123456,false,null",
          "~~~~~~,false,null",
          "a,false,null",
          "ab,false,null",
          "abc,true,0",
          "abe,true,1",
          "abd,false,null",
          "aba,false,null",
          "abdicat,false,null",
          "abdicate,true,2",
          "abdicated,true,2"
        },
        nullValues = "null")
    @ParameterizedTest
    default void detects_prefixes(
        final String string, final boolean expectPrefixed, final Integer expectedValue) {

      // Test odd number of prefixes.
      final Map<String, Integer> prefixes =
          newTestMapBuilder().add("abc", 0).add("abe", 1).add("abdicate", 2).map;

      ImmutablePrefixMap<Integer> prefixMap = newPrefixMap(prefixes);

      assertEquals(expectPrefixed, prefixMap.matchesAnyPrefix(string));
      assertEquals(expectedValue, prefixMap.valueForPrefix(string));
      assertEquals(
          getKeyAndValueByValue(prefixes, expectedValue), prefixMap.keyAndValueForPrefix(string));

      // Test even number of prefixes.
      prefixes.put("xyz", 3);

      prefixMap = newPrefixMap(prefixes);

      assertEquals(expectPrefixed, prefixMap.matchesAnyPrefix(string));
      assertEquals(expectedValue, prefixMap.valueForPrefix(string));
      assertEquals(
          getKeyAndValueByValue(prefixes, expectedValue), prefixMap.keyAndValueForPrefix(string));
    }

    static Stream<Map<String, Integer>> invalidPrefixes() {

      return Stream.of(
          Collections.emptyMap(),
          newTestMapBuilder().add(null, 1).map,
          newTestMapBuilder().add(null, 1).add("asdf", 1).map,
          newTestMapBuilder().add("", 1).map,
          newTestMapBuilder().add("", 1).add("asdf", 1).map,
          newTestMapBuilder().add("asd", 1).add("asdf", 1).map,
          newTestMapBuilder().add("asdf", 1).add("asdfg", 1).map,
          newTestMapBuilder().add("asd", 1).add("asdf", 1).add("asdfg", 1).map,
          // Odd number of keys:
          newTestMapBuilder().add("aaa", 1).add("asdf", 1).add("asdfg", 1).map,
          // Even number of keys:
          newTestMapBuilder().add("asd", 1).add("asdf", 1).add("aaa", 1).add("bbb", 1).map,
          // Invalid map with duplicate keys:
          toMap(Arrays.asList(Pair.of("asdf", 1), Pair.of("asdf", 1))),
          // Invalid map with null entry:
          toMap(Arrays.asList(null, Pair.of("asdf", 1))));
    }

    @MethodSource("invalidPrefixes")
    @ParameterizedTest
    default void throws_illegal_arg_when_provided_invalid_values(
        final Map<String, Integer> prefixes) {

      final Class<? extends Exception> expectedExceptionType =
          prefixes != null ? IllegalArgumentException.class : NullPointerException.class;

      assertThrows(expectedExceptionType, () -> newPrefixMap(prefixes));
    }

    ImmutablePrefixMap<Integer> newPrefixMap(final Map<String, Integer> prefixes);
  }

  interface SuffixMapsTests {

    @CsvSource(
        value = {
          "null,false,null",
          ",false,null",
          "123456,false,null",
          "~~~~~~,false,null",
          "ate,false,null",
          "ab,false,null",
          "abc,true,0",
          "1abc,true,0",
          "abe,true,1",
          "abed,false,null",
          "abd,false,null",
          "aba,false,null",
          "abdicate,true,2",
          "i abdicate,true,2",
          "abdicated,false,null"
        },
        nullValues = "null")
    @ParameterizedTest
    default void detects_suffixes(
        final String string, final boolean expectSuffixed, final Integer expectedValue) {

      // Test odd number of suffixes.
      final Map<String, Integer> suffixes =
          newTestMapBuilder().add("abc", 0).add("abe", 1).add("abdicate", 2).map;

      ImmutableSuffixMap<Integer> suffixMap = newSuffixMap(suffixes);

      assertEquals(expectSuffixed, suffixMap.matchesAnySuffix(string));
      assertEquals(expectedValue, suffixMap.valueForSuffix(string));
      assertEquals(
          getKeyAndValueByValue(suffixes, expectedValue), suffixMap.keyAndValueForSuffix(string));

      // Test even number of suffixes.
      suffixes.put("xyz", 3);

      suffixMap = newSuffixMap(suffixes);

      assertEquals(expectSuffixed, suffixMap.matchesAnySuffix(string));
      assertEquals(expectedValue, suffixMap.valueForSuffix(string));
      assertEquals(
          getKeyAndValueByValue(suffixes, expectedValue), suffixMap.keyAndValueForSuffix(string));
    }

    static Stream<Map<String, Integer>> invalidSuffixes() {

      return Stream.of(
          Collections.emptyMap(),
          newTestMapBuilder().add(null, 1).map,
          newTestMapBuilder().add(null, 1).add("asdf", 1).map,
          newTestMapBuilder().add("", 1).map,
          newTestMapBuilder().add("", 1).add("asdf", 1).map,
          newTestMapBuilder().add("dfg", 1).add("sdfg", 1).map,
          newTestMapBuilder().add("sdfg", 1).add("asdfg", 1).map,
          newTestMapBuilder().add("dfg", 1).add("sdfg", 1).add("asdfg", 1).map,
          // Odd number of keys:
          newTestMapBuilder().add("aaa", 1).add("sdfg", 1).add("asdfg", 1).map,
          // Even number of keys:
          newTestMapBuilder().add("dfg", 1).add("sdfg", 1).add("aaa", 1).add("bbb", 1).map,
          // Invalid map with duplicate keys:
          toMap(Arrays.asList(Pair.of("asdf", 1), Pair.of("asdf", 1))),
          // Invalid map with null entry:
          toMap(Arrays.asList(null, Pair.of("asdf", 1))));
    }

    @MethodSource("invalidSuffixes")
    @ParameterizedTest
    default void throws_illegal_arg_when_provided_invalid_values(
        final Map<String, Integer> suffixes) {

      final Class<? extends Exception> expectedExceptionType =
          suffixes != null ? IllegalArgumentException.class : NullPointerException.class;

      assertThrows(expectedExceptionType, () -> newSuffixMap(suffixes));
    }

    ImmutableSuffixMap<Integer> newSuffixMap(final Map<String, Integer> prefixes);
  }

  static final class BinarySearchArrayPrefixMapTests implements PrefixMapsTests {
    @Override
    public ImmutablePrefixMap<Integer> newPrefixMap(final Map<String, Integer> prefixes) {
      return binarySearchArrayPrefixMap(prefixes);
    }
  }

  static final class BinarySearchArraySuffixMapTests implements SuffixMapsTests {
    @Override
    public ImmutableSuffixMap<Integer> newSuffixMap(final Map<String, Integer> suffixes) {
      return binarySearchArraySuffixMap(suffixes);
    }
  }

  static final class LimitedCharArrayTriePrefixMapTests implements PrefixMapsTests {
    @Override
    public ImmutablePrefixMap<Integer> newPrefixMap(final Map<String, Integer> prefixes) {
      return limitedCharArrayTriePrefixMap('a', 'z', prefixes);
    }
  }

  static final class LimitedCharArrayTrieSuffixMapTests implements SuffixMapsTests {
    @Override
    public ImmutableSuffixMap<Integer> newSuffixMap(final Map<String, Integer> suffixes) {
      return limitedCharArrayTrieSuffixMap('a', 'z', suffixes);
    }
  }

  static final class LimitedCharArrayTrieFixMapTests {

    @CsvSource({"0,0", "1,1", "2,2", "3,4", "4,4", "5,8", "6,8", "7,8", "8,8", "9,16", "16,16"})
    @ParameterizedTest
    void gets_next_power_of_2(final int i, final int expectedPowerOf2) {
      assertEquals(expectedPowerOf2, LimitedCharArrayTrieFixMap.nextPowerOf2(i));
    }

    @CsvSource({
      "0,0", "1,1", "2,2", "3,2", "4,4", "5,4", "6,4", "7,4", "8,8", "9,8", "16,16", "17,16"
    })
    @ParameterizedTest
    void gets_next_prev_of_2(final int i, final int expectedPowerOf2) {
      assertEquals(expectedPowerOf2, LimitedCharArrayTrieFixMap.prevPowerOf2(i));
    }

    @CsvSource({"a,0", "b,0", "c,1", "d,32", "e,64"})
    @ParameterizedTest
    void gets_index_from_char(final char char_, final int offset) {
      assertEquals(char_ - offset, LimitedCharArrayTrieFixMap.toIndex(char_, offset));
    }
  }

  private static final class MapBuilder<K, V> {
    private final Map<K, V> map = new HashMap<>();

    private MapBuilder<K, V> add(final K key, V value) {
      map.put(key, value);
      return this;
    }
  }

  private static MapBuilder<String, Integer> newTestMapBuilder() {
    return new MapBuilder<>();
  }

  private static Map<String, Integer> toMap(final List<Map.Entry<String, Integer>> entries) {
    return new AbstractMap<String, Integer>() {
      @Override
      public Set<Map.Entry<String, Integer>> entrySet() {
        return new AbstractSet<Map.Entry<String, Integer>>() {
          @Override
          public Iterator<Map.Entry<String, Integer>> iterator() {
            return entries.iterator();
          }

          @Override
          public int size() {
            return entries.size();
          }
        };
      }
    };
  }

  private static <K, V> Pair<K, V> getKeyAndValueByValue(final Map<K, V> map, final V value) {
    return map.entrySet().stream()
        .filter(entry -> Objects.equals(entry.getValue(), value))
        .findFirst()
        .map(Pair::fromEntry)
        .orElse(null);
  }

  private FixMapsTests() {}
}
