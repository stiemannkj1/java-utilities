package dev.stiemannkj1.collection.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.junit.jupiter.api.Test;

public final class EnumSetsTests {

  @Test
  void test_atomic_enum_set() {

    final Set<MyEnum> enumSet = EnumSets.atomic(MyEnum.class);
    assertFalse(enumSet.contains(MyEnum.FOO));
    assertFalse(enumSet.contains(MyEnum.BAR));
    assertFalse(enumSet.contains(MyEnum.BAZ));
    assertTrue(enumSet.isEmpty());
    assertEquals(0, enumSet.size());
    assertEquals(EnumSets.atomic(MyEnum.class), enumSet);
    assertEquals(EnumSets.atomic(MyEnum.class).hashCode(), enumSet.hashCode());

    assertTrue(enumSet.containsAll(Collections.<MyEnum>emptyList()));
    assertFalse(enumSet.containsAll(Collections.singleton(MyEnum.FOO)));
    assertFalse(enumSet.containsAll(Collections.singleton(MyEnum.BAR)));
    assertFalse(enumSet.containsAll(Collections.singleton(MyEnum.BAZ)));
    assertFalse(enumSet.containsAll(Arrays.asList(MyEnum.FOO, MyEnum.BAR)));
    assertFalse(enumSet.containsAll(Arrays.asList(MyEnum.BAR, MyEnum.BAZ)));
    assertFalse(enumSet.containsAll(Arrays.asList(MyEnum.FOO, MyEnum.BAZ)));
    assertFalse(enumSet.containsAll(Arrays.asList(MyEnum.FOO, MyEnum.BAR, MyEnum.BAZ)));

    assertTrue(enumSet.containsAll(EnumSets.atomic(MyEnum.class)));
    assertFalse(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.FOO)));
    assertFalse(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.BAR)));
    assertFalse(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.BAZ)));
    assertFalse(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.FOO, MyEnum.BAR)));
    assertFalse(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.BAR, MyEnum.BAZ)));
    assertFalse(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.FOO, MyEnum.BAZ)));
    assertFalse(
        enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.FOO, MyEnum.BAR, MyEnum.BAZ)));

    assertTrue(enumSet.add(MyEnum.FOO));
    assertFalse(enumSet.add(MyEnum.FOO));

    assertEquals(Collections.singleton(MyEnum.FOO), enumSet);
    assertEquals(atomicEnumSet(MyEnum.class, MyEnum.FOO), enumSet);
    assertEquals(atomicEnumSet(MyEnum.class, MyEnum.FOO).hashCode(), enumSet.hashCode());

    assertTrue(enumSet.contains(MyEnum.FOO));
    assertFalse(enumSet.contains(MyEnum.BAR));
    assertFalse(enumSet.contains(MyEnum.BAZ));

    assertFalse(enumSet.isEmpty());
    assertEquals(1, enumSet.size());

    assertTrue(enumSet.containsAll(Collections.<MyEnum>emptyList()));
    assertTrue(enumSet.containsAll(Collections.singleton(MyEnum.FOO)));
    assertFalse(enumSet.containsAll(Collections.singleton(MyEnum.BAR)));
    assertFalse(enumSet.containsAll(Collections.singleton(MyEnum.BAZ)));
    assertFalse(enumSet.containsAll(Arrays.asList(MyEnum.FOO, MyEnum.BAR)));
    assertFalse(enumSet.containsAll(Arrays.asList(MyEnum.BAR, MyEnum.BAZ)));
    assertFalse(enumSet.containsAll(Arrays.asList(MyEnum.FOO, MyEnum.BAZ)));
    assertFalse(enumSet.containsAll(Arrays.asList(MyEnum.FOO, MyEnum.BAR, MyEnum.BAZ)));

    assertTrue(enumSet.containsAll(EnumSets.atomic(MyEnum.class)));
    assertTrue(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.FOO)));
    assertFalse(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.BAR)));
    assertFalse(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.BAZ)));
    assertFalse(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.FOO, MyEnum.BAR)));
    assertFalse(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.BAR, MyEnum.BAZ)));
    assertFalse(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.FOO, MyEnum.BAZ)));
    assertFalse(
        enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.FOO, MyEnum.BAR, MyEnum.BAZ)));

    assertTrue(enumSet.add(MyEnum.BAR));
    assertFalse(enumSet.add(MyEnum.BAR));

    assertEquals(new HashSet<>(Arrays.asList(MyEnum.FOO, MyEnum.BAR)), enumSet);
    assertEquals(atomicEnumSet(MyEnum.class, MyEnum.FOO, MyEnum.BAR), enumSet);
    assertEquals(
        atomicEnumSet(MyEnum.class, MyEnum.FOO, MyEnum.BAR).hashCode(), enumSet.hashCode());

    assertTrue(enumSet.contains(MyEnum.FOO));
    assertTrue(enumSet.contains(MyEnum.BAR));
    assertFalse(enumSet.contains(MyEnum.BAZ));

    assertFalse(enumSet.isEmpty());
    assertEquals(2, enumSet.size());

    assertTrue(enumSet.containsAll(Collections.<MyEnum>emptyList()));
    assertTrue(enumSet.containsAll(Collections.singleton(MyEnum.FOO)));
    assertTrue(enumSet.containsAll(Collections.singleton(MyEnum.BAR)));
    assertFalse(enumSet.containsAll(Collections.singleton(MyEnum.BAZ)));
    assertTrue(enumSet.containsAll(Arrays.asList(MyEnum.FOO, MyEnum.BAR)));
    assertFalse(enumSet.containsAll(Arrays.asList(MyEnum.BAR, MyEnum.BAZ)));
    assertFalse(enumSet.containsAll(Arrays.asList(MyEnum.FOO, MyEnum.BAZ)));
    assertFalse(enumSet.containsAll(Arrays.asList(MyEnum.FOO, MyEnum.BAR, MyEnum.BAZ)));

    assertTrue(enumSet.containsAll(EnumSets.atomic(MyEnum.class)));
    assertTrue(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.FOO)));
    assertTrue(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.BAR)));
    assertFalse(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.BAZ)));
    assertTrue(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.FOO, MyEnum.BAR)));
    assertFalse(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.BAR, MyEnum.BAZ)));
    assertFalse(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.FOO, MyEnum.BAZ)));
    assertFalse(
        enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.FOO, MyEnum.BAR, MyEnum.BAZ)));

    assertTrue(enumSet.add(MyEnum.BAZ));
    assertFalse(enumSet.add(MyEnum.BAZ));

    assertEquals(new HashSet<>(Arrays.asList(MyEnum.FOO, MyEnum.BAR, MyEnum.BAZ)), enumSet);
    assertEquals(atomicEnumSet(MyEnum.class, MyEnum.FOO, MyEnum.BAR, MyEnum.BAZ), enumSet);
    assertEquals(
        atomicEnumSet(MyEnum.class, MyEnum.FOO, MyEnum.BAR, MyEnum.BAZ).hashCode(),
        enumSet.hashCode());

    assertTrue(enumSet.contains(MyEnum.FOO));
    assertTrue(enumSet.contains(MyEnum.BAR));
    assertTrue(enumSet.contains(MyEnum.BAZ));

    assertFalse(enumSet.isEmpty());
    assertEquals(3, enumSet.size());

    assertTrue(enumSet.containsAll(Collections.<MyEnum>emptyList()));
    assertTrue(enumSet.containsAll(Collections.singleton(MyEnum.FOO)));
    assertTrue(enumSet.containsAll(Collections.singleton(MyEnum.BAR)));
    assertTrue(enumSet.containsAll(Collections.singleton(MyEnum.BAZ)));
    assertTrue(enumSet.containsAll(Arrays.asList(MyEnum.FOO, MyEnum.BAR)));
    assertTrue(enumSet.containsAll(Arrays.asList(MyEnum.BAR, MyEnum.BAZ)));
    assertTrue(enumSet.containsAll(Arrays.asList(MyEnum.FOO, MyEnum.BAZ)));
    assertTrue(enumSet.containsAll(Arrays.asList(MyEnum.FOO, MyEnum.BAR, MyEnum.BAZ)));

    assertTrue(enumSet.containsAll(EnumSets.atomic(MyEnum.class)));
    assertTrue(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.FOO)));
    assertTrue(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.BAR)));
    assertTrue(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.BAZ)));
    assertTrue(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.FOO, MyEnum.BAR)));
    assertTrue(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.BAR, MyEnum.BAZ)));
    assertTrue(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.FOO, MyEnum.BAZ)));
    assertTrue(
        enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.FOO, MyEnum.BAR, MyEnum.BAZ)));

    assertTrue(enumSet.remove(MyEnum.BAR));
    assertFalse(enumSet.remove(MyEnum.BAR));

    assertTrue(enumSet.contains(MyEnum.FOO));
    assertFalse(enumSet.contains(MyEnum.BAR));
    assertTrue(enumSet.contains(MyEnum.BAZ));

    assertFalse(enumSet.isEmpty());
    assertEquals(2, enumSet.size());

    assertTrue(enumSet.containsAll(Collections.<MyEnum>emptyList()));
    assertTrue(enumSet.containsAll(Collections.singleton(MyEnum.FOO)));
    assertFalse(enumSet.containsAll(Collections.singleton(MyEnum.BAR)));
    assertTrue(enumSet.containsAll(Collections.singleton(MyEnum.BAZ)));
    assertFalse(enumSet.containsAll(Arrays.asList(MyEnum.FOO, MyEnum.BAR)));
    assertFalse(enumSet.containsAll(Arrays.asList(MyEnum.BAR, MyEnum.BAZ)));
    assertTrue(enumSet.containsAll(Arrays.asList(MyEnum.FOO, MyEnum.BAZ)));
    assertFalse(enumSet.containsAll(Arrays.asList(MyEnum.FOO, MyEnum.BAR, MyEnum.BAZ)));

    assertTrue(enumSet.containsAll(EnumSets.atomic(MyEnum.class)));
    assertTrue(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.FOO)));
    assertFalse(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.BAR)));
    assertTrue(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.BAZ)));
    assertFalse(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.FOO, MyEnum.BAR)));
    assertFalse(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.BAR, MyEnum.BAZ)));
    assertTrue(enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.FOO, MyEnum.BAZ)));
    assertFalse(
        enumSet.containsAll(atomicEnumSet(MyEnum.class, MyEnum.FOO, MyEnum.BAR, MyEnum.BAZ)));

    assertTrue(enumSet.removeAll(Arrays.asList(MyEnum.FOO, MyEnum.BAZ)));
    assertFalse(enumSet.removeAll(Arrays.asList(MyEnum.FOO, MyEnum.BAZ)));

    assertTrue(enumSet.isEmpty());
    assertEquals(0, enumSet.size());

    enumSet.add(MyEnum.FOO);
    enumSet.add(MyEnum.BAZ);

    assertTrue(enumSet.removeAll(atomicEnumSet(MyEnum.class, MyEnum.FOO, MyEnum.BAZ)));
    assertFalse(enumSet.removeAll(atomicEnumSet(MyEnum.class, MyEnum.FOO, MyEnum.BAZ)));

    assertTrue(enumSet.isEmpty());
    assertEquals(0, enumSet.size());

    enumSet.add(MyEnum.BAR);

    enumSet.add(MyEnum.FOO);

    assertTrue(enumSet.removeAll(Arrays.asList(MyEnum.FOO, MyEnum.BAZ)));
    assertFalse(enumSet.removeAll(Arrays.asList(MyEnum.FOO, MyEnum.BAZ)));
    assertEquals(Collections.singleton(MyEnum.BAR), enumSet);

    enumSet.add(MyEnum.FOO);

    assertTrue(enumSet.removeAll(atomicEnumSet(MyEnum.class, MyEnum.FOO, MyEnum.BAZ)));
    assertFalse(enumSet.removeAll(atomicEnumSet(MyEnum.class, MyEnum.FOO, MyEnum.BAZ)));
    assertEquals(Collections.singleton(MyEnum.BAR), enumSet);

    enumSet.add(MyEnum.FOO);
    enumSet.add(MyEnum.BAR);
    enumSet.add(MyEnum.BAZ);

    assertEquals(3, enumSet.size());

    enumSet.clear();
    assertEquals(0, enumSet.size());
    assertTrue(enumSet.isEmpty());

    assertTrue(enumSet.addAll(Arrays.asList(MyEnum.FOO, MyEnum.BAR, MyEnum.BAZ)));
    assertFalse(enumSet.addAll(Arrays.asList(MyEnum.FOO, MyEnum.BAR, MyEnum.BAZ)));
    assertEquals(new HashSet<>(Arrays.asList(MyEnum.values())), enumSet);

    assertFalse(enumSet.retainAll(Arrays.asList(MyEnum.FOO, MyEnum.BAR, MyEnum.BAZ)));
    assertEquals(new HashSet<>(Arrays.asList(MyEnum.values())), enumSet);

    assertTrue(enumSet.retainAll(Arrays.asList(MyEnum.FOO, MyEnum.BAZ)));
    assertFalse(enumSet.retainAll(Arrays.asList(MyEnum.FOO, MyEnum.BAZ)));
    assertEquals(new HashSet<>(Arrays.asList(MyEnum.FOO, MyEnum.BAZ)), enumSet);

    enumSet.clear();

    assertTrue(enumSet.addAll(Arrays.asList(MyEnum.FOO, MyEnum.BAZ)));
    assertFalse(enumSet.addAll(Arrays.asList(MyEnum.FOO, MyEnum.BAZ)));
    assertEquals(new HashSet<>(Arrays.asList(MyEnum.FOO, MyEnum.BAZ)), enumSet);
  }

  @Test
  void test_atomic_enum_set_iterator() {

    final Set<MyEnum> enumSet = EnumSets.atomic(MyEnum.class);

    enumSet.add(MyEnum.FOO);
    enumSet.add(MyEnum.BAR);
    enumSet.add(MyEnum.BAZ);

    Iterator<MyEnum> expectedIterator =
        Arrays.asList(MyEnum.FOO, MyEnum.BAR, MyEnum.BAZ).iterator();
    Iterator<MyEnum> actualIterator = enumSet.iterator();

    while (expectedIterator.hasNext()) {
      assertTrue(actualIterator.hasNext());
      assertEquals(actualIterator.next(), expectedIterator.next());
    }

    expectedIterator = Arrays.asList(MyEnum.FOO, MyEnum.BAR, MyEnum.BAZ).iterator();

    for (final MyEnum myEnum : enumSet) {
      assertTrue(expectedIterator.hasNext());
      assertEquals(expectedIterator.next(), myEnum);
    }

    expectedIterator = Arrays.asList(MyEnum.FOO, MyEnum.BAR, MyEnum.BAZ).iterator();

    actualIterator = enumSet.iterator();

    while (expectedIterator.hasNext()) {

      assertTrue(actualIterator.hasNext());
      final MyEnum expectedNext = expectedIterator.next();
      assertEquals(actualIterator.next(), expectedNext);

      if (expectedNext == MyEnum.BAR) {
        actualIterator.remove();
      }
    }

    // Removals affect the original hash set.
    assertEquals(enumSet, new HashSet<>(Arrays.asList(MyEnum.FOO, MyEnum.BAZ)));

    expectedIterator = Arrays.asList(MyEnum.FOO, MyEnum.BAZ).iterator();
    actualIterator = enumSet.iterator();

    while (expectedIterator.hasNext()) {
      assertTrue(actualIterator.hasNext());
      assertEquals(actualIterator.next(), expectedIterator.next());
    }

    enumSet.add(MyEnum.BAR);
    expectedIterator = Arrays.asList(MyEnum.FOO, MyEnum.BAR, MyEnum.BAZ).iterator();

    actualIterator = enumSet.iterator();

    while (expectedIterator.hasNext()) {

      assertTrue(actualIterator.hasNext());
      final MyEnum expectedNext = expectedIterator.next();
      assertEquals(actualIterator.next(), expectedNext);

      if (expectedNext == MyEnum.BAR) {
        enumSet.remove(MyEnum.BAR);

        // Already removed item is ignored.
        actualIterator.remove();
      }
    }

    assertEquals(enumSet, new HashSet<>(Arrays.asList(MyEnum.FOO, MyEnum.BAZ)));
  }

  private static <E extends Enum<E>> Set<E> atomicEnumSet(final Class<E> enumClass, final E e1) {
    final Set<E> enumSet = EnumSets.atomic(enumClass);
    enumSet.add(e1);
    return enumSet;
  }

  private static <E extends Enum<E>> Set<E> atomicEnumSet(
      final Class<E> enumClass, final E e1, final E e2) {
    final Set<E> enumSet = EnumSets.atomic(enumClass);
    enumSet.add(e1);
    enumSet.add(e2);
    return enumSet;
  }

  private static <E extends Enum<E>> Set<E> atomicEnumSet(
      final Class<E> enumClass, final E e1, final E e2, final E e3) {
    final Set<E> enumSet = EnumSets.atomic(enumClass);
    enumSet.add(e1);
    enumSet.add(e2);
    enumSet.add(e3);
    return enumSet;
  }

  enum MyEnum {
    FOO,
    BAR,
    BAZ
  }
}
