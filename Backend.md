### Documentation

#### `fun keyExists(key: String) : Boolean`

Checks if a certain key has been created previously

 * **Parameters:** `key:` — The key to check against

#### `fun <T : Observable> createSchema(key: String, clazz: KClass<T>)`

Creates the schema of a given class

 * **Parameters:**
   * `key:` — The key where the Schema should be created
   * `clazz:` — KClass extending Observable with the Schema to be created.
 * **See also:** example.ReflectionUtils for some utils

#### `fun <T : Observable> load(key: String, clazz: KClass<T>, steps: List<Step<T, *>>) : Set<T>`

 * **See also:** loadTransformed but without Mapping- or Aggregation Steps

#### `fun <T : Observable> update(key: String, clazz: KClass<T>, obj: T, prop: KProperty<*>, levelInformation: LevelInformation)`

Update a object of Type T

 * **Parameters:**
   * `key:` — Schema key
   * `clazz:` — KClass<T> extending Observable
   * `obj:` — Instance of Type clazz
   * `prop:` — Property of obj which was changed
   * `levelInformation:` — Information about the change possibly made in child Objects of obj

#### `fun <T : Observable, K> delete(key: String, clazz: KClass<T>, pk: K)`

Delete all objects of Type T with a certain primary key

 * **Parameters:**
   * `key:` — Schema key
   * `clazz:` — KClass<T> extending Observable
   * `pk:` — Primary Key of type K

#### `fun <T : Observable> insert(key: String, clazz: KClass<T>, obj: T)`

Inserts an object into a schema

 * **Parameters:**
   * `key:` — Schema key
   * `clazz:` — KClass of the object
   * `obj:` — The object, extending Observable

#### `fun <T : Observable, V: Any> loadTransformed(key: String, clazz: KClass<T>, steps: List<Step<T, *>>, to: KClass<V>) : Set<V> }  abstract class DBBackend : DBAwareObject(), Backend`

Loads Records with some transformation going on before that. f.e. "SELECT COUNT(*) FROM key or "SELECT LOWER(prop) FROM key Records should be parsed into a Object of type T which is guaranteed to be parseable form the result

 * **Parameters:**
   * `key:` — Schema key
   * `clazz:` — KClass extending Observable representing the Observable instance which was used as base-point for the transformations. Should only be used if some steps cannot be done of the Backend-side
   * `steps:` — All Steps to be applied in ascending order
   * `to:` — Desired result KClass
 * **Returns:** Mutable or Immutable Set<V> with all loaded Data. This data is considered complete and accurate by ImposterDB
