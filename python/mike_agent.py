import os
import asyncio
import typer
from dotenv import load_dotenv

load_dotenv()

from langchain_openai import AzureChatOpenAI
from langchain_core.tools import StructuredTool
from langgraph.prebuilt import create_react_agent
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

app = typer.Typer()

async def run_mike(question: str):
    # Path to server script
    server_script = os.path.join(os.path.dirname(os.path.abspath(__file__)), "mcp_server.py")

    server_params = StdioServerParameters(
        command="python",
        args=[server_script],
        env=os.environ
    )

    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()

            # List tools
            mcp_tools = await session.list_tools()

            lc_tools = []
            for tool in mcp_tools.tools:
                # Capture tool_name in closure
                def make_tool_func(tool_name):
                    async def tool_func(text: str):
                        result = await session.call_tool(tool_name, arguments={"text": text})
                        # Parse content from tool result
                        return result.content
                    return tool_func

                lc_tools.append(StructuredTool.from_function(
                    coroutine=make_tool_func(tool.name),
                    name=tool.name,
                    description=tool.description
                ))

            if not os.getenv("AZURE_OPENAI_API_KEY"):
                print("No Azure OpenAI key found. Returning mock response.")
                return "I (Mike) cannot run without a real LLM because I rely on tool calling. Please provide AZURE_OPENAI_API_KEY."

            llm = AzureChatOpenAI(
                azure_deployment=os.getenv("AZURE_OPENAI_DEPLOYMENT_NAME"),
                api_version=os.getenv("AZURE_OPENAI_API_VERSION", "2023-05-15"),
            )

            agent = create_react_agent(llm, lc_tools)

            print(f"Mike is thinking about: {question}")
            result = await agent.ainvoke({"messages": [("user", question)]})
            return result["messages"][-1].content

@app.command()
def ask(question: str):
    """
    Ask Mike a question.
    """
    try:
        response = asyncio.run(run_mike(question))
        print("\n--- Answer ---")
        print(response)
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    app()
