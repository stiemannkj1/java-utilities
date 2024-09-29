package dev.stiemannkj1.bytecode.namespacer;

import static dev.stiemannkj1.bytecode.ClassGenerator.ClassUtil.classAsStream;
import static dev.stiemannkj1.collection.arrays.GrowableArrays.GrowableByteArray.bytes;
import static dev.stiemannkj1.collection.arrays.GrowableArrays.GrowableByteArray.size;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.stiemannkj1.bytecode.ClassGenerator;
import dev.stiemannkj1.bytecode.namespacer.Namespacer.ObjectPool;
import dev.stiemannkj1.collection.arrays.GrowableArrays.GrowableByteArray;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class NamespacerTests {
  public static final int INITIAL_CLASS_ARRAY_CAPACITY = 1 << 16;
  // TODO namespace and merge class names in META-INF/services and META-INF/groovy
  // TODO handle manifest and properties files. Merging properties should probably require users to
  // handle conflicts by creating a properties file with any conflicting properties.
  // TODO consider merging Xml files
  // TODO instead of handling all the merging, only merge binary files if they exist, maybe handle
  // log4j, but maybe not
  // plugin should auto-fail on conflicts and require explicitly choosing from the provided jars or
  // providing a deconflicted file in the project.

  public static final class Before {
    public static List<String> strings() {
      return Arrays.asList(
          // Invalid signatures:
          "",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace;",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace ",
          "dev.stiemannkj1.bytecode.namespacer.NamespacerTests$ConcreteGenericClassToNamespace;",
          "dev.stiemannkj1.bytecode.namespacer.NamespacerTests$ConcreteGenericClassToNamespace ",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$NestedGeneric<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace;>;>;>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-b>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-->;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-+>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-*>;",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-b>;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-->;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-+>;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-*>;)V",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+b>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+->;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<++>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+*>;",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+b>;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+->;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<++>;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+*>;)V",
          "dev.stiemannkj1.bytecode.namespacer.NamespacerTests$ConcreteGenericClassToNamespace",
          "META-INF/services/dev.stiemannkj1.bytecode.namespacer.NamespacerTests$ConcreteGenericClassToNamespace",
          "/META-INF/services/dev.stiemannkj1.bytecode.namespacer.NamespacerTests$ConcreteGenericClassToNamespace",
          "WEB-INF/classes/META-INF/services/dev.stiemannkj1.bytecode.namespacer.NamespacerTests$ConcreteGenericClassToNamespace",
          "/WEB-INF/classes/META-INF/services/dev.stiemannkj1.bytecode.namespacer.NamespacerTests$ConcreteGenericClassToNamespace",
          "WEB-INF/classes/dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace.class",
          "/WEB-INF/classes/dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace.class",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace.class",
          "/dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace.class",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace;",
          "[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$NestedGeneric<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$NestedGeneric<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;)Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$NestedGeneric<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;)Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;",
          "(Ljava/util/List;)V",
          "Ljava/util/List;",
          "Ljava/util/List<-Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;",
          "(Ljava/util/List<-Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;)V",
          "Ljava/util/List<+Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;",
          "(Ljava/util/List<+Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;)V",
          "Ljava/util/List<*>;",
          "(Ljava/util/List<*>;)V",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace<-Ljava/util/List;>;",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace<-Ljava/util/List;>;)V",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace<+Ljava/util/List;>;",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace<+Ljava/util/List;>;)V",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace<*>;",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace<*>;)V",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace<-[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;*+[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;",
          "(BSIJFDCZLdev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;BSIJFDCZ)V",
          "TT;",
          "<T:Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>(BSIJFDCZTT;BSIJFDCZ)V",
          "(BSIJFDCZLjava/lang/Object;BSIJFDCZ)V",
          "Ljava/lang/Object;",
          "<T:Ljava/lang/Object;>(BSIJFDCZTT;BSIJFDCZ)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;)Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "<T:Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$NestedGeneric<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;>(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>;)Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "<T:Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TU;>;U:Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$NestedGeneric<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;>(TT;)TT;",
          "<T:Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$ThrowableToNamespace;>()V^TT;^Ljava/lang/Exception;",
          "<T:Ljava/io/IOException;>()V^TT;^Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$ThrowableToNamespace;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$AbstractGenericClassToNamespace<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$NestedGeneric<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;>;",
          "<T:Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$NestedGeneric<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;>Ljava/lang/Object;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>.NestedNested<TT;>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested$NestedNested;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>.NestedNested<TU;>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$1Local<TU;>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$1Local;",
          "()Ljava/lang/Object;",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$1Local",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$1",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested",
          "dev.stiemannkj1.bytecode.namespacer.NamespacerTests$ConcreteGenericClassToNamespace$Nested$NestedNested",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested$NestedNested",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$ThrowableToNamespace",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$AbstractGenericClassToNamespace",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$NestedGeneric",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTest",
          "([[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;)[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "[[[TT;",
          "<T:Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TU;>;U:Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$NestedGeneric<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;>([[[TT;)[[[TT;",
          "([[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;)[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;",
          "[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;",
          "([Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;)[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;",
          "[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$AbstractGenericClassToNamespace<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$NestedGeneric<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;>;",
          "[[[B",
          "[[[C",
          "[[[D",
          "[[[F",
          "[[[I",
          "[[[J",
          "[[[S",
          "[[[Z",
          "([[[TT;[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;[[[TT;)[[[TT;",
          "([[[B[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;[[[B)[[[B",
          "([[[C[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;[[[C)[[[C",
          "([[[D[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;[[[D)[[[D",
          "([[[F[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;[[[F)[[[F",
          "([[[I[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;[[[I)[[[I",
          "([[[J[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;[[[J)[[[J",
          "([[[S[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;[[[S)[[[S",
          "([[[Z[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;[[[Z)[[[Z",
          "<T:Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace<TT;>;>Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace<TT;>;Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace<Ljava/lang/String;>;Ljava/util/List<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;Ljava/util/List<Ljava/util/List<Ljava/langString;>;>;");
    }
  }

  public static final class ExpectedAfterShorter {
    public static List<String> strings() {
      return Arrays.asList(
          // Invalid signatures:
          "",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace;",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace ",
          "dev.stiemannkj1.bytecode.namespacer.NamespacerTests$ConcreteGenericClassToNamespace;",
          "dev.stiemannkj1.bytecode.namespacer.NamespacerTests$ConcreteGenericClassToNamespace ",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$NestedGeneric<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;",
          "Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace<Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace<Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace<Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace;>;>;>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-b>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-->;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-+>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-*>;",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-b>;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-->;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-+>;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-*>;)V",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+b>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+->;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<++>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+*>;",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+b>;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+->;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<++>;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+*>;)V",
          "now.im.namespaced.NamespacerTests$ConcreteGenericClassToNamespace",
          "META-INF/services/now.im.namespaced.NamespacerTests$ConcreteGenericClassToNamespace",
          "/META-INF/services/now.im.namespaced.NamespacerTests$ConcreteGenericClassToNamespace",
          "WEB-INF/classes/META-INF/services/now.im.namespaced.NamespacerTests$ConcreteGenericClassToNamespace",
          "/WEB-INF/classes/META-INF/services/now.im.namespaced.NamespacerTests$ConcreteGenericClassToNamespace",
          "WEB-INF/classes/now/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace.class",
          "/WEB-INF/classes/now/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace.class",
          "now/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace",
          "now/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace.class",
          "/now/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace.class",
          "Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace;",
          "[Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace;",
          "Lnow/im/namespaced/NamespacerTests$NestedGeneric<Lnow/im/namespaced/NamespacerTests$ClassToNamespace;>;",
          "(Lnow/im/namespaced/NamespacerTests$NestedGeneric<Lnow/im/namespaced/NamespacerTests$ClassToNamespace;>;)Lnow/im/namespaced/NamespacerTests$NestedGeneric<Lnow/im/namespaced/NamespacerTests$ClassToNamespace;>;",
          "(Lnow/im/namespaced/NamespacerTests$ClassToNamespace;)Lnow/im/namespaced/NamespacerTests$ClassToNamespace;",
          "Lnow/im/namespaced/NamespacerTests$ClassToNamespace;",
          "(Ljava/util/List;)V",
          "Ljava/util/List;",
          "Ljava/util/List<-Lnow/im/namespaced/NamespacerTests$ClassToNamespace;>;",
          "(Ljava/util/List<-Lnow/im/namespaced/NamespacerTests$ClassToNamespace;>;)V",
          "Ljava/util/List<+Lnow/im/namespaced/NamespacerTests$ClassToNamespace;>;",
          "(Ljava/util/List<+Lnow/im/namespaced/NamespacerTests$ClassToNamespace;>;)V",
          "Ljava/util/List<*>;",
          "(Ljava/util/List<*>;)V",
          "Lnow/im/namespaced/NamespacerTests$ClassToNamespace<-Ljava/util/List;>;",
          "(Lnow/im/namespaced/NamespacerTests$ClassToNamespace<-Ljava/util/List;>;)V",
          "Lnow/im/namespaced/NamespacerTests$ClassToNamespace<+Ljava/util/List;>;",
          "(Lnow/im/namespaced/NamespacerTests$ClassToNamespace<+Ljava/util/List;>;)V",
          "Lnow/im/namespaced/NamespacerTests$ClassToNamespace<*>;",
          "(Lnow/im/namespaced/NamespacerTests$ClassToNamespace<*>;)V",
          "Lnow/im/namespaced/NamespacerTests$ClassToNamespace<-[[[Lnow/im/namespaced/NamespacerTests$ClassToNamespace;*+[[[Lnow/im/namespaced/NamespacerTests$ClassToNamespace;>;",
          "(BSIJFDCZLnow/im/namespaced/NamespacerTests$ClassToNamespace;BSIJFDCZ)V",
          "TT;",
          "<T:Lnow/im/namespaced/NamespacerTests$ClassToNamespace;>(BSIJFDCZTT;BSIJFDCZ)V",
          "(BSIJFDCZLjava/lang/Object;BSIJFDCZ)V",
          "Ljava/lang/Object;",
          "<T:Ljava/lang/Object;>(BSIJFDCZTT;BSIJFDCZ)V",
          "(Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$Nested;)Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "<T:Lnow/im/namespaced/NamespacerTests$NestedGeneric<Lnow/im/namespaced/NamespacerTests$ClassToNamespace;>;>(Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>;)Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "<T:Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TU;>;U:Lnow/im/namespaced/NamespacerTests$NestedGeneric<Lnow/im/namespaced/NamespacerTests$ClassToNamespace;>;>(TT;)TT;",
          "<T:Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$ThrowableToNamespace;>()V^TT;^Ljava/lang/Exception;",
          "<T:Ljava/io/IOException;>()V^TT;^Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$ThrowableToNamespace;",
          "Lnow/im/namespaced/NamespacerTests$AbstractGenericClassToNamespace<Lnow/im/namespaced/NamespacerTests$NestedGeneric<Lnow/im/namespaced/NamespacerTests$ClassToNamespace;>;>;",
          "<T:Lnow/im/namespaced/NamespacerTests$NestedGeneric<Lnow/im/namespaced/NamespacerTests$ClassToNamespace;>;>Ljava/lang/Object;",
          "Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>.NestedNested<TT;>;",
          "Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$Nested$NestedNested;",
          "Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>.NestedNested<TU;>;",
          "Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$1Local<TU;>;",
          "Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$1Local;",
          "()Ljava/lang/Object;",
          "now/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$1Local",
          "now/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$1",
          "now/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$Nested",
          "now.im.namespaced.NamespacerTests$ConcreteGenericClassToNamespace$Nested$NestedNested",
          "now/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$Nested$NestedNested",
          "now/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$ThrowableToNamespace",
          "now/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace",
          "now/im/namespaced/NamespacerTests$AbstractGenericClassToNamespace",
          "now/im/namespaced/NamespacerTests$NestedGeneric",
          "now/im/namespaced/NamespacerTests$ClassToNamespace",
          "now/im/namespaced/NamespacerTest",
          "([[[Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$Nested;)[[[Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "[[[Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "[[[TT;",
          "<T:Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TU;>;U:Lnow/im/namespaced/NamespacerTests$NestedGeneric<Lnow/im/namespaced/NamespacerTests$ClassToNamespace;>;>([[[TT;)[[[TT;",
          "([[[Lnow/im/namespaced/NamespacerTests$ClassToNamespace;)[[[Lnow/im/namespaced/NamespacerTests$ClassToNamespace;",
          "[[[Lnow/im/namespaced/NamespacerTests$ClassToNamespace;",
          "([Lnow/im/namespaced/NamespacerTests$ClassToNamespace;)[Lnow/im/namespaced/NamespacerTests$ClassToNamespace;",
          "[Lnow/im/namespaced/NamespacerTests$ClassToNamespace;",
          "Lnow/im/namespaced/NamespacerTests$AbstractGenericClassToNamespace<Lnow/im/namespaced/NamespacerTests$NestedGeneric<Lnow/im/namespaced/NamespacerTests$ClassToNamespace;>;>;",
          "[[[B",
          "[[[C",
          "[[[D",
          "[[[F",
          "[[[I",
          "[[[J",
          "[[[S",
          "[[[Z",
          "([[[TT;[[[Lnow/im/namespaced/NamespacerTests$ClassToNamespace;[[[TT;)[[[TT;",
          "([[[B[[[Lnow/im/namespaced/NamespacerTests$ClassToNamespace;[[[B)[[[B",
          "([[[C[[[Lnow/im/namespaced/NamespacerTests$ClassToNamespace;[[[C)[[[C",
          "([[[D[[[Lnow/im/namespaced/NamespacerTests$ClassToNamespace;[[[D)[[[D",
          "([[[F[[[Lnow/im/namespaced/NamespacerTests$ClassToNamespace;[[[F)[[[F",
          "([[[I[[[Lnow/im/namespaced/NamespacerTests$ClassToNamespace;[[[I)[[[I",
          "([[[J[[[Lnow/im/namespaced/NamespacerTests$ClassToNamespace;[[[J)[[[J",
          "([[[S[[[Lnow/im/namespaced/NamespacerTests$ClassToNamespace;[[[S)[[[S",
          "([[[Z[[[Lnow/im/namespaced/NamespacerTests$ClassToNamespace;[[[Z)[[[Z",
          "<T:Lnow/im/namespaced/NamespacerTests$ClassToNamespace<TT;>;>Lnow/im/namespaced/NamespacerTests$ClassToNamespace;Lnow/im/namespaced/NamespacerTests$ClassToNamespace<TT;>;Lnow/im/namespaced/NamespacerTests$ClassToNamespace<Lnow/im/namespaced/NamespacerTests$ClassToNamespace;>;Lnow/im/namespaced/NamespacerTests$ClassToNamespace<Ljava/lang/String;>;Ljava/util/List<Lnow/im/namespaced/NamespacerTests$ClassToNamespace;>;Ljava/util/List<Ljava/util/List<Ljava/langString;>;>;");
    }
  }

  public static final class ExpectedAfterLonger {
    public static List<String> strings() {
      return Arrays.asList(
          // Invalid signatures:
          "",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace;",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace ",
          "dev.stiemannkj1.bytecode.namespacer.NamespacerTests$ConcreteGenericClassToNamespace;",
          "dev.stiemannkj1.bytecode.namespacer.NamespacerTests$ConcreteGenericClassToNamespace ",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$NestedGeneric<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;",
          "Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace<Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace<Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace<Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace;>;>;>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-b>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-->;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-+>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-*>;",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-b>;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-->;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-+>;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-*>;)V",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+b>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+->;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<++>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+*>;",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+b>;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+->;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<++>;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+*>;)V",
          "now.im.namespaced.and.much.much.much.much.much.much.much.much.longer.NamespacerTests$ConcreteGenericClassToNamespace",
          "META-INF/services/now.im.namespaced.and.much.much.much.much.much.much.much.much.longer.NamespacerTests$ConcreteGenericClassToNamespace",
          "/META-INF/services/now.im.namespaced.and.much.much.much.much.much.much.much.much.longer.NamespacerTests$ConcreteGenericClassToNamespace",
          "WEB-INF/classes/META-INF/services/now.im.namespaced.and.much.much.much.much.much.much.much.much.longer.NamespacerTests$ConcreteGenericClassToNamespace",
          "/WEB-INF/classes/META-INF/services/now.im.namespaced.and.much.much.much.much.much.much.much.much.longer.NamespacerTests$ConcreteGenericClassToNamespace",
          "WEB-INF/classes/now/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace.class",
          "/WEB-INF/classes/now/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace.class",
          "now/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace",
          "now/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace.class",
          "/now/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace.class",
          "Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace;",
          "[Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace;",
          "Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$NestedGeneric<Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;>;",
          "(Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$NestedGeneric<Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;>;)Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$NestedGeneric<Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;>;",
          "(Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;)Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;",
          "Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;",
          "(Ljava/util/List;)V",
          "Ljava/util/List;",
          "Ljava/util/List<-Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;>;",
          "(Ljava/util/List<-Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;>;)V",
          "Ljava/util/List<+Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;>;",
          "(Ljava/util/List<+Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;>;)V",
          "Ljava/util/List<*>;",
          "(Ljava/util/List<*>;)V",
          "Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace<-Ljava/util/List;>;",
          "(Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace<-Ljava/util/List;>;)V",
          "Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace<+Ljava/util/List;>;",
          "(Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace<+Ljava/util/List;>;)V",
          "Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace<*>;",
          "(Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace<*>;)V",
          "Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace<-[[[Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;*+[[[Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;>;",
          "(BSIJFDCZLnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;BSIJFDCZ)V",
          "TT;",
          "<T:Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;>(BSIJFDCZTT;BSIJFDCZ)V",
          "(BSIJFDCZLjava/lang/Object;BSIJFDCZ)V",
          "Ljava/lang/Object;",
          "<T:Ljava/lang/Object;>(BSIJFDCZTT;BSIJFDCZ)V",
          "(Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;)Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "<T:Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$NestedGeneric<Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;>;>(Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>;)Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "<T:Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TU;>;U:Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$NestedGeneric<Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;>;>(TT;)TT;",
          "<T:Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace$ThrowableToNamespace;>()V^TT;^Ljava/lang/Exception;",
          "<T:Ljava/io/IOException;>()V^TT;^Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace$ThrowableToNamespace;",
          "Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$AbstractGenericClassToNamespace<Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$NestedGeneric<Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;>;>;",
          "<T:Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$NestedGeneric<Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;>;>Ljava/lang/Object;",
          "Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>.NestedNested<TT;>;",
          "Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace$Nested$NestedNested;",
          "Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>.NestedNested<TU;>;",
          "Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace$1Local<TU;>;",
          "Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace$1Local;",
          "()Ljava/lang/Object;",
          "now/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace$1Local",
          "now/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace$1",
          "now/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace$Nested",
          "now.im.namespaced.and.much.much.much.much.much.much.much.much.longer.NamespacerTests$ConcreteGenericClassToNamespace$Nested$NestedNested",
          "now/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace$Nested$NestedNested",
          "now/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace$ThrowableToNamespace",
          "now/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace",
          "now/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$AbstractGenericClassToNamespace",
          "now/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$NestedGeneric",
          "now/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace",
          "now/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTest",
          "([[[Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;)[[[Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "[[[Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "[[[TT;",
          "<T:Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TU;>;U:Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$NestedGeneric<Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;>;>([[[TT;)[[[TT;",
          "([[[Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;)[[[Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;",
          "[[[Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;",
          "([Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;)[Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;",
          "[Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;",
          "Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$AbstractGenericClassToNamespace<Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$NestedGeneric<Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;>;>;",
          "[[[B",
          "[[[C",
          "[[[D",
          "[[[F",
          "[[[I",
          "[[[J",
          "[[[S",
          "[[[Z",
          "([[[TT;[[[Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;[[[TT;)[[[TT;",
          "([[[B[[[Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;[[[B)[[[B",
          "([[[C[[[Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;[[[C)[[[C",
          "([[[D[[[Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;[[[D)[[[D",
          "([[[F[[[Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;[[[F)[[[F",
          "([[[I[[[Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;[[[I)[[[I",
          "([[[J[[[Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;[[[J)[[[J",
          "([[[S[[[Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;[[[S)[[[S",
          "([[[Z[[[Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;[[[Z)[[[Z",
          "<T:Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace<TT;>;>Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace<TT;>;Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace<Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;>;Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace<Ljava/lang/String;>;Ljava/util/List<Lnow/im/namespaced/and/much/much/much/much/much/much/much/much/longer/NamespacerTests$ClassToNamespace;>;Ljava/util/List<Ljava/util/List<Ljava/langString;>;>;");
    }
  }

  public static final class ExpectedAfterSameSize {
    public static List<String> strings() {
      return Arrays.asList(
          // Invalid signatures:
          "",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace;",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace ",
          "dev.stiemannkj1.bytecode.namespacer.NamespacerTests$ConcreteGenericClassToNamespace;",
          "dev.stiemannkj1.bytecode.namespacer.NamespacerTests$ConcreteGenericClassToNamespace ",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$NestedGeneric<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;",
          "Lnow/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace<Lnow/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace<Lnow/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace<Lnow/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace;>;>;>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-b>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-->;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-+>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-*>;",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-b>;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-->;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-+>;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-*>;)V",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+b>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+->;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<++>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+*>;",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+b>;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+->;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<++>;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+*>;)V",
          "now.im.namespaced.same.size12345678.NamespacerTests$ConcreteGenericClassToNamespace",
          "META-INF/services/now.im.namespaced.same.size12345678.NamespacerTests$ConcreteGenericClassToNamespace",
          "/META-INF/services/now.im.namespaced.same.size12345678.NamespacerTests$ConcreteGenericClassToNamespace",
          "WEB-INF/classes/META-INF/services/now.im.namespaced.same.size12345678.NamespacerTests$ConcreteGenericClassToNamespace",
          "/WEB-INF/classes/META-INF/services/now.im.namespaced.same.size12345678.NamespacerTests$ConcreteGenericClassToNamespace",
          "WEB-INF/classes/now/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace.class",
          "/WEB-INF/classes/now/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace.class",
          "now/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace",
          "now/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace.class",
          "/now/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace.class",
          "Lnow/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace;",
          "[Lnow/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace;",
          "Lnow/im/namespaced/same/size12345678/NamespacerTests$NestedGeneric<Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;>;",
          "(Lnow/im/namespaced/same/size12345678/NamespacerTests$NestedGeneric<Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;>;)Lnow/im/namespaced/same/size12345678/NamespacerTests$NestedGeneric<Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;>;",
          "(Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;)Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;",
          "Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;",
          "(Ljava/util/List;)V",
          "Ljava/util/List;",
          "Ljava/util/List<-Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;>;",
          "(Ljava/util/List<-Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;>;)V",
          "Ljava/util/List<+Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;>;",
          "(Ljava/util/List<+Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;>;)V",
          "Ljava/util/List<*>;",
          "(Ljava/util/List<*>;)V",
          "Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace<-Ljava/util/List;>;",
          "(Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace<-Ljava/util/List;>;)V",
          "Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace<+Ljava/util/List;>;",
          "(Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace<+Ljava/util/List;>;)V",
          "Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace<*>;",
          "(Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace<*>;)V",
          "Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace<-[[[Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;*+[[[Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;>;",
          "(BSIJFDCZLnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;BSIJFDCZ)V",
          "TT;",
          "<T:Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;>(BSIJFDCZTT;BSIJFDCZ)V",
          "(BSIJFDCZLjava/lang/Object;BSIJFDCZ)V",
          "Ljava/lang/Object;",
          "<T:Ljava/lang/Object;>(BSIJFDCZTT;BSIJFDCZ)V",
          "(Lnow/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace$Nested;)Lnow/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "Lnow/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "Lnow/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "<T:Lnow/im/namespaced/same/size12345678/NamespacerTests$NestedGeneric<Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;>;>(Lnow/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>;)Lnow/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "<T:Lnow/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TU;>;U:Lnow/im/namespaced/same/size12345678/NamespacerTests$NestedGeneric<Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;>;>(TT;)TT;",
          "<T:Lnow/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace$ThrowableToNamespace;>()V^TT;^Ljava/lang/Exception;",
          "<T:Ljava/io/IOException;>()V^TT;^Lnow/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace$ThrowableToNamespace;",
          "Lnow/im/namespaced/same/size12345678/NamespacerTests$AbstractGenericClassToNamespace<Lnow/im/namespaced/same/size12345678/NamespacerTests$NestedGeneric<Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;>;>;",
          "<T:Lnow/im/namespaced/same/size12345678/NamespacerTests$NestedGeneric<Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;>;>Ljava/lang/Object;",
          "Lnow/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "Lnow/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "Lnow/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>.NestedNested<TT;>;",
          "Lnow/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace$Nested$NestedNested;",
          "Lnow/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>.NestedNested<TU;>;",
          "Lnow/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace$1Local<TU;>;",
          "Lnow/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace$1Local;",
          "()Ljava/lang/Object;",
          "now/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace$1Local",
          "now/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace$1",
          "now/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace$Nested",
          "now.im.namespaced.same.size12345678.NamespacerTests$ConcreteGenericClassToNamespace$Nested$NestedNested",
          "now/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace$Nested$NestedNested",
          "now/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace$ThrowableToNamespace",
          "now/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace",
          "now/im/namespaced/same/size12345678/NamespacerTests$AbstractGenericClassToNamespace",
          "now/im/namespaced/same/size12345678/NamespacerTests$NestedGeneric",
          "now/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace",
          "now/im/namespaced/same/size12345678/NamespacerTest",
          "([[[Lnow/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace$Nested;)[[[Lnow/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "[[[Lnow/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "[[[TT;",
          "<T:Lnow/im/namespaced/same/size12345678/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TU;>;U:Lnow/im/namespaced/same/size12345678/NamespacerTests$NestedGeneric<Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;>;>([[[TT;)[[[TT;",
          "([[[Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;)[[[Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;",
          "[[[Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;",
          "([Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;)[Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;",
          "[Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;",
          "Lnow/im/namespaced/same/size12345678/NamespacerTests$AbstractGenericClassToNamespace<Lnow/im/namespaced/same/size12345678/NamespacerTests$NestedGeneric<Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;>;>;",
          "[[[B",
          "[[[C",
          "[[[D",
          "[[[F",
          "[[[I",
          "[[[J",
          "[[[S",
          "[[[Z",
          "([[[TT;[[[Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;[[[TT;)[[[TT;",
          "([[[B[[[Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;[[[B)[[[B",
          "([[[C[[[Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;[[[C)[[[C",
          "([[[D[[[Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;[[[D)[[[D",
          "([[[F[[[Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;[[[F)[[[F",
          "([[[I[[[Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;[[[I)[[[I",
          "([[[J[[[Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;[[[J)[[[J",
          "([[[S[[[Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;[[[S)[[[S",
          "([[[Z[[[Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;[[[Z)[[[Z",
          "<T:Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace<TT;>;>Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace<TT;>;Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace<Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;>;Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace<Ljava/lang/String;>;Ljava/util/List<Lnow/im/namespaced/same/size12345678/NamespacerTests$ClassToNamespace;>;Ljava/util/List<Ljava/util/List<Ljava/langString;>;>;");
    }
  }

  public static final class ExpectedAfterNestedNestedReplaced {
    public static List<String> strings() {
      return Arrays.asList(
          // Invalid signatures:
          "",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace;",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace ",
          "dev.stiemannkj1.bytecode.namespacer.NamespacerTests$ConcreteGenericClassToNamespace;",
          "dev.stiemannkj1.bytecode.namespacer.NamespacerTests$ConcreteGenericClassToNamespace ",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$NestedGeneric<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>.NestedNested<TT;>;>;>;>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-b>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-->;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-+>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-*>;",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-b>;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-->;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-+>;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<-*>;)V",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+b>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+->;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<++>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+*>;",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+b>;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+->;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<++>;)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace<+*>;)V",
          "dev.stiemannkj1.bytecode.namespacer.NamespacerTests$ConcreteGenericClassToNamespace",
          "META-INF/services/dev.stiemannkj1.bytecode.namespacer.NamespacerTests$ConcreteGenericClassToNamespace",
          "/META-INF/services/dev.stiemannkj1.bytecode.namespacer.NamespacerTests$ConcreteGenericClassToNamespace",
          "WEB-INF/classes/META-INF/services/dev.stiemannkj1.bytecode.namespacer.NamespacerTests$ConcreteGenericClassToNamespace",
          "/WEB-INF/classes/META-INF/services/dev.stiemannkj1.bytecode.namespacer.NamespacerTests$ConcreteGenericClassToNamespace",
          "WEB-INF/classes/dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace.class",
          "/WEB-INF/classes/dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace.class",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace.class",
          "/dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace.class",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace;",
          "[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$NestedGeneric<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$NestedGeneric<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;)Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$NestedGeneric<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;)Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;",
          "(Ljava/util/List;)V",
          "Ljava/util/List;",
          "Ljava/util/List<-Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;",
          "(Ljava/util/List<-Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;)V",
          "Ljava/util/List<+Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;",
          "(Ljava/util/List<+Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;)V",
          "Ljava/util/List<*>;",
          "(Ljava/util/List<*>;)V",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace<-Ljava/util/List;>;",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace<-Ljava/util/List;>;)V",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace<+Ljava/util/List;>;",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace<+Ljava/util/List;>;)V",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace<*>;",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace<*>;)V",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace<-[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;*+[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;",
          "(BSIJFDCZLdev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;BSIJFDCZ)V",
          "TT;",
          "<T:Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>(BSIJFDCZTT;BSIJFDCZ)V",
          "(BSIJFDCZLjava/lang/Object;BSIJFDCZ)V",
          "Ljava/lang/Object;",
          "<T:Ljava/lang/Object;>(BSIJFDCZTT;BSIJFDCZ)V",
          "(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;)Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "<T:Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$NestedGeneric<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;>(Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>;)Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "<T:Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TU;>;U:Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$NestedGeneric<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;>(TT;)TT;",
          "<T:Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$ThrowableToNamespace;>()V^TT;^Ljava/lang/Exception;",
          "<T:Ljava/io/IOException;>()V^TT;^Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$ThrowableToNamespace;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$AbstractGenericClassToNamespace<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$NestedGeneric<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;>;",
          "<T:Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$NestedGeneric<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;>Ljava/lang/Object;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>.NestedNested<TT;>;",
          "Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$Nested$NestedNested;",
          "Lnow/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TT;>.NestedNested<TU;>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$1Local<TU;>;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$1Local;",
          "()Ljava/lang/Object;",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$1Local",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$1",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested",
          "now.im.namespaced.NamespacerTests$ConcreteGenericClassToNamespace$Nested$NestedNested",
          "now/im/namespaced/NamespacerTests$ConcreteGenericClassToNamespace$Nested$NestedNested",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$ThrowableToNamespace",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$AbstractGenericClassToNamespace",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$NestedGeneric",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace",
          "dev/stiemannkj1/bytecode/namespacer/NamespacerTest",
          "([[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;)[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested;",
          "[[[TT;",
          "<T:Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ConcreteGenericClassToNamespace$Nested<TU;>;U:Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$NestedGeneric<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;>([[[TT;)[[[TT;",
          "([[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;)[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;",
          "[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;",
          "([Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;)[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;",
          "[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;",
          "Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$AbstractGenericClassToNamespace<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$NestedGeneric<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;>;",
          "[[[B",
          "[[[C",
          "[[[D",
          "[[[F",
          "[[[I",
          "[[[J",
          "[[[S",
          "[[[Z",
          "([[[TT;[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;[[[TT;)[[[TT;",
          "([[[B[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;[[[B)[[[B",
          "([[[C[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;[[[C)[[[C",
          "([[[D[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;[[[D)[[[D",
          "([[[F[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;[[[F)[[[F",
          "([[[I[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;[[[I)[[[I",
          "([[[J[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;[[[J)[[[J",
          "([[[S[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;[[[S)[[[S",
          "([[[Z[[[Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;[[[Z)[[[Z",
          "<T:Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace<TT;>;>Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace<TT;>;Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace<Ljava/lang/String;>;Ljava/util/List<Ldev/stiemannkj1/bytecode/namespacer/NamespacerTests$ClassToNamespace;>;Ljava/util/List<Ljava/util/List<Ljava/langString;>;>;");
    }
  }

  @Test
  void test_descriptors_same_size() {
    assertEquals(Before.strings().size(), ExpectedAfterShorter.strings().size());
    assertEquals(Before.strings().size(), ExpectedAfterLonger.strings().size());
    assertEquals(Before.strings().size(), ExpectedAfterSameSize.strings().size());
    assertEquals(Before.strings().size(), ExpectedAfterNestedNestedReplaced.strings().size());
  }

  private static Stream<Arguments> descriptors(
      final Map<String, String> replacements,
      final String classNameAfter,
      final List<String> expectedAfter)
      throws IOException, ReflectiveOperationException {

    final GrowableByteArray classFileBefore = new GrowableByteArray(INITIAL_CLASS_ARRAY_CAPACITY);
    GrowableByteArray.readFully(classFileBefore, classAsStream(Before.class));
    final ClassGenerator classGenerator =
        new ClassGenerator(NamespacerTests.class.getClassLoader());
    final GrowableByteArray classFileAfter = new GrowableByteArray(INITIAL_CLASS_ARRAY_CAPACITY);

    final ObjectPool objectPool = new ObjectPool();

    assertNull(
        Namespacer.namespaceClassFile(
            objectPool,
            classNameToPath(Before.class),
            classFileBefore,
            replacements,
            classFileAfter));

    final Class<?> namespacedClass =
        classGenerator.generateClass(
            classNameAfter, bytes(classFileAfter), 0, size(classFileAfter));

    @SuppressWarnings("unchecked")
    final List<String> namespacedStrings =
        (List<String>) invoke(namespacedClass, null, "strings", new Class<?>[0], new Object[0]);
    assertEquals(expectedAfter.size(), namespacedStrings.size());

    final List<Arguments> args = new ArrayList<>(namespacedStrings.size());

    for (int i = 0; i < namespacedStrings.size(); i++) {
      args.add(Arguments.of(expectedAfter.get(i), namespacedStrings.get(i)));
    }

    return args.stream();
  }

  static Stream<Arguments> descriptorsShorterAfter()
      throws IOException, ReflectiveOperationException {
    final Map<String, String> replacements = new HashMap<>();
    replacements.put("dev.stiemannkj1.bytecode.namespacer.", "now.im.namespaced.");

    for (final Map.Entry<String, String> entry : replacements.entrySet()) {
      assertTrue(entry.getKey().length() > entry.getValue().length());
    }

    return descriptors(
        replacements, "now.im.namespaced.NamespacerTests$Before", ExpectedAfterShorter.strings());
  }

  @MethodSource("descriptorsShorterAfter")
  @ParameterizedTest
  void it_namespaces_all_descriptors_with_shorter_namespaceClassFile(
      final String expected, final String namespaced) {
    assertEquals(expected, namespaced);
  }

  static Stream<Arguments> descriptorsLongerAfter()
      throws IOException, ReflectiveOperationException {
    final Map<String, String> replacements = new HashMap<>();
    replacements.put(
        "dev.stiemannkj1.bytecode.namespacer.",
        "now.im.namespaced.and.much.much.much.much.much.much.much.much.longer.");

    for (final Map.Entry<String, String> entry : replacements.entrySet()) {
      assertTrue(entry.getKey().length() < entry.getValue().length());
    }

    return descriptors(
        replacements,
        "now.im.namespaced.and.much.much.much.much.much.much.much.much.longer.NamespacerTests$Before",
        ExpectedAfterLonger.strings());
  }

  @MethodSource("descriptorsLongerAfter")
  @ParameterizedTest
  void it_namespaces_all_descriptors_with_longer_namespaceClassFile(
      final String expected, final String namespaced) {
    assertEquals(expected, namespaced);
  }

  static Stream<Arguments> descriptorsSameSize() throws IOException, ReflectiveOperationException {
    final Map<String, String> replacements = new HashMap<>();
    replacements.put(
        "dev.stiemannkj1.bytecode.namespacer.", "now.im.namespaced.same.size12345678.");

    for (final Map.Entry<String, String> entry : replacements.entrySet()) {
      assertTrue(entry.getKey().length() == entry.getValue().length());
    }

    return descriptors(
        replacements,
        "now.im.namespaced.same.size12345678.NamespacerTests$Before",
        ExpectedAfterSameSize.strings());
  }

  @MethodSource("descriptorsSameSize")
  @ParameterizedTest
  void it_namespaces_all_descriptors_with_same_size_namespaceClassFile(
      final String expected, final String namespaced) {
    assertEquals(expected, namespaced);
  }

  static Stream<Arguments> descriptorsReplaceNestedNested()
      throws IOException, ReflectiveOperationException {
    final Map<String, String> replacement = new HashMap<>();
    replacement.put(
        "dev.stiemannkj1.bytecode.namespacer.NamespacerTests$ConcreteGenericClassToNamespace$Nested$NestedNested",
        "now.im.namespaced.NamespacerTests$ConcreteGenericClassToNamespace$Nested$NestedNested");
    return descriptors(
        replacement,
        "dev.stiemannkj1.bytecode.namespacer.NamespacerTests$Before",
        ExpectedAfterNestedNestedReplaced.strings());
  }

  @Disabled("TODO handle namespacing nested types.")
  @MethodSource("descriptorsReplaceNestedNested")
  @ParameterizedTest
  void it_namespaces_all_nested_nested_descriptors(final String expected, final String namespaced) {
    assertEquals(expected, namespaced);
  }

  @Test
  void it_namespaces_class() throws Throwable {

    final GrowableByteArray classFileBefore = new GrowableByteArray(INITIAL_CLASS_ARRAY_CAPACITY);
    final Map<String, String> replacement = new HashMap<>();
    replacement.put("dev.stiemannkj1.bytecode.namespacer.", "now.im.namespaced.");
    final ClassGenerator classGenerator = new ClassGenerator(this.getClass().getClassLoader());
    final GrowableByteArray classFileAfter = new GrowableByteArray(INITIAL_CLASS_ARRAY_CAPACITY);

    final ObjectPool objectPool = new ObjectPool();
    String className = "";

    try {

      className =
          "now.im.namespaced.NamespacerTests$ConcreteGenericClassToNamespace$ThrowableToNamespace";
      GrowableByteArray.clear(classFileBefore);
      GrowableByteArray.clear(classFileAfter);
      GrowableByteArray.readFully(
          classFileBefore,
          classAsStream(ConcreteGenericClassToNamespace.ThrowableToNamespace.class));
      assertNull(
          Namespacer.namespaceClassFile(
              objectPool,
              classNameToPath(ClassToNamespace.class),
              classFileBefore,
              replacement,
              classFileAfter));

      classGenerator.generateClass(className, bytes(classFileAfter), 0, size(classFileAfter));

      className = "now.im.namespaced.NamespacerTests$ClassToNamespace";
      GrowableByteArray.clear(classFileBefore);
      GrowableByteArray.clear(classFileAfter);
      GrowableByteArray.readFully(classFileBefore, classAsStream(ClassToNamespace.class));
      assertNull(
          Namespacer.namespaceClassFile(
              objectPool,
              classNameToPath(ClassToNamespace.class),
              classFileBefore,
              replacement,
              classFileAfter));

      final Class<?> namespacedClass =
          classGenerator.generateClass(className, bytes(classFileAfter), 0, size(classFileAfter));

      assertEquals(className, namespacedClass.getTypeName());
      final Object namespaced = namespacedClass.getDeclaredConstructor().newInstance();
      assertEquals("now.im.namespaced.toString", namespaced.toString());

      className = "now.im.namespaced.NamespacerTests$NestedGeneric";
      GrowableByteArray.clear(classFileBefore);
      GrowableByteArray.clear(classFileAfter);
      GrowableByteArray.readFully(classFileBefore, classAsStream(NestedGeneric.class));
      assertNull(
          Namespacer.namespaceClassFile(
              objectPool,
              classNameToPath(AbstractGenericClassToNamespace.class),
              classFileBefore,
              replacement,
              classFileAfter));

      final Class<?> nestedGenericClass =
          classGenerator.generateClass(className, bytes(classFileAfter), 0, size(classFileAfter));

      assertEquals(className, nestedGenericClass.getTypeName());
      final Object nestedGeneric = nestedGenericClass.getDeclaredConstructor().newInstance();

      className = "now.im.namespaced.NamespacerTests$AbstractGenericClassToNamespace";
      GrowableByteArray.clear(classFileBefore);
      GrowableByteArray.clear(classFileAfter);
      GrowableByteArray.readFully(
          classFileBefore, classAsStream(AbstractGenericClassToNamespace.class));
      assertNull(
          Namespacer.namespaceClassFile(
              objectPool,
              classNameToPath(AbstractGenericClassToNamespace.class),
              classFileBefore,
              replacement,
              classFileAfter));

      final Class<?> genericParent =
          classGenerator.generateClass(className, bytes(classFileAfter), 0, size(classFileAfter));

      className = "now.im.namespaced.NamespacerTests$ConcreteGenericClassToNamespace$Nested";
      GrowableByteArray.clear(classFileBefore);
      GrowableByteArray.clear(classFileAfter);
      GrowableByteArray.readFully(
          classFileBefore, classAsStream(ConcreteGenericClassToNamespace.Nested.class));
      assertNull(
          Namespacer.namespaceClassFile(
              objectPool,
              classNameToPath(ClassToNamespace.class),
              classFileBefore,
              replacement,
              classFileAfter));

      classGenerator.generateClass(className, bytes(classFileAfter), 0, size(classFileAfter));

      className = "now.im.namespaced.NamespacerTests$ConcreteGenericClassToNamespace";
      GrowableByteArray.clear(classFileBefore);
      GrowableByteArray.clear(classFileAfter);
      GrowableByteArray.readFully(
          classFileBefore, classAsStream(ConcreteGenericClassToNamespace.class));
      assertNull(
          Namespacer.namespaceClassFile(
              objectPool,
              classNameToPath(ConcreteGenericClassToNamespace.class),
              classFileBefore,
              replacement,
              classFileAfter));

      final Class<?> genericClass =
          classGenerator.generateClass(className, bytes(classFileAfter), 0, size(classFileAfter));
      final Object generic = invoke(genericClass, null, "<init>", new Class<?>[0], new Object[0]);

      assertEquals(
          nestedGeneric,
          invoke(
              genericParent,
              null,
              "staticGenericReturnAndArg",
              new Class[] {nestedGenericClass},
              new Object[] {nestedGeneric}));
      assertEquals(
          namespaced,
          invoke(
              genericParent,
              null,
              "staticReturnAndArg",
              new Class[] {namespacedClass},
              new Object[] {namespaced}));
      assertEquals(
          nestedGeneric,
          invoke(
              genericParent,
              generic,
              "virtualGenericReturnAndArg",
              new Class[] {nestedGenericClass},
              new Object[] {nestedGeneric}));
      assertEquals(
          namespaced,
          invoke(
              genericParent,
              generic,
              "virtualReturnAndArg",
              new Class[] {namespacedClass},
              new Object[] {namespaced}));
      assertEquals(
          nestedGeneric,
          invoke(
              genericClass,
              generic,
              "virtualGenericReturnAndArg",
              new Class[] {nestedGenericClass},
              new Object[] {nestedGeneric}));
      assertEquals(
          namespaced,
          invoke(
              genericClass,
              generic,
              "virtualReturnAndArg",
              new Class[] {namespacedClass},
              new Object[] {namespaced}));
      assertEquals(
          nestedGeneric,
          invoke(
              genericClass,
              generic,
              "abstractGenericReturnAndArg",
              new Class[] {nestedGenericClass},
              new Object[] {nestedGeneric}));
      assertEquals(
          namespaced,
          invoke(
              genericClass,
              generic,
              "abstractReturnAndArg",
              new Class[] {namespacedClass},
              new Object[] {namespaced}));
      assertEquals(
          "now.im.namespaced.toString",
          invoke(namespacedClass, namespaced, "toString", new Class[] {}, new Object[] {}));
    } catch (final Throwable t) {
      ClassGenerator.writeClassOnError(t, className, classFileAfter);
      throw t;
    }
  }

  private static String classNameToPath(final Class<?> aClass) {
    return aClass.getTypeName().replace('.', '/') + ".class";
  }

  private static Object invoke(
      final Class<?> aClass,
      final Object object,
      final String methodName,
      final Class<?>[] argTypes,
      final Object[] args)
      throws ReflectiveOperationException {

    if ("<init>".equals(methodName)) {
      return aClass.getConstructor(argTypes).newInstance(args);
    }

    return aClass.getMethod(methodName, argTypes).invoke(object, args);
  }

  @SuppressWarnings("unused")
  public static final class ClassToNamespace {

    public ClassToNamespace() {}

    @Override
    public String toString() {
      return "dev.stiemannkj1.bytecode.namespacer.toString";
    }
  }

  @SuppressWarnings("unused")
  public static final class NestedGeneric<T> {
    public T getValue() {
      return null;
    }
  }

  @SuppressWarnings("unused")
  public abstract static class AbstractGenericClassToNamespace<
      T extends NestedGeneric<ClassToNamespace>> {

    public AbstractGenericClassToNamespace() {}

    public static <T extends NestedGeneric<ClassToNamespace>> T staticGenericReturnAndArg(
        final T t) {
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

  @SuppressWarnings("unused")
  public static final class ConcreteGenericClassToNamespace
      extends AbstractGenericClassToNamespace<NestedGeneric<ClassToNamespace>> {

    public ConcreteGenericClassToNamespace() {}

    @Override
    public NestedGeneric<ClassToNamespace> abstractGenericReturnAndArg(
        final NestedGeneric<ClassToNamespace> classToNamespace) {
      return classToNamespace;
    }

    @Override
    public ClassToNamespace abstractReturnAndArg(final ClassToNamespace t) {
      return t;
    }

    public static void superGeneric(final List<? super ClassToNamespace> list) {}

    public static void extendsGeneric(final List<? extends ClassToNamespace> list) {}

    public static void wildcard(final List<?> list) {}

    public static <T extends ClassToNamespace> void boundedGenericWithPrimitives(
        byte byte1,
        short short1,
        int int1,
        long long1,
        float float1,
        double double1,
        char char1,
        boolean boolean1,
        final T t,
        byte byte2,
        short short2,
        int int2,
        long long2,
        float float2,
        double double2,
        char char2,
        boolean boolean2) {}

    public static <T> void genericWithPrimitives(
        byte byte1,
        short short1,
        int int1,
        long long1,
        float float1,
        double double1,
        char char1,
        boolean boolean1,
        final T t,
        byte byte2,
        short short2,
        int int2,
        long long2,
        float float2,
        double double2,
        char char2,
        boolean boolean2) {}

    @SuppressWarnings("unused")
    public static final class ThrowableToNamespace extends Throwable {}

    @SuppressWarnings("unused")
    public static void throwing() throws ThrowableToNamespace, Exception {}

    @SuppressWarnings("unused")
    public static <T extends ThrowableToNamespace> void throwingGeneric1() throws T, Exception {}

    @SuppressWarnings("unused")
    public static <T extends IOException> void throwingGeneric2() throws T, ThrowableToNamespace {}

    @SuppressWarnings("unused")
    public final class Nested<T extends NestedGeneric<ClassToNamespace>> {
      public final class NestedNested<U extends T> {}

      public final NestedNested<T> nestedNested = null;
    }

    public static <T extends NestedGeneric<ClassToNamespace>> Nested<T> nested(
        final Nested<T> nested) {
      return nested;
    }

    public static <T extends Nested<U>, U extends NestedGeneric<ClassToNamespace>>
        T genericWithNested(final T t) {
      return t;
    }

    public static Object local() {
      class Local<U> {}
      return new Local<ClassToNamespace>();
    }

    public static Object anonymous() {
      return new Object() {
        @Override
        public String toString() {
          return "foo";
        }
      };
    }

    public static <T extends Nested<U>, U extends NestedGeneric<ClassToNamespace>>
        T[][][] genericWithNestedArray(final T[][][] t) {
      return t;
    }

    public static ClassToNamespace[][][] array3d(final ClassToNamespace[][][] array) {
      return array;
    }

    public static ClassToNamespace[] array(final ClassToNamespace[] array) {
      return array;
    }
  }
}
