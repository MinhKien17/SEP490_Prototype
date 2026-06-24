# Architecture State Report

## 1. Skills Directory Rules Summary

### .skills/general/skill.md (Karpathy Guidelines)
- **Think Before Coding**: State assumptions explicitly, surface tradeoffs, ask if unclear
- **Simplicity First**: Minimum code that solves the problem, no speculative features/abstractions
- **Surgical Changes**: Touch only what's needed, match existing style, don't refactor unrelated code
- **Goal-Driven Execution**: Define verifiable success criteria, loop until verified

### .skills/spring-backend/01-jpa-domain.md (JPA Domain Architect)
- **Schema Fidelity**: Java Enums must exactly match MySQL ENUM constraints
- **Performance Safety**: All `@ManyToOne` and `@OneToMany` must explicitly declare `FetchType.LAZY`
- **Input Security**: DTOs mandatory; domain entities never used directly as HTTP payloads
- **Process**: Map Entity → Implement DTOs (Create/Update/Response) → Enforce Jakarta Validation → Halt for approval

### .skills/spring-backend/02-repository-tenancy.md (Repository Tenancy Enforcer)
- **Soft-Delete Adherence**: All read operations must filter by `active = true`
- **Strict Tenancy**: Global unfiltered queries prohibited; every query must be scoped to tenant (studentId)
- **Required Patterns**: 
  - `Optional<Entity> findByIdAndStudentIdAndActiveTrue(Integer id, Integer studentId);`
  - `List<Entity> findAllByStudentIdAndActiveTrue(Integer studentId);`
- **Rejection**: Standard `findAll()` or `findById()` without `studentId` must be refused

### .skills/spring-backend/03-service-layer.md (Service Layer Transaction Manager)
- **Abstraction**: Strict separation of interface (`Service`) and implementation (`ServiceImpl`)
- **Data Preservation**: Hard SQL `DELETE` commands banned
- **Transactional Integrity**: All write operations must be explicitly wrapped in `@Transactional`
- **Process**: Build Interface (requiring `authenticatedStudentId`) → Implement Business Logic → Soft-Delete (set `active = false`) → Return ResponseDTO (never raw Entity)

### .skills/spring-backend/04-controller-security.md (Controller Security)
- Same content as 03-service-layer.md (appears to be a duplicate)

---

## 2. Current Project Structure Tree-Map

```
src/main/java/com/evidencepilot/
├── EvidencePilotApplication.java
├── ai/
│   ├── AiModelClient.java
│   ├── config/
│   │   └── AiClientConfig.java
│   └── dto/
│       ├── ClaimAnalysisRequest.java
│       ├── ClaimAnalysisResponse.java
│       ├── ClaimMatch.java
│       ├── ClaimMatchRequest.java
│       ├── ClaimMatchResponse.java
│       ├── EmbeddingRequest.java
│       ├── EmbeddingResponse.java
│       ├── GenerateRequest.java
│       ├── GenerateResponse.java
│       ├── ModelsResponse.java
│       ├── ModelSummary.java
│       ├── PaperReviewRequest.java
│       ├── PaperReviewResponse.java
│       └── SectionIssue.java
├── config/
│   ├── AdminInitializer.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwtUtil.java
│   ├── OpenApiConfig.java
│   ├── SecurityConfig.java
│   └── UploadsInitializer.java
├── controller/
│   ├── AuthController.java
│   ├── ClaimController.java
│   ├── DatasetController.java
│   ├── FeedbackController.java
│   ├── GlobalExceptionHandler.java
│   ├── HealthController.java
│   ├── PaperController.java
│   ├── ProjectController.java
│   ├── SourceController.java
│   ├── TraceabilityExportController.java
│   └── UserController.java
├── domain/
│   ├── enums/
│   │   ├── FeedbackStatus.java
│   │   ├── ProjectStatus.java
│   │   └── UserRole.java
│   └── entity/
│       ├── Claim.java
│       ├── Dataset.java
│       ├── EvidenceEdge.java
│       ├── FeedbackRequest.java
│       ├── InstructorFeedback.java
│       ├── Paper.java
│       ├── PaperSection.java
│       ├── Project.java
│       ├── Source.java
│       ├── SourceChunk.java
│       ├── SourceReference.java
│       ├── SourceText.java
│       └── User.java
├── dto/
│   ├── request/
│   │   ├── InstructorFeedbackRequest.java
│   │   ├── LoginRequest.java
│   │   ├── ProjectCreateRequest.java
│   │   ├── ProjectUpdateRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── SubmitReviewRequest.java
│   │   ├── UpdatePasswordRequest.java
│   │   └── UpdateProfileRequest.java
│   └── response/
│       ├── ApiErrorResponse.java
│       ├── AuthResponse.java
│       ├── ClaimResponseDto.java
│       ├── DatasetGraphResponseDto.java
│       ├── DatasetResponseDto.java
│       ├── DatasetSimilarityResponseDto.java
│       ├── DatasetSourceUploadResponseDto.java
│       ├── FeedbackRequestResponseDto.java
│       ├── InstructorFeedbackResponseDto.java
│       ├── PaperResponseDto.java
│       ├── PaperSectionResponseDto.java
│       ├── ProjectResponse.java
│       ├── SourceChunkResponseDto.java
│       ├── SourceReferenceResponseDto.java
│       ├── SourceResponseDto.java
│       ├── TraceabilityExportResponse.java
│       ├── UserDto.java
│       └── UserResponse.java
├── exception/
│   ├── AiValidationException.java
│   └── ResourceNotFoundException.java
├── repository/
│   ├── ClaimRepository.java
│   ├── DatasetRepository.java
│   ├── EvidenceEdgeRepository.java
│   ├── FeedbackRequestRepository.java
│   ├── InstructorFeedbackRepository.java
│   ├── PaperRepository.java
│   ├── PaperSectionRepository.java
│   ├── ProjectRepository.java
│   ├── SourceChunkRepository.java
│   ├── SourceReferenceRepository.java
│   ├── SourceRepository.java
│   ├── SourceTextRepository.java
│   └── UserRepository.java
└── service/
    ├── AiAnalysisService.java
    ├── ClaimMatchingService.java
    ├── CurrentUserService.java
    ├── PaperProcessingService.java
    ├── ProjectService.java
    ├── QdrantClient.java
    ├── QdrantException.java
    ├── QdrantSearchResult.java
    ├── SourceExtractionService.java
    ├── SourceQueryService.java
    └── impl/
        ├── ProjectServiceImpl.java
        └── SourceQueryServiceImpl.java
```

---

## 3. Structural Inconsistencies Found

### Package Organization Inconsistencies

1. **Hybrid Layered/Feature Mix**: The `ai` package follows feature-based organization (contains its own `config/` and `dto/` subdirectories), while the rest of the codebase uses layered organization (`controller/`, `service/`, `repository/`, `dto/` at root level).

2. **Service Package Pollution**: The `service/` package contains infrastructure/client classes (`QdrantClient`, `QdrantException`, `QdrantSearchResult`) alongside business logic services. These should be in a separate `client/` or `infrastructure/` package.

3. **Incomplete Interface/Impl Separation**: Only 2 of 10 services follow the interface/implementation pattern:
   - `ProjectService` → `ProjectServiceImpl` ✓
   - `SourceQueryService` → `SourceQueryServiceImpl` ✓
   - `AiAnalysisService`, `ClaimMatchingService`, `CurrentUserService`, `PaperProcessingService`, `SourceExtractionService` - no interfaces ✗

4. **DTO Organization Split**: Two parallel DTO hierarchies exist:
   - Root-level `dto/request/` and `dto/response/` (standard Spring pattern)
   - Feature-level `ai/dto/` (contains both requests and responses mixed)
   - This creates ambiguity about where new AI-related DTOs should go.

5. **Repository Naming**: All repositories follow Spring Data naming (`*Repository`) but none appear to enforce the tenancy policies from the skills (no `findByIdAndStudentIdAndActiveTrue` style methods visible from file names).

6. **Missing Service Layer for Domain Entities**: Several domain entities (Claim, Dataset, EvidenceEdge, FeedbackRequest, InstructorFeedback, Paper, PaperSection, Source, SourceChunk, SourceReference, SourceText, User) have repositories but no corresponding Service interfaces in the `service/` package.

7. **Config Package Contents**: The root `config/` package mixes security (JWT, SecurityConfig), infrastructure (UploadsInitializer, AdminInitializer), and API documentation (OpenApiConfig) - could be split into `security/`, `infrastructure/`, `docs/` subpackages.

8. **Exception Package Minimal**: Only 2 exceptions exist (`AiValidationException`, `ResourceNotFoundException`) - likely missing domain-specific exceptions for business rule violations.