# Java Utilities

Assorted Java utilities.

## Using

### `FileServer.java`

`FileServer.java` is a Java file server and receiver which depends solely on JDK 11+:

```sh
# Download via jshell:
printf 'import java.net.http.*;HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("https://raw.githubusercontent.com/stiemannkj1/java-utilities/refs/heads/master/src/main/java/stiemannkj1/FileServer.java")).build(),HttpResponse.BodyHandlers.ofFile(Path.of(System.getProperty("user.home")+"/Downloads/FileServer.java")));' | jshell -
# Or download via curl:
# curl -o ~/Downloads/FileServer.java 'https://raw.githubusercontent.com/stiemannkj1/java-utilities/refs/heads/master/src/main/java/stiemannkj1/FileServer.java'
# Run from source:
java ~/Downloads/FileServer.java
```

### Utilities Jar

[![Release](https://jitpack.io/v/stiemannkj1/java-utilities.svg)](https://jitpack.io/#java-utilities)

Add the project as a Gradle dependency via [jitpack.io](https://jitpack.io/):

```
maven {
  url "https://jitpack.io" 
  content { includeGroup "com.github.stiemannkj1" }
}

// Avoid dependency confusion attacks for other repos by excluding this group
// from them. For example, avoid resolving dependencies from this group for
// mavenCentral:
mavenCentral {
  content { excludeGroupByRegex "com[.]github[.]stiemannkj1.*" }
}

implementation 'com.github.stiemannkj1:java-utilities:0.5.0'
```

## Building

Install Java 11 and Gradle 7. If you already have another version of Gradle
installed, you can install the correct version of Gradle via:

```
gradle wrapper
```

If you use Gradle Wrapper to build the project, you'll need to use `./gradlew`
instead of `gradle` in the following commands.

Build a `.jar` with:

```
gradle assemble
```

Run tests with:

```
gradle test
```

Format source with:

```
java -jar vendor/google-java-format-*.jar --replace stiemannkj1/**.java
```

Run all checks and tests with:

```
gradle check
```

## License

[Apache License Version 2.0](./LICENSE.txt)
