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
          <div class="component-grid">
            <button
              v-for="component in components"
              :key="String(component.id)"
              class="component-tile"
              :style="{ gridColumn: `span ${Math.min(component.gridW || 12, 12)}` }"
              draggable="true"
              @dragstart="dragging = component"
              @dragover.prevent
              @drop="dropBefore(component)"
              @click="editComponent(component)"
            >
              <small>{{ component.componentType }} / {{ component.dataSourceCode }}</small>
              <strong>{{ componentTitle(component) }}</strong>
              <span>{{ component.gridW || 12 }}/12 栅格</span>
            </button>
          </div>
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
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  approvePortalMedia,
  listPortalArticles,
  listPortalChannels,
  listPortalHomeComponents,
  listPortalMedia,
  listPortalSites,
  offlinePortalArticle,
  publishPortalArticle,
  publishPortalHome,
  savePortalArticle,
  savePortalChannel,
  savePortalHomeComponent,
  savePortalMedia,
  savePortalSite,
  submitPortalArticleReview,
  type PortalArticle,
  type PortalChannel,
  type PortalHomeComponent,
  type PortalMediaAsset,
  type PortalSite
} from '@/api/crehn/cms';

const activeTab = ref('channel');
const sites = ref<PortalSite[]>([]);
const siteId = ref<string | number>();
const channels = ref<PortalChannel[]>([]);
const articles = ref<PortalArticle[]>([]);
const mediaRows = ref<PortalMediaAsset[]>([]);
const components = ref<PortalHomeComponent[]>([]);
const dragging = ref<PortalHomeComponent>();
const articleQuery = reactive({ status: '' });
const articleStatuses = ['DRAFT', 'IN_REVIEW', 'SCHEDULED', 'PUBLISHED', 'OFFLINE'];
const componentTypes = ['hero', 'news', 'notice', 'activity', 'schedule', 'media', 'stat', 'showcase', 'links'];
const dataSources = ['manual', 'public_articles', 'public_notices', 'public_activity', 'public_results', 'public_stats'];
const siteVisible = ref(false);
const channelVisible = ref(false);
const articleVisible = ref(false);
const mediaVisible = ref(false);
const componentVisible = ref(false);
const publishVisible = ref(false);
const mediaOssId = ref('');
const siteForm = reactive<PortalSite>({});
const channelForm = reactive<PortalChannel>({});
const articleForm = reactive<PortalArticle>({});
const mediaForm = reactive<PortalMediaAsset>({});
const componentForm = reactive<PortalHomeComponent>({});
const publishForm = reactive({ articleId: '' as string | number, scheduledAt: '', reason: '' });
const currentSite = computed(() => sites.value.find((item) => String(item.id) === String(siteId.value)));

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
  const [channelResponse, mediaResponse, componentResponse]: any[] = await Promise.all([
    listPortalChannels(siteId.value),
    listPortalMedia(siteId.value),
    listPortalHomeComponents(siteId.value)
  ]);
  channels.value = channelResponse.data || [];
  mediaRows.value = mediaResponse.data || [];
  components.value = componentResponse.data || [];
  await loadArticles();
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
const componentTitle = (component: PortalHomeComponent) => {
  return JSON.parse(component.configJson || '{}').title || component.componentKey;
};
const dropBefore = async (target: PortalHomeComponent) => {
  if (!dragging.value || dragging.value.id === target.id) return;
  const ordered = components.value.filter((item) => item.id !== dragging.value!.id);
  ordered.splice(ordered.findIndex((item) => item.id === target.id), 0, dragging.value);
  await Promise.all(ordered.map((item, index) => savePortalHomeComponent({ ...item, sortOrder: index + 1 })));
  dragging.value = undefined;
  await changeSite();
};
const publishHome = async () => {
  const { value } = await ElMessageBox.prompt('请输入本次首页发布原因', '发布首页版本', { inputType: 'textarea', inputValidator: (value) => !!value?.trim() || '原因不能为空' });
  await publishPortalHome(siteId.value!, value.trim());
  ElMessage.success('首页版本发布成功');
};
const channelName = (id?: string | number) => channels.value.find((item) => String(item.id) === String(id))?.channelName || id || '-';

onMounted(loadSites);
</script>

<style scoped>
.cms-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.component-grid {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  gap: 12px;
  padding: 16px;
  border: 1px dashed var(--el-border-color);
  border-radius: 10px;
  background: var(--el-fill-color-lighter);
}
.component-tile {
  min-height: 104px;
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
