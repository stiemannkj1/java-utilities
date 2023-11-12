# Java Utilities

Assorted Java utilities.

## Using

Add the project as a Gradle dependency via [jitpack.io](https://jitpack.io/):

```
maven {
  url "https://jitpack.io" 
  content { includeGroup "com.github.stiemannkj1" }
}

// AVOID dependency confusion attacks for other repos:
mavenCentral {
  content { excludeGroupByRegex "com[.]github[.]stiemannkj1.*" }
}

implementation 'com.github.stiemannkj1:java-utilities:0.1'
```

## Building

Install Java 11 and Gradle 7.

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
