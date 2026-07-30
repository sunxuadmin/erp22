# ART-OWNER: BE.RESULT.*

Target package area for splitting `impl/ArtResultServiceImpl.java`.

Planned modules:

- `query/ReviewResultQueryService.java`: `BE.RESULT.QUERY`
- `generate/ReviewResultGenerateService.java`: `BE.RESULT.GENERATE`
- `publish/ReviewResultPublishService.java`: `BE.RESULT.PUBLISH`
- `award/ReviewAwardRuleService.java`: `BE.RESULT.AWARD_RULE`
- `stats/ProjectUploadStatsService.java`: `BE.RESULT.UPLOAD_SUMMARY`
- `overview/ReviewProjectOverviewService.java`: `BE.RESULT.PROJECT_OVERVIEW`
- `visibility/ResultVisibilityService.java`: `BE.RESULT.VISIBILITY`
- `log/ReviewResultLogService.java`: `BE.RESULT.LOG`

Keep `IArtResultService` stable during incremental extraction.
