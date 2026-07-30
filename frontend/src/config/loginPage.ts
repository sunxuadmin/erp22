export const loginPageText = {
  pageTitle: '后台登陆',
  brandName: '河南省第八届大学生艺术展演',
  brandKicker: 'DYZ / ART EXHIBITION',
  sloganLines: ['向美而行', '逐梦未来'],
  subtitle: '',
  description: '',
  meta: [],
  dynamicLabel: '艺术展演',
  ariaLabels: {
    heroInfo: '系统信息'
  },

  entrySectionTitle: '请选择登录入口',
  entrySectionDesc: '',
  entryCards: [
    {
      key: 'submit',
      title: '上报入口',
      desc: '',
      icon: 'UploadFilled',
      loginTitle: '上报登录',
      loginSubtitle: '   '
    },
    {
      key: 'admin',
      title: '管理入口',
      desc: '',
      icon: 'Management',
      loginTitle: '管理入口登录',
      loginSubtitle: '适用于管理员账号登录后进入后台管理。'
    },
    {
      key: 'expert',
      title: '专家入口',
      desc: '',
      icon: 'UserFilled',
      loginTitle: '专家入口登录',
      loginSubtitle: '任务工作台。'
    }
  ],

  formKicker: '',
  formTitle: '账号登录',
  formSubtitle: '',
  selectedEntryLabel: '当前入口',
  backToEntriesText: '返回选择入口',
  tenantPlaceholder: '请选择所属单位',
  usernamePlaceholder: '请输入账号',
  passwordPlaceholder: '请输入密码',
  codePlaceholder: '请输入验证码',
  captchaRefreshTitle: '刷新验证码',
  captchaAlt: '验证码',
  rememberPassword: '记住账号',
  registerText: '注册入口',
  loginButton: '登录系统',
  loggingButton: '正在登录...',
  submitErrorLog: '登录表单校验未通过',

  registerCard: {
    kicker: 'REGISTER',
    title: '学校/专家注册',
    subtitle: '请使用注册码完成学校账号或专家账号注册。',
    currentLabel: '注册入口',
    backToLoginText: '返回登录',
    userTypePlaceholder: '请选择注册类型',
    schoolUserType: '学校账号',
    expertUserType: '专家账号',
    usernamePlaceholder: '请输入账号',
    phonePlaceholder: '请输入手机号',
    registrationCodePlaceholder: '请输入注册码',
    schoolModeSelect: '选择已有学校',
    schoolModeCreate: '新增学校',
    schoolSelectPlaceholder: '请选择学校名称',
    schoolNamePlaceholder: '填写学校名称，不能与已有学校重复',
    passwordPlaceholder: '请输入密码',
    confirmPasswordPlaceholder: '请再次输入密码',
    codePlaceholder: '请输入验证码',
    submitButton: '提交注册',
    submittingButton: '正在注册...',
    noticeTitle: '注册公告',
    successTitle: '系统提示',
    schoolRequiredMessage: '请选择已有学校或填写新学校名称',
    successMessage: '注册成功，账号：{username}'
  },

  footer: 'Copyright 2026  All Rights Reserved.',

  validation: {
    tenantIdRequired: '请选择所属单位',
    usernameRequired: '请输入账号',
    passwordRequired: '请输入密码',
    codeRequired: '请输入验证码',
    userTypeRequired: '请选择注册类型',
    phoneRequired: '请输入手机号',
    phonePattern: '请输入正确的手机号',
    registrationCodeRequired: '请输入注册码',
    confirmPasswordRequired: '请再次输入密码',
    confirmPasswordEqual: '两次输入的密码不一致',
    passwordLength: '密码长度必须在 5 到 20 个字符之间',
    usernameLength: '账号长度必须在 2 到 20 个字符之间',
    passwordPattern: '密码不能包含 < > " \' \\ |'
  }
};
