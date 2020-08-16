# ImposterDB

[![Build Status](https://drone.rpanic.com/api/badges/rpanic/ImposterDB/status.svg)](https://drone.rpanic.com/rpanic/ImposterDB)
[![Release](https://jitpack.io/v/rpanic/ImposterDB.svg)](https://jitpack.io/#rpanic/ImposterDB)

ImposterDB is a database which uses a unique concept for abstraction. It combines a classical data layer in the background with the observer pattern to allow the creation of imposter classes which control immediate events connected with the data. This system will get more obvious in the Usage section.

```kotlin
val db = DB()
db += JsonBackend()

val persons = db.getSet<Person>("persons")

persons.add(Person())

persons.find { name == "Peter Sullivan" }
       .hobbies
       .forEach { println(it) }
       

class Person : Observable(){
    var name: String by observable("")
    val hobbies by detachedSet<Hobby>("hobbies")
}
```

- Creates a new database instance and assigns it a JSON Backend
- Adds a new `Person` instance to the `person` table
- Prints out the hobbies of the person named Peter Sullivan

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

To demonstrate the implementation of a ImposterDB schema object we'll create an object called `Person`:

```kotlin
class Person : Observable(){

    var name: String by observable("")

    var description: String? by observable(null)

    var hobbies: VirtualSet<Hobby> by detachedSet("hobbies")

}
```

It's your typical class with the only difference being it extending Observable and each variable being a delegated property of `observable`. You can pass the default value of the variable in the parentheses of `observable`. 

If you need collections in your class, you can use `observableSet` to make changes to your lists and to objects in that list get passed down to the base object or list. This process can be repeated indefinitely.

### Connections

To connect your instance of ImposterDB to other systems, and subsequently make your data state-dependent on these other systems, 
you have to add at least one Backend to you DB instance.

```kotlin
val db = DB()
db += JsonBackend()
```

Following Backends are currently implemented:
- JsonBackend
- SqlBackend

Custom Backends can be implemented using the `Backend` interface

### Working with data

To get the reference to a set of objects, `DB.getSet(key)` is called.
```kotlin
val set = db.getSet<Person>("persons")

val p = Person()

set.add(p)

p.name = "John Miller"
p.description = "Some text"
p.hobbies.add(Hobby("Coding"))
```

In above example, all changes to the set or `Person` object are synchronized to the backend immediately. 

### Imposter Pattern

Any changes made to the observable get relayed as events to another class, in this case called `PersonObserver`. Every Imposter or Observer is attached to one object and is used to relay the state of an Observable object to another layer or system. 

```kotlin
class PersonObserver(t: Person) : ChangeObserver<Person>(t){

    fun name(new: String){
        println("New name: $new!!!!")
    }

    fun all(prop: KProperty<Any?>, old: Any?, new: Any?){
        println("Property ${prop.name} changed $new")
    }

}
```

The order of parameters is not important, except for the `old` and `new` parameters. When both are required by the imposter method, the old comes before new, but when only one is supplied, new is prioritized.

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

