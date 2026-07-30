-- 隐藏独立的“作品上报查看”菜单；只读查看权限仍保留，入口合并到“项目审核”页面。
update sys_menu
set visible = '1',
    update_time = sysdate(),
    remark = '入口已合并到项目审核页面'
where menu_id = 17980
   or path = 'project-view'
   or component = 'crehn/project-view/index';
