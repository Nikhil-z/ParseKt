# ParseKt [EXPERIMENTAL]

![Android CI](https://github.com/L3K0V/ParseKt/workflows/Android%20CI/badge.svg?branch=master)

For more information about the ParseKt and its features, see the public [documentation](https://l3k0v.github.io/ParseKt/parse/)

## Getting started

Initialize Parse using:

```kotlin
Parse.initialize(context,"appId", "clientKey", "masterKey", "http://127.0.0.1:1337/parse")
```

### Objects

Extend `ParseClass` and annotate with `@Serializable` and `@ParseClassName`. Attach `ParseClassCompation`.

```kotlin
@Serializable
@ParseClassName("GameScore")
class GameScore(var score: Int, 
                var cheatMode: Boolean? = false, 
                var playerName: String? = null) : ParseClass() {
  companion object : ParseClassCompanion()
}
```

No further setup is required.

### CRUD

Save using;

```kotlin
GameScore(100, false, "Test Player").save()
```

Fetch using:

```kotlin
GameScore(100, false, "Test Player").fetch()
```

### Queries

> You can call `query` builder on `ParseObject` or on all `ParseClasses` which have `ParseClassCompanion` object

Create simple queries using:

```kotlin
GameScore.query { greaterThanOrEqualTo(GameScore::score.name, 50) }
```

If you want to get by id:

```kotlin
ParseObject.query{}.get<GameScore>("objectId")
```

You can make compound queries by:

```kotlin
GameScore.query { lessThanOrEqualTo(GameScore::score.name, 50) }
  .or(GameScore.query { greaterThanOrEqualTo("score", 100) })
```

`and` queries:

```kotlin
GameScore.query { lessThanOrEqualTo(GameScore::score.name, 50) }
  .and(GameScore.query { greaterThanOrEqualTo("score", 100) })
```
Then get the result by one of the following:

```kotlin
val query = ParseObject.query { lessThanOrEqualTo(GameScore::score.name, 50) }

val scores = query.find<GameScore>() // Find all matching
val score = query.first<GameScore>() // Find first matching
val count = query.count<GameScore>() // Count matching
```

### Live Queries

`subscribe` method on ParseQuery returns a Flow which can te observed. For example by collecting, new list matching the given query will be returned when there are changes.

```kotlin
query.subscribe<GameScore>().collect { 
    Log.i("ParseLiveQuery", it.toTypedArray().contentToString())
}
```

You can use map, reduce, filter and all other methods from the Flow over the list of matching the query items.

### User

You can extend the default user to provide your own fields:

```kotlin
@Serializable
@ParseClassName("_User")
class AppParseUser(var phone: String? = null, var address: String? = null) : ParseUser() {
    companion object : ParseClassCompanion()
}
```

Then you can create user, assign all the data and sign up. New logins will save the user's data.

```kotlin
val user = AppParseUser().signup("john", "pass")
```

To login:

```kotlin
val user = AppParseUser().login("john", "pass")
```

The user is also an `ParseObject` so you can use `save` and `fetch` on it. And by attaching it `ParseClassCompanion` 
you can query over it like this:

```kotlin
val user = AppParseUser.query { equalsTo("username", "john")}.first()
```
