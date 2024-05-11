package dev.stiemannkj1.bytecode;

import dev.stiemannkj1.allocator.Allocators;
import dev.stiemannkj1.collection.arrays.GrowableArrays;
import dev.stiemannkj1.collection.arrays.GrowableArrays.GrowableByteArray;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static dev.stiemannkj1.util.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class NamespacerTest {

    @Test
    void it_namespaces_class() throws ReflectiveOperationException {
        final Map<String, String> replacement = new HashMap<>();
        replacement.put("dev.stiemannkj1.bytecode", "now.im.namespaced");
        final ClassGenerator classGenerator = new ClassGenerator(this.getClass().getClassLoader());
        final GrowableByteArray classFileAfter = Allocators.JVM_HEAP.allocateObject(GrowableByteArray::new);
        assertNull(Namespacer.namespace(Allocators.JVM_HEAP, classNameToPath(ClassToNamespace.class), readBytes(ClassToNamespace.class), replacement, classFileAfter));
        final Class<?> namespacedClass = classGenerator.generateClass(ClassToNamespace.class.getTypeName(), classFileAfter.array, 0, classFileAfter.size);
       assertEquals("now.im.namespaced.NamespacerTest$ClassToNamespace", namespacedClass.getTypeName());
        assertEquals("now.im.namespaced.toString", namespacedClass.getDeclaredConstructor().newInstance().toString());
    }

    private static String classNameToPath(final Class<?> aClass) {
        return aClass.getTypeName().replace('.', '/') + ".class";
    }

    private static byte[] readBytes(final Class<?> aClass) {

        final URL url = aClass.getResource('/' + classNameToPath(aClass));

        final int maxSize = 1 << 19;
        final byte[] classBytes = new byte[maxSize];

        try (final InputStream inputStream = assertNotNull(url.openStream())) {

            if (inputStream.read(classBytes, 0, classBytes.length) >= maxSize) {
              throw new RuntimeException("Test class file was larger than " + maxSize);
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        return classBytes;
    }

    public static final class ClassToNamespace {

        public ClassToNamespace() {}

        @Override
        public String toString() {
            return "dev.stiemannkj1.bytecode.toString";
        }
    }
}
