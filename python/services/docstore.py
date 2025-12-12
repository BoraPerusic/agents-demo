from abc import ABC, abstractmethod
from typing import List, Dict, Any

class DocStore(ABC):
    @abstractmethod
    async def find_by_keyword(self, text: str) -> List[str]:
        pass

    @abstractmethod
    async def find_by_similarity(self, text: str) -> List[str]:
        pass

    @abstractmethod
    async def find_by_relations(self, text: str) -> List[str]:
        pass

class MockDocStore(DocStore):
    async def find_by_keyword(self, text: str) -> List[str]:
        return [f"Keyword result for '{text}': Document A", f"Keyword result for '{text}': Document B"]

    async def find_by_similarity(self, text: str) -> List[str]:
        return [f"Similarity result for '{text}': Document C", f"Similarity result for '{text}': Document D"]

    async def find_by_relations(self, text: str) -> List[str]:
        return [f"Relation result for '{text}': Document E", f"Relation result for '{text}': Document F"]
