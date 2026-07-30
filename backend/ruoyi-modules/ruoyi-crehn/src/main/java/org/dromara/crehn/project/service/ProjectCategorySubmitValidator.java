package org.dromara.crehn.project.service;

public interface ProjectCategorySubmitValidator {
    boolean supports(ProjectSubmitContext context);

    void validate(ProjectSubmitContext context);
}
