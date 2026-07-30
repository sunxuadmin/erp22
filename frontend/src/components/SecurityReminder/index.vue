<template>
  <el-dialog
    v-model="visible"
    class="security-reminder-dialog"
    modal-class="security-reminder-overlay"
    width="min(720px, calc(100vw - 32px))"
    append-to-body
    align-center
    destroy-on-close
    @close="persistIgnoreIfNeeded"
  >
    <template #header>
      <div class="security-reminder-head">
        <div class="security-reminder-icon">
          <el-icon><Lock /></el-icon>
        </div>
        <div>
          <h2>账号安全提醒</h2>
          <p>检测到当前账号可能仍在使用默认密码，或联系方式尚未完善，请及时修改密码并补充手机号、邮箱。</p>
        </div>
      </div>
    </template>

    <el-skeleton v-if="loading" :rows="5" animated />
    <el-form v-else ref="formRef" :model="form" :rules="rules" label-position="top" class="security-reminder-form">
      <div v-if="reminderReasons.length" class="security-reminder-reasons">
        <el-tag v-for="reason in reminderReasons" :key="reason" effect="plain" type="primary">{{ reason }}</el-tag>
      </div>

      <section class="security-reminder-section">
        <div class="security-reminder-section__title">
          <span>修改密码</span>
          <small v-if="!isPasswordRequired">可选</small>
          <small v-else>必填</small>
        </div>
        <div class="security-reminder-grid">
          <el-form-item label="当前密码" prop="oldPassword">
            <el-input v-model="form.oldPassword" type="password" show-password autocomplete="current-password" placeholder="请输入当前密码" />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="form.newPassword" type="password" show-password autocomplete="new-password" placeholder="至少 8 位" />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input v-model="form.confirmPassword" type="password" show-password autocomplete="new-password" placeholder="请再次输入新密码" />
          </el-form-item>
        </div>
      </section>

      <section class="security-reminder-section">
        <div class="security-reminder-section__title">
          <span>完善联系方式</span>
        </div>
        <div class="security-reminder-grid security-reminder-grid--contact">
          <el-form-item label="手机号" prop="phonenumber">
            <el-input v-model="form.phonenumber" maxlength="11" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" maxlength="50" placeholder="请输入邮箱" />
          </el-form-item>
        </div>
      </section>
    </el-form>

    <template #footer>
      <div class="security-reminder-footer">
        <el-checkbox v-model="form.noRemind">不再提醒</el-checkbox>
        <div class="security-reminder-actions">
          <el-button @click="closeLater">稍后处理</el-button>
          <el-button type="primary" :loading="saving" @click="saveSecurityInfo">保存修改</el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { getUserProfile, updateUserProfile, updateUserPwd } from '@/api/system/user';
import type { UserVO } from '@/api/system/user/types';
import { getSecurityReminderConfig } from '@/api/system/workbench';
import type { SecurityReminderConfig } from '@/api/system/workbench/types';
import { useUserStore } from '@/store/modules/user';
import { validEmail } from '@/utils/validate';

type ProfileUser = Partial<UserVO> & Record<string, any>;

const defaultSecurityReminderConfig: SecurityReminderConfig = {
  enabled: true,
  checkDefaultPassword: true,
  checkMissingPhone: true,
  checkMissingEmail: true
};

const defaultPasswordFlagKeys = ['defaultPasswordFlag', 'isDefaultPassword', 'initPasswordFlag', 'pwdResetFlag'];
const phonePattern = /^1[3456789][0-9]\d{8}$/;

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const userStore = useUserStore();
const formRef = ref<ElFormInstance>();
const visible = ref(false);
const loading = ref(false);
const saving = ref(false);
const profileUser = ref<ProfileUser>({});
const config = reactive<SecurityReminderConfig>({ ...defaultSecurityReminderConfig });

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
  phonenumber: '',
  email: '',
  noRemind: false
});

const currentUserId = computed(() => profileUser.value.userId || userStore.userId);
const ignoreKey = computed(() => (currentUserId.value ? `security-reminder-ignore:${currentUserId.value}` : ''));
const isBlank = (value: unknown) => !String(value ?? '').trim();
const normalizedPhone = computed(() => form.phonenumber.trim());
const normalizedEmail = computed(() => form.email.trim());

const isTruthyFlag = (value: unknown) => value === true || value === 'true' || value === '1' || value === 1 || value === 'Y' || value === 'yes';

const isDefaultPassword = computed(() => {
  // TODO: 后端后续可在 /system/user/profile 返回 defaultPasswordFlag，用于准确判断默认密码登录。
  return defaultPasswordFlagKeys.some((key) => isTruthyFlag(profileUser.value?.[key]));
});

const isPasswordRequired = computed(() => {
  return isDefaultPassword.value || Boolean(form.oldPassword || form.newPassword || form.confirmPassword);
});

const reminderReasons = computed(() => {
  const reasons: string[] = [];
  if (config.checkDefaultPassword && isDefaultPassword.value) reasons.push('疑似默认密码');
  if (config.checkMissingPhone && isBlank(profileUser.value.phonenumber)) reasons.push('手机号未填写');
  if (config.checkMissingEmail && isBlank(profileUser.value.email)) reasons.push('邮箱未填写');
  return reasons;
});

const parseSecurityReminderConfig = (raw: unknown): SecurityReminderConfig => {
  try {
    const parsed = typeof raw === 'string' ? JSON.parse(raw || '{}') : raw || {};
    return {
      enabled: (parsed as any).enabled !== false,
      checkDefaultPassword: (parsed as any).checkDefaultPassword !== false,
      checkMissingPhone: (parsed as any).checkMissingPhone !== false,
      checkMissingEmail: (parsed as any).checkMissingEmail !== false
    };
  } catch {
    return { ...defaultSecurityReminderConfig };
  }
};

const isIgnored = () => {
  if (!ignoreKey.value || typeof localStorage === 'undefined') return false;
  return localStorage.getItem(ignoreKey.value) === '1';
};

const persistIgnoreIfNeeded = () => {
  if (form.noRemind && ignoreKey.value && typeof localStorage !== 'undefined') {
    localStorage.setItem(ignoreKey.value, '1');
  }
};

const validateOldPassword = (_rule: any, value: string, callback: (error?: Error) => void) => {
  if (isPasswordRequired.value && !value) {
    callback(new Error('请输入当前密码'));
    return;
  }
  callback();
};

const validateNewPassword = (_rule: any, value: string, callback: (error?: Error) => void) => {
  if (isPasswordRequired.value && !value) {
    callback(new Error('请输入新密码'));
    return;
  }
  if (value && value.length < 8) {
    callback(new Error('新密码至少 8 位'));
    return;
  }
  callback();
};

const validateConfirmPassword = (_rule: any, value: string, callback: (error?: Error) => void) => {
  if (isPasswordRequired.value && !value) {
    callback(new Error('请确认新密码'));
    return;
  }
  if (value && value !== form.newPassword) {
    callback(new Error('两次输入的新密码不一致'));
    return;
  }
  callback();
};

const validatePhone = (_rule: any, value: string, callback: (error?: Error) => void) => {
  const phone = String(value || '').trim();
  if (config.checkMissingPhone && !phone) {
    callback(new Error('请输入手机号'));
    return;
  }
  if (phone && !phonePattern.test(phone)) {
    callback(new Error('请输入正确的手机号'));
    return;
  }
  callback();
};

const validateEmail = (_rule: any, value: string, callback: (error?: Error) => void) => {
  const email = String(value || '').trim();
  if (config.checkMissingEmail && !email) {
    callback(new Error('请输入邮箱'));
    return;
  }
  if (email && !validEmail(email)) {
    callback(new Error('请输入正确的邮箱'));
    return;
  }
  callback();
};

const rules = computed<ElFormRules>(() => ({
  oldPassword: [{ validator: validateOldPassword, trigger: 'blur' }],
  newPassword: [{ validator: validateNewPassword, trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
  phonenumber: [{ validator: validatePhone, trigger: ['blur', 'change'] }],
  email: [{ validator: validateEmail, trigger: ['blur', 'change'] }]
}));

const loadReminder = async () => {
  loading.value = true;
  try {
    const configRes = await getSecurityReminderConfig().catch(() => ({ data: defaultSecurityReminderConfig }));
    Object.assign(config, parseSecurityReminderConfig((configRes as any).data));
    if (!config.enabled) return;

    const profileRes = await getUserProfile();
    profileUser.value = { ...(profileRes.data?.user || {}) };
    form.phonenumber = String(profileUser.value.phonenumber || '');
    form.email = String(profileUser.value.email || '');

    if (!isIgnored() && reminderReasons.value.length > 0) {
      visible.value = true;
    }
  } finally {
    loading.value = false;
  }
};

const closeLater = () => {
  persistIgnoreIfNeeded();
  visible.value = false;
};

const saveSecurityInfo = () => {
  formRef.value?.validate(async (valid: boolean) => {
    if (!valid) return;
    saving.value = true;
    try {
      if (isPasswordRequired.value) {
        await updateUserPwd(form.oldPassword, form.newPassword);
      }

      const phoneChanged = normalizedPhone.value !== String(profileUser.value.phonenumber || '');
      const emailChanged = normalizedEmail.value !== String(profileUser.value.email || '');
      if (phoneChanged || emailChanged) {
        await updateUserProfile({
          ...profileUser.value,
          phonenumber: normalizedPhone.value,
          email: normalizedEmail.value
        } as any);
      }

      await userStore.getInfo();
      persistIgnoreIfNeeded();
      proxy?.$modal.msgSuccess('账号安全信息已更新');
      visible.value = false;
    } finally {
      saving.value = false;
    }
  });
};

onMounted(() => {
  loadReminder();
});
</script>

<style lang="scss">
.security-reminder-overlay {
  background: linear-gradient(135deg, rgba(219, 234, 254, 0.54), rgba(239, 246, 255, 0.44), rgba(236, 253, 245, 0.26));
  backdrop-filter: blur(8px);
}

.security-reminder-dialog.el-dialog {
  overflow: hidden;
  border: 1px solid rgba(147, 197, 253, 0.52);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 24px 80px rgba(37, 99, 235, 0.18);
  backdrop-filter: blur(18px);
}

.security-reminder-dialog {
  .el-dialog__header {
    margin: 0;
    padding: 24px 28px 12px;
  }

  .el-dialog__body {
    padding: 10px 28px 6px;
  }

  .el-dialog__footer {
    padding: 14px 28px 24px;
  }
}

.security-reminder-head {
  display: flex;
  gap: 14px;
  align-items: flex-start;

  h2 {
    margin: 0;
    color: #0f2f5f;
    font-size: 22px;
    font-weight: 900;
  }

  p {
    margin: 8px 0 0;
    max-width: 560px;
    color: #49627d;
    font-size: 14px;
    line-height: 1.7;
  }
}

.security-reminder-icon {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  border: 1px solid rgba(37, 99, 235, 0.18);
  border-radius: 14px;
  color: #2563eb;
  background: linear-gradient(180deg, rgba(239, 246, 255, 0.94), rgba(219, 234, 254, 0.72));
  box-shadow: 0 10px 26px rgba(37, 99, 235, 0.12);
  font-size: 20px;
}

.security-reminder-form {
  display: grid;
  gap: 14px;
}

.security-reminder-reasons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.security-reminder-section {
  padding: 16px;
  border: 1px solid rgba(191, 219, 254, 0.8);
  border-radius: 18px;
  background: rgba(248, 251, 255, 0.72);
}

.security-reminder-section__title {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #0f2f5f;
  font-weight: 800;

  small {
    color: #5f7b97;
    font-size: 12px;
    font-weight: 700;
  }
}

.security-reminder-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;

  .el-form-item {
    margin-bottom: 0;
  }
}

.security-reminder-grid--contact {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.security-reminder-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.security-reminder-actions {
  display: flex;
  gap: 10px;
}

@media (max-width: 720px) {
  .security-reminder-dialog {
    .el-dialog__header {
      padding: 22px 18px 10px;
    }

    .el-dialog__body {
      padding: 8px 18px 4px;
    }

    .el-dialog__footer {
      padding: 12px 18px 20px;
    }
  }

  .security-reminder-grid,
  .security-reminder-grid--contact {
    grid-template-columns: 1fr;
  }

  .security-reminder-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .security-reminder-actions {
    width: 100%;

    .el-button {
      flex: 1;
    }
  }
}
</style>
