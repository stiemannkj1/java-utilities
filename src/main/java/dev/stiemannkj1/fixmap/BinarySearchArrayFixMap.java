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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class BinarySearchArrayFixMap<T> {

  private final List<Pair<String, T>> sortedFixes;
  private final int minPrefixLength;
  private final boolean forPrefix;

  BinarySearchArrayFixMap(final boolean forPrefix, final Map<String, T> unsortedFixes) {

    final int size = unsortedFixes.size();

    if (size == 0) {
      throw new IllegalArgumentException(
          (forPrefix ? "Prefixes" : "Suffixes") + " cannot be empty.");
    }

    int minPrefixLength = Integer.MAX_VALUE;
    final List<Pair<String, T>> sortedFixes = new ArrayList<>(size);

    for (final Map.Entry<String, T> entry : unsortedFixes.entrySet()) {

      if (entry == null || entry.getKey() == null) {
        throw new IllegalArgumentException("Empty keys are not allowed.");
      }

      final int length = entry.getKey().length();

      if (length == 0) {
        throw new IllegalArgumentException("Empty keys are not allowed.");
      }

      if (length < minPrefixLength) {
        minPrefixLength = length;
      }

      final Pair<String, T> fix = Pair.fromEntry(entry);

      sortedFixes.add(fix);
    }

    sortedFixes.sort(
        (pair1, pair2) -> {
          final int compareTo =
              compareFix(
                  forPrefix, pair1.first.length() > pair2.first.length(), pair1.first, pair2.first);

          if (compareTo == 0) {
            throw new IllegalArgumentException("Duplicate keys found for: " + pair1.first);
          }

          return compareTo;
        });

    for (int i = 0; i < (sortedFixes.size() - 1); i++) {
      validateNeitherMatch(forPrefix, sortedFixes.get(i).first, sortedFixes.get(i + 1).first);
    }

    this.sortedFixes = Collections.unmodifiableList(sortedFixes);
    this.minPrefixLength = minPrefixLength;
    this.forPrefix = forPrefix;
  }

  private static void validateNeitherMatch(
      final boolean forPrefix, final String string1, final String string2) {

    if (string1.length() > string2.length()) {
      validateNotFix(forPrefix, string1, string2);
    } else if (string1.length() < string2.length()) {
      validateNotFix(forPrefix, string2, string1);
    }
  }

  private static void validateNotFix(
      final boolean forPrefix, final String string, final String possibleFix) {

    if (forPrefix && string.startsWith(possibleFix)) {
      throw new IllegalArgumentException(
          "\""
              + possibleFix
              + "\" is a prefix of \""
              + string
              + "\".  Prefixes that are prefixes of each other are not allowed.");
    }

    if (string.endsWith(possibleFix)) {
      throw new IllegalArgumentException(
          "\""
              + possibleFix
              + "\" is a suffix of \""
              + string
              + "\". Suffixes that are suffixes of each other are not allowed.");
    }
  }

  T getValue(final String string) {

    if (string == null) {
      return null;
    }

    if (string.length() < minPrefixLength) {
      return null;
    }

    int max = sortedFixes.size() - 1;
    int min = 0;
    int bisect;

    while (min <= max) {

      bisect = ((max - min) / 2) + min;

      final Pair<String, T> fixPair = sortedFixes.get(bisect);
      final String fix = fixPair.first;
      int compareTo;

      if (fix.length() > string.length()) {
        compareTo = compareFix(forPrefix, true, fix, string);

        if (compareTo == 0) {
          // We don't allow *fixes of *fixes in the map. If the *fix ends with the string to search,
          // it will not match any other *fix in the map.
          return null;
        }
      } else {
        compareTo = compareFix(forPrefix, false, fix, string);
      }

      if (compareTo == 0) {
        return fixPair.second;
      }

      if (compareTo > 0) {
        max = bisect - 1;
      } else {
        min = bisect + 1;
      }
    }

    return null;
  }

  private static int compareFix(
      final boolean forPrefix,
      final boolean fixLargerThanSearchString,
      final String fix,
      final String string) {

    if (forPrefix) {
      return fixLargerThanSearchString
          ? fix.substring(0, string.length()).compareTo(string)
          : fix.compareTo(string.substring(0, fix.length()));
    }

    return fixLargerThanSearchString
        ? fix.substring(fix.length() - string.length()).compareTo(string)
        : fix.compareTo(string.substring(string.length() - fix.length()));
  }
}
