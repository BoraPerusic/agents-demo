# Stage 1: Planning and Setup

## Open Questions and Decisions
1. **DocStore & NLP Service Access**: 
   - I assume the "DocStore" and "NLP" services are external REST APIs. 
   - I will define interfaces for them and create mocks/stubs for development and testing.
   - *Question*: Do you have specific API specs (Swagger/OpenAPI) for these, or should I define a logical interface based on the function descriptions (`findByKeyword`, etc.)?
[Answer: yes, these are external REST APIs. Just create and logical interface and moc/stub; I will wire evertyhing together later on]

2. **MCP Server Implementation**: 
   - The requirement "wrapped in a MCP server" applies to Agent Mike. 
   - *Decision*: I will implement the MCP server "wrapper" in **both** Python and Kotlin to fully demonstrate the stack capabilities in each language.
   - *Question*: Is this preferred, or would you like a single shared MCP server (e.g., in Python) used by both agents?
[Answer: Yes, please, provide MCP server wrappers in **both** Python and Kotlin. For Kotlin, please, use https://github.com/modelcontextprotocol/kotlin-sdk]

3. **Koog Framework Availability**:
   - I will proceed assuming `ai.koog` artifacts are available in the configured repositories.
   - *Question*: Does Koog have built-in MCP (Model Context Protocol) support? If not, I will implement a basic MCP client using Ktor/Sockets.
[Answer: Koog has integration with MCP, I have added the the description and sample at the bottom of the file]

4. **User Interface**:
   - I plan to implement a simple **CLI (Command Line Interface)** for the "chat interface" for both languages.
   - *Question*: Is a CLI sufficient, or do you require a REST API / Web UI for the agents?
[Answer: Yes, CLI is good enough]

5. **Azure OpenAI**:
   - I will configure the agents to use Azure OpenAI via environment variables (`AZURE_OPENAI_API_KEY`, `ENDPOINT`, `DEPLOYMENT_NAME`).
[Answer: Yes, please]

## Task List

### 1. Project Initialization
- [ ] **Folder Structure**
    - Create `python/` and `kotlin/` roots.
    - Add `README.md` with overview.
    - Create `python/.env.example` and `kotlin/.env.example`.

### 2. Python Implementation (LangGraph)
- [ ] **Environment & Dependencies**
    - Setup `python/pyproject.toml` (or `requirements.txt`).
    - Deps: `langgraph`, `langchain-azure-openai`, `mcp`, `typer` (for CLI).
- [ ] **Shared Modules**
    - `services/docstore.py`: Interface and Mock implementation of DocStore.
    - `services/nlp.py`: Interface and Mock implementation of NLP Service (for Rob).
- [ ] **Agent Rob (RAG)**
    - `rob_agent.py`:
        - Define state graph (Input -> NLP -> Search -> Generate -> Output).
        - Implement nodes for each step.
        - Integration with Azure OpenAI.
- [ ] **Agent Mike (MCP)**
    - `mcp_server.py`:
        - Implement an MCP Server using the `mcp` SDK.
        - Expose `findByKeyword`, `findBySimilarity`, `findByRelations` as tools.
    - `mike_agent.py`:
        - Implement an agent that connects to `mcp_server`.
        - Use LangGraph/LangChain to bind tools to the LLM.
- [ ] **Testing**
    - Unit tests with `pytest`.

### 3. Kotlin Implementation (Koog)
- [ ] **Environment & Dependencies**
    - Setup `kotlin/build.gradle.kts` (Gradle Kotlin DSL).
    - Deps: `koog-core`, `ktor-client`, `ktor-server` (for MCP server), `clikt`, `kotest`, `wiremock`.
- [ ] **Shared Modules**
    - `src/main/kotlin/services/DocStore.kt`: Interface and Ktor Client implementation.
    - `src/main/kotlin/services/NlpService.kt`: Interface and Ktor Client implementation.
- [ ] **Agent Rob (RAG)**
    - `src/main/kotlin/agents/RobAgent.kt`:
        - Use Koog DSL to define the agent.
        - Implement the RAG flow (NLP -> Search -> Context -> LLM).
- [ ] **Agent Mike (MCP)**
    - `src/main/kotlin/mcp/DocStoreMcpServer.kt`:
        - Implement a basic MCP Server (JSON-RPC over StdIO or HTTP) if not provided by Koog.
        - Wrap DocStore calls.
    - `src/main/kotlin/agents/MikeAgent.kt`:
        - Configure Koog agent to use the MCP tools.
- [ ] **Testing**
    - Unit tests with Kotest.
    - Integration tests using Wiremock.

### 4. Review & Deliver
- [ ] Verify both agents in both languages against the requirements.
- [ ] Ensure `agents.md` guidelines are met (formatting, logging, config).


Koog and MCP Description and Samples:
```
Model Context Protocol (MCP) is a standardized protocol that lets AI agents interact with external tools and services through a consistent interface.

MCP exposes tools and prompts as API endpoints that AI agents can call. Each tool has a specific name and an input schema that describes its inputs and outputs using the JSON Schema format.

The Koog framework provides integration with MCP servers, enabling you to incorporate MCP tools into your Koog agents.

To learn more about the protocol, see the Model Context Protocol documentation.

MCP servers
MCP servers implement Model Context Protocol and provide a standardized way for AI agents to interact with tools and services.

You can find ready-to-use MCP servers in the MCP Marketplace or MCP DockerHub.

The MCP servers support the following transport protocols to communicate with agents:

Standard input/output (stdio) transport protocol used to communicate with the MCP servers running as separate processes. For example, a Docker container or a CLI tool.
Server-sent events (SSE) transport protocol (optional) used to communicate with the MCP servers over HTTP.
Integration with Koog
The Koog framework integrates with MCP using the MCP SDK with the additional API extensions presented in the agent-mcp module.

This integration lets the Koog agents perform the following:

Connect to MCP servers through various transport mechanisms (stdio, SSE).
Retrieve available tools from an MCP server.
Transform MCP tools into the Koog tool interface.
Register the transformed tools in a tool registry.
Call MCP tools with arguments provided by the LLM.
Key components
Here are the main components of the MCP integration in Koog:

Component	Description
McpTool	Serves as a bridge between the Koog tool interface and the MCP SDK.
McpToolDescriptorParser	Parses MCP tool definitions into the Koog tool descriptor format.
McpToolRegistryProvider	Creates MCP tool registries that connect to MCP servers through various transport mechanisms (stdio, SSE).
Getting started
1. Set up an MCP connection
To use MCP with Koog, you need to set up a connection:

Start an MCP server (either as a process, Docker container, or web service).
Create a transport mechanism to communicate with the server.
MCP servers support the stdio and SSE transport mechanisms to communicate with the agent, so you can connect using one of them.

Connect with stdio
This protocol is used when an MCP server runs as a separate process. Here is an example of setting up an MCP connection using the stdio transport:


// Start an MCP server (for example, as a process)
val process = ProcessBuilder("path/to/mcp/server").start()

// Create the stdio transport 
val transport = McpToolRegistryProvider.defaultStdioTransport(process)
Connect with SSE
This protocol is used when an MCP server runs as a web service. Here is an example of setting up an MCP connection using the SSE transport:


// Create the SSE transport
val transport = McpToolRegistryProvider.defaultSseTransport("http://localhost:8931")
2. Create a tool registry
Once you have the MCP connection, you can create a tool registry with tools from the MCP server in one of the following ways:

Using the provided transport mechanism for communication. For example:

// Create a tool registry with tools from the MCP server
val toolRegistry = McpToolRegistryProvider.fromTransport(
    transport = transport,
    name = "my-client",
    version = "1.0.0"
)
Using an MCP client connected to the MCP server. For example:

// Create a tool registry from an existing MCP client
val toolRegistry = McpToolRegistryProvider.fromClient(
    mcpClient = existingMcpClient
)
3. Integrate with your agent
To use MCP tools with your Koog agent, you need to register the tool registry with the agent:


// Create an agent with the tools
val agent = AIAgent(
    promptExecutor = executor,
    strategy = strategy,
    llmModel = OpenAIModels.Chat.GPT4o,
    toolRegistry = toolRegistry
)

// Run the agent with a task that uses an MCP tool
val result = agent.run("Use the MCP tool to perform a task")
Usage examples
Google Maps MCP integration
This example demonstrates how to connect to a Google Maps server for geographic data using MCP:


// Start the Docker container with the Google Maps MCP server
val process = ProcessBuilder(
    "docker", "run", "-i",
    "-e", "GOOGLE_MAPS_API_KEY=$googleMapsApiKey",
    "mcp/google-maps"
).start()

// Create the ToolRegistry with tools from the MCP server
val toolRegistry = McpToolRegistryProvider.fromTransport(
    transport = McpToolRegistryProvider.defaultStdioTransport(process)
)

// Create and run the agent
val agent = AIAgent(
    promptExecutor = simpleOpenAIExecutor(openAIApiToken),
    llmModel = OpenAIModels.Chat.GPT4o,
    toolRegistry = toolRegistry,
)
agent.run("Get elevation of the Jetbrains Office in Munich, Germany?")
Playwright MCP integration
This example demonstrates how to connect to a Playwright server for web automation using MCP:


// Start the Playwright MCP server
val process = ProcessBuilder(
    "npx", "@playwright/mcp@latest", "--port", "8931"
).start()

// Create the ToolRegistry with tools from the MCP server
val toolRegistry = McpToolRegistryProvider.fromTransport(
    transport = McpToolRegistryProvider.defaultSseTransport("http://localhost:8931")
)

// Create and run the agent
val agent = AIAgent(
    promptExecutor = simpleOpenAIExecutor(openAIApiToken),
    llmModel = OpenAIModels.Chat.GPT4o,
    toolRegistry = toolRegistry,
)
agent.run("Open a browser, navigate to jetbrains.com, accept all cookies, click AI 
```
