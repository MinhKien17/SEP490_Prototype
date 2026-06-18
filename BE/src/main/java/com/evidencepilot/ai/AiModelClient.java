package com.evidencepilot.ai;

import com.evidencepilot.ai.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Low-level HTTP client for the teammate's Ollama Claim Analysis API (v0.2.0).
 *
 * <p>This component is intentionally thin — it owns only HTTP mechanics
 * (serialization, status-code handling, structured logging).
 * All orchestration and DB persistence live in
 * {@link com.evidencepilot.service.AiAnalysisService}.</p>
 *
 * <p><b>Active endpoints covered:</b>
 * <ul>
 *   <li>{@code GET  /health}           – liveness probe</li>
 *   <li>{@code GET  /ai/models}        – list available Ollama models</li>
 *   <li>{@code POST /ai/generate}      – raw prompt generation</li>
 *   <li>{@code POST /match/claim}      – semantic source search for a claim</li>
 *   <li>{@code POST /process/claim}    – deep claim analysis against a known source</li>
 *   <li>{@code POST /review/paper}     – structural review of an uploaded paper</li>
 * </ul>
 * </p>
 *
 * <p><b>Excluded:</b> {@code POST /sources} and {@code POST /papers} — the Java
 * backend is the sole authority for physical file storage; those endpoints are
 * internal AI-team testing scaffolding only.</p>
 */
@Slf4j
@Component
public class AiModelClient {

    private final RestClient restClient;

    public AiModelClient(@Qualifier("aiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    // ── Liveness ───────────────────────────────────────────────────────────────

    /**
     * Calls {@code GET /health} and returns the raw response map.
     * Useful as a startup readiness check.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> health() {
        log.debug("Calling GET /health");
        return restClient.get()
                .uri("/health")
                .retrieve()
                .body(Map.class);
    }

    // ── Models ─────────────────────────────────────────────────────────────────

    /**
     * Calls {@code GET /ai/models} and returns the list of available Ollama models.
     */
    public ModelsResponse listModels() {
        log.debug("Calling GET /ai/models");
        return restClient.get()
                .uri("/ai/models")
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new AiApiException("GET /ai/models", res.getStatusCode().value());
                })
                .body(ModelsResponse.class);
    }

    // ── Raw generation ─────────────────────────────────────────────────────────

    /**
     * Calls {@code POST /ai/generate} with a raw prompt.
     *
     * @param prompt the prompt string (1–12 000 chars per Swagger)
     * @return the model's raw text response
     */
    public GenerateResponse generate(String prompt) {
        log.info("Calling POST /ai/generate (prompt length={})", prompt.length());
        return restClient.post()
                .uri("/ai/generate")
                .body(new GenerateRequest(prompt))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new AiApiException("POST /ai/generate", res.getStatusCode().value());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new AiApiException("POST /ai/generate", res.getStatusCode().value());
                })
                .body(GenerateResponse.class);
    }

    // ── Claim matching ─────────────────────────────────────────────────────────

    /**
     * Calls {@code POST /match/claim} to find sources semantically related to a claim.
     *
     * <p>Use the returned {@link ClaimMatchResponse#matches()} list to pick the best
     * {@code source_id} and {@code excerpt} before calling
     * {@link #processClaim(ClaimAnalysisRequest)}.</p>
     *
     * @param request the match request (use {@link ClaimMatchRequest#of(String)} for defaults)
     * @return ordered list of matching sources with suitability scores
     */
    public ClaimMatchResponse matchClaim(ClaimMatchRequest request) {
        log.info("Calling POST /match/claim (claim length={}, topK={})",
                request.claim().length(), request.topK());
        return restClient.post()
                .uri("/match/claim")
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new AiApiException("POST /match/claim", res.getStatusCode().value());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new AiApiException("POST /match/claim", res.getStatusCode().value());
                })
                .body(ClaimMatchResponse.class);
    }

    // ── Claim analysis ─────────────────────────────────────────────────────────

    /**
     * Calls {@code POST /process/claim} for a full AI analysis of a claim
     * against a specific source.
     *
     * <p>The response contains a {@code verdict}, {@code confidence} score,
     * and a detailed {@code explanation}. These are the primary outputs that
     * the service layer persists to the database.</p>
     *
     * @param request the analysis request with claim text, source_id, and excerpt
     * @return the AI verdict with confidence score and explanation
     */
    public ClaimAnalysisResponse processClaim(ClaimAnalysisRequest request) {
        log.info("Calling POST /process/claim (sourceId={}, claim length={})",
                request.sourceId(), request.claim().length());
        return restClient.post()
                .uri("/process/claim")
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new AiApiException("POST /process/claim", res.getStatusCode().value());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new AiApiException("POST /process/claim", res.getStatusCode().value());
                })
                .body(ClaimAnalysisResponse.class);
    }

    // ── Paper review ───────────────────────────────────────────────────────────

    /**
     * Calls {@code POST /review/paper} to get a structural review of a paper
     * that the AI service already has registered under the given {@code paperId}.
     *
     * <p>Note: {@code paperId} here is an identifier inside the AI service's own
     * registry, not the {@code papers.id} primary key in our MySQL database.</p>
     *
     * @param request the review request
     * @return structural feedback including missing/weak sections and claim recommendations
     */
    public PaperReviewResponse reviewPaper(PaperReviewRequest request) {
        log.info("Calling POST /review/paper (paperId={}, targetStyle={}, useAi={})",
                request.paperId(), request.targetStyle(), request.useAi());
        return restClient.post()
                .uri("/review/paper")
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new AiApiException("POST /review/paper", res.getStatusCode().value());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new AiApiException("POST /review/paper", res.getStatusCode().value());
                })
                .body(PaperReviewResponse.class);
    }

    // ── Internal exception type ────────────────────────────────────────────────

    /**
     * Thrown when the AI API returns a non-2xx HTTP status.
     * Wraps the endpoint path and status code for clear error messages.
     */
    public static final class AiApiException extends RuntimeException {
        private final int statusCode;

        public AiApiException(String endpoint, int statusCode) {
            super("AI API error on " + endpoint + " – HTTP " + statusCode);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
