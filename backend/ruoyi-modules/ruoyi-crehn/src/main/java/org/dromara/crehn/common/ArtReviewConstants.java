package org.dromara.crehn.common;

/**
 * 艺术展演评审业务常量。
 */
public interface ArtReviewConstants {
    String USER_TYPE_SCHOOL = "school";
    String SCHOOL_REVIEW_ENABLED = "enabled";
    String ACTIVITY_ENABLED = "enabled";
    String ACTIVITY_SCOPE_ALL = "all";
    String ACTIVITY_SCOPE_ASSIGNED = "assigned";
    String PROJECT_DRAFT = "draft";
    String PROJECT_PARTICIPANT_SUBMITTED = "participant_submitted";
    String PROJECT_PARTICIPANT_RETURNED = "participant_returned";
    String PROJECT_SCHOOL_APPROVED = "school_approved";
    String PROJECT_SUBMITTED = "school_submitted";
    String PROJECT_RETURNED = "returned";
    String PROJECT_AUDIT_PASSED = "audit_passed";
    String PROJECT_RECYCLED = "recycled";
    String USER_TYPE_EXPERT = "expert";
    String USER_TYPE_PARTICIPANT = "participant";
    String AUDIT_PASS = "pass";
    String AUDIT_RETURN = "return";
    String AUDIT_WITHDRAW_RETURN = "withdraw_return";
    String AUDIT_WITHDRAW_PASS = "withdraw_pass";
    String MESSAGE_TYPE_AUDIT = "audit";
    String MESSAGE_TYPE_RESULT = "result";
    String MESSAGE_TYPE_SCHOOL_REVIEW = "school_review";
    String MESSAGE_READ_UNREAD = "unread";
    String MESSAGE_READ_READ = "read";
    String FILE_ACTIVE = "active";
    String FILE_DELETED = "deleted";
    String AUDIT_ASSIGNMENT_ACTIVE = "active";
    String AUDIT_ASSIGNMENT_DISABLED = "disabled";
    String REVIEW_ASSIGNMENT_ACTIVE = "active";
    String REVIEW_ASSIGNMENT_DISABLED = "disabled";
    String REVIEW_SCORE_DRAFT = "draft";
    String REVIEW_SCORE_SUBMITTED = "submitted";
    String REVIEW_SCORE_MODE_NUMERIC = "numeric_100";
    String REVIEW_SCORE_MODE_GRADE = "grade";
    String REVIEW_SCORE_MODE_COMMENT_ONLY = "comment_only";
    String REVIEW_EXCLUSIVE_SINGLE = "single";
    String REVIEW_EXCLUSIVE_MULTI = "multi";
    String REVIEW_SCORE_VISIBILITY_AFTER_SUBMIT = "after_submit";
    String REVIEW_SCORE_VISIBILITY_HIDDEN = "hidden";
    String REVIEW_SCORE_VISIBILITY_ALWAYS = "always";
    String ASSIGNMENT_BATCH_MERGE = "merge";
    String ASSIGNMENT_BATCH_REMOVE = "remove";
    String ASSIGNMENT_BATCH_SYNC = "sync";
    String ASSIGNMENT_BATCH_TARGET_ASSIGNMENT = "assignment";
    String ASSIGNMENT_BATCH_TARGET_PROJECT = "project";
    String RESULT_DRAFT = "draft";
    String RESULT_PUBLISHED = "published";
    String AWARD_RULE_RANK_RANGE = "rank_range";
    String AWARD_RULE_SCORE_RANGE = "score_range";
    String RESULT_LOG_TARGET_RESULT = "result";
    String RESULT_LOG_TARGET_SCORE = "score";
    String RESULT_LOG_TARGET_PROJECT = "project";
    String RESULT_LOG_ACTION_AWARD_RULE_SAVE = "award_rule_save";
    String RESULT_LOG_ACTION_SCORE_ADJUST = "score_adjust";
    String RESULT_LOG_ACTION_SCORE_RETURN = "score_return";
    String RESULT_LOG_ACTION_SHOWCASE_ORDER_SAVE = "showcase_order_save";
    String RESULT_LOG_ACTION_SCORE_WARNING_RULE_SAVE = "score_warning_rule_save";
    String RESULT_LOG_ACTION_GENERATE_WITHDRAW = "result_generate_withdraw";
}
