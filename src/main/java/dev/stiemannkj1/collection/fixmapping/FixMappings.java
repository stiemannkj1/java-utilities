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

import dev.stiemannkj1.annotation.VisibleForTesting;
import dev.stiemannkj1.util.Pair;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public final class FixMappings {

  @VisibleForTesting
  interface FixMapping<T> {
    Pair<String, T> getKeyAndValue(
        final boolean getLongestMatch, final String string, final AtomicLong debugSearchSteps);
  }

  public interface ImmutablePrefixMatcher {
    boolean matchesAnyPrefix(final String string);
  }

  public interface ImmutableSuffixMatcher {
    boolean matchesAnySuffix(final String string);
  }

  public interface ImmutablePrefixMapping<T> extends ImmutablePrefixMatcher {

    @Override
    default boolean matchesAnyPrefix(final String string) {
      return keyAndValueForPrefix(string) != null;
    }

    default T valueForPrefix(final String string) {
      final Pair<String, T> keyAndValue = keyAndValueForPrefix(string);

      if (keyAndValue == null) {
        return null;
      }

      return keyAndValue.second;
    }

    Pair<String, T> keyAndValueForPrefix(final String string);
  }

  public interface ImmutableSuffixMapping<T> extends ImmutableSuffixMatcher {

    @Override
    default boolean matchesAnySuffix(final String string) {
      return keyAndValueForSuffix(string) != null;
    }

    default T valueForSuffix(final String string) {
      final Pair<String, T> keyAndValue = keyAndValueForSuffix(string);

      if (keyAndValue == null) {
        return null;
      }

      return keyAndValue.second;
    }

    Pair<String, T> keyAndValueForSuffix(final String string);
  }

  private static Map<String, Boolean> toMap(final String... fixes) {
    return Arrays.stream(fixes)
        .collect(Collectors.toMap(key -> key, key -> true, (k1, k2) -> k1, HashMap::new));
  }

  public static ImmutablePrefixMatcher binarySearchArrayPrefixMatcher(final String... prefixes) {
    return new BinarySearchArrayPrefixMapping<>(toMap(prefixes));
  }

  public static <T> ImmutablePrefixMapping<T> binarySearchArrayPrefixMapping(
      final Map<String, T> prefixes) {
    return new BinarySearchArrayPrefixMapping<>(prefixes);
  }

  private static final class BinarySearchArrayPrefixMapping<T>
      extends BinarySearchArrayFixMapping<T> implements ImmutablePrefixMapping<T> {

    private BinarySearchArrayPrefixMapping(final Map<String, T> unsortedPrefixes) {
      super(true, unsortedPrefixes);
    }

    @Override
    public boolean matchesAnyPrefix(final String string) {
      return getKeyAndValue(false, string, null) != null;
    }

    @Override
    public Pair<String, T> keyAndValueForPrefix(final String string) {
      return getKeyAndValue(true, string, null);
    }
  }

  public static ImmutableSuffixMatcher binarySearchArraySuffixMatcher(final String... suffixes) {
    return new BinarySearchArraySuffixMapping<>(toMap(suffixes));
  }

  public static <T> ImmutableSuffixMapping<T> binarySearchArraySuffixMapping(
      final Map<String, T> suffixes) {
    return new BinarySearchArraySuffixMapping<>(suffixes);
  }

  private static final class BinarySearchArraySuffixMapping<T>
      extends BinarySearchArrayFixMapping<T> implements ImmutableSuffixMapping<T> {

    private BinarySearchArraySuffixMapping(final Map<String, T> unsortedSuffixes) {
      super(false, unsortedSuffixes);
    }

    @Override
    public boolean matchesAnySuffix(final String string) {
      return getKeyAndValue(false, string, null) != null;
    }

    @Override
    public Pair<String, T> keyAndValueForSuffix(final String string) {
      return getKeyAndValue(true, string, null);
    }
  }

  public static ImmutablePrefixMatcher limitedCharArrayTriePrefixMatcher(
      final char min, final char max, final String... prefixes) {
    return new LimitedCharArrayTriePrefixMapping<>(min, max, toMap(prefixes));
  }

  public static <T> ImmutablePrefixMapping<T> limitedCharArrayTriePrefixMapping(
      final char min, final char max, final Map<String, T> prefixes) {
    return new LimitedCharArrayTriePrefixMapping<>(min, max, prefixes);
  }

  private static final class LimitedCharArrayTriePrefixMapping<T>
      extends LimitedCharArrayTrieFixMapping<T> implements ImmutablePrefixMapping<T> {
    private LimitedCharArrayTriePrefixMapping(
        final char min, final char max, final Map<String, T> prefixes) {
      super(true, min, max, prefixes);
    }

    @Override
    public boolean matchesAnyPrefix(final String string) {
      return getKeyAndValue(false, string, null) != null;
    }

    @Override
    public Pair<String, T> keyAndValueForPrefix(final String string) {
      return getKeyAndValue(true, string, null);
    }
  }

  public static ImmutableSuffixMatcher limitedCharArrayTrieSuffixMatcher(
      final char min, final char max, final String... suffixes) {
    return new LimitedCharArrayTrieSuffixMapping<>(min, max, toMap(suffixes));
  }

  public static <T> ImmutableSuffixMapping<T> limitedCharArrayTrieSuffixMapping(
      final char min, final char max, final Map<String, T> suffixes) {
    return new LimitedCharArrayTrieSuffixMapping<>(min, max, suffixes);
  }

  private static final class LimitedCharArrayTrieSuffixMapping<T>
      extends LimitedCharArrayTrieFixMapping<T> implements ImmutableSuffixMapping<T> {
    private LimitedCharArrayTrieSuffixMapping(
        final char min, final char max, final Map<String, T> suffixes) {
      super(false, min, max, suffixes);
    }

    @Override
    public boolean matchesAnySuffix(final String string) {
      return getKeyAndValue(false, string, null) != null;
    }

    @Override
    public Pair<String, T> keyAndValueForSuffix(final String string) {
      return getKeyAndValue(true, string, null);
    }
  }

  private FixMappings() {}
}
