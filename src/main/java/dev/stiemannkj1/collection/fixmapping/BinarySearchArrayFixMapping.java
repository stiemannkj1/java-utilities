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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

class BinarySearchArrayFixMapping<T> implements FixMappings.FixMapping<T> {

  private final List<Pair<String, T>> sortedFixes;
  private final int minPrefixLength;
  private final boolean forPrefix;

  BinarySearchArrayFixMapping(final boolean forPrefix, final Map<String, T> unsortedFixes) {

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
                  forPrefix, pair1.first.length() > pair2.first.length(), pair2.first, pair1.first);

          if (pair1.first.length() == pair2.first.length() && compareTo == 0) {
            throw new IllegalArgumentException("Duplicate keys found for: " + pair1.first);
          }

          return compareTo;
        });

    this.sortedFixes = Collections.unmodifiableList(sortedFixes);
    this.minPrefixLength = minPrefixLength;
    this.forPrefix = forPrefix;
  }

  @VisibleForTesting
  @Override
  public Pair<String, T> getKeyAndValue(
      final boolean getLongestMatch, final String string, final AtomicLong debugSearchSteps) {

    if (string == null) {
      return null;
    }

    if (string.length() < minPrefixLength) {
      return null;
    }

    int max = sortedFixes.size() - 1;
    int min = 0;
    int bisect;
    Pair<String, T> lastMatch = null;

    while (min <= max) {

      if (debugSearchSteps != null) {
        debugSearchSteps.incrementAndGet();
      }

      // TODO change to bitwise division
      bisect = ((max - min) / 2) + min;

      final Pair<String, T> fixPair = sortedFixes.get(bisect);
      final String fix = fixPair.first;
      int compareTo = compareFix(forPrefix, fix.length() > string.length(), string, fix);

      if (compareTo == 0) {

        if (fix.length() == string.length()) {
          return fixPair;
        } else if (fix.length() < string.length()) {

          if (!getLongestMatch) {
            return fixPair;
          }

          lastMatch = fixPair;
          compareTo = -1;
        } else {
          compareTo = 1;
        }
      }

      if (compareTo > 0) {
        max = bisect - 1;
      } else {
        min = bisect + 1;
      }
    }

    return lastMatch;
  }

  private static int compareFix(
      final boolean forPrefix,
      final boolean fixLargerThanSearchString,
      final String string,
      final String fix) {

    if (forPrefix) {
      return fixLargerThanSearchString
          ? string.compareTo(fix.substring(0, string.length()))
          : string.substring(0, fix.length()).compareTo(fix);
    }

    return fixLargerThanSearchString
        ? string.compareTo(fix.substring(fix.length() - string.length()))
        : string.substring(string.length() - fix.length()).compareTo(fix);
  }
}
