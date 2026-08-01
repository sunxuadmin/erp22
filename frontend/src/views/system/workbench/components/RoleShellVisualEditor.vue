<template>
  <div class="role-shell-editor">
    <section class="layout-selector" aria-label="菜单导航布局">
      <div class="section-heading">
        <div>
          <strong>选择系统骨架</strong>
          <small>先确定导航位置，再点击下方预览中的区域进行配置</small>
        </div>
        <el-tag effect="plain" type="info">按角色保存</el-tag>
      </div>
      <div class="layout-options">
        <button
          v-for="option in layoutOptions"
          :key="option.value"
          type="button"
          class="layout-option"
          :class="{ 'is-active': form.navType === option.value }"
          @click="form.navType = option.value"
        >
          <span class="layout-option__thumb" :class="`is-${option.value}`">
            <i class="layout-option__header" />
            <i class="layout-option__sidebar" />
            <i class="layout-option__menu" />
            <i class="layout-option__content" />
          </span>
          <span>
            <strong>{{ option.label }}</strong>
            <small>{{ option.description }}</small>
          </span>
          <el-icon v-if="form.navType === option.value"><Select /></el-icon>
        </button>
      </div>
    </section>

    <div class="visual-editor-grid">
      <section class="shell-preview-panel">
        <div class="section-heading">
          <div>
            <strong>系统外壳预览</strong>
            <small>蓝色描边表示当前正在配置的区域</small>
          </div>
          <el-button text type="primary" icon="Setting" @click="activeSection = 'advanced'">高级设置</el-button>
        </div>

        <div class="shell-preview" :class="[`is-${form.navType}`, { 'has-no-tags': !form.tagsView }]" :style="previewVars">
          <aside v-if="form.navType !== 'top'" class="preview-sidebar">
            <button
              type="button"
              class="preview-hotspot preview-brand"
              :class="{ 'is-selected': activeSection === 'brand', 'is-muted': !form.sidebarLogo }"
              @click="activeSection = 'brand'"
            >
              <span v-if="form.sidebarLogo" class="preview-logo">C</span>
              <span v-if="form.sidebarLogo && form.brandTitleMode !== 'hidden'" class="preview-brand__text">{{ brandPreviewText }}</span>
              <span v-else class="preview-placeholder">品牌区已隐藏</span>
            </button>
            <div class="preview-menu-list" aria-hidden="true">
              <span class="is-active"><i />首页</span>
              <span><i />项目管理</span>
              <span><i />评审工作</span>
              <span><i />系统设置</span>
            </div>
          </aside>

          <div class="preview-main">
            <header class="preview-header">
              <button
                v-if="form.navType === 'top'"
                type="button"
                class="preview-hotspot preview-top-brand"
                :class="{ 'is-selected': activeSection === 'brand', 'is-muted': !form.sidebarLogo }"
                @click="activeSection = 'brand'"
              >
                <span v-if="form.sidebarLogo" class="preview-logo">C</span>
                <span v-if="form.sidebarLogo && form.brandTitleMode !== 'hidden'">{{ brandPreviewText }}</span>
                <span v-else class="preview-placeholder">品牌区</span>
              </button>

              <button
                type="button"
                class="preview-hotspot preview-header-title"
                :class="{ 'is-selected': activeSection === 'header', 'is-muted': !headerIdentityVisible }"
                :style="headerTitleStyle"
                @click="activeSection = 'header'"
              >
                <img
                  v-if="headerLogoPreviewUrl"
                  :src="headerLogoPreviewUrl"
                  class="preview-header-logo"
                  :style="{ height: `${Math.min(Math.max(Number(form.navbarTitleLogoHeight) || 28, 16), 36)}px` }"
                  alt=""
                />
                <span v-if="form.navbarTitleVisible">{{ navbarPreviewText }}</span>
                <span v-else-if="!headerLogoPreviewUrl" class="preview-placeholder">Header 标识已隐藏</span>
              </button>

              <div v-if="form.navType !== 'left'" class="preview-top-menu" aria-hidden="true">
                <span class="is-active">首页</span><span>项目管理</span><span>评审工作</span>
              </div>

              <button
                type="button"
                class="preview-hotspot preview-actions"
                :class="{ 'is-selected': activeSection === 'actions' }"
                @click="activeSection = 'actions'"
              >
                <span v-if="form.showScreenfull" title="最大化">⛶</span>
                <span v-if="form.showSizeSelect" title="文字大小">A</span>
                <span v-if="form.showUserAvatar" class="preview-avatar">管</span>
                <span v-if="form.userDisplayMode !== 'hidden'">{{ userPreviewText }}</span>
                <span v-if="!hasVisibleAction" class="preview-placeholder">操作区</span>
              </button>
            </header>

            <button
              type="button"
              class="preview-hotspot preview-breadcrumb"
              :class="{ 'is-selected': activeSection === 'breadcrumb', 'is-muted': !form.breadcrumbVisible || form.navType !== 'left' }"
              @click="activeSection = 'breadcrumb'"
            >
              <template v-if="form.breadcrumbVisible && form.navType === 'left'">
                <span v-if="!form.hideHomeBreadcrumb">首页</span><i v-if="!form.hideHomeBreadcrumb">/</i><span>工作台配置</span>
              </template>
              <span v-else class="preview-placeholder">{{ form.navType === 'left' ? '页面路径已隐藏' : '当前布局不显示页面路径' }}</span>
            </button>

            <button
              type="button"
              class="preview-hotspot preview-tags"
              :class="{ 'is-selected': activeSection === 'tags', 'is-muted': !form.tagsView }"
              @click="activeSection = 'tags'"
            >
              <template v-if="form.tagsView">
                <span v-if="!form.hideHomeTagsView" class="preview-tag">{{ form.tagsIcon ? '⌂ ' : '' }}首页</span>
                <span class="preview-tag is-active">{{ form.tagsIcon ? '⚙ ' : '' }}工作台配置</span>
              </template>
              <span v-else class="preview-placeholder">标签栏已隐藏，点击可重新开启</span>
            </button>

            <main class="preview-workspace" aria-hidden="true">
              <div class="preview-workspace__heading">
                <span />
                <div><i /><i /></div>
              </div>
              <div class="preview-stat-row"><span /><span /><span /></div>
              <div class="preview-table"><i v-for="index in 4" :key="index" /></div>
            </main>
          </div>
        </div>

        <div class="preview-legend">
          <span><i class="is-clickable" />可点击配置</span>
          <span><i class="is-selected" />当前区域</span>
          <span><i class="is-hidden" />已隐藏但仍可配置</span>
        </div>
      </section>

      <aside class="property-panel">
        <div class="property-panel__head">
          <span class="property-panel__icon"
            ><el-icon><Setting /></el-icon
          ></span>
          <div>
            <strong>{{ activeSectionMeta.title }}</strong>
            <small>{{ activeSectionMeta.description }}</small>
          </div>
        </div>

        <el-form :model="form" label-position="top" class="property-form">
          <template v-if="activeSection === 'brand'">
            <el-form-item label="显示左上品牌区">
              <el-switch v-model="form.sidebarLogo" active-text="显示" inactive-text="隐藏" />
              <span class="field-help">关闭后不再保留侧栏宽度占位，Header 标识会固定延伸到左侧。</span>
            </el-form-item>
            <el-form-item label="品牌文字来源">
              <el-radio-group v-model="form.brandTitleMode" :disabled="!form.sidebarLogo" class="compact-radio-group">
                <el-radio-button value="role">按角色</el-radio-button>
                <el-radio-button value="custom">自定义</el-radio-button>
                <el-radio-button value="activity">活动名称</el-radio-button>
                <el-radio-button value="hidden">不显示</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item v-if="form.brandTitleMode === 'custom'" label="品牌文字">
              <el-input
                v-model="form.brandTitle"
                clearable
                maxlength="40"
                show-word-limit
                :disabled="!form.sidebarLogo"
                placeholder="支持 {roleName}、{nickName}、{userName}"
              />
            </el-form-item>
            <el-form-item v-else-if="form.brandTitleMode === 'activity'" label="选择活动">
              <el-select
                v-model="form.brandActivityId"
                filterable
                clearable
                :disabled="!form.sidebarLogo"
                placeholder="选择后保存活动名称快照"
                @change="syncBrandActivityTitle"
              >
                <el-option v-for="activity in activityOptions" :key="activity.id" :label="activity.activityName" :value="activity.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="侧栏折叠时">
              <el-switch
                v-model="form.brandTitleKeepVisibleOnCollapse"
                :disabled="!form.sidebarLogo || form.brandTitleMode === 'hidden'"
                active-text="保留品牌文字"
                inactive-text="仅显示 Logo"
              />
            </el-form-item>
          </template>

          <template v-else-if="activeSection === 'header'">
            <el-form-item label="Header Logo">
              <el-segmented
                v-model="form.navbarTitleLogoMode"
                :options="[
                  { label: '不显示', value: 'hidden' },
                  { label: '系统 Logo', value: 'system' },
                  { label: '上传图片', value: 'asset' }
                ]"
                block
              />
            </el-form-item>
            <template v-if="form.navbarTitleLogoMode === 'asset'">
              <div class="header-logo-upload">
                <div class="header-logo-preview">
                  <img v-if="headerLogoPreviewUrl" :src="headerLogoPreviewUrl" alt="Header Logo 预览" />
                  <span v-else>尚未上传</span>
                </div>
                <div class="header-logo-upload__actions">
                  <el-upload
                    action="#"
                    :auto-upload="false"
                    :show-file-list="false"
                    accept=".png,.jpg,.jpeg,.webp,image/png,image/jpeg,image/webp"
                    :on-change="handleHeaderLogoChange"
                  >
                    <el-button type="primary" plain icon="Upload" :loading="headerLogoUploading">上传或替换</el-button>
                  </el-upload>
                  <el-button :disabled="!form.navbarTitleLogoAssetKey" @click="clearHeaderLogoReference">清空引用</el-button>
                </div>
                <span class="field-help">支持 PNG、JPG、WebP，单张不超过 5MB；清空只移除当前角色引用，不删除物理文件。</span>
              </div>
            </template>
            <el-form-item v-if="form.navbarTitleLogoMode !== 'hidden'" label="Logo 高度">
              <el-input-number v-model="form.navbarTitleLogoHeight" :min="16" :max="36" controls-position="right" />
            </el-form-item>
            <el-form-item label="显示标题文字">
              <el-switch v-model="form.navbarTitleVisible" active-text="显示" inactive-text="隐藏" />
            </el-form-item>
            <el-form-item label="标题文字">
              <el-input
                v-model="form.navbarTitle"
                clearable
                maxlength="60"
                show-word-limit
                :disabled="!form.navbarTitleVisible"
                placeholder="留空时按角色显示学校或工作台名称"
              />
            </el-form-item>
            <el-form-item label="字体">
              <el-select v-model="form.navbarTitleFontFamily">
                <el-option label="系统默认" value="system" />
                <el-option label="微软雅黑" value="microsoft-yahei" />
                <el-option label="黑体" value="simhei" />
                <el-option label="宋体" value="simsun" />
                <el-option label="楷体" value="kaiti" />
              </el-select>
            </el-form-item>
            <el-form-item label="对齐方式">
              <el-segmented
                v-model="form.navbarTitleAlign"
                :options="[
                  { label: '居左', value: 'left' },
                  { label: '居中', value: 'center' },
                  { label: '居右', value: 'right' }
                ]"
                block
              />
            </el-form-item>
            <div class="property-inline-grid">
              <el-form-item label="字号">
                <el-input-number v-model="form.navbarTitleFontSize" :min="12" :max="28" controls-position="right" />
              </el-form-item>
              <el-form-item label="颜色">
                <div class="color-field">
                  <el-color-picker v-model="form.navbarTitleColor" /><el-input v-model="form.navbarTitleColor" maxlength="7" />
                </div>
              </el-form-item>
            </div>
            <el-form-item label="字重">
              <el-radio-group v-model="form.navbarTitleFontWeight">
                <el-radio-button value="normal">常规</el-radio-button>
                <el-radio-button value="medium">中等</el-radio-button>
                <el-radio-button value="bold">加粗</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </template>

          <template v-else-if="activeSection === 'breadcrumb'">
            <el-form-item label="显示页面路径">
              <el-switch v-model="form.breadcrumbVisible" active-text="显示" inactive-text="隐藏" />
            </el-form-item>
            <el-form-item label="首页路径项">
              <el-switch v-model="form.hideHomeBreadcrumb" :disabled="!form.breadcrumbVisible" active-text="隐藏首页" inactive-text="显示首页" />
            </el-form-item>
            <el-alert type="info" :closable="false" show-icon title="页面路径只在左侧导航布局中显示；其他布局会自动忽略。" />
          </template>

          <template v-else-if="activeSection === 'tags'">
            <el-form-item label="显示标签栏">
              <el-switch v-model="form.tagsView" active-text="显示" inactive-text="隐藏" />
            </el-form-item>
            <el-form-item label="标签图标">
              <el-switch v-model="form.tagsIcon" :disabled="!form.tagsView" active-text="显示" inactive-text="隐藏" />
            </el-form-item>
            <el-form-item label="首页标签">
              <el-switch v-model="form.hideHomeTagsView" :disabled="!form.tagsView" active-text="隐藏首页" inactive-text="显示首页" />
            </el-form-item>
          </template>

          <template v-else-if="activeSection === 'actions'">
            <el-form-item label="工具按钮">
              <div class="switch-list">
                <label><span>最大化按钮</span><el-switch v-model="form.showScreenfull" /></label>
                <label><span>文字大小按钮</span><el-switch v-model="form.showSizeSelect" /></label>
                <label><span>用户头像</span><el-switch v-model="form.showUserAvatar" /></label>
              </div>
            </el-form-item>
            <el-form-item label="账号名称">
              <el-radio-group v-model="form.userDisplayMode">
                <el-radio-button value="nickname">昵称</el-radio-button>
                <el-radio-button value="username">登录账号</el-radio-button>
                <el-radio-button value="hidden">不显示</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-alert
              v-if="!form.showUserAvatar && form.userDisplayMode === 'hidden'"
              type="warning"
              :closable="false"
              show-icon
              title="头像和账号名称不能同时隐藏，否则无法进入个人菜单。"
            />
          </template>

          <template v-else>
            <el-form-item label="角色主题色">
              <div class="color-field"><el-color-picker v-model="form.theme" /><el-input v-model="form.theme" maxlength="7" /></div>
              <small class="field-help">影响按钮、菜单选中等系统主题；不是全局工作台背景色。</small>
            </el-form-item>
            <el-form-item label="页面圆角">
              <el-slider v-model="form.radiusBase" :min="0" :max="24" :step="1" show-input />
            </el-form-item>
            <div class="switch-list">
              <label><span>固定 Header</span><el-switch v-model="form.fixedHeader" /></label>
              <label><span>动态浏览器标题</span><el-switch v-model="form.dynamicTitle" /></label>
            </div>
          </template>
        </el-form>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Select, Setting } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { uploadWorkbenchAsset, workbenchAssetUrl } from '@/api/system/workbench';
import type { WorkbenchRoleShellConfig } from '@/api/system/workbench/types';
import systemLogo from '@/assets/logo/logo.png';

type ShellSection = 'brand' | 'header' | 'breadcrumb' | 'tags' | 'actions' | 'advanced';

interface ActivityOption {
  id?: string | number;
  activityName?: string;
}

const props = withDefaults(
  defineProps<{
    model: WorkbenchRoleShellConfig;
    activityOptions?: ActivityOption[];
    roleName?: string;
  }>(),
  {
    activityOptions: () => [],
    roleName: '当前角色'
  }
);

const form = toRef(props, 'model');
const activeSection = ref<ShellSection>('brand');
const headerLogoUploading = ref(false);

const layoutOptions: Array<{
  value: WorkbenchRoleShellConfig['navType'];
  label: string;
  description: string;
}> = [
  { value: 'left', label: '左侧导航', description: '菜单固定在左侧，结构最清晰' },
  { value: 'mix', label: '混合导航', description: '左侧菜单配合顶部模块导航' },
  { value: 'top', label: '顶部导航', description: '菜单集中在 Header，释放横向空间' }
];

const sectionMeta: Record<ShellSection, { title: string; description: string }> = {
  brand: { title: '左上品牌区', description: '设置 Logo、品牌文字来源和侧栏折叠行为' },
  header: { title: 'Header 标识', description: '设置顶部 Logo、标题文字、对齐和字体样式' },
  breadcrumb: { title: '页面路径', description: '设置面包屑及首页路径项是否显示' },
  tags: { title: '标签栏', description: '集中设置 Tags-Views、图标和首页标签' },
  actions: { title: '右上操作区', description: '设置工具按钮、头像和账号名称' },
  advanced: { title: '高级设置', description: '设置角色主题、圆角和框架级行为' }
};

const activeSectionMeta = computed(() => sectionMeta[activeSection.value]);
const brandPreviewText = computed(() => {
  if (form.value.brandTitleMode === 'role') return props.roleName || '角色工作台';
  return form.value.brandTitle || (form.value.brandTitleMode === 'activity' ? '活动名称' : '工作台');
});
const navbarPreviewText = computed(() => form.value.navbarTitle || `${props.roleName || '当前角色'}工作台`);
const headerLogoPreviewUrl = computed(() => {
  if (form.value.navbarTitleLogoMode === 'system') return systemLogo;
  if (form.value.navbarTitleLogoMode === 'asset') return workbenchAssetUrl(form.value.navbarTitleLogoAssetKey);
  return '';
});
const headerIdentityVisible = computed(() => form.value.navbarTitleVisible || Boolean(headerLogoPreviewUrl.value));
const userPreviewText = computed(() => (form.value.userDisplayMode === 'username' ? 'admin' : '管理员'));
const hasVisibleAction = computed(
  () => form.value.showScreenfull || form.value.showSizeSelect || form.value.showUserAvatar || form.value.userDisplayMode !== 'hidden'
);
const previewVars = computed(() => ({
  '--shell-theme': form.value.theme || '#2563eb',
  '--shell-radius': `${Math.min(Math.max(Number(form.value.radiusBase) || 0, 0), 24)}px`
}));
const navbarTitleFontFamilies = {
  system: 'Avenir, "Helvetica Neue", Arial, "Microsoft YaHei", sans-serif',
  'microsoft-yahei': '"Microsoft YaHei", "微软雅黑", Arial, sans-serif',
  simhei: 'SimHei, "黑体", "Microsoft YaHei", sans-serif',
  simsun: 'SimSun, "宋体", serif',
  kaiti: 'KaiTi, "楷体", "STKaiti", serif'
} as const;
const headerTitleStyle = computed(() => ({
  color: form.value.navbarTitleColor || '#29445f',
  fontSize: `${Math.min(Math.max(Number(form.value.navbarTitleFontSize) || 15, 12), 28)}px`,
  fontWeight: form.value.navbarTitleFontWeight === 'normal' ? '400' : form.value.navbarTitleFontWeight === 'medium' ? '500' : '700',
  fontFamily: navbarTitleFontFamilies[form.value.navbarTitleFontFamily],
  justifyContent: form.value.navbarTitleAlign === 'center' ? 'center' : form.value.navbarTitleAlign === 'right' ? 'flex-end' : 'flex-start',
  textAlign: form.value.navbarTitleAlign
}));

const handleHeaderLogoChange = async (uploadFile: { raw?: File; name?: string }) => {
  const file = uploadFile.raw;
  if (!file) return;
  const extension = file.name.split('.').pop()?.toLowerCase() || '';
  const allowedExtensions = ['png', 'jpg', 'jpeg', 'webp'];
  const allowedTypes = ['', 'image/png', 'image/jpeg', 'image/webp'];
  if (!allowedExtensions.includes(extension) || !allowedTypes.includes(file.type)) {
    ElMessage.warning('仅支持 PNG、JPG、WebP 图片');
    return;
  }
  if (file.size <= 0 || file.size > 5 * 1024 * 1024) {
    ElMessage.warning('Logo 图片大小必须在 5MB 以内');
    return;
  }
  headerLogoUploading.value = true;
  try {
    const { data } = await uploadWorkbenchAsset(file);
    form.value.navbarTitleLogoMode = 'asset';
    form.value.navbarTitleLogoAssetKey = data.fileKey;
    ElMessage.success('Header Logo 已上传，请保存角色配置');
  } catch (error: any) {
    ElMessage.error(error?.msg || 'Header Logo 上传失败');
  } finally {
    headerLogoUploading.value = false;
  }
};

const clearHeaderLogoReference = () => {
  form.value.navbarTitleLogoAssetKey = '';
};

const syncBrandActivityTitle = (activityId?: string | number) => {
  const activity = props.activityOptions.find((item) => String(item.id) === String(activityId));
  form.value.brandTitle = String(activity?.activityName || '');
};
</script>

<style scoped lang="scss">
.role-shell-editor {
  display: grid;
  gap: 18px;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.section-heading > div:first-child {
  display: grid;
  gap: 4px;
}

.section-heading small,
.property-panel__head small,
.layout-option small,
.field-help {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.layout-selector {
  display: grid;
  gap: 12px;
}

.layout-options {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.layout-option {
  position: relative;
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  min-width: 0;
  padding: 12px;
  color: var(--el-text-color-primary);
  text-align: left;
  cursor: pointer;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  transition: 0.18s ease;
}

.layout-option:hover,
.layout-option.is-active {
  border-color: var(--el-color-primary-light-3);
  box-shadow: 0 0 0 2px var(--el-color-primary-light-9);
}

.layout-option.is-active {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.layout-option > span:nth-child(2) {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.layout-option__thumb {
  position: relative;
  display: block;
  width: 86px;
  height: 54px;
  overflow: hidden;
  background: #f7f9fc;
  border: 1px solid #dbe5f0;
  border-radius: 6px;
}

.layout-option__thumb i {
  position: absolute;
  display: block;
}

.layout-option__header {
  top: 0;
  right: 0;
  left: 0;
  height: 11px;
  background: #ffffff;
  border-bottom: 1px solid #e3eaf2;
}

.layout-option__sidebar {
  top: 0;
  bottom: 0;
  left: 0;
  width: 20px;
  background: #173a67;
}

.layout-option__menu {
  top: 3px;
  left: 25px;
  width: 34px;
  height: 5px;
  background: var(--el-color-primary-light-5);
  border-radius: 2px;
}

.layout-option__content {
  top: 18px;
  right: 7px;
  bottom: 7px;
  left: 27px;
  background: #ffffff;
  border: 1px solid #e3eaf2;
  border-radius: 3px;
}

.layout-option__thumb.is-left .layout-option__menu {
  display: none;
}

.layout-option__thumb.is-top .layout-option__sidebar {
  display: none;
}

.layout-option__thumb.is-top .layout-option__header {
  height: 15px;
  background: #173a67;
}

.layout-option__thumb.is-top .layout-option__menu {
  top: 5px;
  left: 29px;
  background: #ffffff;
}

.layout-option__thumb.is-top .layout-option__content {
  top: 22px;
  left: 7px;
}

.visual-editor-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.65fr) minmax(330px, 0.8fr);
  align-items: start;
  gap: 16px;
}

.shell-preview-panel,
.property-panel {
  min-width: 0;
  padding: 16px;
  background: var(--el-fill-color-extra-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
}

.shell-preview-panel {
  display: grid;
  gap: 14px;
}

.shell-preview {
  --shell-theme: #2563eb;
  --shell-radius: 8px;
  display: flex;
  min-height: 430px;
  overflow: hidden;
  background: #edf2f8;
  border: 1px solid #ced9e6;
  border-radius: min(var(--shell-radius), 14px);
  box-shadow: 0 12px 30px rgba(27, 55, 87, 0.08);
}

.preview-sidebar {
  flex: 0 0 172px;
  color: #dce8f7;
  background: linear-gradient(180deg, #173a67 0%, #102c50 100%);
}

.preview-brand,
.preview-top-brand {
  justify-content: flex-start;
  gap: 8px;
  color: inherit;
  font-weight: 700;
}

.preview-brand {
  width: 100%;
  height: 51px;
  padding: 0 13px;
  border-width: 0 0 1px;
  border-color: rgba(255, 255, 255, 0.12);
  border-radius: 0;
}

.preview-logo {
  display: inline-grid;
  flex: 0 0 auto;
  width: 25px;
  height: 25px;
  color: #ffffff;
  place-items: center;
  background: var(--shell-theme);
  border-radius: 7px;
}

.preview-brand__text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-menu-list {
  display: grid;
  gap: 5px;
  padding: 13px 9px;
}

.preview-menu-list span {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 9px 10px;
  font-size: 12px;
  border-radius: max(calc(var(--shell-radius) * 0.6), 2px);
}

.preview-menu-list span.is-active {
  color: #ffffff;
  background: var(--shell-theme);
}

.preview-menu-list i {
  width: 11px;
  height: 11px;
  border: 1px solid currentColor;
  border-radius: 3px;
}

.preview-main {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
}

.preview-header {
  display: flex;
  align-items: center;
  min-height: 51px;
  padding: 0 8px;
  background: #ffffff;
  border-bottom: 1px solid #e4eaf1;
}

.preview-hotspot {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  color: inherit;
  cursor: pointer;
  background: transparent;
  border: 1px dashed transparent;
  border-radius: 6px;
  outline: none;
  transition: 0.16s ease;
}

.preview-hotspot:hover {
  border-color: var(--shell-theme);
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.12);
}

.preview-hotspot.is-selected {
  border-color: var(--shell-theme);
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.16);
}

.preview-hotspot.is-muted {
  color: #8b99a8;
  background: repeating-linear-gradient(-45deg, #f7f9fb, #f7f9fb 5px, #edf1f5 5px, #edf1f5 10px);
}

.preview-header-title {
  display: flex;
  align-items: center;
  gap: 7px;
  flex: 0 1 260px;
  padding: 7px 9px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-header-logo {
  width: auto;
  max-width: 72px;
  max-height: 30px;
  object-fit: contain;
  flex: 0 0 auto;
}

.preview-actions {
  justify-content: flex-end;
  gap: 8px;
  margin-left: auto;
  padding: 6px 8px;
  color: #42566e;
  font-size: 12px;
}

.preview-avatar {
  display: inline-grid;
  width: 24px;
  height: 24px;
  color: #ffffff;
  place-items: center;
  background: #7890aa;
  border-radius: 50%;
}

.preview-breadcrumb {
  align-self: flex-start;
  gap: 7px;
  min-height: 31px;
  margin: 8px 10px 0;
  padding: 5px 9px;
  color: #62748a;
  font-size: 11px;
}

.preview-tags {
  gap: 6px;
  min-height: 37px;
  margin-top: 7px;
  padding: 4px 10px;
  background: #ffffff;
  border-width: 1px 0;
  border-color: transparent;
  border-radius: 0;
}

.preview-tag {
  padding: 5px 9px;
  color: #63748a;
  font-size: 11px;
  border: 1px solid #e0e7ef;
  border-radius: max(calc(var(--shell-radius) * 0.6), 2px);
}

.preview-tag.is-active {
  color: var(--shell-theme);
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-5);
}

.preview-placeholder {
  color: #95a2b1;
  font-size: 11px;
  font-weight: 400;
}

.preview-workspace {
  display: grid;
  flex: 1;
  align-content: start;
  gap: 12px;
  margin: 10px;
  padding: 14px;
  background: #ffffff;
  border: 1px solid #e1e8f0;
  border-radius: var(--shell-radius);
}

.preview-workspace__heading {
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.preview-workspace__heading > span {
  width: 32%;
  height: 11px;
  background: #d7e1ec;
  border-radius: 4px;
}

.preview-workspace__heading div {
  display: flex;
  gap: 7px;
}

.preview-workspace__heading i {
  width: 42px;
  height: 20px;
  background: var(--shell-theme);
  border-radius: 4px;
  opacity: 0.78;
}

.preview-stat-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.preview-stat-row span {
  height: 48px;
  background: #f4f8fd;
  border: 1px solid #dce8f5;
  border-radius: var(--shell-radius);
}

.preview-table {
  display: grid;
  overflow: hidden;
  border: 1px solid #e3e9f0;
  border-radius: var(--shell-radius);
}

.preview-table i {
  height: 30px;
  border-bottom: 1px solid #edf1f5;
}

.preview-table i:first-child {
  background: #f4f7fa;
}

.preview-table i:last-child {
  border-bottom: 0;
}

.shell-preview.is-top {
  display: block;
}

.shell-preview.is-top .preview-header {
  color: #edf5ff;
  background: #173a67;
}

.shell-preview.is-top .preview-header-title {
  color: #ffffff !important;
}

.preview-top-brand {
  flex: 0 0 auto;
  padding: 6px 8px;
}

.preview-top-menu {
  display: flex;
  align-items: center;
  align-self: stretch;
  gap: 2px;
  margin-left: 8px;
  font-size: 11px;
}

.preview-top-menu span {
  display: grid;
  padding: 0 8px;
  place-items: center;
}

.preview-top-menu span.is-active {
  background: rgba(255, 255, 255, 0.12);
}

.shell-preview.is-top .preview-actions {
  color: #edf5ff;
}

.shell-preview.is-mix .preview-header-title {
  flex-basis: 170px;
}

.shell-preview.is-mix .preview-top-menu {
  color: #4e6279;
}

.preview-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.preview-legend span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.preview-legend i {
  width: 14px;
  height: 10px;
  border-radius: 3px;
}

.preview-legend i.is-clickable {
  border: 1px dashed var(--el-color-primary-light-3);
}

.preview-legend i.is-selected {
  background: var(--el-color-primary-light-9);
  border: 1px solid var(--el-color-primary);
}

.preview-legend i.is-hidden {
  background: repeating-linear-gradient(-45deg, #f7f9fb, #f7f9fb 3px, #e7ecf2 3px, #e7ecf2 6px);
  border: 1px solid #d6dee7;
}

.property-panel {
  position: sticky;
  top: 12px;
  display: grid;
  gap: 16px;
  background: var(--el-bg-color);
}

.property-panel__head {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  padding-bottom: 13px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.property-panel__head > div {
  display: grid;
  gap: 3px;
}

.property-panel__icon {
  display: grid;
  width: 38px;
  height: 38px;
  color: var(--el-color-primary);
  font-size: 18px;
  place-items: center;
  background: var(--el-color-primary-light-9);
  border-radius: 9px;
}

.property-form :deep(.el-form-item) {
  margin-bottom: 17px;
}

.property-form :deep(.el-form-item__label) {
  margin-bottom: 5px;
  font-weight: 600;
}

.property-form :deep(.el-select),
.property-form :deep(.el-input-number),
.property-form :deep(.el-segmented) {
  width: 100%;
}

.compact-radio-group {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  width: 100%;
}

.compact-radio-group :deep(.el-radio-button__inner) {
  width: 100%;
}

.property-inline-grid {
  display: grid;
  grid-template-columns: minmax(110px, 0.7fr) minmax(0, 1.3fr);
  gap: 12px;
}

.color-field {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 8px;
}

.color-field .el-input {
  flex: 1;
  min-width: 0;
}

.switch-list {
  display: grid;
  width: 100%;
  gap: 8px;
}

.switch-list label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 38px;
  padding: 6px 10px;
  background: var(--el-fill-color-extra-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 7px;
}

.field-help {
  display: block;
  width: 100%;
  margin-top: 5px;
}

.header-logo-upload {
  display: grid;
  gap: 10px;
  margin: -4px 0 17px;
  padding: 12px;
  background: var(--el-fill-color-extra-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.header-logo-preview {
  display: grid;
  min-height: 64px;
  padding: 8px;
  color: var(--el-text-color-secondary);
  place-items: center;
  background: var(--el-bg-color);
  border: 1px dashed var(--el-border-color);
  border-radius: 7px;
}

.header-logo-preview img {
  width: auto;
  max-width: 180px;
  max-height: 48px;
  object-fit: contain;
}

.header-logo-upload__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 1180px) {
  .layout-options {
    grid-template-columns: 1fr;
  }

  .visual-editor-grid {
    grid-template-columns: 1fr;
  }

  .property-panel {
    position: static;
  }
}

@media (max-width: 680px) {
  .shell-preview {
    min-height: 350px;
  }

  .preview-sidebar {
    flex-basis: 116px;
  }

  .preview-brand__text,
  .preview-menu-list span {
    font-size: 10px;
  }

  .preview-header-title {
    flex-basis: 120px;
    font-size: 12px !important;
  }

  .preview-actions > span:not(.preview-avatar):not(.preview-placeholder) {
    display: none;
  }

  .property-inline-grid {
    grid-template-columns: 1fr;
  }
}
</style>
