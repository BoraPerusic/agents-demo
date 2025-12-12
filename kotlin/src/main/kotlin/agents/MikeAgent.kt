package agents

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*

class MikeCommand : CliktCommand() {
    val question by argument(help = "The question")

    override fun run() = runBlocking {
        echo("Mike (MCP Agent) starting...")
        val server = mcp.DocStoreMcpServer()

        // 1. Initialize MCP
        server.handle(mcp.JsonRpcRequest("2.0", "initialize", id = JsonPrimitive(1)))

        // 2. Get Tools
        val listResp = server.handle(mcp.JsonRpcRequest("2.0", "tools/list", id = JsonPrimitive(2)))
        val mcpTools = listResp?.result?.jsonObject?.get("tools")?.jsonArray ?: buildJsonArray {}

        val apiKey = System.getenv("AZURE_OPENAI_API_KEY")
        if (apiKey.isNullOrBlank()) {
            echo("No Azure OpenAI Key. Using Mock behavior for demonstration.")
            echo("--- Agent Thinking ---")
            echo("LLM decided to call: findByKeyword with arg '$question'")

            val toolToCall = "findByKeyword"
            val callReq = mcp.JsonRpcRequest("2.0", "tools/call", buildJsonObject {
                put("name", toolToCall)
                put("arguments", buildJsonObject {
                    put("text", question)
                })
            }, id = JsonPrimitive(3))

            val callResp = server.handle(callReq)
            val content = callResp?.result?.jsonObject?.get("content")?.jsonArray?.get(0)?.jsonObject?.get("text")?.jsonPrimitive?.content

            echo("--- Tool Output ---")
            echo(content)
            echo("--- Final Answer ---")
            echo("(Mock LLM Final Answer using above content)")
            return@runBlocking
        }

        // 3. Convert to OpenAI Tools
        val openAiTools = buildJsonArray {
            mcpTools.forEach { tool ->
                add(buildJsonObject {
                    put("type", "function")
                    put("function", buildJsonObject {
                        put("name", tool.jsonObject["name"]!!)
                        put("description", tool.jsonObject["description"]!!)
                        put("parameters", tool.jsonObject["inputSchema"]!!)
                    })
                })
            }
        }

        // 4. Agent Loop
        val messages = mutableListOf<JsonObject>(
            buildJsonObject { put("role", "user"); put("content", question) }
        )

        val client = HttpClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        try {
            var turn = 0
            while (turn < 5) {
                echo("--- Turn $turn ---")
                val responseJson = callLLM(client, apiKey, messages, openAiTools)

                // Check error
                if (responseJson.containsKey("error")) {
                    echo("Error from LLM: ${responseJson["error"]}")
                    break
                }

                val choice = responseJson["choices"]?.jsonArray?.get(0)?.jsonObject
                val message = choice?.get("message")?.jsonObject ?: break
                val finishReason = choice["finish_reason"]?.jsonPrimitive?.content

                messages.add(message)

                if (finishReason == "tool_calls") {
                    val toolCalls = message["tool_calls"]?.jsonArray
                    echo("LLM wants to call tools: ${toolCalls?.size}")

                    toolCalls?.forEach { tc ->
                        val func = tc.jsonObject["function"]!!.jsonObject
                        val name = func["name"]!!.jsonPrimitive.content
                        val argsStr = func["arguments"]!!.jsonPrimitive.content
                        val id = tc.jsonObject["id"]!!.jsonPrimitive.content

                        echo("Calling tool: $name with $argsStr")

                        // Parse args
                        val argsJson = try {
                            Json.parseToJsonElement(argsStr).jsonObject
                        } catch(e: Exception) {
                            buildJsonObject {}
                        }

                        // Call MCP
                        val callReq = mcp.JsonRpcRequest("2.0", "tools/call", buildJsonObject {
                            put("name", name)
                            put("arguments", argsJson)
                        }, id = JsonPrimitive(turn * 100 + 3))

                        val callResp = server.handle(callReq)
                        val content = callResp?.result?.jsonObject?.get("content")?.jsonArray?.get(0)?.jsonObject?.get("text")?.jsonPrimitive?.content ?: "Error"

                        echo("Tool Output: $content")

                        messages.add(buildJsonObject {
                            put("role", "tool")
                            put("tool_call_id", id)
                            put("content", content)
                        })
                    }
                } else {
                    echo("--- Final Answer ---")
                    echo(message["content"]?.jsonPrimitive?.content)
                    break
                }
                turn++
            }
        } catch (e: Exception) {
            echo("Exception: ${e.message}")
            e.printStackTrace()
        } finally {
            client.close()
        }
    }

    suspend fun callLLM(client: HttpClient, apiKey: String, messages: List<JsonObject>, tools: JsonArray): JsonObject {
        val endpoint = System.getenv("AZURE_OPENAI_ENDPOINT")
        val deployment = System.getenv("AZURE_OPENAI_DEPLOYMENT_NAME")
        val version = System.getenv("AZURE_OPENAI_API_VERSION") ?: "2023-05-15"
        val url = "$endpoint/openai/deployments/$deployment/chat/completions?api-version=$version"

        val resp = client.post(url) {
            header("api-key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("messages", JsonArray(messages))
                put("tools", tools)
                put("tool_choice", "auto")
            })
        }
        return Json.parseToJsonElement(resp.bodyAsText()).jsonObject
    }
}

fun main(args: Array<String>) = MikeCommand().main(args)
