from abc import ABC, abstractmethod
import random

class NLPService(ABC):
    @abstractmethod
    async def analyze(self, question: str) -> str:
        """
        Analyzes the question and returns the suggested search method.
        Returns one of: 'keyword', 'similarity', 'relations'.
        """
        pass

class MockNLPService(NLPService):
    async def analyze(self, question: str) -> str:
        # Simple heuristic or random for mock
        if "who" in question.lower() or "connect" in question.lower():
            return "relations"
        elif "like" in question.lower() or "similar" in question.lower():
            return "similarity"
        else:
            return "keyword"
