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

class LimitedCharArrayTrieFixMap<T> {

  private static final class Node<T> {
    private final Node<T>[] nodes;
    private final Pair<String, T> keyValuePair;

    private Node(final Node<T>[] nodes, final Pair<String, T> keyValuePair) {
      if (nodes == null && keyValuePair == null) {
        throw new NullPointerException();
      }

      if (nodes != null && keyValuePair != null) {
        throw new IllegalArgumentException(
            "Node may not be a terminal node and include a subtree.");
      }

      this.nodes = nodes;
      this.keyValuePair = keyValuePair;
    }

    private static <T> Node<T> subtree(final Node<T>[] nodes) {
      return new Node<>(nodes, null);
    }

    private static <T> Node<T> terminal(final Map.Entry<String, T> entry) {
      return new Node<>(null, Pair.fromEntry(entry));
    }

    private boolean isTerminal() {
      return this.keyValuePair != null;
    }
  }

  private final boolean forPrefix;
  private final Node<T>[] nodes;
  private final char min;
  private final int offset;
  private final char max;
  private final int minPrefixLength;

  LimitedCharArrayTrieFixMap(
      final boolean forPrefix, final char min, final char max, final Map<String, T> fixes) {

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

        final boolean terminates = (i == length - 1);

        final int charAsIndex = toIndex(char_, offset);

        Node<T> currentNode = currentNodes[charAsIndex];

        if (currentNode != null
            && (currentNode.isTerminal() || (terminates && !currentNode.isTerminal()))) {
          if (forPrefix) {
            throw new IllegalArgumentException(
                "\""
                    + fixString
                    + "\" matches another prefix.  Prefixes that are prefixes of each other are not allowed.");
          } else {
            throw new IllegalArgumentException(
                "\""
                    + fixString
                    + "\" matches another suffix.  Suffixes that are suffixes of each other are not allowed.");
          }
        }

        if (terminates) {
          currentNodes[charAsIndex] = Node.terminal(fix);
          break;
        }

        if (currentNode == null) {

          @SuppressWarnings("unchecked")
          final Node<T>[] nodes = new Node[trieNodeLength];
          currentNode = Node.subtree(nodes);
          currentNodes[charAsIndex] = currentNode;
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

  Pair<String, T> getKeyAndValue(final String string) {

    if (string == null) {
      return null;
    }

    final int length = string.length();

    if (length < minPrefixLength) {
      return null;
    }

    Node<T>[] currentNodes = nodes;

    for (int i = 0; i < length; i++) {

      final char char_ = string.charAt(forPrefix ? i : length - i - 1);

      if (char_ < min || max < char_) {
        return null;
      }

      final Node<T> currentNode = currentNodes[toIndex(char_, offset)];

      if (currentNode == null) {
        return null;
      }

      if (currentNode.isTerminal()) {
        return currentNode.keyValuePair;
      }

      currentNodes = currentNode.nodes;
    }

    return null;
  }
}
