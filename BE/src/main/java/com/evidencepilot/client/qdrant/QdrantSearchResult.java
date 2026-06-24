package com.evidencepilot.client.qdrant;

import java.math.BigDecimal;

public record QdrantSearchResult(
        String chunkId,
        BigDecimal score
) {
}
