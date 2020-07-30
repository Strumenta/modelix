package org.modelix.model.util.pmap;

import org.modelix.model.util.StreamUtils;

import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class SmallPMap<K, V> implements CustomPMap<K, V> {
  private static final SmallPMap EMPTY = new Multiple(new Object[0], new Object[0]);
  public static <K, V> SmallPMap<K, V> empty() {
    return EMPTY;
  }
  private static <K, V> SmallPMap<K, V> create(Object[] keys, Object[] values) {
    if (keys.length == 0) {
      return empty();
    }
    if (keys.length == 1) {
      return new Single<K, V>((K) keys[0], (V) values[0]);
    }
    return new Multiple<K, V>(keys, values);
  }

  public static <K, V> SmallPMap<K, V> createS(Iterable<K> keys, Iterable<V> values) {
    return create(
            StreamUtils.toStream(keys).toArray(Object[]::new),
            StreamUtils.toStream(values).toArray(Object[]::new));
  }

  @Override
  public abstract V get(K key);
  @Override
  public abstract SmallPMap<K, V> put(K key, V value);
  @Override
  public abstract SmallPMap<K, V> remove(K key);
  @Override
  public abstract Iterable<K> keys();
  @Override
  public abstract Iterable<V> values();

  public static class Single<K, V> extends SmallPMap<K, V> {
    private K key;
    private V value;

    private Single(K key, V value) {
      this.key = key;
      this.value = value;
    }
    @Override
    public V get(K key) {
      return (Objects.equals(this.key, key) ? value : null);
    }
    @Override
    public Iterable<K> keys() {
      return Collections.singleton(key);
    }
    @Override
    public SmallPMap<K, V> put(K key, V value) {
      if (value == null) {
        return remove(key);
      }
      if (Objects.equals(key, this.key)) {
        if (Objects.equals(value, this.value)) {
          return this;
        } else {
          return new Single<K, V>(key, value);
        }
      }
      return create(new Object[]{this.key, key}, new Object[]{this.value, value});
    }
    @Override
    public SmallPMap<K, V> remove(K key) {
      return (Objects.equals(key, this.key) ? EMPTY : this);
    }
    @Override
    public Iterable<V> values() {
      return Collections.singleton(value);
    }
    @Override
    public boolean containsKey(K key) {
      return Objects.equals(key, this.key);
    }
  }

  public static class Multiple<K, V> extends SmallPMap<K, V> {
    private Object[] keys;
    private Object[] values;

    private Multiple(Object[] keys, Object[] values) {
      this.keys = keys;
      this.values = values;
    }

    @Override
    public V get(K key) {
      for (int i = 0; i < keys.length; i++) {
        if (Objects.equals(keys[i], key)) {
          return (V) values[i];
        }
      }
      return null;
    }

    @Override
    public SmallPMap<K, V> put(K key, V value) {
      if (value == null) {
        return remove(key);
      }

      int index = COWArrays.indexOf(keys, key);
      if (index != -1) {
        if (Objects.equals(value, values[index])) {
          return this;
        } else {
          return create(keys, COWArrays.set(values, index, value));
        }
      } else {
        return create(COWArrays.add(keys, key), COWArrays.add(values, value));
      }
    }

    @Override
    public SmallPMap<K, V> remove(K key) {
      int index = COWArrays.indexOf(keys, key);
      if (index != -1) {
        return create(COWArrays.removeAt(keys, index), COWArrays.removeAt(values, index));
      } else {
        return this;
      }
    }

    @Override
    public Iterable<K> keys() {
      return Stream.of(keys).map(it -> (K)it).collect(Collectors.toList());
    }

    @Override
    public Iterable<V> values() {
      return Stream.of(values).map(it -> (V)it).collect(Collectors.toList());
    }

    @Override
    public boolean containsKey(K key) {
      for (Object k : this.keys) {
        if (Objects.equals(k, key)) {
          return true;
        }
      }
      return false;
    }
  }
}