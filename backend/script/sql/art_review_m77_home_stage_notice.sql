-- M77：首页底部可配置内容由“首页公告”调整为“展演阶段”

update notice_template
set template_name = '默认展演阶段',
    notice_title = '展演阶段安排',
    notice_content = '请关注活动启动、作品报送、省级推荐、线上展演与成果展示等阶段安排。具体内容可由后台管理员维护更新。',
    notice_group = '展演阶段',
    update_time = sysdate()
where template_code = 'home_default_notice'
  and notice_group = '首页公告';

update notice_template
set notice_group = '展演阶段',
    update_time = sysdate()
where notice_group = '首页公告';
