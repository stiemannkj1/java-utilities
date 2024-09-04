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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class NamespacerTest {
  // B C D F I J S Z V
  // https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.7.9.1

  // TODO namespace and merge class names in META-INF/services and META-INF/groovy
  // TODO handle manifest and properties files. Merging properties should probably require users to
  // handle conflicts by creating a properties file with any conflicting properties.
  // TODO consider merging Xml files

  public static final class Before {
    public static List<String> strings() {
      return Arrays.asList(
          // Invalid signatures:
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace",
          "dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace;",
          "dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace ",
          "dev.stiemannkj1.bytecode.NamespacerTest$ConcreteGenericClassToNamespace;",
          "dev.stiemannkj1.bytecode.NamespacerTest$ConcreteGenericClassToNamespace ",
          "(Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested;Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$NestedGeneric<Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace<Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace<Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace<Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace;>;>;>;",
          "dev.stiemannkj1.bytecode.NamespacerTest$ConcreteGenericClassToNamespace",
          "META-INF/services/dev.stiemannkj1.bytecode.NamespacerTest$ConcreteGenericClassToNamespace",
          "/META-INF/services/dev.stiemannkj1.bytecode.NamespacerTest$ConcreteGenericClassToNamespace",
          "WEB-INF/classes/META-INF/services/dev.stiemannkj1.bytecode.NamespacerTest$ConcreteGenericClassToNamespace",
          "/WEB-INF/classes/META-INF/services/dev.stiemannkj1.bytecode.NamespacerTest$ConcreteGenericClassToNamespace",
          "WEB-INF/classes/dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace.class",
          "/WEB-INF/classes/dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace.class",
          "dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace",
          "dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace.class",
          "/dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace.class",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace;",
          "[Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$NestedGeneric<Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>;",
          "(Ldev/stiemannkj1/bytecode/NamespacerTest$NestedGeneric<Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>;)Ldev/stiemannkj1/bytecode/NamespacerTest$NestedGeneric<Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>;",
          "(Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;)Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;",
          "(Ljava/util/List;)V",
          "Ljava/util/List;",
          "Ljava/util/List<-Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>;",
          "(Ljava/util/List<-Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>;)V",
          "Ljava/util/List<+Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>;",
          "(Ljava/util/List<+Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>;)V",
          "Ljava/util/List<*>;",
          "(Ljava/util/List<*>;)V",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace<-Ljava/util/List;>;",
          "(Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace<-Ljava/util/List;>;)V",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace<+Ljava/util/List;>;",
          "(Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace<+Ljava/util/List;>;)V",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace<*>;",
          "(Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace<*>;)V",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace<-Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;*+Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>;",
          "(BSIJFDCZLdev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;BSIJFDCZ)V",
          "TT;",
          "<T:Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>(BSIJFDCZTT;BSIJFDCZ)V",
          "(BSIJFDCZLjava/lang/Object;BSIJFDCZ)V",
          "Ljava/lang/Object;",
          "<T:Ljava/lang/Object;>(BSIJFDCZTT;BSIJFDCZ)V",
          "(Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested;)Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "<T:Ldev/stiemannkj1/bytecode/NamespacerTest$NestedGeneric<Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>;>(Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>;)Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "<T:Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TU;>;U:Ldev/stiemannkj1/bytecode/NamespacerTest$NestedGeneric<Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>;>(TT;)TT;",
          "<T:Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$ThrowableToNamespace;>()V^TT;^Ljava/lang/Exception;",
          "<T:Ljava/io/IOException;>()V^TT;^Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$ThrowableToNamespace;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$AbstractGenericClassToNamespace<Ldev/stiemannkj1/bytecode/NamespacerTest$NestedGeneric<Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>;>;",
          "<T:Ldev/stiemannkj1/bytecode/NamespacerTest$NestedGeneric<Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>;>Ljava/lang/Object;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>.NestedNested<TT;>;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested$NestedNested;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>.NestedNested<TU;>;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$1Local<TU;>;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$1Local;",
          "()Ljava/lang/Object;",
          "dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$1Local",
          "dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$1",
          "dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested",
          "dev.stiemannkj1.bytecode.NamespacerTest$ConcreteGenericClassToNamespace$Nested$NestedNested",
          "dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested$NestedNested",
          "dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$ThrowableToNamespace",
          "dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace",
          "dev/stiemannkj1/bytecode/NamespacerTest$AbstractGenericClassToNamespace",
          "dev/stiemannkj1/bytecode/NamespacerTest$NestedGeneric",
          "dev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace",
          "dev/stiemannkj1/bytecode/NamespacerTest",
          "([[[Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested;)[[[Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "[[[Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "[[[TT;",
          "<T:Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TU;>;U:Ldev/stiemannkj1/bytecode/NamespacerTest$NestedGeneric<Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>;>([[[TT;)[[[TT;",
          "([[[Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;)[[[Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;",
          "[[[Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;",
          "([Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;)[Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;",
          "[Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$AbstractGenericClassToNamespace<Ldev/stiemannkj1/bytecode/NamespacerTest$NestedGeneric<Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>;>;");
    }
  }

  public static final class ExpectedAfterShorter {
    public static List<String> strings() {
      return Arrays.asList(
          // Invalid signatures:
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace",
          "dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace;",
          "dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace ",
          "dev.stiemannkj1.bytecode.NamespacerTest$ConcreteGenericClassToNamespace;",
          "dev.stiemannkj1.bytecode.NamespacerTest$ConcreteGenericClassToNamespace ",
          "(Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested;Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$NestedGeneric<Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;",
          "Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace<Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace<Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace<Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace;>;>;>;",
          "now.im.namespaced.NamespacerTest$ConcreteGenericClassToNamespace",
          "META-INF/services/now.im.namespaced.NamespacerTest$ConcreteGenericClassToNamespace",
          "/META-INF/services/now.im.namespaced.NamespacerTest$ConcreteGenericClassToNamespace",
          "WEB-INF/classes/META-INF/services/now.im.namespaced.NamespacerTest$ConcreteGenericClassToNamespace",
          "/WEB-INF/classes/META-INF/services/now.im.namespaced.NamespacerTest$ConcreteGenericClassToNamespace",
          "WEB-INF/classes/now/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace.class",
          "/WEB-INF/classes/now/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace.class",
          "now/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace",
          "now/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace.class",
          "/now/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace.class",
          "Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace;",
          "[Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace;",
          "Lnow/im/namespaced/NamespacerTest$NestedGeneric<Lnow/im/namespaced/NamespacerTest$ClassToNamespace;>;",
          "(Lnow/im/namespaced/NamespacerTest$NestedGeneric<Lnow/im/namespaced/NamespacerTest$ClassToNamespace;>;)Lnow/im/namespaced/NamespacerTest$NestedGeneric<Lnow/im/namespaced/NamespacerTest$ClassToNamespace;>;",
          "(Lnow/im/namespaced/NamespacerTest$ClassToNamespace;)Lnow/im/namespaced/NamespacerTest$ClassToNamespace;",
          "Lnow/im/namespaced/NamespacerTest$ClassToNamespace;",
          "(Ljava/util/List;)V",
          "Ljava/util/List;",
          "Ljava/util/List<-Lnow/im/namespaced/NamespacerTest$ClassToNamespace;>;",
          "(Ljava/util/List<-Lnow/im/namespaced/NamespacerTest$ClassToNamespace;>;)V",
          "Ljava/util/List<+Lnow/im/namespaced/NamespacerTest$ClassToNamespace;>;",
          "(Ljava/util/List<+Lnow/im/namespaced/NamespacerTest$ClassToNamespace;>;)V",
          "Ljava/util/List<*>;",
          "(Ljava/util/List<*>;)V",
          "Lnow/im/namespaced/NamespacerTest$ClassToNamespace<-Ljava/util/List;>;",
          "(Lnow/im/namespaced/NamespacerTest$ClassToNamespace<-Ljava/util/List;>;)V",
          "Lnow/im/namespaced/NamespacerTest$ClassToNamespace<+Ljava/util/List;>;",
          "(Lnow/im/namespaced/NamespacerTest$ClassToNamespace<+Ljava/util/List;>;)V",
          "Lnow/im/namespaced/NamespacerTest$ClassToNamespace<*>;",
          "(Lnow/im/namespaced/NamespacerTest$ClassToNamespace<*>;)V",
          "Lnow/im/namespaced/NamespacerTest$ClassToNamespace<-Lnow/im/namespaced/NamespacerTest$ClassToNamespace;*+Lnow/im/namespaced/NamespacerTest$ClassToNamespace;>;",
          "(BSIJFDCZLnow/im/namespaced/NamespacerTest$ClassToNamespace;BSIJFDCZ)V",
          "TT;",
          "<T:Lnow/im/namespaced/NamespacerTest$ClassToNamespace;>(BSIJFDCZTT;BSIJFDCZ)V",
          "(BSIJFDCZLjava/lang/Object;BSIJFDCZ)V",
          "Ljava/lang/Object;",
          "<T:Ljava/lang/Object;>(BSIJFDCZTT;BSIJFDCZ)V",
          "(Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$Nested;)Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "<T:Lnow/im/namespaced/NamespacerTest$NestedGeneric<Lnow/im/namespaced/NamespacerTest$ClassToNamespace;>;>(Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>;)Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "<T:Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TU;>;U:Lnow/im/namespaced/NamespacerTest$NestedGeneric<Lnow/im/namespaced/NamespacerTest$ClassToNamespace;>;>(TT;)TT;",
          "<T:Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$ThrowableToNamespace;>()V^TT;^Ljava/lang/Exception;",
          "<T:Ljava/io/IOException;>()V^TT;^Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$ThrowableToNamespace;",
          "Lnow/im/namespaced/NamespacerTest$AbstractGenericClassToNamespace<Lnow/im/namespaced/NamespacerTest$NestedGeneric<Lnow/im/namespaced/NamespacerTest$ClassToNamespace;>;>;",
          "<T:Lnow/im/namespaced/NamespacerTest$NestedGeneric<Lnow/im/namespaced/NamespacerTest$ClassToNamespace;>;>Ljava/lang/Object;",
          "Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>.NestedNested<TT;>;",
          "Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$Nested$NestedNested;",
          "Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>.NestedNested<TU;>;",
          "Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$1Local<TU;>;",
          "Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$1Local;",
          "()Ljava/lang/Object;",
          "now/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$1Local",
          "now/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$1",
          "now/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$Nested",
          "now.im.namespaced.NamespacerTest$ConcreteGenericClassToNamespace$Nested$NestedNested",
          "now/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$Nested$NestedNested",
          "now/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$ThrowableToNamespace",
          "now/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace",
          "now/im/namespaced/NamespacerTest$AbstractGenericClassToNamespace",
          "now/im/namespaced/NamespacerTest$NestedGeneric",
          "now/im/namespaced/NamespacerTest$ClassToNamespace",
          "now/im/namespaced/NamespacerTest",
          "([[[Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$Nested;)[[[Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "[[[Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "[[[TT;",
          "<T:Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TU;>;U:Lnow/im/namespaced/NamespacerTest$NestedGeneric<Lnow/im/namespaced/NamespacerTest$ClassToNamespace;>;>([[[TT;)[[[TT;",
          "([[[Lnow/im/namespaced/NamespacerTest$ClassToNamespace;)[[[Lnow/im/namespaced/NamespacerTest$ClassToNamespace;",
          "[[[Lnow/im/namespaced/NamespacerTest$ClassToNamespace;",
          "([Lnow/im/namespaced/NamespacerTest$ClassToNamespace;)[Lnow/im/namespaced/NamespacerTest$ClassToNamespace;",
          "[Lnow/im/namespaced/NamespacerTest$ClassToNamespace;",
          "Lnow/im/namespaced/NamespacerTest$AbstractGenericClassToNamespace<Lnow/im/namespaced/NamespacerTest$NestedGeneric<Lnow/im/namespaced/NamespacerTest$ClassToNamespace;>;>;");
    }
  }

  public static final class ExpectedAfterLonger {
    public static List<String> strings() {
      return Arrays.asList(
          // Invalid signatures:
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace",
          "dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace;",
          "dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace ",
          "dev.stiemannkj1.bytecode.NamespacerTest$ConcreteGenericClassToNamespace;",
          "dev.stiemannkj1.bytecode.NamespacerTest$ConcreteGenericClassToNamespace ",
          "(Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested;Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$NestedGeneric<Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;",
          "Lnow/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace<Lnow/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace<Lnow/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace<Lnow/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace;>;>;>;",
          "now.im.namespaced.and.longer.NamespacerTest$ConcreteGenericClassToNamespace",
          "META-INF/services/now.im.namespaced.and.longer.NamespacerTest$ConcreteGenericClassToNamespace",
          "/META-INF/services/now.im.namespaced.and.longer.NamespacerTest$ConcreteGenericClassToNamespace",
          "WEB-INF/classes/META-INF/services/now.im.namespaced.and.longer.NamespacerTest$ConcreteGenericClassToNamespace",
          "/WEB-INF/classes/META-INF/services/now.im.namespaced.and.longer.NamespacerTest$ConcreteGenericClassToNamespace",
          "WEB-INF/classes/now/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace.class",
          "/WEB-INF/classes/now/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace.class",
          "now/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace",
          "now/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace.class",
          "/now/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace.class",
          "Lnow/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace;",
          "[Lnow/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace;",
          "Lnow/im/namespaced/and/longer/NamespacerTest$NestedGeneric<Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace;>;",
          "(Lnow/im/namespaced/and/longer/NamespacerTest$NestedGeneric<Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace;>;)Lnow/im/namespaced/and/longer/NamespacerTest$NestedGeneric<Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace;>;",
          "(Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace;)Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace;",
          "Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace;",
          "(Ljava/util/List;)V",
          "Ljava/util/List;",
          "Ljava/util/List<-Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace;>;",
          "(Ljava/util/List<-Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace;>;)V",
          "Ljava/util/List<+Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace;>;",
          "(Ljava/util/List<+Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace;>;)V",
          "Ljava/util/List<*>;",
          "(Ljava/util/List<*>;)V",
          "Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace<-Ljava/util/List;>;",
          "(Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace<-Ljava/util/List;>;)V",
          "Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace<+Ljava/util/List;>;",
          "(Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace<+Ljava/util/List;>;)V",
          "Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace<*>;",
          "(Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace<*>;)V",
          "Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace<-Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace;*+Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace;>;",
          "(BSIJFDCZLnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace;BSIJFDCZ)V",
          "TT;",
          "<T:Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace;>(BSIJFDCZTT;BSIJFDCZ)V",
          "(BSIJFDCZLjava/lang/Object;BSIJFDCZ)V",
          "Ljava/lang/Object;",
          "<T:Ljava/lang/Object;>(BSIJFDCZTT;BSIJFDCZ)V",
          "(Lnow/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace$Nested;)Lnow/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "Lnow/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "Lnow/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "<T:Lnow/im/namespaced/and/longer/NamespacerTest$NestedGeneric<Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace;>;>(Lnow/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>;)Lnow/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "<T:Lnow/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TU;>;U:Lnow/im/namespaced/and/longer/NamespacerTest$NestedGeneric<Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace;>;>(TT;)TT;",
          "<T:Lnow/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace$ThrowableToNamespace;>()V^TT;^Ljava/lang/Exception;",
          "<T:Ljava/io/IOException;>()V^TT;^Lnow/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace$ThrowableToNamespace;",
          "Lnow/im/namespaced/and/longer/NamespacerTest$AbstractGenericClassToNamespace<Lnow/im/namespaced/and/longer/NamespacerTest$NestedGeneric<Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace;>;>;",
          "<T:Lnow/im/namespaced/and/longer/NamespacerTest$NestedGeneric<Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace;>;>Ljava/lang/Object;",
          "Lnow/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "Lnow/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "Lnow/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>.NestedNested<TT;>;",
          "Lnow/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace$Nested$NestedNested;",
          "Lnow/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>.NestedNested<TU;>;",
          "Lnow/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace$1Local<TU;>;",
          "Lnow/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace$1Local;",
          "()Ljava/lang/Object;",
          "now/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace$1Local",
          "now/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace$1",
          "now/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace$Nested",
          "now.im.namespaced.and.longer.NamespacerTest$ConcreteGenericClassToNamespace$Nested$NestedNested",
          "now/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace$Nested$NestedNested",
          "now/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace$ThrowableToNamespace",
          "now/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace",
          "now/im/namespaced/and/longer/NamespacerTest$AbstractGenericClassToNamespace",
          "now/im/namespaced/and/longer/NamespacerTest$NestedGeneric",
          "now/im/namespaced/and/longer/NamespacerTest$ClassToNamespace",
          "now/im/namespaced/and/longer/NamespacerTest",
          "([[[Lnow/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace$Nested;)[[[Lnow/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "[[[Lnow/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "[[[TT;",
          "<T:Lnow/im/namespaced/and/longer/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TU;>;U:Lnow/im/namespaced/and/longer/NamespacerTest$NestedGeneric<Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace;>;>([[[TT;)[[[TT;",
          "([[[Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace;)[[[Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace;",
          "[[[Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace;",
          "([Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace;)[Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace;",
          "[Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace;",
          "Lnow/im/namespaced/and/longer/NamespacerTest$AbstractGenericClassToNamespace<Lnow/im/namespaced/and/longer/NamespacerTest$NestedGeneric<Lnow/im/namespaced/and/longer/NamespacerTest$ClassToNamespace;>;>;");
    }
  }

  public static final class ExpectedAfterSameSize {
    public static List<String> strings() {
      return Arrays.asList(
          // Invalid signatures:
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace",
          "dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace;",
          "dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace ",
          "dev.stiemannkj1.bytecode.NamespacerTest$ConcreteGenericClassToNamespace;",
          "dev.stiemannkj1.bytecode.NamespacerTest$ConcreteGenericClassToNamespace ",
          "(Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested;Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$NestedGeneric<Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;",
          "Lnow/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace<Lnow/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace<Lnow/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace<Lnow/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace;>;>;>;",
          "now.im.namespaced.same12.NamespacerTest$ConcreteGenericClassToNamespace",
          "META-INF/services/now.im.namespaced.same12.NamespacerTest$ConcreteGenericClassToNamespace",
          "/META-INF/services/now.im.namespaced.same12.NamespacerTest$ConcreteGenericClassToNamespace",
          "WEB-INF/classes/META-INF/services/now.im.namespaced.same12.NamespacerTest$ConcreteGenericClassToNamespace",
          "/WEB-INF/classes/META-INF/services/now.im.namespaced.same12.NamespacerTest$ConcreteGenericClassToNamespace",
          "WEB-INF/classes/now/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace.class",
          "/WEB-INF/classes/now/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace.class",
          "now/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace",
          "now/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace.class",
          "/now/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace.class",
          "Lnow/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace;",
          "[Lnow/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace;",
          "Lnow/im/namespaced/same12/NamespacerTest$NestedGeneric<Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace;>;",
          "(Lnow/im/namespaced/same12/NamespacerTest$NestedGeneric<Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace;>;)Lnow/im/namespaced/same12/NamespacerTest$NestedGeneric<Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace;>;",
          "(Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace;)Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace;",
          "Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace;",
          "(Ljava/util/List;)V",
          "Ljava/util/List;",
          "Ljava/util/List<-Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace;>;",
          "(Ljava/util/List<-Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace;>;)V",
          "Ljava/util/List<+Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace;>;",
          "(Ljava/util/List<+Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace;>;)V",
          "Ljava/util/List<*>;",
          "(Ljava/util/List<*>;)V",
          "Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace<-Ljava/util/List;>;",
          "(Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace<-Ljava/util/List;>;)V",
          "Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace<+Ljava/util/List;>;",
          "(Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace<+Ljava/util/List;>;)V",
          "Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace<*>;",
          "(Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace<*>;)V",
          "Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace<-Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace;*+Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace;>;",
          "(BSIJFDCZLnow/im/namespaced/same12/NamespacerTest$ClassToNamespace;BSIJFDCZ)V",
          "TT;",
          "<T:Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace;>(BSIJFDCZTT;BSIJFDCZ)V",
          "(BSIJFDCZLjava/lang/Object;BSIJFDCZ)V",
          "Ljava/lang/Object;",
          "<T:Ljava/lang/Object;>(BSIJFDCZTT;BSIJFDCZ)V",
          "(Lnow/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace$Nested;)Lnow/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "Lnow/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "Lnow/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "<T:Lnow/im/namespaced/same12/NamespacerTest$NestedGeneric<Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace;>;>(Lnow/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>;)Lnow/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "<T:Lnow/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TU;>;U:Lnow/im/namespaced/same12/NamespacerTest$NestedGeneric<Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace;>;>(TT;)TT;",
          "<T:Lnow/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace$ThrowableToNamespace;>()V^TT;^Ljava/lang/Exception;",
          "<T:Ljava/io/IOException;>()V^TT;^Lnow/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace$ThrowableToNamespace;",
          "Lnow/im/namespaced/same12/NamespacerTest$AbstractGenericClassToNamespace<Lnow/im/namespaced/same12/NamespacerTest$NestedGeneric<Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace;>;>;",
          "<T:Lnow/im/namespaced/same12/NamespacerTest$NestedGeneric<Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace;>;>Ljava/lang/Object;",
          "Lnow/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "Lnow/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "Lnow/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>.NestedNested<TT;>;",
          "Lnow/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace$Nested$NestedNested;",
          "Lnow/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>.NestedNested<TU;>;",
          "Lnow/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace$1Local<TU;>;",
          "Lnow/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace$1Local;",
          "()Ljava/lang/Object;",
          "now/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace$1Local",
          "now/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace$1",
          "now/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace$Nested",
          "now.im.namespaced.same12.NamespacerTest$ConcreteGenericClassToNamespace$Nested$NestedNested",
          "now/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace$Nested$NestedNested",
          "now/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace$ThrowableToNamespace",
          "now/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace",
          "now/im/namespaced/same12/NamespacerTest$AbstractGenericClassToNamespace",
          "now/im/namespaced/same12/NamespacerTest$NestedGeneric",
          "now/im/namespaced/same12/NamespacerTest$ClassToNamespace",
          "now/im/namespaced/same12/NamespacerTest",
          "([[[Lnow/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace$Nested;)[[[Lnow/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "[[[Lnow/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "[[[TT;",
          "<T:Lnow/im/namespaced/same12/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TU;>;U:Lnow/im/namespaced/same12/NamespacerTest$NestedGeneric<Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace;>;>([[[TT;)[[[TT;",
          "([[[Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace;)[[[Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace;",
          "[[[Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace;",
          "([Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace;)[Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace;",
          "[Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace;",
          "Lnow/im/namespaced/same12/NamespacerTest$AbstractGenericClassToNamespace<Lnow/im/namespaced/same12/NamespacerTest$NestedGeneric<Lnow/im/namespaced/same12/NamespacerTest$ClassToNamespace;>;>;");
    }
  }

  public static final class ExpectedAfterNestedNestedReplaced {
    public static List<String> strings() {
      return Arrays.asList(
          // Invalid signatures:
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace",
          "dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace;",
          "dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace ",
          "dev.stiemannkj1.bytecode.NamespacerTest$ConcreteGenericClassToNamespace;",
          "dev.stiemannkj1.bytecode.NamespacerTest$ConcreteGenericClassToNamespace ",
          "(Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested;Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$NestedGeneric<Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace<Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace<Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace<Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>.NestedNested<TT;>;>;>;>;",
          "dev.stiemannkj1.bytecode.NamespacerTest$ConcreteGenericClassToNamespace",
          "META-INF/services/dev.stiemannkj1.bytecode.NamespacerTest$ConcreteGenericClassToNamespace",
          "/META-INF/services/dev.stiemannkj1.bytecode.NamespacerTest$ConcreteGenericClassToNamespace",
          "WEB-INF/classes/META-INF/services/dev.stiemannkj1.bytecode.NamespacerTest$ConcreteGenericClassToNamespace",
          "/WEB-INF/classes/META-INF/services/dev.stiemannkj1.bytecode.NamespacerTest$ConcreteGenericClassToNamespace",
          "WEB-INF/classes/dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace.class",
          "/WEB-INF/classes/dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace.class",
          "dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace",
          "dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace.class",
          "/dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace.class",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace;",
          "[Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$NestedGeneric<Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>;",
          "(Ldev/stiemannkj1/bytecode/NamespacerTest$NestedGeneric<Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>;)Ldev/stiemannkj1/bytecode/NamespacerTest$NestedGeneric<Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>;",
          "(Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;)Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;",
          "(Ljava/util/List;)V",
          "Ljava/util/List;",
          "Ljava/util/List<-Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>;",
          "(Ljava/util/List<-Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>;)V",
          "Ljava/util/List<+Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>;",
          "(Ljava/util/List<+Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>;)V",
          "Ljava/util/List<*>;",
          "(Ljava/util/List<*>;)V",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace<-Ljava/util/List;>;",
          "(Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace<-Ljava/util/List;>;)V",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace<+Ljava/util/List;>;",
          "(Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace<+Ljava/util/List;>;)V",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace<*>;",
          "(Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace<*>;)V",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace<-Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;*+Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>;",
          "(BSIJFDCZLdev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;BSIJFDCZ)V",
          "TT;",
          "<T:Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>(BSIJFDCZTT;BSIJFDCZ)V",
          "(BSIJFDCZLjava/lang/Object;BSIJFDCZ)V",
          "Ljava/lang/Object;",
          "<T:Ljava/lang/Object;>(BSIJFDCZTT;BSIJFDCZ)V",
          "(Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested;)Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "<T:Ldev/stiemannkj1/bytecode/NamespacerTest$NestedGeneric<Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>;>(Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>;)Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "<T:Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TU;>;U:Ldev/stiemannkj1/bytecode/NamespacerTest$NestedGeneric<Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>;>(TT;)TT;",
          "<T:Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$ThrowableToNamespace;>()V^TT;^Ljava/lang/Exception;",
          "<T:Ljava/io/IOException;>()V^TT;^Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$ThrowableToNamespace;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$AbstractGenericClassToNamespace<Ldev/stiemannkj1/bytecode/NamespacerTest$NestedGeneric<Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>;>;",
          "<T:Ldev/stiemannkj1/bytecode/NamespacerTest$NestedGeneric<Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>;>Ljava/lang/Object;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>.NestedNested<TT;>;",
          "Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$Nested$NestedNested;",
          "Lnow/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TT;>.NestedNested<TU;>;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$1Local<TU;>;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$1Local;",
          "()Ljava/lang/Object;",
          "dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$1Local",
          "dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$1",
          "dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested",
          "now.im.namespaced.NamespacerTest$ConcreteGenericClassToNamespace$Nested$NestedNested",
          "now/im/namespaced/NamespacerTest$ConcreteGenericClassToNamespace$Nested$NestedNested",
          "dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$ThrowableToNamespace",
          "dev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace",
          "dev/stiemannkj1/bytecode/NamespacerTest$AbstractGenericClassToNamespace",
          "dev/stiemannkj1/bytecode/NamespacerTest$NestedGeneric",
          "dev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace",
          "dev/stiemannkj1/bytecode/NamespacerTest",
          "([[[Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested;)[[[Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "[[[Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested;",
          "[[[TT;",
          "<T:Ldev/stiemannkj1/bytecode/NamespacerTest$ConcreteGenericClassToNamespace$Nested<TU;>;U:Ldev/stiemannkj1/bytecode/NamespacerTest$NestedGeneric<Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>;>([[[TT;)[[[TT;",
          "([[[Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;)[[[Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;",
          "[[[Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;",
          "([Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;)[Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;",
          "[Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;",
          "Ldev/stiemannkj1/bytecode/NamespacerTest$AbstractGenericClassToNamespace<Ldev/stiemannkj1/bytecode/NamespacerTest$NestedGeneric<Ldev/stiemannkj1/bytecode/NamespacerTest$ClassToNamespace;>;>;");
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
      throws ReflectiveOperationException {

    final GrowableByteArray classFileBefore =
        Allocators.JVM_HEAP.allocateObject(GrowableByteArray::new);
    readBytes(Before.class, classFileBefore);
    final ClassGenerator classGenerator = new ClassGenerator(NamespacerTest.class.getClassLoader());
    final GrowableByteArray classFileAfter =
        Allocators.JVM_HEAP.allocateObject(GrowableByteArray::new);

    assertNull(
        Namespacer.namespace(
            Allocators.JVM_HEAP,
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

  static Stream<Arguments> descriptorsShorterAfter() throws ReflectiveOperationException {
    final Map<String, String> replacement = new HashMap<>();
    replacement.put("dev.stiemannkj1.bytecode", "now.im.namespaced");
    return descriptors(
        replacement, "now.im.namespaced.NamespacerTest$Before", ExpectedAfterShorter.strings());
  }

  @MethodSource("descriptorsShorterAfter")
  @ParameterizedTest
  void it_namespaces_all_descriptors_with_shorter_namespace(
      final String expected, final String namespaced) {
    assertEquals(expected, namespaced);
  }

  static Stream<Arguments> descriptorsLongerAfter() throws ReflectiveOperationException {
    final Map<String, String> replacement = new HashMap<>();
    replacement.put("dev.stiemannkj1.bytecode", "now.im.namespaced.and.longer");
    return descriptors(
        replacement,
        "now.im.namespaced.and.longer.NamespacerTest$Before",
        ExpectedAfterLonger.strings());
  }

  @MethodSource("descriptorsLongerAfter")
  @ParameterizedTest
  void it_namespaces_all_descriptors_with_longer_namespace(
      final String expected, final String namespaced) {
    assertEquals(expected, namespaced);
  }

  static Stream<Arguments> descriptorsSameSize() throws ReflectiveOperationException {
    final Map<String, String> replacement = new HashMap<>();
    replacement.put("dev.stiemannkj1.bytecode", "now.im.namespaced.same12");
    return descriptors(
        replacement,
        "now.im.namespaced.same12.NamespacerTest$Before",
        ExpectedAfterSameSize.strings());
  }

  @MethodSource("descriptorsSameSize")
  @ParameterizedTest
  void it_namespaces_all_descriptors_with_same_size_namespace(
      final String expected, final String namespaced) {
    assertEquals(expected, namespaced);
  }

  static Stream<Arguments> descriptorsReplaceNestedNested() throws ReflectiveOperationException {
    final Map<String, String> replacement = new HashMap<>();
    replacement.put(
        "dev.stiemannkj1.bytecode.NamespacerTest$ConcreteGenericClassToNamespace$Nested$NestedNested",
        "now.im.namespaced.NamespacerTest$ConcreteGenericClassToNamespace$Nested$NestedNested");
    return descriptors(
        replacement,
        "dev.stiemannkj1.bytecode.NamespacerTest$Before",
        ExpectedAfterNestedNestedReplaced.strings());
  }

  @MethodSource("descriptorsReplaceNestedNested")
  @ParameterizedTest
  void it_namespaces_all_nested_nested_descriptors(final String expected, final String namespaced) {
    assertEquals(expected, namespaced);
  }

  @Test
  void it_namespaces_class() throws Throwable {

    final GrowableByteArray classFileBefore =
        Allocators.JVM_HEAP.allocateObject(GrowableByteArray::new);
    readBytes(ClassToNamespace.class, classFileBefore);
    final Map<String, String> replacement = new HashMap<>();
    replacement.put("dev.stiemannkj1.bytecode", "now.im.namespaced");
    final ClassGenerator classGenerator = new ClassGenerator(this.getClass().getClassLoader());
    final GrowableByteArray classFileAfter =
        Allocators.JVM_HEAP.allocateObject(GrowableByteArray::new);

    String className = "";

    try {

      className = "now.im.namespaced.NamespacerTest$ClassToNamespace";
      assertNull(
          Namespacer.namespace(
              Allocators.JVM_HEAP,
              classNameToPath(ClassToNamespace.class),
              classFileBefore,
              replacement,
              classFileAfter));

      final Class<?> namespacedClass =
          classGenerator.generateClass(className, bytes(classFileAfter), 0, size(classFileAfter));

      assertEquals(className, namespacedClass.getTypeName());
      final Object namespaced = namespacedClass.getDeclaredConstructor().newInstance();
      assertEquals("now.im.namespaced.toString", namespaced.toString());

      className = "now.im.namespaced.NamespacerTest$NestedGeneric";
      GrowableByteArray.clear(classFileBefore);
      GrowableByteArray.clear(classFileAfter);
      readBytes(NestedGeneric.class, classFileBefore);
      assertNull(
          Namespacer.namespace(
              Allocators.JVM_HEAP,
              classNameToPath(AbstractGenericClassToNamespace.class),
              classFileBefore,
              replacement,
              classFileAfter));

      final Class<?> nestedGenericClass =
          classGenerator.generateClass(className, bytes(classFileAfter), 0, size(classFileAfter));

      assertEquals(className, nestedGenericClass.getTypeName());
      final Object nestedGeneric = nestedGenericClass.getDeclaredConstructor().newInstance();

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
      final Class<?> genericParent =
          classGenerator.generateClass(className, bytes(classFileAfter), 0, size(classFileAfter));

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

  public static final class ClassToNamespace {

    public ClassToNamespace() {}

    @Override
    public String toString() {
      return "dev.stiemannkj1.bytecode.toString";
    }
  }

  public static final class NestedGeneric<T> {
    public T getValue() {
      return null;
    }
  }

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
      return null;
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

    public static final class ThrowableToNamespace extends Throwable {}

    public static void throwing() throws ThrowableToNamespace, Exception {}

    public static <T extends ThrowableToNamespace> void throwingGeneric1() throws T, Exception {}

    public static <T extends IOException> void throwingGeneric2() throws T, ThrowableToNamespace {}

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
