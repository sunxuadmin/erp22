<template>
  <div class="p-2">
    <el-card shadow="hover" class="mb-[10px]">
      <el-form :model="queryParams" :inline="true">
        <el-form-item label="消息标题">
          <el-input v-model="queryParams.title" clearable placeholder="请输入消息标题" @keyup.enter="getList" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.readStatus" clearable placeholder="全部" style="width: 140px">
            <el-option label="未读" value="unread" />
            <el-option label="已读" value="read" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="getList">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          <el-button v-hasPermi="['crehn:message:read']" type="success" plain icon="Check" @click="markAllRead">全部已读</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover">
      <template #header>
        <div class="message-header">
          <span>我的消息</span>
          <el-tag v-if="unreadCount > 0" type="danger">未读 {{ unreadCount }}</el-tag>
        </div>
      </template>
      <el-table v-loading="loading" border :data="rows" class="art-viewable-table" @row-click="openDetail">
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.readStatus === 'unread' ? 'danger' : 'info'">{{ row.readStatus === 'unread' ? '未读' : '已读' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标题" prop="title" min-width="220" show-overflow-tooltip />
        <el-table-column label="项目ID" prop="projectId" width="100" />
        <el-table-column label="发送时间" prop="sentAt" width="170" />
        <el-table-column label="读取时间" prop="readTime" width="170" />
        <el-table-column label="操作" width="190" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" icon="View" @click.stop="openDetail(row)">查看</el-button>
            <el-button v-if="row.readStatus === 'unread'" v-hasPermi="['crehn:message:read']" link type="success" icon="Check" @click.stop="markRead(row)">已读</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <ArtPopupDialog v-model="detail.visible" :title="detail.row?.title || '消息详情'" width="90%" wide append-to-body>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="状态">{{ detail.row?.readStatus === 'unread' ? '未读' : '已读' }}</el-descriptions-item>
        <el-descriptions-item label="项目ID">{{ detail.row?.projectId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发送时间">{{ detail.row?.sentAt || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div class="message-content">{{ detail.row?.content || '-' }}</div>
      <template #footer>
        <el-button v-if="detail.row?.readStatus === 'unread'" type="primary" @click="markRead(detail.row)">标记已读</el-button>
        <el-button @click="detail.visible = false">关闭</el-button>
      </template>
    </ArtPopupDialog>
  </div>
</template>

<script setup name="ArtMessage" lang="ts">
import { listMyMessage, markAllMessageRead, markMessageRead, getUnreadMessageCount } from '@/api/crehn/message';
import { UserMessageVO } from '@/api/crehn/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const loading = ref(false);
const rows = ref<UserMessageVO[]>([]);
const total = ref(0);
const unreadCount = ref(0);
const queryParams = reactive<any>({ pageNum: 1, pageSize: 10, title: '', readStatus: '' });
const detail = reactive<{ visible: boolean; row?: UserMessageVO }>({ visible: false, row: undefined });

const getList = async () => {
  loading.value = true;
  try {
    const res = await listMyMessage({ ...queryParams, messageType: 'audit' });
    rows.value = res.rows || [];
    total.value = res.total || 0;
    await refreshUnreadCount();
  } finally {
    loading.value = false;
  }
};

const refreshUnreadCount = async () => {
  const res = await getUnreadMessageCount();
  unreadCount.value = Number(res.data || 0);
};

const resetQuery = () => {
  queryParams.title = '';
  queryParams.readStatus = '';
  queryParams.pageNum = 1;
  getList();
};

const openDetail = async (row: UserMessageVO) => {
  detail.row = row;
  detail.visible = true;
  if (row.readStatus === 'unread') {
    await markRead(row, false);
  }
};

const markRead = async (row?: UserMessageVO, showTip = true) => {
  if (!row?.id) return;
  await markMessageRead(row.id);
  row.readStatus = 'read';
  row.readTime = new Date().toLocaleString();
  if (detail.row?.id === row.id) {
    detail.row = { ...row };
  }
  await refreshUnreadCount();
  if (showTip) proxy?.$modal.msgSuccess('已标记为已读');
};

const markAllRead = async () => {
  await markAllMessageRead();
  proxy?.$modal.msgSuccess('已全部标记为已读');
  await getList();
};

onMounted(getList);
</script>

<style scoped>
.message-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.message-content {
  margin-top: 16px;
  padding: 14px;
  min-height: 120px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-fill-color-light);
  white-space: pre-wrap;
  line-height: 1.8;
}
</style>
