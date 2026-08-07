import { competitionHomeContent, type CompetitionHomeVersion } from "../../config/competitionHome";
import type { CompetitionHomeModule } from "../../config/competitionHomeModules";

type ModuleConfig = Record<string, string | number | boolean>;

const lineRows = <T>(items: T[], keys: Array<keyof T>) => items
  .map((item) => keys.map((key) => String(item[key])).join("|"))
  .join("\n");

function fallbackConfigs(version: CompetitionHomeVersion): Record<string, ModuleConfig> {
  const isV2 = version === "v2";
  const variant = competitionHomeContent.variants[version];
  return {
    "competition-nav": {
      brand: competitionHomeContent.brand,
      slogan: "设计创新 · 赋能河南",
      brandMark: "CH",
      navItems: isV2 ? "首页|top\n赛道实验室|tracks\n报送路径|journey\n时间坐标|timeline\n通知|notice" : "大赛首页|top\n赛道设置|tracks\n报送流程|journey\n时间安排|timeline\n通知公告|notice",
      loginLabel: "登录",
      actionLabel: "作品报送",
      menuOpenLabel: "打开菜单",
      menuCloseLabel: "关闭菜单",
      sticky: true,
      portalModalEyebrow: isV2 ? "SUBMISSION SYSTEM" : "SUBMISSION PLATFORM",
      portalModalTitle: "作品报送平台正在筹备中",
      portalModalBody: isV2 ? "学校联络员信息提交后，组委会将分发学校登录账号。正式通道开放后，参赛者可完成注册和作品上传。" : "学校联络员信息提交后，组委会将分发学校登录账号。正式上传通道开放后，参赛者可完成注册与作品上传。",
      portalModalPromptLabel: isV2 ? "当前请先完成" : "请先完成",
      portalModalPrompt: "8 月 30 日前提交加盖学校公章的联络人信息表",
      portalModalButton: "联系组委会",
      noticeModalEyebrow: "OFFICIAL NOTICE",
      noticeModalTitle: "大赛通知要点",
      noticeModalBody: isV2 ? "本届主题为“设计创新，赋能河南”，面向全省普通高校在校学生和相关专业教师。" : "本届主题为“设计创新，赋能河南”，面向全省普通高校在校学生和相关专业教师，按组别、类别分别评奖。",
      noticeModalRules: "作品须为 2026 年 1 月 1 日以后设计的原创作品。\n同一作品只能选投一个赛道、一个类别。\n作品正面不得出现作者姓名和单位信息。\n活动不收取任何费用。",
      noticeModalButton: isV2 ? "查看时间坐标" : "查看时间安排",
      modalCloseLabel: "关闭"
    },
    "competition-hero": {
      documentTitle: isV2 ? "创意河南｜艺术 × 科技明亮版" : "创意河南｜第六届全省高校艺术设计大赛",
      motto: "设计创新 · 赋能河南",
      mottoEn: "CREATIVE HENAN",
      artText: "ART",
      yearText: "2026",
      editionText: "全省高等学校第六届",
      editionEn: isV2 ? "THE 6TH" : "",
      competitionTitle: "“创意河南”\n艺术设计大赛",
      summary: isV2 ? "全省高等学校第六届“创意河南”艺术设计大赛，以“设计创新，赋能河南”为主题，让艺术感知与科技能力在河南相遇。" : "全省高等学校第六届“创意河南”艺术设计大赛，以“设计创新，赋能河南”为主题，面向全省高校学生与教师征集优秀设计成果。",
      primaryActionLabel: isV2 ? "启动作品报送" : "进入作品报送",
      secondaryActionLabel: isV2 ? "探索竞赛赛道" : "查看大赛通知",
      organizerLabel: "主办单位",
      organizer: competitionHomeContent.organizer,
      groupsLabel: isV2 ? "参赛对象" : "参赛组别",
      groups: isV2 ? "全省高校学生与教师" : "学生组 / 教师组",
      feeLabel: isV2 ? "" : "参赛费用",
      fee: isV2 ? "" : competitionHomeContent.fee,
      uploadDeadlineLabel: isV2 ? "UPLOAD WINDOW" : "作品上传截止",
      uploadDeadline: competitionHomeContent.uploadDeadline,
      trackCardLabel: isV2 ? "SYSTEM / OPEN" : "本届设置",
      trackCardValue: isV2 ? "2 TRACKS\n12 CREATIVE DIRECTIONS" : "2 大赛道 · 12 个方向",
      statusLabel: isV2 ? "" : "报送状态",
      status: isV2 ? "" : competitionHomeContent.status,
      axisStart: "01",
      axisEnd: "12",
      axisArt: "ART",
      axisTech: "TECH",
      artAlt: isV2 ? "透明虹彩玻璃、参数化波纹与数据光点组成的艺术科技雕塑" : "紫、金、青、蓝与珊瑚色玻璃丝带组成的创意河南抽象艺术雕塑"
    },
    "competition-key-facts": {
      deadlineAt: "2026-10-01T00:00:00+08:00",
      invalidCountdownValue: "—",
      countdownLabel: isV2 ? "DAYS" : "天后关闭",
      countdownValue: "",
      countdownSuffix: isV2 ? "" : "作品上传通道",
      fact1Label: isV2 ? "01 / THEME" : "01\n学校联络员信息",
      fact1Value: isV2 ? competitionHomeContent.theme : "8 月 30 日前",
      fact2Label: isV2 ? "02 / GROUPS" : "02\n参赛作品上传",
      fact2Value: isV2 ? "学生组 · 教师组" : "9 月 30 日截止",
      fact3Label: isV2 ? "03 / DEADLINE" : "03\n校方审核提交",
      fact3Value: isV2 ? "作品上传 09.30" : "10 月 10 日截止",
      fact4Label: isV2 ? "04 / STATUS" : "",
      fact4Value: isV2 ? competitionHomeContent.status : ""
    },
    "competition-tracks": {
      eyebrow: isV2 ? "CH / 01 · TRACK LAB" : "COMPETITION TRACKS",
      title: isV2 ? "艺术是感知，\n科技是新的画笔。" : "两条赛道，让好设计\n真正抵达河南。",
      summary: isV2 ? "从专业设计创新，到艺术赋能乡村；让作品不仅停留在屏幕上，也可以进入空间、产业、生活与未来。" : "从专业设计创新，到艺术赋能乡村；让创意不仅被看见，更成为推动文化、空间、产业与生活更新的真实力量。",
      trackALabel: isV2 ? "TRACK A" : "赛道 A",
      trackAMark: isV2 ? "A" : "创",
      trackABadge: isV2 ? "07 MODULES" : "7 个类别",
      trackATitle: "创意河南艺术设计",
      trackAEnglish: "DESIGN FOR HENAN",
      trackADescription: isV2 ? "体现“创意河南”新质发展理念，凸显创新性、引领性与服务性，推动优秀成果从创意走向应用。" : "体现“创意河南”新质发展理念，凸显创新性、引领性与服务性，注重设计成果的市场转化与实际应用。",
      trackAQuota: isV2 ? "≤ 20|每校每组|件（套）" : "≤ 20|每校每组限额|件（套）",
      trackAAuthorQuota: isV2 ? "≤ 3|每件设计者|人" : "≤ 3|每件作品设计者|人",
      trackADirections: isV2 ? "V01|视觉传达|品牌 / IP / 海报 / 包装 / 字体 / 插画\nS02|环境设计|景观 / 建筑 / 室内 / 展陈 / 公共艺术\nP03|工业设计|产品设计 / 概念设计 / 未来生活\nD04|数媒综合|动画 / 影像 / VR·AR / AIGC / 交互\nF05|服装服饰|服装 / 服饰 / 鞋帽包饰 / 时尚设计\nC06|文旅文创|地域文化 / 文创产品 / 旅游开发\nT07|艺术与科技|光影 / 传感 / 数字雕塑 / 跨媒体实验" : "01|视觉传达|品牌、IP、海报、包装、书籍、字体、插画与绘本\n02|环境设计|景观、建筑、规划、室内、展陈、公共艺术与家具\n03|工业设计|产品设计与面向未来生活的概念设计\n04|数媒综合|动画、影像、交互、VR / AR、元宇宙与 AIGC\n05|服装服饰|服装、服饰、鞋帽包饰与时尚设计\n06|文旅文创|文化传承、地域文创与旅游产品开发\n07|艺术与科技|光影、传感交互、数字雕塑与跨媒体实验",
      trackBLabel: isV2 ? "TRACK B" : "赛道 B",
      trackBMark: isV2 ? "B" : "乡",
      trackBBadge: isV2 ? "05 MODULES" : "5 个方向",
      trackBTitle: "艺术赋美乡村创意设计",
      trackBEnglish: "ART FOR RURAL REVITALIZATION",
      trackBDescription: isV2 ? "聚焦文化振兴、空间提质、产业升级与数字文旅创新，强调艺术性、实用性与真正的落地价值。" : "聚焦乡村文化振兴、人居环境提质、特色产业升级与数字文旅创新，征集兼具艺术性、创新性、实用性与落地性的作品。",
      trackBQuota: isV2 ? "≤ 5|每校每组|件（套）" : "≤ 5|每校每组限额|件（套）",
      trackBAuthorQuota: isV2 ? "≤ 3|每件设计者|人" : "≤ 3|每件作品设计者|人",
      trackBDirections: isV2 ? "R01|乡村记忆留存|让乡土文化与村落记忆被重新看见\nR02|乡村空间提质|用低成本、可持续设计激活公共空间\nR03|乡村特产升级|以品牌与包装提升特色产品价值\nR04|乡村文创业态|把乡土资源转化为可落地的新业态\nR05|乡村数字传播|用数字艺术、VR 与乡村 IP 创新传播" : "01|乡村记忆留存|以插画、纪实视觉与文创设计留住乡土文化基因\n02|乡村空间提质|低成本、可持续地改造农房、院落与公共文化空间\n03|乡村特产升级|用包装、品牌 VI 与营销视觉提升特色产品价值\n04|乡村文创业态|开发乡土文创、民俗衍生品与轻量化庭院业态\n05|乡村数字传播|通过数字绘画、动漫、影像、VR 与乡村 IP 创新传播"
    },
    "competition-art-tech": {
      eyebrow: "ART × TECHNOLOGY",
      title: "当创意连接算法、材料与空间，\n设计开始拥有新的感官。",
      summary: isV2 ? "" : "让感知、算法与材料互相启发，作品在屏幕之外继续生长。",
      items: lineRows(competitionHomeContent.artTechFeatures, ["index", "title", "description"])
    },
    "competition-journey": {
      eyebrow: isV2 ? "CH / 02 · SUBMISSION PATH" : "SUBMISSION JOURNEY",
      title: isV2 ? "让作品沿着正确路径，\n进入省级舞台。" : "从校级初评，\n到省级舞台。",
      summary: isV2 ? "" : "大赛不接受个人单独申报。请由学校统一组织、审核并按限额推荐。",
      statusLabel: isV2 ? "" : "报送平台筹备中",
      status: "",
      deskTitle: isV2 ? "" : "准备好作品，也要准备好正确的文件。",
      deskSummary: isV2 ? "" : "请先核对版面尺寸、格式、大小、匿名要求与签章材料，避免在截止前集中返工。",
      actionLabel: isV2 ? "查看平台状态" : "查看报送状态",
      stepPrefix: isV2 ? "STEP" : "",
      items: isV2 ? "01|校|确定学校联络员|每校确定 1 名联络员兼系统管理员，统一对接报名与报送工作。\n02|号|领取学校账号|提交联络员信息后，由组委会向学校分发登录账号和密码。\n03|传|参赛者上传作品|师生完成注册，并按类别要求上传作品、海报和登记材料。\n04|审|学校审核提交|学校管理员审核推荐作品，完成省赛项目提交。" : lineRows(competitionHomeContent.steps, ["index", "mark", "title", "description"])
    },
    "competition-file-specs": {
      eyebrow: isV2 ? "FILE SPECIFICATION LAB" : "FILE SPECIFICATION",
      title: isV2 ? "作品很大胆，\n文件必须很精确。" : "准备好作品，\n也要准备好正确的文件。",
      summary: isV2 ? "上传前请完成格式、尺寸、大小、匿名与签章材料校验。" : "上传前请再次核对尺寸、格式、大小和匿名要求。",
      items: isV2 ? "01|STATIC|90 × 120 cm|竖幅 · 300 DPI · RGB · JPG\u2028单幅作品 ≤ 10MB\n02|VIDEO|MP4 · 1080P|≤ 500MB · 时长 ≤ 5 分钟\u2028画面不得含个人或单位信息\n03|INTERACTIVE|二维码 + 海报|APP / 元宇宙另附可执行包\u2028与功能演示视频" : lineRows(competitionHomeContent.fileSpecs, ["index", "label", "value", "description"])
    },
    "competition-timeline": {
      eyebrow: isV2 ? "CH / 03 · KEY COORDINATES" : "KEY DATES",
      title: isV2 ? "把握每一个\n关键时间坐标。" : "重要时间\n一目了然。",
      summary: isV2 ? "建议学校为内部初评、签章与大文件上传预留时间。" : "建议各学校为内部初评、材料签章与大文件上传预留充足时间。",
      axisLabels: "JUL|AUG|SEP|OCT|NOV",
      actionLabel: isV2 ? "" : "查看完整通知",
      items: isV2 ? "01|08.30|2026|联络员报备|提交加盖学校公章的联络人信息表\n02|09.30|2026|作品上传截止|24:00 关闭参赛作品上传通道\n03|10.10|2026|学校审核截止|完成审核推荐与省赛项目提交\n04|10 / MID–LATE|2026|专家评审|按组别和类别开展省级复评\n05|11 / MID|2026|作品展览|集中呈现优秀设计成果与创新实践" : lineRows(competitionHomeContent.milestones, ["index", "date", "year", "title", "description"])
    },
    "competition-notice-downloads": {
      eyebrow: isV2 ? "OFFICIAL NOTICE / 2026.07.29" : "NOTICE & DOWNLOADS",
      title: isV2 ? "准备好创意，\n也准备好每一份材料。" : "通知与\n报送材料",
      summary: isV2 ? "参赛须由学校统一组织。大赛不接受个人单独申报，活动不收取任何费用。" : "活动不收取任何费用，页面内容以正式发布通知为准。",
      actionLabel: isV2 ? "阅读通知要点" : "阅读大赛通知",
      noticeActionLabel: isV2 ? "" : "查看通知内容",
      noticeArtMark: isV2 ? "CH\n2026" : "",
      noticeEyebrow: isV2 ? "" : "置顶通知",
      noticeDate: isV2 ? "" : competitionHomeContent.notice.date,
      noticeTitle: isV2 ? "" : competitionHomeContent.notice.title,
      noticeSummary: isV2 ? "" : competitionHomeContent.notice.summary,
      items: isV2 ? "01|参赛作品登记表|参赛单位统一填报\n02|学生组作品汇总表|参赛单位统一填报\n03|教师组作品汇总表|参赛单位统一填报\n04|参赛单位联络人信息表|8 月 30 日前提交" : lineRows(competitionHomeContent.downloads, ["index", "title", "description"])
    },
    "competition-contact": {
      eyebrow: "",
      title: "",
      summary: "",
      contacts: lineRows(competitionHomeContent.contacts, ["label", "value"]),
      organizers: ""
    },
    "competition-footer": {
      brand: competitionHomeContent.brand,
      slogan: isV2 ? "设计创新 · 赋能河南" : "设计创新，赋能河南",
      organizerLabel: "主办单位",
      organizer: competitionHomeContent.organizer,
      contractorLabel: "承办单位",
      contractors: competitionHomeContent.contractors.join("\n"),
      disclaimer: competitionHomeContent.disclaimer,
      brandMark: "CH",
      versionV1Label: isV2 ? "V1 液态玻璃" : "液态玻璃",
      versionV2Label: isV2 ? "V2 艺术科技" : "艺术科技",
      backTopLabel: isV2 ? "BACK TO TOP ↑" : "返回顶部 ↑",
      signature: isV2 ? "CREATIVE HENAN · ART × TECHNOLOGY · 2026" : "CREATIVE HENAN ART & DESIGN COMPETITION 2026"
    }
  };
}

const componentOrder = [
  "competition-nav", "competition-hero", "competition-key-facts", "competition-tracks", "competition-art-tech",
  "competition-journey", "competition-file-specs", "competition-timeline", "competition-notice-downloads", "competition-contact", "competition-footer"
];

const componentHeights = [1, 5, 2, 7, 4, 5, 4, 6, 5, 3, 2];

export function createCompetitionHomeFallback(version: CompetitionHomeVersion): CompetitionHomeModule[] {
  const configs = fallbackConfigs(version);
  const isV2 = version === "v2";
  let gridY = 0;
  return componentOrder.filter((componentType) => isV2 || componentType !== "competition-art-tech").map((componentType, index) => {
    const gridH = componentHeights[componentOrder.indexOf(componentType)] || 1;
    const module: CompetitionHomeModule = {
      componentKey: `${version}-fallback-${componentType.replace("competition-", "")}`,
      componentType,
      config: configs[componentType] || {},
      gridX: 0,
      gridY,
      gridW: 12,
      gridH,
      sortOrder: index + 1
    };
    gridY += gridH;
    return module;
  });
}
