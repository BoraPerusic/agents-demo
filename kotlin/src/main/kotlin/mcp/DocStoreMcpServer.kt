package mcp

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import services.MockDocStore
import java.util.Scanner

@Serializable
data class JsonRpcRequest(val jsonrpc: String, val method: String, val params: JsonElement? = null, val id: JsonElement? = null)

@Serializable
data class JsonRpcResponse(val jsonrpc: String, val result: JsonElement? = null, val error: JsonElement? = null, val id: JsonElement? = null)

class DocStoreMcpServer {
    val docstore = MockDocStore()

    suspend fun run() {
        val scanner = Scanner(System.`in`)
        while (scanner.hasNextLine()) {
            val line = scanner.nextLine()
            if (line.isBlank()) continue
            try {
                // MCP messages are JSON-RPC 2.0
                val request = Json.decodeFromString<JsonRpcRequest>(line)
                val response = handle(request)
                if (response != null) {
                    println(Json.encodeToString(JsonRpcResponse.serializer(), response))
                }
            } catch (e: Exception) {
                // Ignore errors or log to stderr
                 System.err.println("Error: ${e.message}")
            }
        }
    }

    suspend fun handle(req: JsonRpcRequest): JsonRpcResponse? {
        return when (req.method) {
            "initialize" -> JsonRpcResponse("2.0", buildJsonObject {
                put("protocolVersion", "2024-11-05")
                put("serverInfo", buildJsonObject { put("name", "KotlinDocStore"); put("version", "1.0") })
                put("capabilities", buildJsonObject { put("tools", buildJsonObject {}) })
            }, null, req.id)
            "notifications/initialized" -> null
            "tools/list" -> JsonRpcResponse("2.0", buildJsonObject {
                put("tools", buildJsonArray {
                    add(tool("findByKeyword", "Find by keyword"))
                    add(tool("findBySimilarity", "Find by similarity"))
                    add(tool("findByRelations", "Find by relations"))
                })
            }, null, req.id)
            "tools/call" -> {
                val params = req.params?.jsonObject ?: return error(req.id, -32602, "Invalid params")
                val name = params["name"]?.jsonPrimitive?.content ?: return error(req.id, -32602, "Missing name")
                val args = params["arguments"]?.jsonObject
                val text = args?.get("text")?.jsonPrimitive?.content ?: ""

                val result = when(name) {
                    "findByKeyword" -> docstore.findByKeyword(text)
                    "findBySimilarity" -> docstore.findBySimilarity(text)
                    "findByRelations" -> docstore.findByRelations(text)
                    else -> return error(req.id, -32601, "Method not found")
                }

                JsonRpcResponse("2.0", buildJsonObject {
                    put("content", buildJsonArray {
                        add(buildJsonObject {
                            put("type", "text")
                            put("text", result.joinToString("\n"))
                        })
                    })
                }, null, req.id)
            }
            "ping" -> JsonRpcResponse("2.0", buildJsonObject {}, null, req.id)
            else -> error(req.id, -32601, "Method not found: ${req.method}")
        }
    }

    fun tool(name: String, desc: String) = buildJsonObject {
        put("name", name)
        put("description", desc)
        put("inputSchema", buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("text", buildJsonObject { put("type", "string") })
            })
            put("required", buildJsonArray { add("text") })
        })
    }

    fun error(id: JsonElement?, code: Int, msg: String) = JsonRpcResponse("2.0", null, buildJsonObject {
        put("code", code)
        put("message", msg)
    }, id)
}

fun main() = runBlocking {
    DocStoreMcpServer().run()
}
