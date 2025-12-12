package agents

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import kotlinx.coroutines.runBlocking
import services.MockDocStore
import services.MockNlpService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

// Simulation of Koog DSL
object Koog {
    fun agent(name: String, block: suspend (String) -> String): Agent = Agent(name, block)
}

class Agent(val name: String, val handler: suspend (String) -> String) {
    suspend fun chat(message: String): String = handler(message)
}

class RobCommand : CliktCommand() {
    val question by argument(help = "The question to ask")

    override fun run() = runBlocking {
        val rob = Koog.agent("Rob") { question ->
            echo("--- NLP Analyzing ---")
            val nlp = MockNlpService()
            val searchType = nlp.analyze(question)
            echo("Suggested Search: $searchType")

            echo("--- Searching DocStore ---")
            val docstore = MockDocStore()
            val docs = when (searchType) {
                "keyword" -> docstore.findByKeyword(question)
                "similarity" -> docstore.findBySimilarity(question)
                "relations" -> docstore.findByRelations(question)
                else -> emptyList()
            }
            echo("Found ${docs.size} documents.")

            echo("--- Generating Answer ---")
            val apiKey = System.getenv("AZURE_OPENAI_API_KEY")
            if (apiKey.isNullOrBlank()) {
                echo("No Azure OpenAI key found, using Mock LLM.")
                val docsText = docs.joinToString("\n")
                "I found these documents relevant:\n$docsText\n\nBased on them, the answer is: [Simulated Answer for '$question']"
            } else {
                callAzureOpenAI(question, docs, apiKey)
            }
        }

        val answer = rob.chat(question)
        echo("\n--- Answer ---")
        echo(answer)
    }

    suspend fun callAzureOpenAI(question: String, docs: List<String>, apiKey: String): String {
        val endpoint = System.getenv("AZURE_OPENAI_ENDPOINT")
        val deployment = System.getenv("AZURE_OPENAI_DEPLOYMENT_NAME")
        val version = System.getenv("AZURE_OPENAI_API_VERSION") ?: "2023-05-15"

        val url = "$endpoint/openai/deployments/$deployment/chat/completions?api-version=$version"

        val client = HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val docsText = docs.joinToString("\n")
        val prompt = """
            You are a helpful assistant. Use ONLY the following documents to answer the user's question.

            First, analyze the relevance of the given documents and provide a feedback starting with "Feedback: ".
            Then, compose the final answer starting with "Answer: ".
            If the answer is not in the documents, state that in the feedback and answer "I don't know".

            Documents:
            $docsText

            Question: $question
        """.trimIndent()

        try {
            val response = client.post(url) {
                header("api-key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("messages", buildJsonArray {
                        add(buildJsonObject {
                            put("role", "user")
                            put("content", prompt)
                        })
                    })
                })
            }
            val body = response.bodyAsText()
            val json = Json.parseToJsonElement(body)
            return json.jsonObject["choices"]?.jsonArray?.get(0)?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content ?: "Error parsing response: $body"
        } catch (e: Exception) {
            return "Error calling LLM: ${e.message}"
        } finally {
            client.close()
        }
    }
}

fun main(args: Array<String>) = RobCommand().main(args)
