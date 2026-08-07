import type { PortalHomeComponent } from '@/api/crehn/cms';

export type CompetitionHomeVersion = 'v1' | 'v2';
export type CompetitionFieldKind = 'text' | 'textarea' | 'select' | 'switch';
export type CompetitionComponentConfigValue = string | number | boolean;
export type CompetitionComponentConfig = Record<string, CompetitionComponentConfigValue>;

export interface CompetitionComponentField {
  key: string;
  label: string;
  kind: CompetitionFieldKind;
  placeholder?: string;
  options?: Array<{ label: string; value: string }>;
}

export interface CompetitionComponentDefinition {
  type: string;
  label: string;
  description: string;
  defaultConfig: CompetitionComponentConfig;
  versionDefaultConfigs: Record<CompetitionHomeVersion, CompetitionComponentConfig>;
  versionDefaultEnabled: Record<CompetitionHomeVersion, boolean>;
  fields: CompetitionComponentField[];
  defaultHeight: number;
}

const text = (key: string, label: string, placeholder?: string): CompetitionComponentField => ({ key, label, kind: 'text', placeholder });
const textarea = (key: string, label: string, placeholder?: string): CompetitionComponentField => ({ key, label, kind: 'textarea', placeholder });

const v1Nav = {
  brand: '创意河南', slogan: '设计创新 · 赋能河南', brandMark: 'CH',
  navItems: '大赛首页|top\n赛道设置|tracks\n报送流程|journey\n时间安排|timeline\n通知公告|notice',
  loginLabel: '登录', actionLabel: '作品报送', menuOpenLabel: '打开菜单', menuCloseLabel: '关闭菜单', sticky: true,
  portalModalEyebrow: 'SUBMISSION PLATFORM', portalModalTitle: '作品报送平台正在筹备中',
  portalModalBody: '学校联络员信息提交后，组委会将分发学校登录账号。正式上传通道开放后，参赛者可完成注册与作品上传。',
  portalModalPromptLabel: '请先完成', portalModalPrompt: '8 月 30 日前提交加盖学校公章的联络人信息表', portalModalButton: '联系组委会',
  noticeModalEyebrow: 'OFFICIAL NOTICE', noticeModalTitle: '大赛通知要点',
  noticeModalBody: '本届主题为“设计创新，赋能河南”，面向全省普通高校在校学生和相关专业教师，按组别、类别分别评奖。',
  noticeModalRules: '作品须为 2026 年 1 月 1 日以后设计的原创作品。\n同一作品只能选投一个赛道、一个类别。\n作品正面不得出现作者姓名和单位信息。\n活动不收取任何费用。',
  noticeModalButton: '查看时间安排', modalCloseLabel: '关闭'
};

const v2Nav = {
  brand: '创意河南', slogan: '设计创新 · 赋能河南', brandMark: 'CH',
  navItems: '首页|top\n赛道实验室|tracks\n报送路径|journey\n时间坐标|timeline\n通知|notice',
  loginLabel: '登录', actionLabel: '作品报送', menuOpenLabel: '打开菜单', menuCloseLabel: '关闭菜单', sticky: true,
  portalModalEyebrow: 'SUBMISSION SYSTEM', portalModalTitle: '作品报送平台正在筹备中',
  portalModalBody: '学校联络员信息提交后，组委会将分发学校登录账号。正式通道开放后，参赛者可完成注册和作品上传。',
  portalModalPromptLabel: '当前请先完成', portalModalPrompt: '8 月 30 日前提交加盖学校公章的联络人信息表', portalModalButton: '联系组委会',
  noticeModalEyebrow: 'OFFICIAL NOTICE', noticeModalTitle: '大赛通知要点',
  noticeModalBody: '本届主题为“设计创新，赋能河南”，面向全省普通高校在校学生和相关专业教师。',
  noticeModalRules: '作品须为 2026 年 1 月 1 日以后设计的原创作品。\n同一作品只能选投一个赛道、一个类别。\n作品正面不得出现作者姓名和单位信息。\n活动不收取任何费用。',
  noticeModalButton: '查看时间坐标', modalCloseLabel: '关闭'
};

const v1Hero = {
  documentTitle: '创意河南｜第六届全省高校艺术设计大赛', motto: '设计创新 · 赋能河南', mottoEn: 'CREATIVE HENAN', artText: 'ART', yearText: '2026',
  editionText: '全省高等学校第六届', editionEn: '', competitionTitle: '“创意河南”\n艺术设计大赛',
  summary: '全省高等学校第六届“创意河南”艺术设计大赛，以“设计创新，赋能河南”为主题，面向全省高校学生与教师征集优秀设计成果。',
  primaryActionLabel: '进入作品报送', secondaryActionLabel: '查看大赛通知', organizerLabel: '主办单位', organizer: '河南省教育厅',
  groupsLabel: '参赛组别', groups: '学生组 / 教师组', feeLabel: '参赛费用', fee: '公益赛事 · 免报名费',
  uploadDeadlineLabel: '作品上传截止', uploadDeadline: '9 月 30 日 · 24:00', trackCardLabel: '本届设置', trackCardValue: '2 大赛道 · 12 个方向',
  statusLabel: '报送状态', status: '平台筹备中', axisStart: '01', axisEnd: '12', axisArt: 'ART', axisTech: 'TECH', artAlt: '紫、金、青、蓝与珊瑚色玻璃丝带组成的创意河南抽象艺术雕塑'
};

const v2Hero = {
  documentTitle: '创意河南｜艺术 × 科技明亮版', motto: '设计创新 · 赋能河南', mottoEn: 'CREATIVE HENAN', artText: 'ART', yearText: '2026',
  editionText: '全省高等学校第六届', editionEn: 'THE 6TH', competitionTitle: '“创意河南”\n艺术设计大赛',
  summary: '全省高等学校第六届“创意河南”艺术设计大赛，以“设计创新，赋能河南”为主题，让艺术感知与科技能力在河南相遇。',
  primaryActionLabel: '启动作品报送', secondaryActionLabel: '探索竞赛赛道', organizerLabel: '主办单位', organizer: '河南省教育厅',
  groupsLabel: '参赛对象', groups: '全省高校学生与教师', feeLabel: '', fee: '', uploadDeadlineLabel: 'UPLOAD WINDOW', uploadDeadline: 'UNTIL 09.30 · 24:00',
  trackCardLabel: 'SYSTEM / OPEN', trackCardValue: '2 TRACKS\n12 CREATIVE DIRECTIONS', statusLabel: '', status: '', axisStart: '01', axisEnd: '12', axisArt: 'ART', axisTech: 'TECH',
  artAlt: '透明虹彩玻璃、参数化波纹与数据光点组成的艺术科技雕塑'
};

const v1Facts = {
  deadlineAt: '2026-10-01T00:00:00+08:00', invalidCountdownValue: '—', countdownLabel: '天后关闭', countdownSuffix: '作品上传通道', countdownValue: '',
  fact1Label: '01\n学校联络员信息', fact1Value: '8 月 30 日前', fact2Label: '02\n参赛作品上传', fact2Value: '9 月 30 日截止',
  fact3Label: '03\n校方审核提交', fact3Value: '10 月 10 日截止', fact4Label: '', fact4Value: ''
};

const v2Facts = {
  deadlineAt: '2026-10-01T00:00:00+08:00', invalidCountdownValue: '—', countdownLabel: 'DAYS', countdownSuffix: '', countdownValue: '',
  fact1Label: '01 / THEME', fact1Value: '设计创新，赋能河南', fact2Label: '02 / GROUPS', fact2Value: '学生组 · 教师组',
  fact3Label: '03 / DEADLINE', fact3Value: '作品上传 09.30', fact4Label: '04 / STATUS', fact4Value: '平台筹备中'
};

const v1Tracks = {
  eyebrow: 'COMPETITION TRACKS', title: '两条赛道，让好设计\n真正抵达河南。',
  summary: '从专业设计创新，到艺术赋能乡村；让创意不仅被看见，更成为推动文化、空间、产业与生活更新的真实力量。',
  trackALabel: '赛道 A', trackAMark: '创', trackABadge: '7 个类别', trackATitle: '创意河南艺术设计', trackAEnglish: 'DESIGN FOR HENAN',
  trackADescription: '体现“创意河南”新质发展理念，凸显创新性、引领性与服务性，注重设计成果的市场转化与实际应用。',
  trackAQuota: '≤ 20|每校每组限额|件（套）', trackAAuthorQuota: '≤ 3|每件作品设计者|人',
  trackADirections: '01|视觉传达|品牌、IP、海报、包装、书籍、字体、插画与绘本\n02|环境设计|景观、建筑、规划、室内、展陈、公共艺术与家具\n03|工业设计|产品设计与面向未来生活的概念设计\n04|数媒综合|动画、影像、交互、VR / AR、元宇宙与 AIGC\n05|服装服饰|服装、服饰、鞋帽包饰与时尚设计\n06|文旅文创|文化传承、地域文创与旅游产品开发\n07|艺术与科技|光影、传感交互、数字雕塑与跨媒体实验',
  trackBLabel: '赛道 B', trackBMark: '乡', trackBBadge: '5 个方向', trackBTitle: '艺术赋美乡村创意设计', trackBEnglish: 'ART FOR RURAL REVITALIZATION',
  trackBDescription: '聚焦乡村文化振兴、人居环境提质、特色产业升级与数字文旅创新，征集兼具艺术性、创新性、实用性与落地性的作品。',
  trackBQuota: '≤ 5|每校每组限额|件（套）', trackBAuthorQuota: '≤ 3|每件作品设计者|人',
  trackBDirections: '01|乡村记忆留存|以插画、纪实视觉与文创设计留住乡土文化基因\n02|乡村空间提质|低成本、可持续地改造农房、院落与公共文化空间\n03|乡村特产升级|用包装、品牌 VI 与营销视觉提升特色产品价值\n04|乡村文创业态|开发乡土文创、民俗衍生品与轻量化庭院业态\n05|乡村数字传播|通过数字绘画、动漫、影像、VR 与乡村 IP 创新传播'
};

const v2Tracks = {
  eyebrow: 'CH / 01 · TRACK LAB', title: '艺术是感知，\n科技是新的画笔。',
  summary: '从专业设计创新，到艺术赋能乡村；让作品不仅停留在屏幕上，也可以进入空间、产业、生活与未来。',
  trackALabel: 'TRACK A', trackAMark: 'A', trackABadge: '07 MODULES', trackATitle: '创意河南艺术设计', trackAEnglish: 'DESIGN FOR HENAN',
  trackADescription: '体现“创意河南”新质发展理念，凸显创新性、引领性与服务性，推动优秀成果从创意走向应用。', trackAQuota: '≤ 20|每校每组|件（套）', trackAAuthorQuota: '≤ 3|每件设计者|人',
  trackADirections: 'V01|视觉传达|品牌 / IP / 海报 / 包装 / 字体 / 插画\nS02|环境设计|景观 / 建筑 / 室内 / 展陈 / 公共艺术\nP03|工业设计|产品设计 / 概念设计 / 未来生活\nD04|数媒综合|动画 / 影像 / VR·AR / AIGC / 交互\nF05|服装服饰|服装 / 服饰 / 鞋帽包饰 / 时尚设计\nC06|文旅文创|地域文化 / 文创产品 / 旅游开发\nT07|艺术与科技|光影 / 传感 / 数字雕塑 / 跨媒体实验',
  trackBLabel: 'TRACK B', trackBMark: 'B', trackBBadge: '05 MODULES', trackBTitle: '艺术赋美乡村创意设计', trackBEnglish: 'ART FOR RURAL REVITALIZATION',
  trackBDescription: '聚焦文化振兴、空间提质、产业升级与数字文旅创新，强调艺术性、实用性与真正的落地价值。', trackBQuota: '≤ 5|每校每组|件（套）', trackBAuthorQuota: '≤ 3|每件设计者|人',
  trackBDirections: 'R01|乡村记忆留存|让乡土文化与村落记忆被重新看见\nR02|乡村空间提质|用低成本、可持续设计激活公共空间\nR03|乡村特产升级|以品牌与包装提升特色产品价值\nR04|乡村文创业态|把乡土资源转化为可落地的新业态\nR05|乡村数字传播|用数字艺术、VR 与乡村 IP 创新传播'
};

const v1ArtTech = {
  eyebrow: 'ART × TECHNOLOGY', title: '当创意连接算法、材料与空间，\n设计开始拥有新的感官。', summary: '让感知、算法与材料互相启发，作品在屏幕之外继续生长。',
  items: '01|AIGC 创意设计|探索人工智能与视觉叙事、审美表达的协同创作。\n02|沉浸与交互|以传感、光影、AR 与实时数据构建可参与的作品。\n03|跨媒体实验|让数字雕塑、生物材料与空间装置发生新的连接。'
};
const v2ArtTech = { eyebrow: 'ART × TECHNOLOGY', title: '当创意连接算法、材料与空间，\n设计开始拥有新的感官。', summary: '', items: '01|AIGC 创意设计|探索人工智能与视觉叙事、审美表达的协同创作。\n02|沉浸与交互|以传感、光影、AR 与实时数据构建可参与的作品。\n03|跨媒体实验|让数字雕塑、生物材料与空间装置发生新的连接。' };

const v1Journey = {
  eyebrow: 'SUBMISSION JOURNEY', title: '从校级初评，\n到省级舞台。', summary: '大赛不接受个人单独申报。请由学校统一组织、审核并按限额推荐。',
  statusLabel: '报送平台筹备中', status: '', deskTitle: '准备好作品，也要准备好正确的文件。', deskSummary: '请先核对版面尺寸、格式、大小、匿名要求与签章材料，避免在截止前集中返工。', actionLabel: '查看报送状态', stepPrefix: '',
  items: '01|校|确定学校联络员|每校确定 1 名联络员兼系统管理员，统一对接报名与作品报送。\n02|号|领取学校账号|提交联络员信息后，由组委会向学校分发登录账号与密码。\n03|传|参赛者上传作品|师生完成平台注册，按类别要求上传作品、海报与登记材料。\n04|审|校方审核提交|学校管理员审核推荐作品，并在 10 月 10 日前完成省赛提交。'
};
const v2Journey = {
  eyebrow: 'CH / 02 · SUBMISSION PATH', title: '让作品沿着正确路径，\n进入省级舞台。', summary: '', statusLabel: '', status: '', deskTitle: '', deskSummary: '', actionLabel: '查看平台状态', stepPrefix: 'STEP',
  items: '01|校|确定学校联络员|每校确定 1 名联络员兼系统管理员，统一对接报名与报送工作。\n02|号|领取学校账号|提交联络员信息后，由组委会向学校分发登录账号和密码。\n03|传|参赛者上传作品|师生完成注册，并按类别要求上传作品、海报和登记材料。\n04|审|学校审核提交|学校管理员审核推荐作品，完成省赛项目提交。'
};

const v1Files = {
  eyebrow: 'FILE SPECIFICATION', title: '准备好作品，\n也要准备好正确的文件。', summary: '上传前请再次核对尺寸、格式、大小和匿名要求。',
  items: '01|静态作品|90 × 120 cm|竖幅 · 300 DPI · RGB · JPG · 单幅 ≤ 10MB\n02|视频作品|MP4 · 1080P|≤ 500MB · 时长 ≤ 5 分钟 · 不得含个人或单位信息\n03|交互 / 科技|二维码 + 海报|APP / 元宇宙作品另附可执行包与功能演示视频'
};
const v2Files = {
  eyebrow: 'FILE SPECIFICATION LAB', title: '作品很大胆，\n文件必须很精确。', summary: '上传前请完成格式、尺寸、大小、匿名与签章材料校验。',
  items: '01|STATIC|90 × 120 cm|竖幅 · 300 DPI · RGB · JPG\u2028单幅作品 ≤ 10MB\n02|VIDEO|MP4 · 1080P|≤ 500MB · 时长 ≤ 5 分钟\u2028画面不得含个人或单位信息\n03|INTERACTIVE|二维码 + 海报|APP / 元宇宙另附可执行包\u2028与功能演示视频'
};

const v1Timeline = {
  eyebrow: 'KEY DATES', title: '重要时间\n一目了然。', summary: '建议各学校为内部初评、材料签章与大文件上传预留充足时间。',
  axisLabels: 'JUL|AUG|SEP|OCT|NOV', actionLabel: '查看完整通知',
  items: '01|08.30|2026|学校联络员报备|参赛学校提交加盖公章的联络人信息表。\n02|09.30|2026|参赛作品上传截止|24:00 关闭上传通道，请提前完成材料校验。\n03|10.10|2026|校方审核提交截止|学校管理员完成审核推荐与省赛项目提交。\n04|10 月中下旬|2026|专家评审|主办单位组织专家按组别、类别开展复评。\n05|11 月中旬|2026|作品展览|集中呈现优秀设计成果与创新实践。'
};
const v2Timeline = {
  eyebrow: 'CH / 03 · KEY COORDINATES', title: '把握每一个\n关键时间坐标。', summary: '建议学校为内部初评、签章与大文件上传预留时间。', axisLabels: 'JUL|AUG|SEP|OCT|NOV', actionLabel: '',
  items: '01|08.30|2026|联络员报备|提交加盖学校公章的联络人信息表\n02|09.30|2026|作品上传截止|24:00 关闭参赛作品上传通道\n03|10.10|2026|学校审核截止|完成审核推荐与省赛项目提交\n04|10 / MID–LATE|2026|专家评审|按组别和类别开展省级复评\n05|11 / MID|2026|作品展览|集中呈现优秀设计成果与创新实践'
};

const v1Notice = {
  eyebrow: 'NOTICE & DOWNLOADS', title: '通知与报送材料', summary: '', actionLabel: '阅读大赛通知', noticeActionLabel: '查看通知内容', noticeArtMark: '',
  noticeEyebrow: '置顶通知', noticeDate: '2026.07.29', noticeTitle: '关于举办全省高等学校第六届“创意河南”艺术设计大赛的通知',
  noticeSummary: '面向全省普通高等学校在校学生和相关专业教师，设置“创意河南艺术设计”“艺术赋美乡村创意设计”两大赛道。',
  items: '01|参赛作品登记表|学生组、教师组通用\n02|学生组作品汇总表|参赛单位盖章提交\n03|教师组作品汇总表|参赛单位盖章提交\n04|参赛单位联络人信息表|8 月 30 日前提交'
};
const v2Notice = {
  eyebrow: 'OFFICIAL NOTICE / 2026.07.29', title: '准备好创意，\n也准备好每一份材料。', summary: '参赛须由学校统一组织。大赛不接受个人单独申报，活动不收取任何费用。', actionLabel: '阅读通知要点', noticeActionLabel: '', noticeArtMark: 'CH\n2026',
  noticeEyebrow: '', noticeDate: '', noticeTitle: '', noticeSummary: '',
  items: '01|参赛作品登记表|参赛单位统一填报\n02|学生组作品汇总表|参赛单位统一填报\n03|教师组作品汇总表|参赛单位统一填报\n04|参赛单位联络人信息表|8 月 30 日前提交'
};

const v1Contact = {
  eyebrow: '', title: '', summary: '',
  contacts: '活动邮箱|Chuangyihenan@126.com\n承办单位咨询|乔老师 135 2505 1888\n省教育厅咨询|0371-6969 1070\n联系地址|河南师范大学美术学院',
  organizers: ''
};
const v2Contact = {
  eyebrow: '', title: '', summary: '',
  contacts: '活动邮箱|Chuangyihenan@126.com\n承办单位咨询|乔老师 135 2505 1888\n省教育厅咨询|0371-6969 1070\n联系地址|河南师范大学美术学院', organizers: ''
};

const v1Footer = {
  brand: '创意河南', slogan: '设计创新，赋能河南', organizerLabel: '主办单位', organizer: '河南省教育厅',
  contractorLabel: '承办单位', contractors: '河南师范大学美术学院\n郑州美术学院', disclaimer: '页面内容以正式发布通知为准',
  brandMark: 'CH', versionV1Label: '液态玻璃', versionV2Label: '艺术科技', backTopLabel: '返回顶部 ↑', signature: 'CREATIVE HENAN ART & DESIGN COMPETITION 2026'
};
const v2Footer = {
  brand: '创意河南', slogan: '设计创新，赋能河南', organizerLabel: '主办单位', organizer: '河南省教育厅', contractorLabel: '承办单位', contractors: '河南师范大学美术学院\n郑州美术学院', disclaimer: '页面内容以正式发布通知为准',
  brandMark: 'CH', versionV1Label: 'V1 液态玻璃', versionV2Label: 'V2 艺术科技', backTopLabel: 'BACK TO TOP ↑', signature: 'CREATIVE HENAN · ART × TECHNOLOGY · 2026'
};

const titleFields: CompetitionComponentField[] = [text('eyebrow', '英文眉题'), textarea('title', '模块标题'), textarea('summary', '模块说明')];
const versioned = (v1: CompetitionComponentConfig, v2: CompetitionComponentConfig, v1Enabled = true, v2Enabled = true) => ({ defaultConfig: v1, versionDefaultConfigs: { v1, v2 }, versionDefaultEnabled: { v1: v1Enabled, v2: v2Enabled } });

export const competitionComponentRegistry: CompetitionComponentDefinition[] = [
  {
    type: 'competition-nav', label: '顶部导航', description: '品牌、菜单、受控动作和两种弹窗文案', defaultHeight: 1,
    ...versioned(v1Nav, v2Nav),
    fields: [
      text('brand', '品牌名称'), text('slogan', '品牌标语'), text('brandMark', '品牌缩写'), textarea('navItems', '导航菜单', '每行：文字|锚点'),
      text('loginLabel', '登录按钮'), text('actionLabel', '报送按钮'), text('menuOpenLabel', '打开菜单提示'), text('menuCloseLabel', '关闭菜单提示'), { key: 'sticky', label: '吸顶导航', kind: 'switch' },
      text('portalModalEyebrow', '报送弹窗眉题'), text('portalModalTitle', '报送弹窗标题'), textarea('portalModalBody', '报送弹窗正文'), text('portalModalPromptLabel', '报送弹窗提示标题'), textarea('portalModalPrompt', '报送弹窗提示'), text('portalModalButton', '报送弹窗按钮'),
      text('noticeModalEyebrow', '通知弹窗眉题'), text('noticeModalTitle', '通知弹窗标题'), textarea('noticeModalBody', '通知弹窗正文'), textarea('noticeModalRules', '通知弹窗规则', '每行一条规则'), text('noticeModalButton', '通知弹窗按钮'), text('modalCloseLabel', '关闭弹窗提示')
    ]
  },
  {
    type: 'competition-hero', label: '大赛主视觉', description: 'ART、届次、主视觉、浮卡与赛事元信息', defaultHeight: 5,
    ...versioned(v1Hero, v2Hero),
    fields: [
      text('documentTitle', '浏览器页面标题'), text('motto', '主题短句'), text('mottoEn', '主题英文'), text('artText', 'ART 字样'), text('yearText', '年份'), text('editionText', '届次'), text('editionEn', '届次英文'), textarea('competitionTitle', '大赛名称'), textarea('summary', '主视觉说明'),
      text('primaryActionLabel', '主按钮'), text('secondaryActionLabel', '次按钮'), text('organizerLabel', '主办单位标签'), text('organizer', '主办单位'), text('groupsLabel', '参赛组别标签'), text('groups', '参赛组别'), text('feeLabel', '费用标签'), text('fee', '费用说明'),
      text('uploadDeadlineLabel', '截止浮卡标签'), text('uploadDeadline', '截止时间'), text('trackCardLabel', '赛道浮卡标签'), text('trackCardValue', '赛道浮卡内容'), text('statusLabel', '状态浮卡标签'), text('status', '平台状态'), text('axisStart', '主视觉坐标起点'), text('axisEnd', '主视觉坐标终点'), text('axisArt', '主视觉纵轴上文字'), text('axisTech', '主视觉纵轴下文字'), text('artAlt', '主视觉替代文字')
    ]
  },
  {
    type: 'competition-key-facts', label: '倒计时与数据轨', description: '截止倒计时、事项和科技数据轨', defaultHeight: 2,
    ...versioned(v1Facts, v2Facts),
    fields: [
      text('deadlineAt', '倒计时截止时间', 'ISO 8601，例如 2026-10-01T00:00:00+08:00'), text('invalidCountdownValue', '无效截止时间占位'), text('countdownLabel', '倒计时标签'), text('countdownValue', '倒计时数值'), text('countdownSuffix', '倒计时说明'),
      text('fact1Label', '数据 1 标签'), text('fact1Value', '数据 1 内容'), text('fact2Label', '数据 2 标签'), text('fact2Value', '数据 2 内容'),
      text('fact3Label', '数据 3 标签'), text('fact3Value', '数据 3 内容'), text('fact4Label', '数据 4 标签'), text('fact4Value', '数据 4 内容')
    ]
  },
  {
    type: 'competition-tracks', label: '赛道与方向', description: '两条赛道、限额和 12 个方向', defaultHeight: 7,
    ...versioned(v1Tracks, v2Tracks),
    fields: [
      ...titleFields,
      text('trackALabel', '赛道 A 标签'), text('trackAMark', '赛道 A 标记'), text('trackABadge', '赛道 A 数量'), text('trackATitle', '赛道 A 名称'), text('trackAEnglish', '赛道 A 英文说明'), textarea('trackADescription', '赛道 A 说明'), text('trackAQuota', '赛道 A 配额', '数值|说明'), text('trackAAuthorQuota', '赛道 A 作者配额', '数值|说明'), textarea('trackADirections', '赛道 A 方向', '每行：编码|名称|说明'),
      text('trackBLabel', '赛道 B 标签'), text('trackBMark', '赛道 B 标记'), text('trackBBadge', '赛道 B 数量'), text('trackBTitle', '赛道 B 名称'), text('trackBEnglish', '赛道 B 英文说明'), textarea('trackBDescription', '赛道 B 说明'), text('trackBQuota', '赛道 B 配额', '数值|说明'), text('trackBAuthorQuota', '赛道 B 作者配额', '数值|说明'), textarea('trackBDirections', '赛道 B 方向', '每行：编码|名称|说明')
    ]
  },
  { type: 'competition-art-tech', label: '艺术 × 科技', description: '科技宣言与三项艺术科技特征', defaultHeight: 4, ...versioned(v1ArtTech, v2ArtTech, false, true), fields: [...titleFields, textarea('items', '科技特征', '每行：序号|名称|说明')] },
  {
    type: 'competition-journey', label: '报送流程', description: '四步路径、状态与受控按钮', defaultHeight: 5, ...versioned(v1Journey, v2Journey),
    fields: [...titleFields, text('statusLabel', '状态标签'), textarea('status', '状态说明'), textarea('deskTitle', '报送提示标题'), textarea('deskSummary', '报送提示说明'), text('actionLabel', '状态按钮'), text('stepPrefix', 'V2 步骤前缀；留空隐藏'), textarea('items', '流程步骤', '每行：序号|标记|名称|说明')]
  },
  { type: 'competition-file-specs', label: '文件规范', description: '静态、视频和交互作品要求', defaultHeight: 4, ...versioned(v1Files, v2Files), fields: [...titleFields, textarea('items', '文件类型', '每行：序号|类型|规格|说明')] },
  { type: 'competition-timeline', label: '重要时间', description: '时间轴、坐标轴和通知按钮', defaultHeight: 6, ...versioned(v1Timeline, v2Timeline), fields: [...titleFields, text('axisLabels', '时间轴标签', '使用 | 分隔'), text('actionLabel', '通知按钮；留空隐藏'), textarea('items', '时间节点', '每行：序号|日期|年份|名称|说明')] },
  {
    type: 'competition-notice-downloads', label: '通知与附件', description: '通知卡片、附件和受控动作', defaultHeight: 5, ...versioned(v1Notice, v2Notice),
    fields: [...titleFields, text('actionLabel', '通知按钮'), text('noticeActionLabel', '通知卡按钮；留空隐藏'), textarea('noticeArtMark', 'V2 通知图案文字；留空隐藏'), text('noticeEyebrow', '通知眉题'), text('noticeDate', '通知日期'), textarea('noticeTitle', '通知标题'), textarea('noticeSummary', '通知摘要'), textarea('items', '附件列表', '每行：序号|名称|说明')]
  },
  { type: 'competition-contact', label: '联系与机构', description: '联系信息和组织机构', defaultHeight: 3, ...versioned(v1Contact, v2Contact), fields: [...titleFields, textarea('contacts', '联系方式', '每行：名称|内容'), textarea('organizers', '组织机构', '每行一项')] },
  {
    type: 'competition-footer', label: '页脚', description: '组织机构、页脚说明和版本切换', defaultHeight: 2, ...versioned(v1Footer, v2Footer),
    fields: [text('brand', '品牌名称'), text('brandMark', '页脚品牌缩写；留空隐藏'), text('slogan', '品牌标语'), text('organizerLabel', '主办标签'), text('organizer', '主办单位'), text('contractorLabel', '承办标签'), textarea('contractors', '承办单位', '每行一项'), text('disclaimer', '页脚说明'), text('signature', '页脚赛事签名'), text('versionV1Label', 'V1 切换文字'), text('versionV2Label', 'V2 切换文字'), text('backTopLabel', '返回顶部按钮')]
  }
];

export const competitionComponentTypes = competitionComponentRegistry.map((item) => item.type);
export const competitionComponentConfigKeys = Object.fromEntries(competitionComponentRegistry.map((item) => [item.type, item.fields.map((field) => field.key)]));
export const findCompetitionComponent = (type?: string) => competitionComponentRegistry.find((item) => item.type === type);

export const getCompetitionDefaultConfig = (type: string | undefined, version: CompetitionHomeVersion = 'v1'): CompetitionComponentConfig => {
  const definition = findCompetitionComponent(type);
  return { ...(definition?.versionDefaultConfigs[version] || definition?.defaultConfig || {}) };
};

const isCompetitionComponentConfigValue = (value: unknown): value is CompetitionComponentConfigValue => typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean';
const versionForComponent = (component?: PortalHomeComponent): CompetitionHomeVersion => component?.componentKey?.startsWith('v2-') ? 'v2' : 'v1';

export const parseCompetitionComponentConfig = (component?: PortalHomeComponent) => {
  const definition = findCompetitionComponent(component?.componentType);
  const allowedKeys = new Set(definition?.fields.map((field) => field.key) || []);
  let stored: CompetitionComponentConfig = {};
  try {
    const parsed = JSON.parse(component?.configJson || '{}') as unknown;
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
      for (const [key, value] of Object.entries(parsed)) {
        if (allowedKeys.has(key) && isCompetitionComponentConfigValue(value)) stored[key] = value;
      }
    }
  } catch {
    stored = {};
  }
  return { ...getCompetitionDefaultConfig(component?.componentType, versionForComponent(component)), ...stored };
};

export const createCompetitionHomePreset = (version: CompetitionHomeVersion, siteId?: string | number) => {
  let gridY = 0;
  return competitionComponentRegistry.map((definition, index): PortalHomeComponent => {
    const height = definition.defaultHeight;
    const component: PortalHomeComponent = {
      siteId,
      pageCode: 'home',
      componentKey: `${version}-${definition.type.replace('competition-', '')}`,
      componentType: definition.type,
      dataSourceCode: 'manual',
      configJson: JSON.stringify(getCompetitionDefaultConfig(definition.type, version)),
      gridX: 0,
      gridY,
      gridW: 12,
      gridH: height,
      sortOrder: index + 1,
      enabled: definition.versionDefaultEnabled[version]
    };
    gridY += height;
    return component;
  });
};
