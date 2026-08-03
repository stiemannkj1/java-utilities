# `SJK.java`

SJK is a "Software Joy Kit" for Java. It is a single Java file which depends
solely on JDK 8 to simplify Java development and supplement the JDK.

## Usage

Download via `jshell`:

```sh
printf 'import java.net.http.*;HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("https://raw.githubusercontent.com/stiemannkj1/java-utilities/refs/heads/master/stiemannkj1/SJK.java")).build(),HttpResponse.BodyHandlers.ofFile(Path.of(System.getProperty("user.home")+"/Downloads/SJK.java")));' | jshell -
```

Or download via `curl`:

```sh
curl -o ~/Downloads/SJK.java 'https://raw.githubusercontent.com/stiemannkj1/java-utilities/refs/heads/master/stiemannkj1/SJK.java'
```

Run from source to see CLI usage:

```sh
java ~/Downloads/SJK.java
```

To add `SJK.java` as a dependency to your project, simply create a folder under
your source directory named `stiemannkj1/` and add it to that folder:
`stiemannkj1/SJK.java`.

Run a file server:

```sh
java ~/Downloads/SJK.java fileServer --port 8080
```

Download a file:

```sh
java ~/Downloads/SJK.java --download https://stiemannkj1.gitlab.com/output.txt output.txt
```

Upload a file:

```sh
java ~/Downloads/SJK.java --upload source.txt https://stiemannkj1.gitlab.com/source.txt
```

Build a `.jar`:

```sh
java ~/Downloads/SJK.java javac --clean --release 8 --manifest 'Main-Class: stiemannkj1.SJK' ./stiemannkj1 build/ --jar build/sjk.jar
```

Run tests:

```
java ~/Downloads/SJK.java fileServerTest
```

Format source:

```
java -jar vendor/gfmt.jar --replace stiemannkj1/**.java
```

* TODO add code to generate `sjk` and `sjk.bat` and add them to `.gitignore`.

## License

[Apache License Version 2.0](./LICENSE.txt)
