-- M11.1 additive custom score-warning formula storage.
-- The expression is parsed by the backend allow-list evaluator.

alter table review_score_warning_config
    add column if not exists formula_expression varchar(200) null comment '受限评分差异公式表达式';
