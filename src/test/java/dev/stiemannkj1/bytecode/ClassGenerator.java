package dev.stiemannkj1.bytecode;

public final class ClassGenerator extends ClassLoader {

  public Class<?> generateClass(
      final String className, final byte[] classBytes, final int offset, final int length) {
    return defineClass(className, classBytes, offset, length);
  }

  ClassGenerator(final ClassLoader parent) {
    super(parent);
  }
}
