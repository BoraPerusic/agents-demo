# Stage 1: Planning and Setup

## Open Questions and Decisions
1. **DocStore & NLP Service Access**:
   - I assume the "DocStore" and "NLP" services are external REST APIs.
   - I will define interfaces for them and create mocks/stubs for development and testing.
   - *Question*: Do you have specific API specs (Swagger/OpenAPI) for these, or should I define a logical interface based on the function descriptions (`findByKeyword`, etc.)?

2. **MCP Server Implementation**:
   - The requirement "wrapped in a MCP server" applies to Agent Mike.
   - *Decision*: I will implement the MCP server "wrapper" in **both** Python and Kotlin to fully demonstrate the stack capabilities in each language.
   - *Question*: Is this preferred, or would you like a single shared MCP server (e.g., in Python) used by both agents?

3. **Koog Framework Availability**:
   - I will proceed assuming `ai.koog` artifacts are available in the configured repositories.
   - *Question*: Does Koog have built-in MCP (Model Context Protocol) support? If not, I will implement a basic MCP client using Ktor/Sockets.

4. **User Interface**:
   - I plan to implement a simple **CLI (Command Line Interface)** for the "chat interface" for both languages.
   - *Question*: Is a CLI sufficient, or do you require a REST API / Web UI for the agents?

5. **Azure OpenAI**:
   - I will configure the agents to use Azure OpenAI via environment variables (`AZURE_OPENAI_API_KEY`, `ENDPOINT`, `DEPLOYMENT_NAME`).

## Task List

### 1. Project Initialization
- [x] **Folder Structure**
    - Create `python/` and `kotlin/` roots.
    - Add `README.md` with overview.
    - Create `python/.env.example` and `kotlin/.env.example`.

### 2. Python Implementation (LangGraph)
- [x] **Environment & Dependencies**
    - Setup `python/pyproject.toml` (or `requirements.txt`).
    - Deps: `langgraph`, `langchain-azure-openai`, `mcp`, `typer` (for CLI).
- [x] **Shared Modules**
    - `services/docstore.py`: Interface and Mock implementation of DocStore.
    - `services/nlp.py`: Interface and Mock implementation of NLP Service (for Rob).
- [x] **Agent Rob (RAG)**
    - `rob_agent.py`:
        - Define state graph (Input -> NLP -> Search -> Generate -> Output).
        - Implement nodes for each step.
        - Integration with Azure OpenAI.
- [x] **Agent Mike (MCP)**
    - `mcp_server.py`:
        - Implement an MCP Server using the `mcp` SDK.
        - Expose `findByKeyword`, `findBySimilarity`, `findByRelations` as tools.
    - `mike_agent.py`:
        - Implement an agent that connects to `mcp_server`.
        - Use LangGraph/LangChain to bind tools to the LLM.
- [ ] **Testing**
    - Unit tests with `pytest`.

### 3. Kotlin Implementation (Koog)
- [x] **Environment & Dependencies**
    - Setup `kotlin/build.gradle.kts` (Gradle Kotlin DSL).
    - Deps: `koog-core`, `ktor-client`, `ktor-server` (for MCP server), `clikt`, `kotest`, `wiremock`.
- [x] **Shared Modules**
    - `src/main/kotlin/services/DocStore.kt`: Interface and Ktor Client implementation.
    - `src/main/kotlin/services/NlpService.kt`: Interface and Ktor Client implementation.
- [x] **Agent Rob (RAG)**
    - `src/main/kotlin/agents/RobAgent.kt`:
        - Use Koog DSL to define the agent.
        - Implement the RAG flow (NLP -> Search -> Context -> LLM).
- [x] **Agent Mike (MCP)**
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
