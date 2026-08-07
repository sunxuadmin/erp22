import type { PortalHomeComponent } from '@/api/crehn/cms';

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
  fields: CompetitionComponentField[];
  defaultHeight: number;
}

const titleFields: CompetitionComponentField[] = [
  { key: 'eyebrow', label: '英文眉题', kind: 'text' },
  { key: 'title', label: '模块标题', kind: 'textarea' },
  { key: 'summary', label: '模块说明', kind: 'textarea' }
];

export const competitionComponentRegistry: CompetitionComponentDefinition[] = [
  {
    type: 'competition-nav',
    label: '顶部导航',
    description: '品牌、届次、锚点导航与报送入口',
    defaultHeight: 1,
    defaultConfig: { brand: '创意河南', edition: '第六届 · 2026', actionLabel: '作品报送', sticky: true },
    fields: [
      { key: 'brand', label: '品牌名称', kind: 'text' },
      { key: 'edition', label: '届次', kind: 'text' },
      { key: 'actionLabel', label: '主按钮文字', kind: 'text' },
      { key: 'sticky', label: '吸顶导航', kind: 'switch' }
    ]
  },
  {
    type: 'competition-hero',
    label: '大赛主视觉',
    description: '首屏主题、说明、状态与艺术雕塑',
    defaultHeight: 5,
    defaultConfig: {
      eyebrow: 'THE 6TH · CREATIVE HENAN · 2026',
      title: '让创意，\n为河南发生。',
      summary: '全省高等学校第六届“创意河南”艺术设计大赛，面向全省高校学生与教师征集优秀设计成果。',
      primaryActionLabel: '进入作品报送',
      secondaryActionLabel: '查看大赛通知',
      status: '平台筹备中',
      visualStyle: 'sculpture',
      organizer: '河南省教育厅',
      groups: '学生组 · 教师组',
      fee: '公益赛事 · 免报名费',
      uploadDeadline: '9 月 30 日 · 24:00'
    },
    fields: [
      ...titleFields,
      { key: 'primaryActionLabel', label: '主按钮', kind: 'text' },
      { key: 'secondaryActionLabel', label: '次按钮', kind: 'text' },
      { key: 'status', label: '平台状态', kind: 'text' },
      { key: 'organizer', label: '主办单位', kind: 'text' },
      { key: 'groups', label: '参赛组别', kind: 'text' },
      { key: 'fee', label: '费用说明', kind: 'text' },
      { key: 'uploadDeadline', label: '上传截止', kind: 'text' },
      {
        key: 'visualStyle',
        label: '主视觉样式',
        kind: 'select',
        options: [
          { label: '参数化雕塑', value: 'sculpture' },
          { label: '液态玻璃环', value: 'liquid-ring' }
        ]
      }
    ]
  },
  {
    type: 'competition-key-facts',
    label: '关键数据',
    description: '组别、截止时间、赛道和方向统计',
    defaultHeight: 2,
    defaultConfig: { title: '赛事关键数据', item1: '2 条赛道', item2: '12 个作品方向', item3: '学生组 · 教师组', item4: '9 月 30 日截止' },
    fields: [
      { key: 'title', label: '辅助标题', kind: 'text' },
      { key: 'item1', label: '数据 1', kind: 'text' },
      { key: 'item2', label: '数据 2', kind: 'text' },
      { key: 'item3', label: '数据 3', kind: 'text' },
      { key: 'item4', label: '数据 4', kind: 'text' }
    ]
  },
  {
    type: 'competition-tracks',
    label: '赛道与方向',
    description: '两条赛道切换及 12 个作品方向',
    defaultHeight: 7,
    defaultConfig: {
      eyebrow: 'COMPETITION TRACKS',
      title: '两条赛道，让好设计\n真正抵达河南。',
      summary: '从专业设计创新，到艺术赋能乡村，让创意成为推动河南发展的真实力量。',
      showLimits: true,
      trackATitle: '创意河南艺术设计',
      trackADescription: '体现“创意河南”新质发展理念，推动优秀成果从创意走向应用。',
      trackADirections:
        'V01|视觉传达|品牌、IP、海报、包装、书籍、字体、插画与绘本\nS02|环境设计|景观、建筑、规划、室内、展陈、公共艺术与家具\nP03|工业设计|产品设计与面向未来生活的概念设计\nD04|数媒综合|动画、影像、交互、VR / AR、元宇宙与 AIGC\nF05|服装服饰|服装、服饰、鞋帽包饰与时尚设计\nC06|文旅文创|文化传承、地域文创与旅游产品开发\nT07|艺术与科技|光影、传感交互、数字雕塑与跨媒体实验',
      trackBTitle: '艺术赋美乡村创意设计',
      trackBDescription: '聚焦乡村文化振兴、人居环境提质、特色产业升级与数字文旅创新。',
      trackBDirections:
        'R01|乡村记忆留存|以插画、纪实视觉与文创设计留住乡土文化基因\nR02|乡村空间提质|低成本、可持续地改造农房、院落与公共文化空间\nR03|乡村特产升级|用包装、品牌 VI 与营销视觉提升特色产品价值\nR04|乡村文创业态|开发乡土文创、民俗衍生品与轻量化庭院业态\nR05|乡村数字传播|通过数字绘画、动漫、影像、VR 与乡村 IP 创新传播'
    },
    fields: [
      ...titleFields,
      { key: 'showLimits', label: '显示报送限额', kind: 'switch' },
      { key: 'trackATitle', label: '赛道 A 名称', kind: 'text' },
      { key: 'trackADescription', label: '赛道 A 说明', kind: 'textarea' },
      { key: 'trackADirections', label: '赛道 A 方向', kind: 'textarea', placeholder: '每行：编码|名称|说明' },
      { key: 'trackBTitle', label: '赛道 B 名称', kind: 'text' },
      { key: 'trackBDescription', label: '赛道 B 说明', kind: 'textarea' },
      { key: 'trackBDirections', label: '赛道 B 方向', kind: 'textarea', placeholder: '每行：编码|名称|说明' }
    ]
  },
  {
    type: 'competition-art-tech',
    label: '艺术 × 科技',
    description: 'AIGC、沉浸交互与跨媒体实验',
    defaultHeight: 4,
    defaultConfig: {
      eyebrow: 'ART × TECHNOLOGY',
      title: '当创意连接算法、材料与空间',
      summary: '设计开始拥有新的感官。',
      items:
        '01|AIGC 创意设计|探索人工智能与视觉叙事、审美表达的协同创作。\n02|沉浸与交互|以传感、光影、AR 与实时数据构建可参与的作品。\n03|跨媒体实验|让数字雕塑、生物材料与空间装置发生新的连接。'
    },
    fields: [...titleFields, { key: 'items', label: '科技特征', kind: 'textarea', placeholder: '每行：序号|名称|说明' }]
  },
  {
    type: 'competition-journey',
    label: '报送流程',
    description: '从校级初评到省级舞台的四步流程',
    defaultHeight: 5,
    defaultConfig: {
      eyebrow: 'SUBMISSION JOURNEY',
      title: '从校级初评，\n到省级舞台。',
      summary: '按学校组织、参赛者填报、校方审核的顺序完成报送。',
      items:
        '01|校|确定学校联络员|每校确定 1 名联络员兼系统管理员。\n02|号|领取学校账号|由组委会向学校分发登录账号与密码。\n03|传|参赛者上传作品|师生按类别要求上传作品和登记材料。\n04|审|校方审核提交|学校管理员审核推荐作品并完成省赛提交。'
    },
    fields: [...titleFields, { key: 'items', label: '流程步骤', kind: 'textarea', placeholder: '每行：序号|标记|名称|说明' }]
  },
  {
    type: 'competition-file-specs',
    label: '文件规范',
    description: '静态、视频和交互作品格式要求',
    defaultHeight: 4,
    defaultConfig: {
      eyebrow: 'FILE SPECIFICATION',
      title: '准备好作品，\n也要准备好正确的文件。',
      summary: '上传前请再次核对尺寸、格式、大小和匿名要求。',
      actionLabel: '查看报送规范',
      items:
        '01|静态作品|90 × 120 cm|竖幅 · 300 DPI · RGB · JPG · 单幅 ≤ 10MB\n02|视频作品|MP4 · 1080P|≤ 500MB · 时长 ≤ 5 分钟\n03|交互 / 科技|二维码 + 海报|另附可执行包与功能演示视频'
    },
    fields: [
      ...titleFields,
      { key: 'actionLabel', label: '按钮文字', kind: 'text' },
      { key: 'items', label: '文件类型', kind: 'textarea', placeholder: '每行：序号|类型|规格|说明' }
    ]
  },
  {
    type: 'competition-timeline',
    label: '重要时间',
    description: '联络员、上传、审核、评审和展览节点',
    defaultHeight: 6,
    defaultConfig: {
      eyebrow: 'KEY DATES',
      title: '重要时间\n一目了然。',
      summary: '请为审核、修改和材料盖章预留时间。',
      items:
        '01|08.30|2026|学校联络员报备|提交联络人信息表。\n02|09.30|2026|参赛作品上传截止|24:00 关闭上传通道。\n03|10.10|2026|校方审核提交截止|完成审核推荐与省赛提交。\n04|10 月中下旬|2026|专家评审|按组别、类别开展复评。\n05|11 月中旬|2026|作品展览|集中呈现优秀设计成果。'
    },
    fields: [...titleFields, { key: 'items', label: '时间节点', kind: 'textarea', placeholder: '每行：序号|日期|年份|名称|说明' }]
  },
  {
    type: 'competition-notice-downloads',
    label: '通知与附件',
    description: '正式通知说明和四项材料入口',
    defaultHeight: 5,
    defaultConfig: {
      eyebrow: 'NOTICE & DOWNLOADS',
      title: '通知与\n报送材料',
      summary: '附件尚未接入时统一显示平台筹备中。',
      actionLabel: '阅读大赛通知',
      noticeDate: '2026.07.29',
      noticeTitle: '关于举办全省高等学校第六届“创意河南”艺术设计大赛的通知',
      noticeSummary: '面向全省普通高等学校在校学生和相关专业教师，设置两大赛道。',
      items:
        '01|参赛作品登记表|学生组、教师组通用\n02|学生组作品汇总表|参赛单位盖章提交\n03|教师组作品汇总表|参赛单位盖章提交\n04|参赛单位联络人信息表|8 月 30 日前提交'
    },
    fields: [
      ...titleFields,
      { key: 'actionLabel', label: '通知按钮', kind: 'text' },
      { key: 'noticeDate', label: '通知日期', kind: 'text' },
      { key: 'noticeTitle', label: '通知标题', kind: 'textarea' },
      { key: 'noticeSummary', label: '通知摘要', kind: 'textarea' },
      { key: 'items', label: '附件列表', kind: 'textarea', placeholder: '每行：序号|名称|说明' }
    ]
  },
  {
    type: 'competition-contact',
    label: '联系与机构',
    description: '联系方式、主办承办单位及声明',
    defaultHeight: 3,
    defaultConfig: {
      eyebrow: 'CONTACT',
      title: '联系组委会',
      summary: '页面内容以正式发布通知为准。',
      showOrganizers: true,
      contacts: '活动邮箱|Chuangyihenan@126.com\n承办单位咨询|乔老师 135 2505 1888\n省教育厅咨询|0371-6969 1070\n联系地址|河南师范大学美术学院',
      organizers: '主办：河南省教育厅\n承办：河南师范大学美术学院、郑州美术学院'
    },
    fields: [
      ...titleFields,
      { key: 'showOrganizers', label: '显示组织机构', kind: 'switch' },
      { key: 'contacts', label: '联系方式', kind: 'textarea', placeholder: '每行：名称|内容' },
      { key: 'organizers', label: '组织机构', kind: 'textarea', placeholder: '每行一项' }
    ]
  },
  {
    type: 'competition-footer',
    label: '页脚',
    description: '品牌、组织机构和版权说明',
    defaultHeight: 2,
    defaultConfig: { brand: '创意河南', slogan: '创意，让河南发生。', copyright: 'CREATIVE HENAN · 2026' },
    fields: [
      { key: 'brand', label: '品牌名称', kind: 'text' },
      { key: 'slogan', label: '品牌标语', kind: 'text' },
      { key: 'copyright', label: '版权文字', kind: 'text' }
    ]
  }
];

export const competitionComponentTypes = competitionComponentRegistry.map((item) => item.type);

export const findCompetitionComponent = (type?: string) => competitionComponentRegistry.find((item) => item.type === type);

const isCompetitionComponentConfigValue = (value: unknown): value is CompetitionComponentConfigValue => {
  return typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean';
};

export const parseCompetitionComponentConfig = (component?: PortalHomeComponent) => {
  const definition = findCompetitionComponent(component?.componentType);
  let stored: CompetitionComponentConfig = {};
  try {
    const parsed = JSON.parse(component?.configJson || '{}') as unknown;
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
      for (const [key, value] of Object.entries(parsed)) {
        if (isCompetitionComponentConfigValue(value)) stored[key] = value;
      }
    }
  } catch {
    stored = {};
  }
  return { ...(definition?.defaultConfig || {}), ...stored };
};

export const createCompetitionHomePreset = (version: 'v1' | 'v2', siteId?: string | number) => {
  let gridY = 0;
  return competitionComponentRegistry.map((definition, index): PortalHomeComponent => {
    const height = definition.defaultHeight;
    const component: PortalHomeComponent = {
      siteId,
      pageCode: 'home',
      componentKey: `${version}-${definition.type.replace('competition-', '')}`,
      componentType: definition.type,
      dataSourceCode: 'manual',
      configJson: JSON.stringify(definition.defaultConfig),
      gridX: 0,
      gridY,
      gridW: 12,
      gridH: height,
      sortOrder: index + 1,
      enabled: true
    };
    gridY += height;
    return component;
  });
};
