# ART-OWNER: FE.PROJECT_EDIT.* composables

Target directory for project edit state and workflow composables.

- `useProjectEditContext.ts`: `FE.PROJECT_EDIT.CONTEXT`
- `useProjectMaterials.ts`: `FE.PROJECT_EDIT.MATERIAL_UPLOAD`
- `useProjectMembers.ts`: `FE.PROJECT_EDIT.MEMBER_TABLE`
- `useProjectPreview.ts`: `FE.PROJECT_EDIT.FILE_PREVIEW`
- `useProjectLeaveGuard.ts`: `FE.PROJECT_EDIT.LEAVE_GUARD`

Do not move logic here without updating `docs/refactor/crehn-decomposition-map.md`.
