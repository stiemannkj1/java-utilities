# Java Utilities

Assorted Java utilities.

## Using

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

Install Java 11.

Build a `.jar` with:

```
./gradlew assemble
```

Run tests with:

```
./gradlew test
```

Format source with:

```
./gradlew spotlessApply
```

Run all checks and tests with:

```
./gradlew check
```

## License

[Apache License Version 2.0](./LICENSE.txt)

Gradle source and binaries are included for compilation and testing under the
terms in [.gradle/gradle/LICENSE](.gradle/gradle/LICENSE).

Maven source and binaries are included for compilation and testing under the
terms in [.maven/maven/LICENSE](.maven/maven/LICENSE).
