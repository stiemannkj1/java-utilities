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
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

class LimitedCharArrayTrieFixMapping<T> implements FixMappings.FixMapping<T> {

  private static final class Node<T> {
    private Node<T>[] nodes;
    private Pair<String, T> keyValuePair;

    private boolean isMatch() {
      return this.keyValuePair != null;
    }
  }

  private final boolean forPrefix;
  private final Node<T>[] nodes;
  private final char min;
  private final int offset;
  private final char max;
  private final int minPrefixLength;

  LimitedCharArrayTrieFixMapping(
      final boolean forPrefix, final char min, final char max, final Map<String, T> fixes) {

    // TODO implement Patricia Trie to save space

    if (max < min) {
      throw new IllegalArgumentException("Max character must be more than min character.");
    }

    if (fixes.isEmpty()) {
      throw new IllegalArgumentException(
          (forPrefix ? "Prefixes" : "Suffixes") + " cannot be empty.");
    }

    this.forPrefix = forPrefix;
    this.min = min;
    this.max = max;
    this.offset = prevPowerOf2(min);

    int trieNodeLength = nextPowerOf2(toIndex(max, offset));

    @SuppressWarnings("unchecked")
    final Node<T>[] initialNodes = new Node[trieNodeLength];
    this.nodes = initialNodes;

    int minLength = Integer.MAX_VALUE;

    for (final Map.Entry<String, T> fix : fixes.entrySet()) {

      if (fix == null) {
        throw new IllegalArgumentException("Empty keys are not allowed.");
      }

      final String fixString = fix.getKey();

      if (fix.getKey() == null) {
        throw new IllegalArgumentException("Empty keys are not allowed.");
      }

      final int length = fixString.length();

      if (length == 0) {
        throw new IllegalArgumentException("Empty keys are not allowed.");
      }

      if (length < minLength) {
        minLength = length;
      }

      Node<T>[] currentNodes = this.nodes;

      for (int i = 0; i < length; i++) {

        final char char_ = fixString.charAt(forPrefix ? i : length - i - 1);

        if (char_ < min) {
          throw new IllegalArgumentException(
              char_ + " is less than minimum allowed character of " + min);
        }

        if (char_ > max) {
          throw new IllegalArgumentException(
              char_ + " is more than maximum allowed character of " + min);
        }

        final boolean endOfMatch = (i == length - 1);

        final int charAsIndex = toIndex(char_, offset);

        Node<T> currentNode = currentNodes[charAsIndex];

        if (currentNode == null) {
          currentNode = new Node<>();
          currentNodes[charAsIndex] = currentNode;
        } else if (endOfMatch && currentNode.isMatch()) {
          throw new IllegalArgumentException("Duplicate keys found for: " + fixString);
        }

        if (endOfMatch) {
          currentNode.keyValuePair = Pair.fromEntry(fix);
          break;
        } else if (currentNode.nodes == null) {
          @SuppressWarnings("unchecked")
          final Node<T>[] nodes = new Node[trieNodeLength];
          currentNode.nodes = nodes;
        }

        currentNodes = currentNode.nodes;
      }
    }

    this.minPrefixLength = minLength;
  }

  static int nextPowerOf2(int v) {

    // https://graphics.stanford.edu/~seander/bithacks.html#RoundUpPowerOf2
    v--;
    v |= v >> 1;
    v |= v >> 2;
    v |= v >> 4;
    v |= v >> 8;
    v |= v >> 16;
    v++;

    return v;
  }

  static int prevPowerOf2(final int v) {

    // https://graphics.stanford.edu/~seander/bithacks.html#DetermineIfPowerOf2
    if ((v & (v - 1)) == 0) {
      return v;
    }

    return nextPowerOf2(v) >> 1;
  }

  static int toIndex(final char char_, final int offset) {
    // Offset is always a power of 2, so we can safely subtract using XOR.
    return char_ ^ offset;
  }

  @VisibleForTesting
  @Override
  public Pair<String, T> getKeyAndValue(
      final boolean getLongestMatch, final String string, final AtomicLong debugSearchSteps) {

    if (string == null) {
      return null;
    }

    final int length = string.length();

    if (length < minPrefixLength) {
      return null;
    }

    Node<T>[] currentNodes = nodes;
    Pair<String, T> lastMatch = null;

    for (int i = 0; i < length; i++) {

      if (debugSearchSteps != null) {
        debugSearchSteps.incrementAndGet();
      }

      final char char_ = string.charAt(forPrefix ? i : length - i - 1);

      if (char_ < min || max < char_) {
        break;
      }

      final Node<T> currentNode = currentNodes[toIndex(char_, offset)];

      if (currentNode == null) {
        break;
      }

      if (currentNode.isMatch()) {

        if (!getLongestMatch) {
          return currentNode.keyValuePair;
        }

        lastMatch = currentNode.keyValuePair;
      }

      if (currentNode.nodes == null) {
        break;
      }

      currentNodes = currentNode.nodes;
    }

    return lastMatch;
  }
}
