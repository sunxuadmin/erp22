-- 首页内容块富文本/PDF 详情扩容。
-- 不新增表，仅扩大摘要与配置 JSON 容量，避免长正文、图文 HTML/Markdown 配置溢出 TEXT。

alter table home_content_card
    modify column card_content mediumtext null comment 'card summary/content';

alter table home_content_card
    modify column config_json mediumtext null comment 'card config json';
