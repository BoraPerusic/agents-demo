import pytest
import sys
import os

# Fix path
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from python.services.docstore import MockDocStore

@pytest.mark.asyncio
async def test_find_by_keyword():
    store = MockDocStore()
    results = await store.find_by_keyword("test")
    assert len(results) == 2
    assert "Document A" in results[0]
