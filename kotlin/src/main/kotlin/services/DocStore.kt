package services

interface DocStore {
    suspend fun findByKeyword(text: String): List<String>
    suspend fun findBySimilarity(text: String): List<String>
    suspend fun findByRelations(text: String): List<String>
}

class MockDocStore : DocStore {
    override suspend fun findByKeyword(text: String): List<String> {
        return listOf("Keyword result for '$text': Document A", "Keyword result for '$text': Document B")
    }

    override suspend fun findBySimilarity(text: String): List<String> {
        return listOf("Similarity result for '$text': Document C", "Similarity result for '$text': Document D")
    }

    override suspend fun findByRelations(text: String): List<String> {
        return listOf("Relation result for '$text': Document E", "Relation result for '$text': Document F")
    }
}
