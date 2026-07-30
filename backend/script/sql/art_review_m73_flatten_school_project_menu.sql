-- --------------------------------------------------
-- M73 flatten school project menus
-- Hide "我的项目" and move reporting categories directly under "艺术展演".
-- --------------------------------------------------

update sys_menu
set visible = '1',
    status = '0',
    remark = '已隐藏，仅保留项目按钮权限；报送大类直接挂在艺术展演下',
    update_time = sysdate()
where menu_id = 17003;

update sys_menu
set parent_id = 17000,
    order_num = case menu_id
        when 17310 then 3
        when 17311 then 4
        when 17312 then 5
        when 17313 then 6
        when 17314 then 7
        when 17334 then 8
        else order_num
    end,
    update_time = sysdate()
where menu_id in (17310, 17311, 17312, 17313, 17314, 17334);
