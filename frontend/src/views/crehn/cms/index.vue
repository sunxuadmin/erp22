<template>
  <div class="p-2">
    <el-card class="mb-2" shadow="never">
      <div class="cms-toolbar">
        <el-select v-model="siteId" placeholder="选择门户站点" style="width: 260px" @change="changeSite">
          <el-option v-for="site in sites" :key="String(site.id)" :label="site.siteName" :value="site.id" />
        </el-select>
        <el-button v-hasPermi="['crehn:cms:site:edit']" @click="editSite()">新增站点</el-button>
        <el-button v-if="currentSite" v-hasPermi="['crehn:cms:site:edit']" @click="editSite(currentSite)">编辑站点</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-empty v-if="!siteId" description="请先新建或选择门户站点" />
      <el-tabs v-else v-model="activeTab">
        <el-tab-pane label="栏目" name="channel">
          <el-button v-hasPermi="['crehn:cms:channel:edit']" type="primary" class="mb-2" @click="editChannel()">新增栏目</el-button>
          <el-table :data="channels" border>
            <el-table-column label="栏目编码" prop="channelCode" />
            <el-table-column label="栏目名称" prop="channelName" />
            <el-table-column label="导航位置" prop="navigationPosition" />
            <el-table-column label="排序" prop="sortOrder" width="90" />
            <el-table-column label="可见范围" prop="visibility" width="110" />
            <el-table-column label="操作" width="90">
              <template #default="{ row }"><el-button link type="primary" @click="editChannel(row)">编辑</el-button></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="文章" name="article">
          <div class="cms-toolbar mb-2">
            <el-button v-hasPermi="['crehn:cms:article:edit']" type="primary" @click="editArticle()">新增文章</el-button>
            <el-select v-model="articleQuery.status" clearable placeholder="全部状态" @change="loadArticles">
              <el-option v-for="status in articleStatuses" :key="status" :label="status" :value="status" />
            </el-select>
          </div>
          <el-table :data="articles" border>
            <el-table-column label="标题" prop="title" min-width="220" />
            <el-table-column label="栏目" width="150">
              <template #default="{ row }">{{ channelName(row.channelId) }}</template>
            </el-table-column>
            <el-table-column label="状态" prop="status" width="120" />
            <el-table-column label="发布时间" prop="publishedAt" width="170" />
            <el-table-column label="操作" min-width="260">
              <template #default="{ row }">
                <el-button v-if="['DRAFT', 'OFFLINE'].includes(row.status)" link type="primary" @click="editArticle(row)">编辑</el-button>
                <el-button v-if="row.status === 'DRAFT'" link type="warning" @click="submitReview(row)">送审</el-button>
                <el-button v-if="row.status === 'IN_REVIEW'" link type="success" @click="openPublish(row)">发布</el-button>
                <el-button v-if="['PUBLISHED', 'SCHEDULED'].includes(row.status)" link type="danger" @click="offline(row)">下线</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="媒体" name="media">
          <el-button v-hasPermi="['crehn:cms:media:edit']" type="primary" class="mb-2" @click="editMedia()">登记媒体</el-button>
          <el-table :data="mediaRows" border>
            <el-table-column label="标题" prop="title" />
            <el-table-column label="类型" prop="mediaType" width="100" />
            <el-table-column label="OSS ID" prop="ossId" width="130" />
            <el-table-column label="内容SHA-256" prop="contentSha256" min-width="260" show-overflow-tooltip />
            <el-table-column label="复核状态" prop="scanStatus" width="110" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button
                  v-if="row.scanStatus === 'pending' || row.scanStatus === 'rejected'"
                  v-hasPermi="['crehn:cms:article:publish']"
                  link
                  type="success"
                  @click="approveMedia(row)"
                >
                  人工复核通过
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="首页组件" name="home">
          <div class="cms-toolbar mb-2">
            <el-button v-hasPermi="['crehn:cms:home:edit']" type="primary" @click="editComponent()">新增组件</el-button>
            <el-button v-hasPermi="['crehn:cms:home:publish']" type="success" @click="publishHome">发布当前首页版本</el-button>
          </div>
          <div ref="homeGridRef" class="component-grid grid-stack">
            <div
              v-for="component in components"
              :key="String(component.id)"
              class="grid-stack-item"
              :gs-id="String(component.id)"
              :gs-x="component.gridX || 0"
              :gs-y="component.gridY || 0"
              :gs-w="Math.min(component.gridW || 12, 12)"
              :gs-h="component.gridH || 1"
            >
              <div class="grid-stack-item-content component-tile" @click="editComponent(component)">
                <small>{{ component.componentType }} / {{ component.dataSourceCode }}</small>
                <strong>{{ componentTitle(component) }}</strong>
                <span>{{ component.gridW || 12 }}/12 栅格</span>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="主题与布局" name="layout">
          <el-alert
            v-if="!canEditHome"
            class="mb-2"
            type="warning"
            show-icon
            :closable="false"
            title="当前账号没有首页布局编辑权限，需要增加权限，请先在角色权限管理中增加权限"
          />
          <div class="cms-toolbar mb-2">
            <el-button v-hasPermi="['crehn:cms:home:edit']" type="primary" @click="editLayout()">新增布局</el-button>
            <el-button v-hasPermi="['crehn:cms:home:edit']" :disabled="!selectedLayout" @click="editLayout(selectedLayout)">编辑当前布局</el-button>
            <el-button v-hasPermi="['crehn:cms:home:edit']" :disabled="!selectedLayout" @click="activateSelectedLayout">切换为当前布局</el-button>
            <el-button v-hasPermi="['crehn:cms:home:publish']" type="success" :disabled="!selectedLayout" @click="publishHome">发布当前布局</el-button>
          </div>
          <el-table :data="layouts" border highlight-current-row @current-change="selectLayout">
            <el-table-column label="布局名称" prop="layoutName" min-width="180" />
            <el-table-column label="布局编码" prop="layoutCode" width="160" />
            <el-table-column label="视觉版本" prop="renderVersion" width="110" />
            <el-table-column label="主题" width="130">
              <template #default="{ row }">{{ themeSummary(row) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.active" type="success">当前草稿</el-tag>
                <el-tag v-else type="info">已保存</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220">
              <template #default="{ row }">
                <el-button v-hasPermi="['crehn:cms:home:edit']" link type="primary" @click="editLayout(row)">编辑</el-button>
                <el-button v-hasPermi="['crehn:cms:home:edit']" link type="success" :disabled="row.active" @click="activateLayout(row)">切换</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!layouts.length" description="尚未保存布局，可从当前首页组件创建布局" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="siteVisible" title="门户站点" width="560px">
      <el-form label-width="90px">
        <el-form-item label="站点编码"><el-input v-model="siteForm.siteCode" /></el-form-item>
        <el-form-item label="站点名称"><el-input v-model="siteForm.siteName" /></el-form-item>
        <el-form-item label="绑定域名"><el-input v-model="siteForm.domainName" /></el-form-item>
        <el-form-item label="备案号"><el-input v-model="siteForm.filingNo" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="siteVisible = false">取消</el-button><el-button type="primary" @click="submitSite">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="channelVisible" title="门户栏目" width="560px">
      <el-form label-width="90px">
        <el-form-item label="栏目编码"><el-input v-model="channelForm.channelCode" /></el-form-item>
        <el-form-item label="栏目名称"><el-input v-model="channelForm.channelName" /></el-form-item>
        <el-form-item label="上级栏目">
          <el-select v-model="channelForm.parentId" clearable>
            <el-option label="顶级栏目" :value="0" />
            <el-option v-for="item in channels" :key="String(item.id)" :label="item.channelName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="channelForm.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="channelVisible = false">取消</el-button><el-button type="primary" @click="submitChannel">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="articleVisible" title="门户文章" width="760px">
      <el-form label-width="90px">
        <el-form-item label="栏目"><el-select v-model="articleForm.channelId"><el-option v-for="item in channels" :key="String(item.id)" :label="item.channelName" :value="item.id" /></el-select></el-form-item>
        <el-form-item label="标题"><el-input v-model="articleForm.title" maxlength="255" /></el-form-item>
        <el-form-item label="摘要"><el-input v-model="articleForm.summary" type="textarea" maxlength="1000" show-word-limit /></el-form-item>
        <el-form-item label="正文"><el-input v-model="articleForm.contentMarkdown" type="textarea" :rows="14" placeholder="使用 Markdown 编写，不接受任意 HTML" /></el-form-item>
        <el-form-item label="可见范围"><el-select v-model="articleForm.visibility"><el-option label="公开" value="public" /><el-option label="登录可见" value="login" /><el-option label="学校可见" value="school" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="articleVisible = false">取消</el-button><el-button type="primary" @click="submitArticle">保存草稿</el-button></template>
    </el-dialog>

    <el-dialog v-model="mediaVisible" title="登记门户媒体" width="560px">
      <el-form label-width="90px">
        <el-form-item label="上传文件"><file-upload v-model="mediaOssId" :limit="1" /></el-form-item>
        <el-form-item label="媒体类型"><el-select v-model="mediaForm.mediaType"><el-option label="图片" value="image" /><el-option label="视频" value="video" /><el-option label="音频" value="audio" /><el-option label="文档" value="document" /></el-select></el-form-item>
        <el-form-item label="标题"><el-input v-model="mediaForm.title" /></el-form-item>
        <el-form-item label="替代文本"><el-input v-model="mediaForm.altText" /></el-form-item>
        <el-form-item label="版权方"><el-input v-model="mediaForm.copyrightOwner" /></el-form-item>
        <el-form-item label="来源"><el-input v-model="mediaForm.sourceName" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="mediaVisible = false">取消</el-button><el-button type="primary" @click="submitMedia">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="componentVisible" title="首页组件" width="620px">
      <el-form label-width="100px">
        <el-form-item label="组件标识"><el-input v-model="componentForm.componentKey" /></el-form-item>
        <el-form-item label="组件类型"><el-select v-model="componentForm.componentType"><el-option v-for="item in componentTypes" :key="item" :label="item" :value="item" /></el-select></el-form-item>
        <el-form-item label="数据源"><el-select v-model="componentForm.dataSourceCode"><el-option v-for="item in dataSources" :key="item" :label="item" :value="item" /></el-select></el-form-item>
        <el-form-item label="栅格宽度"><el-slider v-model="componentForm.gridW" :min="1" :max="12" show-input /></el-form-item>
        <el-form-item label="组件配置"><el-input v-model="componentForm.configJson" type="textarea" :rows="6" placeholder='JSON对象，例如 {"title":"通知公告"}' /></el-form-item>
      </el-form>
      <template #footer><el-button @click="componentVisible = false">取消</el-button><el-button type="primary" @click="submitComponent">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="layoutVisible" title="门户主题与布局" width="720px">
      <el-form label-width="100px">
        <el-form-item label="布局编码"><el-input v-model="layoutForm.layoutCode" maxlength="64" /></el-form-item>
        <el-form-item label="布局名称"><el-input v-model="layoutForm.layoutName" maxlength="128" /></el-form-item>
        <el-form-item label="视觉版本">
          <el-select v-model="layoutForm.renderVersion">
            <el-option label="参考稿版" value="v1" />
            <el-option label="竖卡展厅版" value="v2" />
          </el-select>
        </el-form-item>
        <el-form-item label="主题预设">
          <el-select v-model="selectedThemePreset" @change="applyThemePreset">
            <el-option v-for="preset in themePresets" :key="preset.id" :label="preset.label" :value="preset.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="页面起色"><el-color-picker v-model="themeDraft.pageBgStart" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="页面中色"><el-color-picker v-model="themeDraft.pageBgMiddle" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="页面终色"><el-color-picker v-model="themeDraft.pageBgEnd" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="主标题色"><el-color-picker v-model="themeDraft.heroTitle" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="强调色"><el-color-picker v-model="themeDraft.heroAccent" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="卡片标题"><el-color-picker v-model="themeDraft.cardTitle" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="玻璃透明度"><el-slider v-model="themeDraft.glassCardOpacity" :min="0.08" :max="0.96" :step="0.01" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="玻璃模糊"><el-slider v-model="themeDraft.glassBlur" :min="6" :max="32" :step="1" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="页脚字号"><el-slider v-model="themeDraft.footerFontSize" :min="8" :max="18" :step="1" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="玻璃高光"><el-slider v-model="themeDraft.glassHighlightOpacity" :min="0.2" :max="1" :step="0.01" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="玻璃阴影"><el-slider v-model="themeDraft.glassShadowOpacity" :min="0" :max="0.4" :step="0.01" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="粒子透明度"><el-slider v-model="themeDraft.particleOpacity" :min="0" :max="0.6" :step="0.01" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="粒子大小"><el-slider v-model="themeDraft.particleSize" :min="1" :max="6" :step="0.5" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="粒子光晕"><el-slider v-model="themeDraft.particleGlow" :min="0" :max="24" :step="1" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="渲染策略"><el-tag type="success">CSS 合成动画</el-tag></el-form-item></el-col>
        </el-row>
        <el-alert type="info" :closable="false" title="本布局会保存当前首页组件的受控快照；发布后公共门户才会切换，未发布草稿不会被匿名访问。" />
      </el-form>
      <template #footer>
        <el-button @click="layoutVisible = false">取消</el-button>
        <el-button v-hasPermi="['crehn:cms:home:edit']" type="primary" @click="submitLayout">保存布局</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="publishVisible" title="发布文章" width="520px">
      <el-form label-width="90px">
        <el-form-item label="定时发布"><el-date-picker v-model="publishForm.scheduledAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" clearable /></el-form-item>
        <el-form-item label="发布原因"><el-input v-model="publishForm.reason" type="textarea" maxlength="500" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="publishVisible = false">取消</el-button><el-button type="primary" @click="submitPublish">确认发布</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { GridStack } from 'gridstack';
import 'gridstack/dist/gridstack.min.css';
import {
  activatePortalPageLayout,
  approvePortalMedia,
  listPortalArticles,
  listPortalChannels,
  listPortalHomeComponents,
  listPortalPageLayouts,
  listPortalMedia,
  listPortalSites,
  offlinePortalArticle,
  publishPortalArticle,
  publishPortalHome,
  publishPortalHomeLayout,
  savePortalArticle,
  savePortalChannel,
  savePortalHomeComponent,
  savePortalPageLayout,
  savePortalMedia,
  savePortalSite,
  submitPortalArticleReview,
  type PortalArticle,
  type PortalChannel,
  type PortalHomeComponent,
  type PortalMediaAsset,
  type PortalPageLayout,
  type PortalSite
} from '@/api/crehn/cms';
import { checkPermi } from '@/utils/permission';
import { clonePortalLayoutComponents, createPortalLayoutComponentDraft, parsePortalLayoutComponents } from './portalLayoutDraft';

const activeTab = ref('channel');
const sites = ref<PortalSite[]>([]);
const siteId = ref<string | number>();
const channels = ref<PortalChannel[]>([]);
const articles = ref<PortalArticle[]>([]);
const mediaRows = ref<PortalMediaAsset[]>([]);
const components = ref<PortalHomeComponent[]>([]);
const homeGridRef = ref<HTMLElement>();
let homeGrid: GridStack | undefined;
const layouts = ref<PortalPageLayout[]>([]);
const selectedLayout = ref<PortalPageLayout>();
const articleQuery = reactive({ status: '' });
const articleStatuses = ['DRAFT', 'IN_REVIEW', 'SCHEDULED', 'PUBLISHED', 'OFFLINE'];
const componentTypes = ['hero', 'news', 'notice', 'activity', 'schedule', 'media', 'stat', 'showcase', 'links'];
const dataSources = ['manual', 'public_articles', 'public_notices', 'public_activity', 'public_results', 'public_stats'];
const siteVisible = ref(false);
const channelVisible = ref(false);
const articleVisible = ref(false);
const mediaVisible = ref(false);
const componentVisible = ref(false);
const layoutVisible = ref(false);
const publishVisible = ref(false);
const mediaOssId = ref('');
const siteForm = reactive<PortalSite>({});
const channelForm = reactive<PortalChannel>({});
const articleForm = reactive<PortalArticle>({});
const mediaForm = reactive<PortalMediaAsset>({});
const componentForm = reactive<PortalHomeComponent>({});
const layoutForm = reactive<PortalPageLayout>({});
const layoutComponentDraft = ref<PortalHomeComponent[]>([]);
const publishForm = reactive({ articleId: '' as string | number, scheduledAt: '', reason: '' });
const selectedThemePreset = ref('fresh-blue');
const themeDraft = reactive({
  pageBgStart: '#edf9fb',
  pageBgMiddle: '#edf4ff',
  pageBgEnd: '#eef2ff',
  heroTitle: '#079fe2',
  heroAccent: '#5748d8',
  cardTitle: '#0b326b',
  glassCardOpacity: 0.72,
  glassBlur: 6,
  footerFontSize: 10,
  glassHighlightOpacity: 0.92,
  glassShadowOpacity: 0.12,
  particleOpacity: 0.24,
  particleSize: 3,
  particleGlow: 10
});
const themePresets = [
  {
    id: 'fresh-blue',
    label: '清爽蓝绿',
    values: { pageBgStart: '#edf9fb', pageBgMiddle: '#edf4ff', pageBgEnd: '#eef2ff', heroTitle: '#079fe2', heroAccent: '#5748d8', cardTitle: '#0b326b', glassCardOpacity: 0.72, glassBlur: 6, footerFontSize: 10, glassHighlightOpacity: 0.92, glassShadowOpacity: 0.12, particleOpacity: 0.24, particleSize: 3, particleGlow: 10 }
  },
  {
    id: 'ink-showcase',
    label: '深色展厅',
    values: { pageBgStart: '#101827', pageBgMiddle: '#172942', pageBgEnd: '#2d1f42', heroTitle: '#73d7ff', heroAccent: '#c19cff', cardTitle: '#eaf5ff', glassCardOpacity: 0.38, glassBlur: 14, footerFontSize: 10, glassHighlightOpacity: 0.72, glassShadowOpacity: 0.26, particleOpacity: 0.34, particleSize: 3.5, particleGlow: 16 }
  },
  {
    id: 'paper-warm',
    label: '暖白纸张',
    values: { pageBgStart: '#fffaf0', pageBgMiddle: '#fff5e8', pageBgEnd: '#f4ecff', heroTitle: '#c85d3b', heroAccent: '#8a4fbb', cardTitle: '#4b3041', glassCardOpacity: 0.86, glassBlur: 6, footerFontSize: 11, glassHighlightOpacity: 0.98, glassShadowOpacity: 0.08, particleOpacity: 0.18, particleSize: 2.5, particleGlow: 8 }
  }
];
const currentSite = computed(() => sites.value.find((item) => String(item.id) === String(siteId.value)));
const canEditHome = computed(() => checkPermi(['crehn:cms:home:edit']));

const replaceForm = <T extends object>(target: T, source: Partial<T>) => {
  Object.keys(target).forEach((key) => delete (target as any)[key]);
  Object.assign(target, source);
};

const loadSites = async () => {
  const response: any = await listPortalSites();
  sites.value = response.data || [];
  if (!siteId.value && sites.value.length) siteId.value = sites.value[0].id;
  if (siteId.value) await changeSite();
};

const changeSite = async () => {
  if (!siteId.value) return;
  const [channelResponse, mediaResponse, componentResponse, layoutResponse]: any[] = await Promise.all([
    listPortalChannels(siteId.value),
    listPortalMedia(siteId.value),
    listPortalHomeComponents(siteId.value),
    listPortalPageLayouts(siteId.value)
  ]);
  channels.value = channelResponse.data || [];
  mediaRows.value = mediaResponse.data || [];
  components.value = componentResponse.data || [];
  layouts.value = layoutResponse.data || [];
  const activeLayout = layouts.value.find((item) => item.active);
  selectedLayout.value = activeLayout || layouts.value[0];
  if (!activeLayout || !(await applyLayoutComponents(activeLayout))) {
    await nextTick();
    refreshHomeGrid();
  }
  await loadArticles();
};

const refreshHomeGrid = () => {
  if (!homeGridRef.value) return;
  homeGrid?.destroy(false);
  homeGrid = GridStack.init(
    {
      column: 12,
      float: true,
      margin: 8,
      cellHeight: 72,
      draggable: { handle: '.component-tile' },
      resizable: { handles: 'e,se,s,sw,w' }
    },
    homeGridRef.value
  );
  homeGrid.on('change', (_event, nodes) => {
    nodes.forEach((node) => {
      const target = components.value.find((item) => String(item.id) === String(node.id));
      if (!target) return;
      Object.assign(target, {
        gridX: node.x ?? target.gridX ?? 0,
        gridY: node.y ?? target.gridY ?? 0,
        gridW: node.w ?? target.gridW ?? 12,
        gridH: node.h ?? target.gridH ?? 1
      });
    });
  });
};

const loadArticles = async () => {
  if (!siteId.value) return;
  const response: any = await listPortalArticles({ siteId: siteId.value, status: articleQuery.status, pageNum: 1, pageSize: 100 });
  articles.value = response.rows || [];
};

const editSite = (row?: PortalSite) => {
  replaceForm(siteForm, row ? { ...row } : { status: 'enabled' });
  siteVisible.value = true;
};
const submitSite = async () => {
  const response: any = await savePortalSite(siteForm);
  siteVisible.value = false;
  await loadSites();
  siteId.value = response.data.id;
};
const editChannel = (row?: PortalChannel) => {
  replaceForm(channelForm, row ? { ...row } : { siteId: siteId.value, parentId: 0, sortOrder: 0, visibility: 'public', status: 'enabled' });
  channelVisible.value = true;
};
const submitChannel = async () => {
  await savePortalChannel(channelForm);
  channelVisible.value = false;
  await changeSite();
};
const editArticle = (row?: PortalArticle) => {
  replaceForm(articleForm, row ? { ...row } : { siteId: siteId.value, visibility: 'public', pinned: false });
  articleVisible.value = true;
};
const submitArticle = async () => {
  await savePortalArticle(articleForm);
  articleVisible.value = false;
  await loadArticles();
};
const submitReview = async (row: PortalArticle) => {
  await submitPortalArticleReview(row.id!);
  await loadArticles();
};
const openPublish = (row: PortalArticle) => {
  Object.assign(publishForm, { articleId: row.id!, scheduledAt: '', reason: '' });
  publishVisible.value = true;
};
const submitPublish = async () => {
  if (!publishForm.reason.trim()) return ElMessage.warning('发布原因不能为空');
  await publishPortalArticle({ articleId: publishForm.articleId, scheduledAt: publishForm.scheduledAt || undefined, reason: publishForm.reason });
  publishVisible.value = false;
  await loadArticles();
};
const offline = async (row: PortalArticle) => {
  const { value } = await ElMessageBox.prompt('请输入下线原因', '下线文章', { inputType: 'textarea', inputValidator: (value) => !!value?.trim() || '原因不能为空' });
  await offlinePortalArticle({ articleId: row.id!, reason: value.trim() });
  await loadArticles();
};
const editMedia = () => {
  mediaOssId.value = '';
  replaceForm(mediaForm, { siteId: siteId.value, mediaType: 'image' });
  mediaVisible.value = true;
};
const submitMedia = async () => {
  const ossId = mediaOssId.value.split(',').filter(Boolean)[0];
  if (!ossId) return ElMessage.warning('请先上传媒体文件');
  await savePortalMedia({ ...mediaForm, ossId });
  mediaVisible.value = false;
  await changeSite();
};
const approveMedia = async (row: PortalMediaAsset) => {
  const { value } = await ElMessageBox.prompt('请输入人工复核结论或依据', '媒体人工复核', {
    inputType: 'textarea',
    inputValidator: (value) => !!value?.trim() || '复核原因不能为空'
  });
  await approvePortalMedia(row.id!, value.trim());
  await changeSite();
};
const editComponent = (row?: PortalHomeComponent) => {
  replaceForm(componentForm, row ? { ...row } : { siteId: siteId.value, pageCode: 'home', componentType: 'news', dataSourceCode: 'public_articles', gridW: 12, gridH: 1, enabled: true, configJson: '{}' });
  componentVisible.value = true;
};
const submitComponent = async () => {
  JSON.parse(componentForm.configJson || '{}');
  await savePortalHomeComponent(componentForm);
  componentVisible.value = false;
  await changeSite();
};
const applyThemePreset = (presetId = selectedThemePreset.value) => {
  const preset = themePresets.find((item) => item.id === presetId);
  if (preset) Object.assign(themeDraft, preset.values);
};
const parseTheme = (themeJson?: string) => {
  if (!themeJson) return;
  try {
    const parsed = JSON.parse(themeJson) as Record<string, unknown>;
    Object.keys(themeDraft).forEach((key) => {
      if (key in parsed) (themeDraft as any)[key] = parsed[key];
    });
  } catch {
    applyThemePreset();
  }
};
const themeSummary = (layout: PortalPageLayout) => {
  try {
    const theme = JSON.parse(layout.themeJson || '{}') as Record<string, unknown>;
    return String(theme.heroTitle || '默认主题');
  } catch {
    return '默认主题';
  }
};
const applyLayoutComponents = async (layout?: PortalPageLayout) => {
  const parsed = parsePortalLayoutComponents(layout?.componentJson);
  if (!parsed) {
    ElMessage.warning('布局组件快照无效，已保留当前组件');
    return false;
  }
  components.value = clonePortalLayoutComponents(parsed);
  await nextTick();
  refreshHomeGrid();
  return true;
};
const selectLayout = (layout?: PortalPageLayout) => {
  if (layout) selectedLayout.value = layout;
};
const editLayout = (layout?: PortalPageLayout) => {
  const componentDraft = createPortalLayoutComponentDraft(layout, components.value);
  if (!componentDraft) {
    ElMessage.error('当前布局组件快照无效，已阻止覆盖保存');
    return;
  }
  layoutComponentDraft.value = componentDraft;
  const source = layout
    ? { ...layout }
    : {
        siteId: siteId.value,
        pageCode: 'home',
        layoutCode: `layout-${layouts.value.length + 1}`,
        layoutName: `创意河南布局 ${layouts.value.length + 1}`,
        renderVersion: 'v1',
        sortOrder: layouts.value.length + 1,
        enabled: true,
        active: false
      };
  replaceForm(layoutForm, source);
  applyThemePreset('fresh-blue');
  parseTheme(layout?.themeJson);
  layoutVisible.value = true;
};
const submitLayout = async () => {
  if (!layoutForm.layoutCode?.trim() || !layoutForm.layoutName?.trim()) {
    return ElMessage.warning('布局编码和布局名称不能为空');
  }
  await savePortalPageLayout({
    ...layoutForm,
    siteId: siteId.value,
    pageCode: 'home',
    themeJson: JSON.stringify(themeDraft),
    componentJson: JSON.stringify(layoutComponentDraft.value)
  });
  layoutVisible.value = false;
  await changeSite();
  ElMessage.success('布局已保存，发布后公共门户才会切换');
};
const activateLayout = async (layout: PortalPageLayout) => {
  if (!layout.layoutCode) return;
  await activatePortalPageLayout(siteId.value!, layout.layoutCode);
  await changeSite();
  ElMessage.success(`已切换草稿布局：${layout.layoutName || layout.layoutCode}`);
};
const activateSelectedLayout = () => {
  if (selectedLayout.value) void activateLayout(selectedLayout.value);
};
const componentTitle = (component: PortalHomeComponent) => {
  return JSON.parse(component.configJson || '{}').title || component.componentKey;
};
const publishHome = async () => {
  const { value } = await ElMessageBox.prompt('请输入本次首页发布原因', '发布首页版本', { inputType: 'textarea', inputValidator: (value) => !!value?.trim() || '原因不能为空' });
  if (selectedLayout.value?.layoutCode) {
    await publishPortalHomeLayout(siteId.value!, selectedLayout.value.layoutCode, value.trim());
  } else {
    await publishPortalHome(siteId.value!, value.trim());
  }
  ElMessage.success('首页版本发布成功');
};
const channelName = (id?: string | number) => channels.value.find((item) => String(item.id) === String(id))?.channelName || id || '-';

onMounted(loadSites);
onBeforeUnmount(() => homeGrid?.destroy(false));
</script>

<style scoped>
.cms-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.component-grid {
  min-height: 260px;
  padding: 16px;
  border: 1px dashed var(--el-border-color);
  border-radius: 10px;
  background: var(--el-fill-color-lighter);
}
.component-tile {
  height: 100%;
  display: grid;
  gap: 8px;
  align-content: center;
  padding: 14px;
  border: 1px solid var(--el-border-color);
  border-radius: 10px;
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
  text-align: left;
  cursor: grab;
}
.component-tile small,
.component-tile span {
  color: var(--el-text-color-secondary);
}
</style>
