# Open Spec: Modern Kotlin Code Style

## Status
`complete` - Archived on 2025-12-19

## Overview
This specification defines the modern Kotlin code style guidelines for the JSONSchema-to-Mermaid project. All Kotlin code (both existing and new) must follow these guidelines to ensure idiomatic, functional, and maintainable code.

## Goals
1. Embrace Kotlin's functional programming capabilities
2. Use modern Kotlin 2.x idioms and language features
3. Eliminate Java-style imperative patterns in favor of declarative, functional code
4. Ensure consistency across the entire codebase
5. Make code more readable, concise, and maintainable

## Modern Kotlin Style Guidelines

### 1. Prefer Functional Transformations Over Imperative Loops

**Don't do this (Java-style):**
```kotlin
val result = mutableListOf<String>()
for (item in items) {
    if (item.isValid) {
        result.add(item.name)
    }
}
```

**Do this (Kotlin-style):**
```kotlin
val result = items
    .filter { it.isValid }
    .map { it.name }
```

### 2. Use Expression Bodies for Short Functions

**Don't do this:**
```kotlin
fun getName(): String {
    return name
}
```

**Do this:**
```kotlin
fun getName(): String = name
```

### 3. Use `when` as an Expression

**Don't do this:**
```kotlin
fun getType(value: Int): String {
    if (value == 1) {
        return "one"
    } else if (value == 2) {
        return "two"
    } else {
        return "other"
    }
}
```

**Do this:**
```kotlin
fun getType(value: Int): String = when (value) {
    1 -> "one"
    2 -> "two"
    else -> "other"
}
```

### 4. Use Scope Functions Appropriately

Use `let`, `run`, `apply`, `also`, and `with` for cleaner null handling and object initialization:

```kotlin
// Null-safe transformation
val length = name?.let { it.length } ?: 0

// Object initialization
val person = Person().apply {
    name = "John"
    age = 30
}

// Running code with context
val result = config.run {
    "$host:$port/$path"
}
```

### 5. Use `takeIf` and `takeUnless` for Conditional Returns

**Don't do this:**
```kotlin
fun findValidItem(item: Item): Item? {
    return if (item.isValid) item else null
}
```

**Do this:**
```kotlin
fun findValidItem(item: Item): Item? = item.takeIf { it.isValid }
```

### 6. Use Destructuring Declarations

```kotlin
// For maps
map.forEach { (key, value) -> println("$key: $value") }

// For data classes
val (name, age) = person
```

### 7. Use `buildList`, `buildMap`, `buildSet` for Collection Construction

**Don't do this:**
```kotlin
val list = mutableListOf<String>()
list.add("first")
if (condition) {
    list.add("second")
}
```

**Do this:**
```kotlin
val list = buildList {
    add("first")
    if (condition) {
        add("second")
    }
}
```

### 8. Use `buildString` for String Construction

**Don't do this:**
```kotlin
val sb = StringBuilder()
sb.append("Header\n")
items.forEach { sb.append("  $it\n") }
val result = sb.toString()
```

**Do this:**
```kotlin
val result = buildString {
    appendLine("Header")
    items.forEach { appendLine("  $it") }
}
```

### 9. Prefer Immutability

- Use `val` instead of `var` wherever possible
- Use immutable collections (`List`, `Set`, `Map`) instead of mutable ones
- Only use mutable state when absolutely necessary and encapsulate it

### 10. Use Extension Functions for Utility Operations

```kotlin
// Define extensions for common operations
fun String.sanitize(): String = this
    .split(Regex("[^A-Za-z0-9]+"))
    .filter { it.isNotBlank() }
    .joinToString("") { it.replaceFirstChar { c -> c.uppercaseChar() } }
```

### 11. Use Sealed Classes and Exhaustive `when`

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}

fun handle(result: Result<String>) = when (result) {
    is Result.Success -> println(result.data)
    is Result.Error -> println(result.message)
}
```

### 12. Use Sequence for Large Data Processing

```kotlin
// For large collections with multiple transformations
items.asSequence()
    .filter { it.isValid }
    .map { it.transform() }
    .take(10)
    .toList()
```

### 13. Use Named Arguments for Clarity

```kotlin
// Unclear
formatField("name", "String", true, false)

// Clear
formatField(
    propertyName = "name",
    typeName = "String",
    isRequired = true,
    isArray = false
)
```

### 14. Use Default Parameters Instead of Overloads

```kotlin
// Don't create multiple overloads
fun format(value: String, prefix: String = "", suffix: String = ""): String =
    "$prefix$value$suffix"
```

### 15. Prefer `getOrElse`, `getOrPut`, `firstOrNull` etc.

```kotlin
// Don't do manual null checks
val value = map.getOrElse(key) { defaultValue }
val first = list.firstOrNull() ?: default
```

### 16. Use `require`, `check`, `error` for Preconditions

```kotlin
fun process(value: Int) {
    require(value >= 0) { "Value must be non-negative: $value" }
    // ...
}
```

### 17. Use `?.let { }` for Null-Safe Chains

```kotlin
// Chain operations on nullable values
user?.address?.city?.let { cityName ->
    println("City: $cityName")
}
```

### 18. Prefer `ifEmpty`, `ifBlank` for Default Values

```kotlin
val name = input.ifBlank { "Anonymous" }
val items = list.ifEmpty { listOf("default") }
```

### 19. Use `fold`/`reduce` for Accumulation

```kotlin
val sum = items.fold(0) { acc, item -> acc + item.value }
```

### 20. Use `associate`, `associateWith`, `associateBy` for Map Creation

```kotlin
val nameToAge = people.associate { it.name to it.age }
val byId = items.associateBy { it.id }
```

## Kotlin 2.x Specific Features

### Context Receivers (if using Kotlin 2.0+)
Use context receivers for cleaner dependency injection in DSL-style code.

### Data Objects
Use `data object` for singleton value classes.

### Value Classes
Use `@JvmInline value class` for type-safe wrappers without runtime overhead.

## Anti-Patterns to Avoid

1. **Avoid `!!` (non-null assertion)** - Use safe calls or proper null handling
2. **Avoid mutable global state** - Use dependency injection
3. **Avoid `lateinit` in most cases** - Prefer nullable with proper initialization
4. **Avoid manual iteration** - Use functional transformations
5. **Avoid `var` when `val` is possible**
6. **Avoid explicit type declarations when inferrable** - Let Kotlin infer types
7. **Avoid Java-style getters/setters** - Use Kotlin properties
8. **Avoid `companion object` for constants** - Use top-level `const val`
9. **Avoid returning `Unit` explicitly**
10. **Avoid unnecessary `it` in single-parameter lambdas when context is clear**

## Enforcement

1. Code review must verify adherence to these guidelines
2. Prefer automated tooling (detekt) to catch common issues
3. New code must follow all guidelines
4. Existing code should be refactored when touched

## Files Refactored

All Kotlin source files have been refactored to follow modern Kotlin style. The following files were updated:

### Main Source Files (src/main/kotlin/)
- ✅ `diagram/MermaidGenerator.kt` - Expression body
- ✅ `diagram/MermaidDiagramBuilder.kt` - Expression body
- ✅ `diagram/DiagramOutputBuilder.kt` - buildString, appendLine, destructuring
- ✅ `diagram/PropertyFormatter.kt` - Expression bodies, when expressions, let/elvis
- ✅ `diagram/EnglishSingularizer.kt` - when expressions, let, extension function
- ✅ `diagram/MermaidGeneratorUtils.kt` - Expression bodies, safe calls
- ✅ `relationship/RelationshipBuilder.kt` - buildString, let, expression bodies
- ✅ `relationship/InheritanceHandler.kt` - let, expression bodies, safe calls
- ✅ `cli/App.kt` - runCatching pattern
- ✅ `cli/CliOptions.kt` - Already good (data class)
- ✅ `cli/CliService.kt` - Already good (DI, SRP)
- ✅ `cli/ConfigFileResolver.kt` - Expression bodies, let, firstOrNull
- ✅ `cli/SourceResolver.kt` - Expression bodies, when, takeIf
- ✅ `cli/PreferencesBuilder.kt` - let/elvis chain, expression bodies
- ✅ `cli/DiagnosticLogger.kt` - Expression bodies, runCatching
- ✅ `cli/OutputWriter.kt` - Already good
- ✅ `schema/ClassRegistry.kt` - getOrPut
- ✅ `schema_files/SchemaFilesReader.kt` - Expression bodies, buildList, in operator
- ✅ `schema_files/RefResolver.kt` - require, when expression bodies

### Test Files (src/test/kotlin/)
- ✅ `test_util/GoldenTestUtil.kt` - Expression bodies, check instead of error

## Implementation Notes

When refactoring:
1. Maintain existing test coverage - all tests must pass
2. Refactor incrementally - one file or one function at a time
3. Run tests after each change
4. Preserve existing behavior exactly
5. Use golden file tests to verify output hasn't changed

## Related Documents
- `openspec/project.md` - Project conventions (will be updated with these guidelines)
- `CONTRIBUTING.md` - Contribution guidelines

## Changelog
- 2025-12-19: Initial specification created and all existing code refactored to modern Kotlin style
- 2025-12-19: Specification archived after successful codebase-wide refactoring completion


