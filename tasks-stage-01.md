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
