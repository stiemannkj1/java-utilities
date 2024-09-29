package dev.stiemannkj1.util;

import java.util.Collection;
import java.util.Map;

public final class Require {

  public static <T> T notNull(final T t, final String name) {

    if (t == null) {
      throw new NullPointerException(name + " must not be null.");
    }

    return t;
  }

  public static <K, V> Map<K, V> notEmpty(final Map<K, V> map, final String name) {

    if (notNull(map, name).isEmpty()) {
      throw new IllegalStateException(name + " must not be empty.");
    }

    return map;
  }

  public static <T> Collection<T> notEmpty(final Collection<T> collection, final String name) {

    if (notNull(collection, name).isEmpty()) {
      throw new IllegalStateException(name + " must not be empty.");
    }

    return collection;
  }

  public static <T extends CharSequence> T notEmpty(final T string, final String name) {

    if (notNull(string, name).length() == 0) {
      throw new IllegalStateException(name + " must not be empty.");
    }

    return string;
  }

  public static <T> T[] notEmpty(final T[] array, final String name) {

    if (notNull(array, name).length == 0) {
      throw new IllegalStateException(name + " must not be empty.");
    }

    return array;
  }

  private Require() {}
}
