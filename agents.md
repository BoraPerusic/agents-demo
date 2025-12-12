## Frontend

### Do
- use Typescript for logic, use plain JS for embedded scripts
- wherever possible, use Kotlin/JS for frontend
- use Vue for frontend
- keep components small
- keep diffs small and focused
- always split into CSS and HTML files, never inline CSS or HTML into code
- unless trivial, separate JS script from HTML template

### Don't
- don't hardcode colors
- don't use `<div>` if a component already exists
- don't bring in new heavy dependencies without approval

## Backend

### Tech Stack
- Kotlin
- Ktor
- kotlinx.serialization or Ktor JSON serialization
- SQLite for dev databases; PostgreSQL for production
- gRPC for internal communication
- JetBrains Exposed for SQL DSL; use the DSL, not the ORM
- Kotest for testing, StringSpec variant
- Wiremock for mocking external services in integration tests
- ANTLR4 for parsing + grammars
- Gradle for build automation
- slf4j for logging
- OpenTelemetry for tracing, Prometheus for metrics
- HOCON (Human-Optimized Config Object Notation) (com.typesafe.config) for configuration (see `application.conf`)
- Clikt for command line parsing and CLI applications
- Kover for code coverage
- Flyway for database migrations

### Do
- use Kotlin wherever possible for all backend logic
- use Kotlin coroutines (suspend fun) for async logic
- create class-level loggers and log often with DEBUG level
- when asked to serialize / deserialize data with multi-type fields, use sealed Interface and internal classes as specified below in the "Serialization Example" section 
- use builders with fluent logic wherever appropriate
- use "companion object" factories for constructors and DSLs
- strictly prefer smaller classes and short methods
- detach logic from presentation / communication (e.g. always separate "handlers" from "routes")
- create both unit and component tests; for multiple components always test one component at a time, mocking (with Wiremock) the other ones
- for larger features use TDD: first design the tests, get them approved, then implement the feature
- use Iterable and iterators when possible, try to avoid specific implementations unless necessary
- use mutable collections when needed; try to avoid copying immutable ones
- comment the code extensively

### Don't
- don't use Spring unless specifically instructed
- don't install dependencies without approval, apart from the pre-approved ones in the "Tech Stack" section
- don't create large files; multiple classes in a single file are fine, but keep the files below 300 lines, unless necessary

## General Instructions

### Planning 
- unless explicitly asked to implement immediately, always prepare a detailed working plan in advance
- Always read through the instructions, either in the prompt or in a specific requirements file.
- If the requirements are written in Stages, always work only on one Stage at a time.
- Always prepare a detailed task list for the given Stage in a file `tasks-stage-xx.md` in the project root directory so that I can review the plan. Use checkboxes so that we can follow up the progress later on.
- While planning, do NOT implement anything yet, focus on analysis and planning. You can use a specific section Open Questiions to ask questions I need to answer before the implementation starts. Please, ask also to confirm any assumptions and defaults you have made in the planning. You will help me a lot with explicitly asking questions, as my requirements might be unclear or incomplete.
- While planning, do NOT touch any code or files. Do NOT refactor any files or pieces of code that seem unused at this time, we will need them later on. Prepare the task list and get back to me for review before changing anything.

### Implementation
- do ONLY what asked for
- don't refactor code outside what you have been asked for
- don't delete any "unused" code without approval
- NEVER merge anything
- if a detailed task list is available, mark the checkboxes as you go

### Safety and permissions
Allowed: read/list files, lint/test single files, git push to a new branch, PR creation
Ask first: installs, deletes, full builds

### PR checklist
- format and type check pass
- unit tests green
- diff small with a short summary

### When stuck or working for a long time
- take a break and let me review the progress
- ask a question
- break, summarize the progress and propose a plan going forward
- store the progress in the project root directory as `progress-stage-xx.md` and the current plan in `fwd-stage-xx.md`




## Examples

### Serialization Example
This is the example of a multi-type field in a data class.
Please, note that the required behavior is to accept the primitives or arrays WITHOUT specifying the "value" or "values" field.
Preferred implementation would be to use the inner classes in the interface, like this:
```kotlin
@Serializable
sealed interface MetadataValue {
    @Serializable
    data class MetadataSingle(val value: String) : MetadataValue
    @Serializable
    data class MetadataList(val values: List<String>) : MetadataValue
}
```

This is the example interface, classes and custom serializer:
```kotlin
@Serializable
sealed interface MetadataValue

@Serializable
data class MetadataSingle(val value: String) : MetadataValue

@Serializable
data class MetadataList(val values: List<String>) : MetadataValue

/**
 * Parse a JSON string representing metadata into a Map<String, MetadataValue>.
 * Values can be a string or an array of strings. Other types will be stringified.
 */
fun parseMetadataJson(jsonText: String): Map<String, MetadataValue> {
    val root = Json.parseToJsonElement(jsonText)
    if (root !is kotlinx.serialization.json.JsonObject) return emptyMap()
    val out = mutableMapOf<String, MetadataValue>()
    for ((k, v) in root) {
        out[k] = when (v) {
            is JsonPrimitive -> MetadataSingle(v.content)
            is JsonArray -> MetadataList(v.mapNotNull { (it as? JsonPrimitive)?.content })
            else -> MetadataSingle(v.toString())
        }
    }
    return out
}

/**
 * Custom serializer for a single MetadataValue that supports the nested forms:
 * {"value":"A"} and {"values":["B","C"]}.
 * Also leniently accepts primitives and arrays.
 */
object MetadataValueSerializer : KSerializer<MetadataValue> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MetadataValue")

    override fun deserialize(decoder: Decoder): MetadataValue {
        val jd = decoder as? JsonDecoder ?: error("MetadataValueSerializer requires Json")
        val elem = jd.decodeJsonElement()
        return when (elem) {
            is JsonObject -> {
                val v = elem["value"]
                val vs = elem["values"]
                when {
                    v is JsonPrimitive -> MetadataSingle(v.content)
                    vs is JsonArray -> MetadataList(vs.mapNotNull { (it as? JsonPrimitive)?.content })
                    // Fallbacks
                    elem.size == 1 && elem.values.firstOrNull() is JsonPrimitive ->
                        MetadataSingle((elem.values.first() as JsonPrimitive).content)
                    elem.size == 1 && elem.values.firstOrNull() is JsonArray ->
                        MetadataList(((elem.values.first() as JsonArray).mapNotNull { (it as? JsonPrimitive)?.content }))
                    else -> MetadataSingle(elem.toString())
                }
            }
            is JsonPrimitive -> MetadataSingle(elem.content)
            is JsonArray -> MetadataList(elem.mapNotNull { (it as? JsonPrimitive)?.content })
            else -> MetadataSingle(elem.toString())
        }
    }

    override fun serialize(encoder: Encoder, value: MetadataValue) {
        val je = encoder as? JsonEncoder ?: error("MetadataValueSerializer requires Json")
        val obj = when (value) {
            is MetadataSingle -> buildJsonObject { put("value", JsonPrimitive(value.value)) }
            is MetadataList -> buildJsonObject {
                put("values", JsonArray(value.values.map { JsonPrimitive(it) }))
            }
        }
        je.encodeJsonElement(obj)
    }
}
```
