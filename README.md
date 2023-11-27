# Java Utilities

Assorted Java utilities.

## Using

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

implementation 'com.github.stiemannkj1:java-utilities:0.4'
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
gradle spotlessApply
```

Run all checks and tests with:

```
gradle check
```

## License

[Apache License Version 2.0](./LICENSE.txt)
