package dev.stiemannkj1.bytecode;

import static dev.stiemannkj1.collection.arrays.GrowableArrays.GrowableByteArray.bytes;
import static dev.stiemannkj1.collection.arrays.GrowableArrays.GrowableByteArray.size;
import static dev.stiemannkj1.util.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.stiemannkj1.allocator.Allocators;
import dev.stiemannkj1.collection.arrays.GrowableArrays.GrowableByteArray;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class NamespacerTest {

  @Test
  void it_namespaces_class() throws Throwable {
    final GrowableByteArray classFileBefore =
        Allocators.JVM_HEAP.allocateObject(GrowableByteArray::new);
    readBytes(ClassToNamespace.class, classFileBefore);
    final Map<String, String> replacement = new HashMap<>();
    replacement.put("dev.stiemannkj1.bytecode", "now.im.namespaced");
    replacement.put("dev/stiemannkj1/bytecode", "now/im/namespaced");
    replacement.put("Ldev/stiemannkj1/bytecode", "Lnow/im/namespaced");
    final ClassGenerator classGenerator = new ClassGenerator(this.getClass().getClassLoader());
    final GrowableByteArray classFileAfter =
        Allocators.JVM_HEAP.allocateObject(GrowableByteArray::new);
    assertNull(
        Namespacer.namespace(
            Allocators.JVM_HEAP,
            classNameToPath(ClassToNamespace.class),
            classFileBefore,
            replacement,
            classFileAfter));

    String className = "now.im.namespaced.NamespacerTest$ClassToNamespace";

    try {
      final Class<?> namespacedClass =
          classGenerator.generateClass(className, bytes(classFileAfter), 0, size(classFileAfter));

      assertEquals(className, namespacedClass.getTypeName());
      final Object namespaced = namespacedClass.getDeclaredConstructor().newInstance();
      assertEquals("now.im.namespaced.toString", namespaced.toString());

      GrowableByteArray.clear(classFileBefore);
      GrowableByteArray.clear(classFileAfter);
      readBytes(AbstractGenericClassToNamespace.class, classFileBefore);
      assertNull(
              Namespacer.namespace(
                      Allocators.JVM_HEAP,
                      classNameToPath(AbstractGenericClassToNamespace.class),
                      classFileBefore,
                      replacement,
                      classFileAfter));

      className = "now.im.namespaced.NamespacerTest$AbstractGenericClassToNamespace";
      final Class<?> genericParent = classGenerator.generateClass(className, bytes(classFileAfter), 0, size(classFileAfter));

      GrowableByteArray.clear(classFileBefore);
      GrowableByteArray.clear(classFileAfter);
      readBytes(ConcreteGenericClassToNamespace.class, classFileBefore);
      assertNull(
              Namespacer.namespace(
                      Allocators.JVM_HEAP,
                      classNameToPath(ConcreteGenericClassToNamespace.class),
                      classFileBefore,
                      replacement,
                      classFileAfter));

      className = "now.im.namespaced.NamespacerTest$ConcreteGenericClassToNamespace";
      final Class<?> genericClass = classGenerator.generateClass(className, bytes(classFileAfter), 0, size(classFileAfter));
      final Object generic = invoke(genericClass, null, "<init>", new Class<?>[0], new Object[0]);

      assertEquals(namespaced, invoke(genericParent, null, "staticGenericReturnAndArg", new Class[] { namespacedClass }, new Object[] { namespaced }));
      assertEquals(namespaced, invoke(genericParent, null, "staticReturnAndArg", new Class[] { namespacedClass }, new Object[] { namespaced }));
      assertEquals(namespaced, invoke(genericParent, generic, "virtualGenericReturnAndArg", new Class[] { namespacedClass }, new Object[] { namespaced }));
      assertEquals(namespaced, invoke(genericParent, generic, "virtualReturnAndArg", new Class[] { namespacedClass }, new Object[] { namespaced }));
      assertEquals(namespaced, invoke(genericClass, generic, "abstractGenericReturnAndArg", new Class[] { namespacedClass }, new Object[] { namespaced }));
      assertEquals(namespaced, invoke(genericClass, generic, "abstractReturnAndArg", new Class[] { namespacedClass }, new Object[] { namespaced }));
    } catch (final Throwable t) {
      ClassGenerator.writeClass(t, className, classFileAfter);
      throw t;
    }
  }

  private static String classNameToPath(final Class<?> aClass) {
    return aClass.getTypeName().replace('.', '/') + ".class";
  }

  private static void readBytes(final Class<?> aClass, final GrowableByteArray array) {

    final URL url = aClass.getResource('/' + classNameToPath(aClass));
    final int maxSize = 1 << 20;
    GrowableByteArray.growIfNecessary(array, maxSize);

    try (final InputStream inputStream = assertNotNull(url.openStream())) {

      final int read = GrowableByteArray.read(inputStream, array, maxSize);

      if (read >= maxSize) {
        throw new RuntimeException("Test class file was larger than " + maxSize);
      }
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Object invoke(final Class<?> aClass, final Object object, final String methodName, final Class<?>[] argTypes, final Object[] args) throws ReflectiveOperationException {

    if ("<init>".equals(methodName)) {
      return aClass.getConstructor(argTypes).newInstance(args);
    }
     return aClass.getMethod(methodName, argTypes).invoke(object, args);
  }

  public static final class ClassToNamespace {

    public ClassToNamespace() {}

    @Override
    public String toString() {
      return "dev.stiemannkj1.bytecode.toString";
    }
  }

  public abstract static class AbstractGenericClassToNamespace<T extends ClassToNamespace> {

    public AbstractGenericClassToNamespace() {}

    public static <T extends ClassToNamespace> T staticGenericReturnAndArg(final T t) {
      return t;
    }

    public T virtualGenericReturnAndArg(final T t) {
      return t;
    }

    public abstract T abstractGenericReturnAndArg(final T t);

    public static ClassToNamespace staticReturnAndArg(final ClassToNamespace t) {
      return t;
    }

    public ClassToNamespace virtualReturnAndArg(final ClassToNamespace t) {
      return t;
    }

    public abstract ClassToNamespace abstractReturnAndArg(final ClassToNamespace t);
  }

  public static final class ConcreteGenericClassToNamespace
      extends AbstractGenericClassToNamespace<ClassToNamespace> {

    public ConcreteGenericClassToNamespace() {}

    @Override
    public ClassToNamespace abstractGenericReturnAndArg(final ClassToNamespace classToNamespace) {
      return classToNamespace;
    }

    @Override
    public ClassToNamespace abstractReturnAndArg(final ClassToNamespace classToNamespace) {
      return classToNamespace;
    }
  }
}
