#ImposterDB

ImposterDB is a database which uses a unique concept for abstraction. It combines a classical data layer in the background with the observer pattern to allow the creation of imposter classes which control immediate events connected with the data. This system will get more obvious in the Usage section.


##Installation

Get the library over at <jitpack.io>:

Gradle:

```gradle
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}

dependencies {
        implementation 'com.github.rpanic:ImposterDB:master-SNAPSHOT'
}
```

or Maven:

```xml
<repositories>
  <repository>
      <id>jitpack.io</id>
      <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependency>
    <groupId>com.github.rpanic</groupId>
    <artifactId>ImposterDB</artifactId>
    <version>master-SNAPSHOT</version>
</dependency>
```
