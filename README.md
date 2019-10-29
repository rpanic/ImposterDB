# ImposterDB

ImposterDB is a database which uses a unique concept for abstraction. It combines a classical data layer in the background with the observer pattern to allow the creation of imposter classes which control immediate events connected with the data. This system will get more obvious in the Usage section.


## Installation

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

## Implementation

To demonstrate the implementation of a ImposterDB table we'll create an object called `Person`:

```kotlin
class Person : Observable(){

    var name: String by observable("")

    var description: String? by observable(null)

}
```

It's your typical class with the big difference being it extending Observable and each variable being a delegated property of `observable`. This class only holds variables and thus can be compared to a data class. You can pass the default value of the variable in the parentheses of `observable`.

Any observable events gets outsourced to another class, in this case called `PersonObserver`. The constructor takes Person as a parameter and passes this on to the extended class `ChangeObserver`. The function names need to either match any member name of the class passed in the constructor or be named "all" to be affected by any change happening to a Person.
```kotlin
class PersonObserver(t: Person) : ChangeObserver<Person>(t){

    fun name(new: String){
        println("New name: $new!!!!")
    }

    fun all(new: Any?){
        println("Prop changed $new")
    }

}
```

To get the reference to a single object, `DB.getObject("...") { }` is called.
```kotlin
val obj = DB.getObject("person") {
      Person()
}

obj.name = "John Miller"

obj.description = "This is some random stuff"
```
Lists work similarly:
```kotlin
val list = DB.getList<Person>("persons")

val p1 = Person()

list.add(p1)

p1.name = "John Miller 2"
p1.description = "Something"

val p2 = list.add(Person())

p2.name = "John Miller 3"
p2.description = "Something else"
```
