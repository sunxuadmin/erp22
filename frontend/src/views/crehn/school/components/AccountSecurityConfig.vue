<template>
  <section class="account-security-config">
    <div class="account-security-config__header">
      <div>
        <h2>账号安全提醒</h2>
        <p>统一设置学校和专家账号登录后的密码与联系方式完善提醒。</p>
      </div>
      <div class="account-security-config__actions">
        <el-button icon="Refresh" plain :loading="loading" @click="loadConfig">刷新</el-button>
        <el-button v-hasPermi="['system:workbench:edit']" type="primary" icon="Check" :loading="saving" @click="saveConfig">保存配置</el-button>
      </div>
    </div>

    <el-alert class="mb-3" type="info" :closable="false" show-icon title="此处配置为全系统统一策略，不按单位或单个账号分别设置。" />

    <el-form :model="form" label-width="132px" class="account-security-config__form">
      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <el-form-item label="启用提醒">
            <el-switch v-model="form.enabled" active-text="启用" inactive-text="关闭" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="默认密码提醒">
            <el-switch v-model="form.checkDefaultPassword" active-text="检查" inactive-text="忽略" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="手机号为空提醒">
            <el-switch v-model="form.checkMissingPhone" active-text="检查" inactive-text="忽略" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="邮箱为空提醒">
            <el-switch v-model="form.checkMissingEmail" active-text="检查" inactive-text="忽略" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
  </section>
</template>

<script setup lang="ts">
import { getSecurityReminderConfig, saveSecurityReminderConfig } from '@/api/system/workbench';
import type { SecurityReminderConfig } from '@/api/system/workbench/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const loading = ref(false);
const saving = ref(false);
const defaultConfig: SecurityReminderConfig = {
  enabled: true,
  checkDefaultPassword: true,
  checkMissingPhone: true,
  checkMissingEmail: true
};
const form = reactive<SecurityReminderConfig>({ ...defaultConfig });

const parseConfig = (raw: unknown): SecurityReminderConfig => {
  try {
    const parsed = typeof raw === 'string' ? JSON.parse(raw || '{}') : raw || {};
    return {
      enabled: (parsed as any).enabled !== false,
      checkDefaultPassword: (parsed as any).checkDefaultPassword !== false,
      checkMissingPhone: (parsed as any).checkMissingPhone !== false,
      checkMissingEmail: (parsed as any).checkMissingEmail !== false
    };
  } catch {
    return { ...defaultConfig };
  }
};

const loadConfig = async () => {
  loading.value = true;
  try {
    const res: any = await getSecurityReminderConfig();
    Object.assign(form, parseConfig(res.data));
  } finally {
    loading.value = false;
  }
};

const saveConfig = async () => {
  saving.value = true;
  try {
    await saveSecurityReminderConfig({ ...form });
    await loadConfig();
    proxy?.$modal.msgSuccess('账号安全提醒配置已保存');
  } finally {
    saving.value = false;
  }
};

onMounted(loadConfig);
</script>

<style scoped lang="scss">
.account-security-config {
  padding: 8px 4px;
}

.account-security-config__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;

  h2 {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
  }

  p {
    margin: 6px 0 0;
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }
}

.account-security-config__actions {
  display: flex;
  flex: 0 0 auto;
  gap: 8px;
}

.account-security-config__form {
  max-width: 920px;
}

@media (max-width: 640px) {
  .account-security-config__header {
    flex-direction: column;
  }

  .account-security-config__actions {
    width: 100%;

    .el-button {
      flex: 1;
    }
  }
}
</style>
