import sys
import os
import asyncio

# Ensure we can import from 'python' package if running from root
current_dir = os.path.dirname(os.path.abspath(__file__))
root_dir = os.path.dirname(current_dir)
if root_dir not in sys.path:
    sys.path.append(root_dir)

from mcp.server.fastmcp import FastMCP
try:
    from python.services.docstore import MockDocStore
except ImportError:
    from services.docstore import MockDocStore

mcp = FastMCP("DocStore")
docstore = MockDocStore()

@mcp.tool()
async def find_by_keyword(text: str) -> list[str]:
    """Search for documents by keyword."""
    return await docstore.find_by_keyword(text)

@mcp.tool()
async def find_by_similarity(text: str) -> list[str]:
    """Search for documents by semantic similarity."""
    return await docstore.find_by_similarity(text)

@mcp.tool()
async def find_by_relations(text: str) -> list[str]:
    """Search for documents by graph relations."""
    return await docstore.find_by_relations(text)

if __name__ == "__main__":
    mcp.run()
