package com.evidencepilot.client.qdrant;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QdrantSearchRequestTest {

    @Test
    void forDocumentFiltersByStoredDocumentIdPayloadKey() {
        QdrantSearchRequest request = QdrantSearchRequest.forDocument(
                "document-1",
                List.of(0.1f, 0.2f),
                3
        );

        assertThat(request.filter().must())
                .singleElement()
                .extracting(QdrantSearchRequest.Condition::key)
                .isEqualTo("document_id");
    }
}
