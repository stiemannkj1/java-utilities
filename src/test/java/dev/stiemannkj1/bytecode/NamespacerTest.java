package dev.stiemannkj1.bytecode;

import dev.stiemannkj1.allocator.Allocators;
import dev.stiemannkj1.collection.arrays.GrowableArrays;
import dev.stiemannkj1.collection.arrays.GrowableArrays.GrowableByteArray;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static dev.stiemannkj1.collection.arrays.GrowableArrays.GrowableByteArray.bytes;
import static dev.stiemannkj1.collection.arrays.GrowableArrays.GrowableByteArray.size;
import static dev.stiemannkj1.util.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class NamespacerTest {

    @Test
    void it_namespaces_class() throws Throwable {
        final GrowableByteArray classFileBefore = Allocators.JVM_HEAP.allocateObject(GrowableByteArray::new);
        readBytes(ClassToNamespace.class, classFileBefore);
        final Map<String, String> replacement = new HashMap<>();
        replacement.put("dev.stiemannkj1.bytecode", "now.im.namespaced");
        replacement.put("dev/stiemannkj1/bytecode", "now/im/namespaced");
        replacement.put("Ldev/stiemannkj1/bytecode", "Lnow/im/namespaced");
        final ClassGenerator classGenerator = new ClassGenerator(this.getClass().getClassLoader());
        final GrowableByteArray classFileAfter = Allocators.JVM_HEAP.allocateObject(GrowableByteArray::new);
        assertNull(Namespacer.namespace(Allocators.JVM_HEAP, classNameToPath(ClassToNamespace.class), classFileBefore, replacement, classFileAfter));

        try {
            final Class<?> namespacedClass = classGenerator.generateClass("now.im.namespaced.NamespacerTest$ClassToNamespace", bytes(classFileAfter), 0, size(classFileAfter));
            assertEquals("now.im.namespaced.NamespacerTest$ClassToNamespace", namespacedClass.getTypeName());
            assertEquals("now.im.namespaced.toString", namespacedClass.getDeclaredConstructor().newInstance().toString());
        } catch (final Throwable t) {
            try {
                final byte[] failedClass = new byte[size(classFileAfter)];
                System.arraycopy(bytes(classFileAfter), 0, failedClass, 0, size(classFileAfter));
                final File failedClassFile = new File(System.getProperty("project.build.dir") + File.separator + this.getClass().getSimpleName() + File.separator + ClassToNamespace.class.getTypeName().replace(".", File.separator) + ".class");
                final boolean ignored = failedClassFile.getParentFile().mkdirs();
                Files.write(failedClassFile.toPath(), failedClass);
            } catch (final IOException e) {
                t.addSuppressed(e);
            }
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

    public static final class ClassToNamespace {

        public ClassToNamespace() {}

        @Override
        public String toString() {
            return "dev.stiemannkj1.bytecode.toString";
        }
    }
}
