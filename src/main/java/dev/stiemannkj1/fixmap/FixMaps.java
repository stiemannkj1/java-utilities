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

import dev.stiemannkj1.util.Pair;
import java.util.Map;

public final class FixMaps {

  public interface ImmutablePrefixMap<T> {

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

  public interface ImmutableSuffixMap<T> {

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

  public static <T> ImmutablePrefixMap<T> binarySearchArrayPrefixMap(
      final Map<String, T> prefixes) {
    return new BinarySearchArrayPrefixMap<>(prefixes);
  }

  private static final class BinarySearchArrayPrefixMap<T> extends BinarySearchArrayFixMap<T>
      implements ImmutablePrefixMap<T> {

    private BinarySearchArrayPrefixMap(final Map<String, T> unsortedPrefixes) {
      super(true, unsortedPrefixes);
    }

    @Override
    public Pair<String, T> keyAndValueForPrefix(final String string) {
      return getKeyAndValue(string);
    }
  }

  public static <T> ImmutableSuffixMap<T> binarySearchArraySuffixMap(
      final Map<String, T> suffixes) {
    return new BinarySearchArraySuffixMap<>(suffixes);
  }

  private static final class BinarySearchArraySuffixMap<T> extends BinarySearchArrayFixMap<T>
      implements ImmutableSuffixMap<T> {

    private BinarySearchArraySuffixMap(final Map<String, T> unsortedSuffixes) {
      super(false, unsortedSuffixes);
    }

    @Override
    public Pair<String, T> keyAndValueForSuffix(final String string) {
      return getKeyAndValue(string);
    }
  }

  public static <T> ImmutablePrefixMap<T> limitedCharArrayTriePrefixMap(
      final char min, final char max, final Map<String, T> prefixes) {
    return new LimitedCharArrayTriePrefixMap<>(min, max, prefixes);
  }

  private static final class LimitedCharArrayTriePrefixMap<T> extends LimitedCharArrayTrieFixMap<T>
      implements ImmutablePrefixMap<T> {
    private LimitedCharArrayTriePrefixMap(
        final char min, final char max, final Map<String, T> prefixes) {
      super(true, min, max, prefixes);
    }

    @Override
    public Pair<String, T> keyAndValueForPrefix(final String string) {
      return getKeyAndValue(string);
    }
  }

  public static <T> ImmutableSuffixMap<T> limitedCharArrayTrieSuffixMap(
      final char min, final char max, final Map<String, T> suffixes) {
    return new LimitedCharArrayTrieSuffixMap<>(min, max, suffixes);
  }

  private static final class LimitedCharArrayTrieSuffixMap<T> extends LimitedCharArrayTrieFixMap<T>
      implements ImmutableSuffixMap<T> {
    private LimitedCharArrayTrieSuffixMap(
        final char min, final char max, final Map<String, T> suffixes) {
      super(false, min, max, suffixes);
    }

    @Override
    public Pair<String, T> keyAndValueForSuffix(final String string) {
      return getKeyAndValue(string);
    }
  }

  private FixMaps() {}
}
