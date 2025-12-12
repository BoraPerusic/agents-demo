package services

interface NlpService {
    suspend fun analyze(question: String): String
}

class MockNlpService : NlpService {
    override suspend fun analyze(question: String): String {
        return when {
            "who" in question.lowercase() || "connect" in question.lowercase() -> "relations"
            "like" in question.lowercase() || "similar" in question.lowercase() -> "similarity"
            else -> "keyword"
        }
    }
}
