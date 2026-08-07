export type CompetitionHomeVersion = "v1" | "v2";

export type CompetitionHomeActionId =
  | "login"
  | "submit"
  | "platform-status"
  | "official-notice"
  | "download-registration"
  | "download-student-summary"
  | "download-teacher-summary"
  | "download-contact";

export interface CompetitionHomeAction {
  id: CompetitionHomeActionId;
  label: string;
}

export interface CompetitionHomeVariantCopy {
  documentTitle: string;
  brandEn: string;
  eyebrow: string;
  heroLines: string[];
  heroSummary: string;
  trackEyebrow: string;
  trackTitle: string[];
  trackSummary: string;
  journeyEyebrow: string;
  journeyTitle: string[];
  fileTitle: string[];
  timelineEyebrow: string;
  timelineTitle: string[];
  noticeEyebrow: string;
  noticeTitle: string[];
}

export interface CompetitionDirection {
  code: string;
  title: string;
  description: string;
}

export interface CompetitionTrack {
  id: "track-a" | "track-b";
  badge: string;
  shortMark: string;
  english: string;
  title: string;
  description: string;
  perSchoolLimit: string;
  authorLimit: string;
  directions: CompetitionDirection[];
}

export interface CompetitionHomeContent {
  brand: string;
  edition: string;
  theme: string;
  organizer: string;
  groups: string;
  fee: string;
  status: string;
  uploadDeadline: string;
  variants: Record<CompetitionHomeVersion, CompetitionHomeVariantCopy>;
  tracks: CompetitionTrack[];
  artTechFeatures: Array<{ index: string; title: string; description: string }>;
  steps: Array<{ index: string; mark: string; title: string; description: string }>;
  fileSpecs: Array<{ index: string; label: string; value: string; description: string }>;
  milestones: Array<{ index: string; date: string; year: string; title: string; description: string }>;
  notice: { date: string; title: string; summary: string };
  downloads: Array<{ actionId: CompetitionHomeActionId; index: string; title: string; description: string }>;
  contacts: Array<{ label: string; value: string }>;
  contractors: string[];
  disclaimer: string;
}

export const futureCompetitionHomeIntegrations = [
  { actionId: "login", todo: "TODO(PORTAL-004): 接入统一登录入口并由服务端返回可用角色。" },
  { actionId: "submit", todo: "TODO(PORTAL-004): 接入活动开放状态与作品报送入口。" },
  { actionId: "platform-status", todo: "TODO(PORTAL-004): 接入公开活动状态只读接口。" },
  { actionId: "official-notice", todo: "TODO(PORTAL-004): 接入 CMS 已发布通知详情。" },
  { actionId: "download-*", todo: "TODO(PORTAL-004): 接入受控文件标识和授权下载接口。" }
] as const;

const defaultCompetitionHomeContent: CompetitionHomeContent = {
  brand: "创意河南",
  edition: "第六届 · 2026",
  theme: "设计创新，赋能河南",
  organizer: "河南省教育厅",
  groups: "学生组 · 教师组",
  fee: "公益赛事 · 免报名费",
  status: "平台筹备中",
  uploadDeadline: "9 月 30 日 · 24:00",
  variants: {
    v1: {
      documentTitle: "创意河南｜第六届全省高校艺术设计大赛",
      brandEn: "CREATIVE HENAN",
      eyebrow: "THE 6TH · CREATIVE HENAN · 2026",
      heroLines: ["让创意，", "为河南发生。"],
      heroSummary: "全省高等学校第六届“创意河南”艺术设计大赛，面向全省高校学生与教师征集优秀设计成果。",
      trackEyebrow: "COMPETITION TRACKS",
      trackTitle: ["两条赛道，让好设计", "真正抵达河南。"],
      trackSummary: "从专业设计创新，到艺术赋能乡村；让创意不仅被看见，更成为推动文化、空间、产业与生活更新的真实力量。",
      journeyEyebrow: "SUBMISSION JOURNEY",
      journeyTitle: ["从校级初评，", "到省级舞台。"],
      fileTitle: ["准备好作品，", "也要准备好正确的文件。"],
      timelineEyebrow: "KEY DATES",
      timelineTitle: ["重要时间", "一目了然。"],
      noticeEyebrow: "NOTICE & DOWNLOADS",
      noticeTitle: ["通知与", "报送材料"]
    },
    v2: {
      documentTitle: "创意河南｜艺术 × 科技明亮版",
      brandEn: "ART × TECHNOLOGY",
      eyebrow: "CH / 2026 · ART × TECHNOLOGY",
      heroLines: ["创意，", "为想象安装", "未来。"],
      heroSummary: "全省高等学校第六届“创意河南”艺术设计大赛，让艺术感知与科技能力在河南相遇。",
      trackEyebrow: "CH / 01 · TRACK LAB",
      trackTitle: ["艺术是感知，", "科技是新的画笔。"],
      trackSummary: "从专业设计创新，到艺术赋能乡村；让作品不仅停留在屏幕上，也可以进入空间、产业、生活与未来。",
      journeyEyebrow: "CH / 02 · SUBMISSION PATH",
      journeyTitle: ["让作品沿着正确路径，", "进入省级舞台。"],
      fileTitle: ["作品很大胆，", "文件必须很精确。"],
      timelineEyebrow: "CH / 03 · KEY COORDINATES",
      timelineTitle: ["把握每一个", "关键时间坐标。"],
      noticeEyebrow: "OFFICIAL NOTICE / 2026.07.29",
      noticeTitle: ["准备好创意，", "也准备好每一份材料。"]
    }
  },
  tracks: [
    {
      id: "track-a",
      badge: "赛道 A · 7 个类别",
      shortMark: "创",
      english: "DESIGN FOR HENAN",
      title: "创意河南艺术设计",
      description: "体现“创意河南”新质发展理念，凸显创新性、引领性与服务性，推动优秀成果从创意走向应用。",
      perSchoolLimit: "≤ 20 件（套）/ 每校每组",
      authorLimit: "≤ 3 人 / 每件作品",
      directions: [
        { code: "V01", title: "视觉传达", description: "品牌、IP、海报、包装、书籍、字体、插画与绘本" },
        { code: "S02", title: "环境设计", description: "景观、建筑、规划、室内、展陈、公共艺术与家具" },
        { code: "P03", title: "工业设计", description: "产品设计与面向未来生活的概念设计" },
        { code: "D04", title: "数媒综合", description: "动画、影像、交互、VR / AR、元宇宙与 AIGC" },
        { code: "F05", title: "服装服饰", description: "服装、服饰、鞋帽包饰与时尚设计" },
        { code: "C06", title: "文旅文创", description: "文化传承、地域文创与旅游产品开发" },
        { code: "T07", title: "艺术与科技", description: "光影、传感交互、数字雕塑与跨媒体实验" }
      ]
    },
    {
      id: "track-b",
      badge: "赛道 B · 5 个方向",
      shortMark: "乡",
      english: "ART FOR RURAL REVITALIZATION",
      title: "艺术赋美乡村创意设计",
      description: "聚焦乡村文化振兴、人居环境提质、特色产业升级与数字文旅创新，征集兼具艺术性、创新性、实用性与落地性的作品。",
      perSchoolLimit: "≤ 5 件（套）/ 每校每组",
      authorLimit: "≤ 3 人 / 每件作品",
      directions: [
        { code: "R01", title: "乡村记忆留存", description: "以插画、纪实视觉与文创设计留住乡土文化基因" },
        { code: "R02", title: "乡村空间提质", description: "低成本、可持续地改造农房、院落与公共文化空间" },
        { code: "R03", title: "乡村特产升级", description: "用包装、品牌 VI 与营销视觉提升特色产品价值" },
        { code: "R04", title: "乡村文创业态", description: "开发乡土文创、民俗衍生品与轻量化庭院业态" },
        { code: "R05", title: "乡村数字传播", description: "通过数字绘画、动漫、影像、VR 与乡村 IP 创新传播" }
      ]
    }
  ],
  artTechFeatures: [
    { index: "01", title: "AIGC 创意设计", description: "探索人工智能与视觉叙事、审美表达的协同创作。" },
    { index: "02", title: "沉浸与交互", description: "以传感、光影、AR 与实时数据构建可参与的作品。" },
    { index: "03", title: "跨媒体实验", description: "让数字雕塑、生物材料与空间装置发生新的连接。" }
  ],
  steps: [
    { index: "01", mark: "校", title: "确定学校联络员", description: "每校确定 1 名联络员兼系统管理员，统一对接报名与作品报送。" },
    { index: "02", mark: "号", title: "领取学校账号", description: "提交联络员信息后，由组委会向学校分发登录账号与密码。" },
    { index: "03", mark: "传", title: "参赛者上传作品", description: "师生完成平台注册，按类别要求上传作品、海报与登记材料。" },
    { index: "04", mark: "审", title: "校方审核提交", description: "学校管理员审核推荐作品，并在 10 月 10 日前完成省赛提交。" }
  ],
  fileSpecs: [
    { index: "01", label: "静态作品", value: "90 × 120 cm", description: "竖幅 · 300 DPI · RGB · JPG · 单幅 ≤ 10MB" },
    { index: "02", label: "视频作品", value: "MP4 · 1080P", description: "≤ 500MB · 时长 ≤ 5 分钟 · 画面不得含个人或单位信息" },
    { index: "03", label: "交互 / 科技", value: "二维码 + 海报", description: "APP / 元宇宙作品另附可执行包与功能演示视频" }
  ],
  milestones: [
    { index: "01", date: "08.30", year: "2026", title: "学校联络员报备", description: "参赛学校提交加盖公章的联络人信息表。" },
    { index: "02", date: "09.30", year: "2026", title: "参赛作品上传截止", description: "24:00 关闭上传通道，请提前完成材料校验。" },
    { index: "03", date: "10.10", year: "2026", title: "校方审核提交截止", description: "学校管理员完成审核推荐与省赛项目提交。" },
    { index: "04", date: "10 月中下旬", year: "2026", title: "专家评审", description: "主办单位组织专家按组别、类别开展复评。" },
    { index: "05", date: "11 月中旬", year: "2026", title: "作品展览", description: "集中呈现优秀设计成果与创新实践。" }
  ],
  notice: {
    date: "2026.07.29",
    title: "关于举办全省高等学校第六届“创意河南”艺术设计大赛的通知",
    summary: "面向全省普通高等学校在校学生和相关专业教师，设置“创意河南艺术设计”“艺术赋美乡村创意设计”两大赛道。"
  },
  downloads: [
    { actionId: "download-registration", index: "01", title: "参赛作品登记表", description: "学生组、教师组通用" },
    { actionId: "download-student-summary", index: "02", title: "学生组作品汇总表", description: "参赛单位盖章提交" },
    { actionId: "download-teacher-summary", index: "03", title: "教师组作品汇总表", description: "参赛单位盖章提交" },
    { actionId: "download-contact", index: "04", title: "参赛单位联络人信息表", description: "8 月 30 日前提交" }
  ],
  contacts: [
    { label: "活动邮箱", value: "Chuangyihenan@126.com" },
    { label: "承办单位咨询", value: "乔老师 135 2505 1888" },
    { label: "省教育厅咨询", value: "0371-6969 1070" },
    { label: "联系地址", value: "河南师范大学美术学院" }
  ],
  contractors: ["河南师范大学美术学院", "郑州美术学院"],
  disclaimer: "页面内容以正式发布通知为准"
};

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

function isText(value: unknown): value is string {
  return typeof value === "string" && value.trim().length > 0;
}

function isVariantCopy(value: unknown): value is CompetitionHomeVariantCopy {
  if (!isRecord(value)) return false;
  return (
    [
      "documentTitle",
      "brandEn",
      "eyebrow",
      "heroSummary",
      "trackEyebrow",
      "trackSummary",
      "journeyEyebrow",
      "timelineEyebrow",
      "noticeEyebrow"
    ].every((key) => isText(value[key])) &&
    ["heroLines", "trackTitle", "journeyTitle", "fileTitle", "timelineTitle", "noticeTitle"].every(
      (key) => Array.isArray(value[key]) && (value[key] as unknown[]).length > 0 && (value[key] as unknown[]).every(isText)
    )
  );
}

function isDirection(value: unknown): value is CompetitionDirection {
  return isRecord(value) && isText(value.code) && isText(value.title) && isText(value.description);
}

function isTrack(value: unknown): value is CompetitionTrack {
  return (
    isRecord(value) &&
    (value.id === "track-a" || value.id === "track-b") &&
    ["badge", "shortMark", "english", "title", "description", "perSchoolLimit", "authorLimit"].every((key) =>
      isText(value[key])
    ) &&
    Array.isArray(value.directions) &&
    value.directions.length > 0 &&
    value.directions.every(isDirection)
  );
}

function hasTextFields(value: unknown, fields: string[]) {
  return isRecord(value) && fields.every((field) => isText(value[field]));
}

function isCompetitionHomeContent(value: unknown): value is CompetitionHomeContent {
  if (!isRecord(value)) return false;
  const candidate = value as Record<string, unknown>;
  const tracks = candidate.tracks;
  const variants = candidate.variants;
  return (
    ["brand", "edition", "theme", "organizer", "groups", "fee", "status", "uploadDeadline", "disclaimer"].every(
      (key) => isText(candidate[key])
    ) &&
    isRecord(variants) &&
    isVariantCopy(variants.v1) &&
    isVariantCopy(variants.v2) &&
    Array.isArray(tracks) &&
    tracks.length === 2 &&
    tracks.every(isTrack) &&
    tracks.reduce((total, track) => total + track.directions.length, 0) === 12 &&
    Array.isArray(candidate.artTechFeatures) &&
    candidate.artTechFeatures.length === 3 &&
    candidate.artTechFeatures.every((item) => hasTextFields(item, ["index", "title", "description"])) &&
    Array.isArray(candidate.steps) &&
    candidate.steps.length === 4 &&
    candidate.steps.every((item) => hasTextFields(item, ["index", "mark", "title", "description"])) &&
    Array.isArray(candidate.fileSpecs) &&
    candidate.fileSpecs.length === 3 &&
    candidate.fileSpecs.every((item) => hasTextFields(item, ["index", "label", "value", "description"])) &&
    Array.isArray(candidate.milestones) &&
    candidate.milestones.length === 5 &&
    candidate.milestones.every((item) => hasTextFields(item, ["index", "date", "year", "title", "description"])) &&
    hasTextFields(candidate.notice, ["date", "title", "summary"]) &&
    Array.isArray(candidate.downloads) &&
    candidate.downloads.length === 4 &&
    candidate.downloads.every((item) => hasTextFields(item, ["actionId", "index", "title", "description"])) &&
    Array.isArray(candidate.contacts) &&
    candidate.contacts.length > 0 &&
    candidate.contacts.every((item) => hasTextFields(item, ["label", "value"])) &&
    Array.isArray(candidate.contractors) &&
    candidate.contractors.length > 0 &&
    candidate.contractors.every(isText)
  );
}

const runtimeCompetitionHome = (
  typeof window === "undefined"
    ? undefined
    : (window.CREHN_PORTAL_CONFIG as unknown as { competitionHome?: unknown } | undefined)?.competitionHome
);

if (runtimeCompetitionHome !== undefined && !isCompetitionHomeContent(runtimeCompetitionHome)) {
  console.warn("Ignoring invalid competitionHome runtime config; using the built-in PORTAL-004 content.");
}

export const competitionHomeContent = isCompetitionHomeContent(runtimeCompetitionHome)
  ? runtimeCompetitionHome
  : defaultCompetitionHomeContent;
