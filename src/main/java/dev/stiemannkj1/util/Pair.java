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
package dev.stiemannkj1.util;

import java.util.Map;
import java.util.Objects;

public class Pair<T1, T2> implements Map.Entry<T1, T2> {
  public final T1 first;
  public final T2 second;

  private Pair(final T1 first, final T2 second) {
    this.first = first;
    this.second = second;
  }

  public T1 first() {
    return first;
  }

  public T2 second() {
    return second;
  }

  public T1 left() {
    return first;
  }

  public T2 right() {
    return second;
  }

  public T1 key() {
    return first;
  }

  public T2 value() {
    return second;
  }

  @Override
  public T1 getKey() {
    return first;
  }

  @Override
  public T2 getValue() {
    return second;
  }

  @Override
  public T2 setValue(final T2 value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean equals(final Object o) {

    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final Pair<?, ?> pair = (Pair<?, ?>) o;

    return Objects.equals(first, pair.first) && Objects.equals(second, pair.second);
  }

  @Override
  public int hashCode() {
    return Objects.hash(first, second);
  }

  @Override
  public String toString() {
    return "{" + first + ',' + second + '}';
  }

  public static <T1, T2> Pair<T1, T2> of(final T1 first, final T2 second) {
    return new Pair<>(first, second);
  }

  public static <K, V> Pair<K, V> fromEntry(final Map.Entry<K, V> entry) {
    return new Pair<>(entry.getKey(), entry.getValue());
  }

  public static final class NameValue<V> extends Pair<String, V> {
    private NameValue(final String name, final V value) {
      super(name, value);
    }

    public String name() {
      return first;
    }
  }

  public static <V> NameValue<V> nameValuePair(final String name, final V value) {
    return new NameValue<>(name, value);
  }
}
