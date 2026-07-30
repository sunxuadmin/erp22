# ART-OWNER: BE.PROJECT.*

Target package area for splitting `impl/ArtProjectServiceImpl.java`.

Planned modules:

- `query/ProjectQueryService.java`: `BE.PROJECT.QUERY`
- `submit/ProjectDraftSubmitService.java`: `BE.PROJECT.DRAFT_SUBMIT`
- `member/ProjectMemberTemplateService.java`: `BE.PROJECT.MEMBER_TEMPLATE`
- `member/ProjectMemberImportService.java`: `BE.PROJECT.MEMBER_IMPORT`
- `file/ProjectFileService.java`: `BE.PROJECT.FILE_UPLOAD`
- `recycle/ProjectRecycleService.java`: `BE.PROJECT.RECYCLE`
- `quota/ProjectQuotaGuard.java`: `BE.PROJECT.QUOTA_GUARD`
- `assembler/ProjectDetailAssembler.java`: `BE.PROJECT.DETAIL_ASSEMBLER`
- `scope/ProjectScopeGuard.java`: `BE.PROJECT.SCOPE_GUARD`

Keep `IArtProjectService` stable during incremental extraction.
