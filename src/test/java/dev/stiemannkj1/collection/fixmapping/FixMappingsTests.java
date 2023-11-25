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
package dev.stiemannkj1.collection.fixmapping;

import static dev.stiemannkj1.collection.fixmapping.FixMappings.binarySearchArrayPrefixMapping;
import static dev.stiemannkj1.collection.fixmapping.FixMappings.binarySearchArraySuffixMapping;
import static dev.stiemannkj1.collection.fixmapping.FixMappings.limitedCharArrayTriePrefixMapping;
import static dev.stiemannkj1.collection.fixmapping.FixMappings.limitedCharArrayTrieSuffixMapping;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.stiemannkj1.collection.fixmapping.FixMappings.ImmutablePrefixMapping;
import dev.stiemannkj1.collection.fixmapping.FixMappings.ImmutableSuffixMapping;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

final class FixMappingsTests {

  // TODO test unicode

  interface PrefixMappersTests {

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
          "abd,true,2",
          "aba,false,null",
          "abd,true,2",
          "abdi,true,2",
          "abdic,true,2",
          "abdica,true,2",
          "abdicat,true,2",
          "abdicat,true,2",
          "abdicate,true,3",
          "abdicated,true,3",
          "x,false,null",
          "xy,false,null"
        },
        nullValues = "null")
    @ParameterizedTest
    @SuppressWarnings("unchecked")
    default void detects_prefixes(
        final String string, final boolean expectPrefixed, final Integer expectedValue) {

      // Test odd number of prefixes.
      final Map<String, Integer> prefixes =
          newTestMapBuilder().add("abc", 0).add("abe", 1).add("abd", 2).add("abdicate", 3).map;

      ImmutablePrefixMapping<Integer> prefixMap = newPrefixMap(prefixes);

      assertEquals(expectPrefixed, prefixMap.matchesAnyPrefix(string));
      assertEquals(expectedValue, prefixMap.valueForPrefix(string));
      assertEquals(
          getKeyAndValueByValue(prefixes, expectedValue), prefixMap.keyAndValueForPrefix(string));

      assertFirstMatchSearchTakesLessSteps(
          string, expectedValue, (FixMappings.FixMapping<Integer>) prefixMap);

      // Test odd number of prefixes.
      prefixes.put("xyz", 4);

      prefixMap = newPrefixMap(prefixes);

      assertEquals(expectPrefixed, prefixMap.matchesAnyPrefix(string));
      assertEquals(expectedValue, prefixMap.valueForPrefix(string));
      assertEquals(
          getKeyAndValueByValue(prefixes, expectedValue), prefixMap.keyAndValueForPrefix(string));

      assertFirstMatchSearchTakesLessSteps(
          string, expectedValue, (FixMappings.FixMapping<Integer>) prefixMap);
    }

    @SuppressWarnings("unchecked")
    default void detects_prefixes_with_minimal_steps(
        final String string,
        final String firstMatchEvenKeys,
        final String firstMatchOddKeys,
        final int firstMatchStepsEvenKeys,
        final int longestMatchStepsEvenKeys,
        final int firstMatchStepsOddKeys,
        final int longestMatchStepsOddKeys) {

      // Test even number of prefixes.
      final Map<String, Integer> prefixes =
          newTestMapBuilder().add("abc", 0).add("abe", 1).add("abd", 2).add("abdicate", 3).map;

      ImmutablePrefixMapping<Integer> prefixMap = newPrefixMap(prefixes);

      final AtomicLong firstSearchSteps = new AtomicLong();
      final AtomicLong longestSearchSteps = new AtomicLong();

      firstSearchSteps.set(0);
      longestSearchSteps.set(0);

      assertEquals(
          Pair.of(firstMatchEvenKeys, prefixes.get(firstMatchEvenKeys)),
          ((FixMappings.FixMapping<Integer>) prefixMap)
              .getKeyAndValue(false, string, firstSearchSteps));
      assertEquals(
          Pair.of("abdicate", 3),
          ((FixMappings.FixMapping<Integer>) prefixMap)
              .getKeyAndValue(true, string, longestSearchSteps));

      assertEquals(firstMatchStepsEvenKeys, firstSearchSteps.get());
      assertEquals(longestMatchStepsEvenKeys, longestSearchSteps.get());

      // Test odd number of prefixes.
      prefixes.put("aaa", 4);

      prefixMap = newPrefixMap(prefixes);

      firstSearchSteps.set(0);
      longestSearchSteps.set(0);

      assertEquals(
          Pair.of(firstMatchOddKeys, prefixes.get(firstMatchOddKeys)),
          ((FixMappings.FixMapping<Integer>) prefixMap)
              .getKeyAndValue(false, string, firstSearchSteps));
      assertEquals(
          Pair.of("abdicate", 3),
          ((FixMappings.FixMapping<Integer>) prefixMap)
              .getKeyAndValue(true, string, longestSearchSteps));

      assertEquals(firstMatchStepsOddKeys, firstSearchSteps.get());
      assertEquals(longestMatchStepsOddKeys, longestSearchSteps.get());
    }

    @Test
    default void allows_prefixes_with_matching_suffixes() {

      // Test odd number of suffixes.
      final Map<String, Integer> suffixes =
          newTestMapBuilder().add("bdicate", 0).add("abdicate", 2).map;

      assertDoesNotThrow(() -> newPrefixMap(suffixes));
    }

    static Stream<Map<String, Integer>> invalidPrefixes() {

      return Stream.of(
          Collections.emptyMap(),
          newTestMapBuilder().add(null, 1).map,
          newTestMapBuilder().add(null, 1).add("asdf", 1).map,
          newTestMapBuilder().add("", 1).map,
          newTestMapBuilder().add("", 1).add("asdf", 1).map,
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

    ImmutablePrefixMapping<Integer> newPrefixMap(final Map<String, Integer> prefixes);
  }

  interface SuffixMappersTests {

    @CsvSource(
        value = {
          "null,false,null",
          ",false,null",
          "123456,false,null",
          "~~~~~~,false,null",
          "a,false,null",
          "ab,false,null",
          "abc,true,0",
          "1abc,true,0",
          "abe,true,1",
          "abed,false,null",
          "abd,false,null",
          "aba,false,null",
          "at,false,null",
          "ate,true,2",
          "cate,true,2",
          "icate,true,2",
          "dicate,true,2",
          "bdicate,true,2",
          "abdicate,true,3",
          "i abdicate,true,3",
          "abdicated,false,null",
          "z,false,null",
          "yz,false,null"
        },
        nullValues = "null")
    @ParameterizedTest
    @SuppressWarnings("unchecked")
    default void detects_suffixes(
        final String string, final boolean expectSuffixed, final Integer expectedValue) {

      // Test even number of suffixes.
      final Map<String, Integer> suffixes =
          newTestMapBuilder().add("abc", 0).add("abe", 1).add("ate", 2).add("abdicate", 3).map;

      ImmutableSuffixMapping<Integer> suffixMap = newSuffixMap(suffixes);

      assertEquals(expectSuffixed, suffixMap.matchesAnySuffix(string));
      assertEquals(expectedValue, suffixMap.valueForSuffix(string));
      assertEquals(
          getKeyAndValueByValue(suffixes, expectedValue), suffixMap.keyAndValueForSuffix(string));

      assertFirstMatchSearchTakesLessSteps(
          string, expectedValue, (FixMappings.FixMapping<Integer>) suffixMap);

      // Test odd number of suffixes.
      suffixes.put("xyz", 4);

      suffixMap = newSuffixMap(suffixes);

      assertEquals(expectSuffixed, suffixMap.matchesAnySuffix(string));
      assertEquals(expectedValue, suffixMap.valueForSuffix(string));
      assertEquals(
          getKeyAndValueByValue(suffixes, expectedValue), suffixMap.keyAndValueForSuffix(string));

      assertFirstMatchSearchTakesLessSteps(
          string, expectedValue, (FixMappings.FixMapping<Integer>) suffixMap);
    }

    @SuppressWarnings("unchecked")
    default void detects_suffixes_with_minimal_steps(
        final String string,
        final String firstMatchEvenKeys,
        final String firstMatchOddKeys,
        final int firstMatchStepsEvenKeys,
        final int longestMatchStepsEvenKeys,
        final int firstMatchStepsOddKeys,
        final int longestMatchStepsOddKeys) {

      // Test even number of suffixes.
      final Map<String, Integer> suffixes =
          newTestMapBuilder().add("abc", 0).add("abe", 1).add("ate", 2).add("abdicate", 3).map;

      ImmutableSuffixMapping<Integer> suffixMap = newSuffixMap(suffixes);

      final AtomicLong firstSearchSteps = new AtomicLong();
      final AtomicLong longestSearchSteps = new AtomicLong();

      firstSearchSteps.set(0);
      longestSearchSteps.set(0);

      assertEquals(
          Pair.of(firstMatchEvenKeys, suffixes.get(firstMatchEvenKeys)),
          ((FixMappings.FixMapping<Integer>) suffixMap)
              .getKeyAndValue(false, string, firstSearchSteps));
      assertEquals(
          Pair.of("abdicate", 3),
          ((FixMappings.FixMapping<Integer>) suffixMap)
              .getKeyAndValue(true, string, longestSearchSteps));

      assertEquals(firstMatchStepsEvenKeys, firstSearchSteps.get());
      assertEquals(longestMatchStepsEvenKeys, longestSearchSteps.get());

      // Test odd number of suffixes.
      suffixes.put("aaa", 4);

      suffixMap = newSuffixMap(suffixes);

      firstSearchSteps.set(0);
      longestSearchSteps.set(0);

      assertEquals(
          Pair.of(firstMatchOddKeys, suffixes.get(firstMatchOddKeys)),
          ((FixMappings.FixMapping<Integer>) suffixMap)
              .getKeyAndValue(false, string, firstSearchSteps));
      assertEquals(
          Pair.of("abdicate", 3),
          ((FixMappings.FixMapping<Integer>) suffixMap)
              .getKeyAndValue(true, string, longestSearchSteps));

      assertEquals(firstMatchStepsOddKeys, firstSearchSteps.get());
      assertEquals(longestMatchStepsOddKeys, longestSearchSteps.get());
    }

    @Test
    default void allows_suffixes_with_matching_prefixes() {

      // Test odd number of suffixes.
      final Map<String, Integer> suffixes =
          newTestMapBuilder().add("abdicat", 0).add("abdicate", 2).map;

      assertDoesNotThrow(() -> newSuffixMap(suffixes));
    }

    static Stream<Map<String, Integer>> invalidSuffixes() {

      return Stream.of(
          Collections.emptyMap(),
          newTestMapBuilder().add(null, 1).map,
          newTestMapBuilder().add(null, 1).add("asdf", 1).map,
          newTestMapBuilder().add("", 1).map,
          newTestMapBuilder().add("", 1).add("asdf", 1).map,
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

    ImmutableSuffixMapping<Integer> newSuffixMap(final Map<String, Integer> prefixes);
  }

  private static void assertFirstMatchSearchTakesLessSteps(
      String string, Integer expectedValue, FixMappings.FixMapping<Integer> fixMapping) {

    final AtomicLong firstSearchSteps = new AtomicLong(0);
    final AtomicLong longestSearchSteps = new AtomicLong(0);

    final Pair<String, Integer> firstMatch =
        fixMapping.getKeyAndValue(false, string, firstSearchSteps);

    if (expectedValue != null) {
      assertNotNull(firstMatch);
    } else {
      assertNull(firstMatch);
    }

    final Pair<String, Integer> longestMatch =
        fixMapping.getKeyAndValue(true, string, longestSearchSteps);

    if (expectedValue != null) {
      assertNotNull(longestMatch);
    } else {
      assertNull(longestMatch);
    }

    assertTrue(firstSearchSteps.get() <= longestSearchSteps.get());
  }

  static final class BinarySearchArrayPrefixMapTests implements PrefixMappersTests {
    @Override
    public ImmutablePrefixMapping<Integer> newPrefixMap(final Map<String, Integer> prefixes) {
      return binarySearchArrayPrefixMapping(prefixes);
    }

    @CsvSource(
        value = {
          "abdicate,abd,abdicate,1,2,1,1",
          "abdicated,abd,abdicate,1,3,1,2",
        })
    @ParameterizedTest
    @Override
    public void detects_prefixes_with_minimal_steps(
        final String string,
        final String firstMatchEvenKeys,
        final String firstMatchOddKeys,
        final int firstMatchStepsEvenKeys,
        final int longestMatchStepsEvenKeys,
        final int firstMatchStepsOddKeys,
        final int longestMatchStepsOddKeys) {
      PrefixMappersTests.super.detects_prefixes_with_minimal_steps(
          string,
          firstMatchEvenKeys,
          firstMatchOddKeys,
          firstMatchStepsEvenKeys,
          longestMatchStepsEvenKeys,
          firstMatchStepsOddKeys,
          longestMatchStepsOddKeys);
    }
  }

  static final class BinarySearchArraySuffixMapTests implements SuffixMappersTests {
    @Override
    public ImmutableSuffixMapping<Integer> newSuffixMap(final Map<String, Integer> suffixes) {
      return binarySearchArraySuffixMapping(suffixes);
    }

    @CsvSource(
        value = {
          "abdicate,abdicate,ate,1,1,2,3",
          "i abdicate,abdicate,ate,1,2,2,3",
        })
    @ParameterizedTest
    @Override
    public void detects_suffixes_with_minimal_steps(
        final String string,
        final String firstMatchEvenKeys,
        final String firstMatchOddKeys,
        final int firstMatchStepsEvenKeys,
        final int longestMatchStepsEvenKeys,
        final int firstMatchStepsOddKeys,
        final int longestMatchStepsOddKeys) {
      SuffixMappersTests.super.detects_suffixes_with_minimal_steps(
          string,
          firstMatchEvenKeys,
          firstMatchOddKeys,
          firstMatchStepsEvenKeys,
          longestMatchStepsEvenKeys,
          firstMatchStepsOddKeys,
          longestMatchStepsOddKeys);
    }
  }

  static final class LimitedCharArrayTriePrefixMapTests implements PrefixMappersTests {
    @Override
    public ImmutablePrefixMapping<Integer> newPrefixMap(final Map<String, Integer> prefixes) {
      return limitedCharArrayTriePrefixMapping('a', 'z', prefixes);
    }

    @CsvSource(
        value = {
          "abdicate,abd,abd,3,8,3,8",
          "abdicated,abd,abd,3,8,3,8",
        })
    @ParameterizedTest
    @Override
    public void detects_prefixes_with_minimal_steps(
        final String string,
        final String firstMatchEvenKeys,
        final String firstMatchOddKeys,
        final int firstMatchStepsEvenKeys,
        final int longestMatchStepsEvenKeys,
        final int firstMatchStepsOddKeys,
        final int longestMatchStepsOddKeys) {
      PrefixMappersTests.super.detects_prefixes_with_minimal_steps(
          string,
          firstMatchEvenKeys,
          firstMatchOddKeys,
          firstMatchStepsEvenKeys,
          longestMatchStepsEvenKeys,
          firstMatchStepsOddKeys,
          longestMatchStepsOddKeys);
    }
  }

  static final class LimitedCharArrayTrieSuffixMapTests implements SuffixMappersTests {
    @Override
    public ImmutableSuffixMapping<Integer> newSuffixMap(final Map<String, Integer> suffixes) {
      return limitedCharArrayTrieSuffixMapping('a', 'z', suffixes);
    }

    @CsvSource(
        value = {
          "abdicate,ate,ate,3,8,3,8",
          "i abdicate,ate,ate,3,8,3,8",
        })
    @ParameterizedTest
    @Override
    public void detects_suffixes_with_minimal_steps(
        final String string,
        final String firstMatchEvenKeys,
        final String firstMatchOddKeys,
        final int firstMatchStepsEvenKeys,
        final int longestMatchStepsEvenKeys,
        final int firstMatchStepsOddKeys,
        final int longestMatchStepsOddKeys) {
      SuffixMappersTests.super.detects_suffixes_with_minimal_steps(
          string,
          firstMatchEvenKeys,
          firstMatchOddKeys,
          firstMatchStepsEvenKeys,
          longestMatchStepsEvenKeys,
          firstMatchStepsOddKeys,
          longestMatchStepsOddKeys);
    }
  }

  static final class LimitedCharArrayTrieFixMapperTests {

    @CsvSource({"0,0", "1,1", "2,2", "3,4", "4,4", "5,8", "6,8", "7,8", "8,8", "9,16", "16,16"})
    @ParameterizedTest
    void gets_next_power_of_2(final int i, final int expectedPowerOf2) {
      assertEquals(expectedPowerOf2, LimitedCharArrayTrieFixMapping.nextPowerOf2(i));
    }

    @CsvSource({
      "0,0", "1,1", "2,2", "3,2", "4,4", "5,4", "6,4", "7,4", "8,8", "9,8", "16,16", "17,16"
    })
    @ParameterizedTest
    void gets_next_prev_of_2(final int i, final int expectedPowerOf2) {
      assertEquals(expectedPowerOf2, LimitedCharArrayTrieFixMapping.prevPowerOf2(i));
    }

    @CsvSource({"a,0", "b,0", "c,1", "d,32", "e,64"})
    @ParameterizedTest
    void gets_index_from_char(final char char_, final int offset) {
      assertEquals(char_ - offset, LimitedCharArrayTrieFixMapping.toIndex(char_, offset));
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
    return new AbstractMap<>() {
      @Override
      public Set<Map.Entry<String, Integer>> entrySet() {
        return new AbstractSet<>() {
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

  private FixMappingsTests() {}
}
