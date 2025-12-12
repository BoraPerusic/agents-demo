import os
import asyncio
from typing import TypedDict, List, Optional
import typer
from dotenv import load_dotenv

# Load env before imports
load_dotenv()

from langchain_openai import AzureChatOpenAI
from langchain_core.messages import HumanMessage, SystemMessage

# Import services
# Assuming running from 'python' directory or PYTHONPATH is set
try:
    from services.docstore import MockDocStore
    from services.nlp import MockNLPService
except ImportError:
    # fix path for direct execution
    import sys
    sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    from python.services.docstore import MockDocStore
    from python.services.nlp import MockNLPService

# Define State
class AgentState(TypedDict):
    question: str
    search_type: Optional[str]
    documents: List[str]
    answer: Optional[str]

# Nodes
async def analyze_node(state: AgentState):
    print("--- NLP Analyzing ---")
    nlp = MockNLPService()
    search_type = await nlp.analyze(state["question"])
    print(f"Suggested Search: {search_type}")
    return {"search_type": search_type}

async def search_node(state: AgentState):
    print("--- Searching DocStore ---")
    docstore = MockDocStore()
    search_type = state["search_type"]
    question = state["question"]

    if search_type == "keyword":
        docs = await docstore.find_by_keyword(question)
    elif search_type == "similarity":
        docs = await docstore.find_by_similarity(question)
    elif search_type == "relations":
        docs = await docstore.find_by_relations(question)
    else:
        docs = []

    print(f"Found {len(docs)} documents.")
    return {"documents": docs}

async def generate_node(state: AgentState):
    print("--- Generating Answer ---")
    # Check if we have Azure keys, else mock
    if not os.getenv("AZURE_OPENAI_API_KEY"):
        print("No Azure OpenAI key found, using Mock LLM.")
        docs_text = "\n".join(state["documents"])
        answer = f"I found these documents relevant:\n{docs_text}\n\nBased on them, the answer is: [Simulated Answer for '{state['question']}']"
        return {"answer": answer}

    llm = AzureChatOpenAI(
        azure_deployment=os.getenv("AZURE_OPENAI_DEPLOYMENT_NAME"),
        api_version=os.getenv("AZURE_OPENAI_API_VERSION", "2023-05-15"),
    )

    docs_text = "\n".join(state["documents"])
    prompt = f"""
    You are a helpful assistant. Use ONLY the following documents to answer the user's question.

    First, analyze the relevance of the given documents and provide a feedback starting with "Feedback: ".
    Then, compose the final answer starting with "Answer: ".
    If the answer is not in the documents, state that in the feedback and answer "I don't know".

    Documents:
    {docs_text}

    Question: {state["question"]}
    """

    response = await llm.ainvoke([HumanMessage(content=prompt)])
    return {"answer": response.content}

# Graph Construction
from langgraph.graph import StateGraph, END

workflow = StateGraph(AgentState)
workflow.add_node("analyze", analyze_node)
workflow.add_node("search", search_node)
workflow.add_node("generate", generate_node)

workflow.set_entry_point("analyze")
workflow.add_edge("analyze", "search")
workflow.add_edge("search", "generate")
workflow.add_edge("generate", END)

app_graph = workflow.compile()

# CLI
app = typer.Typer()

@app.command()
def ask(question: str):
    """
    Ask Rob a question.
    """
    print(f"Rob is thinking about: {question}")
    try:
        result = asyncio.run(app_graph.ainvoke({"question": question}))
        print("\n--- Answer ---")
        print(result["answer"])
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    app()
