package dev.stiemannkj1.util;

import java.util.HashMap;
import java.util.Map;

public final class MapBuilder<K, V> {

  public final Map<K, V> mapRef = new HashMap<>();

  public MapBuilder<K, V> add(final K key, V value) {
    mapRef.put(key, value);
    return this;
  }
}
