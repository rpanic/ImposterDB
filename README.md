# ImposterDB

ImposterDB is a database which uses a unique concept for abstraction. It combines a classical data layer in the background with the observer pattern to allow the creation of imposter classes which control immediate events connected with the data. This system will get more obvious in the Usage section.

## Quick example

This is a quick example of how you can use ImposterDB to 



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

### State

To demonstrate the implementation of a ImposterDB table we'll create an object called `Person`:

```kotlin
class Person : Observable(){

    var name: String by observable("")

    var description: String? by observable(null)

    var hobbies: MutableList<Hobby> by observableList()

}
```

It's your typical class with the only difference being it extending Observable and each variable being a delegated property of `observable`. You can pass the default value of the variable in the parentheses of `observable`. 

If you need collections in your class, you can use `observableList` to make changes to your lists and to objects in that list get passed down to the base object or list. This process can be repeated indefinitely.

### Imposter Pattern

Any changes made to the observable gets outsourced as events to another class, in this case called `PersonObserver`. Every Imposter or Observer is attached to one object and is used to relay the state of an Observable object to another layer or system. 

```kotlin
class PersonObserver(t: Person) : ChangeObserver<Person>(t){

    fun name(new: String){
        println("New name: $new!!!!")
    }

    fun all(prop: KProperty<Any?>, old, Any?, new: Any?){
        println("Prop changed $new")
    }

}
```

The order of parameters is not important, except for the `old` and `new` parameters. When both are required by the imposter method, the old comes before new, but when only one is supplied, new is prioritized.

### Start

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
p1.hobbies.add(Hobby("Coding"))
```

### Transactions

To not execute Imposters immediatly after changing an Observable while also keeping an ACID state in all connected systems, one can use Transactions.

```kotlin
DB.tx {
  person.name = "Random name"
}
```

When a exception occures in one of the Imposters, all previously done changes get rolled back and the state of the object is the same as before the transaction.

To keep this consistent state also in your connected system, it is important to base your imposter´s functionality only on the parameters given in the observing method. Using outside or old states can distort the outcome of a rollback-call and therefore break the ACID state.

// This class only holds variables and thus can be compared to a data class. You can pass the default value of the variable in the parentheses of `observable`.

Any observable events gets outsourced to another class, in this case called `PersonObserver`. The constructor takes Person as a parameter and passes this on to the extended class `ChangeObserver`. The function names need to either match any member name of the class passed in the constructor or be named "all" to be affected by any change happening to a Person.
