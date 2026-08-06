<template>
  <main class="activation-page">
    <section class="activation-card">
      <div class="activation-brand">CREHN</div>
      <h1>参赛者账号激活</h1>
      <p>请使用学校联络员发放的一次性激活码，由参赛者本人设置密码。</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <el-form-item label="一次性激活码" prop="activationCode">
          <el-input v-model.trim="form.activationCode" maxlength="32" autocomplete="one-time-code" />
        </el-form-item>
        <el-button class="activation-preview-button" :loading="previewing" @click="previewProfile">核对本人资料</el-button>
        <el-descriptions v-if="preview" class="activation-preview" :column="1" border>
          <el-descriptions-item label="姓名">{{ preview.participantName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="学校">{{ preview.schoolName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="活动">{{ preview.activityName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="证件号">{{ preview.identityNoMasked || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ preview.phonenumberMasked || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ preview.emailMasked || '-' }}</el-descriptions-item>
        </el-descriptions>
        <el-form-item label="设置密码" prop="password">
          <el-input v-model="form.password" type="password" show-password maxlength="30" autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password maxlength="30" autocomplete="new-password" />
        </el-form-item>
        <el-checkbox v-model="confirmed">我确认由本人激活账号，并已核对学校发放的个人资料</el-checkbox>
        <el-button class="activation-submit" type="primary" :loading="submitting" @click="submit">确认激活</el-button>
      </el-form>
      <router-link to="/login">已有账号，返回登录</router-link>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { activateParticipant, previewParticipantActivation, type ParticipantActivationPreview } from '@/api/crehn/participant';

const router = useRouter();
const formRef = ref<FormInstance>();
const submitting = ref(false);
const previewing = ref(false);
const confirmed = ref(false);
const preview = ref<ParticipantActivationPreview>();
const previewedCode = ref('');
const form = reactive({ activationCode: '', password: '', confirmPassword: '' });
const rules: FormRules = {
  activationCode: [{ required: true, message: '请输入激活码', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 30, message: '密码长度为8至30位', trigger: 'blur' },
    {
      validator: (_rule, value, callback) =>
        /[A-Za-z]/.test(value) && /\d/.test(value) ? callback() : callback(new Error('密码必须同时包含字母和数字')),
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    {
      validator: (_rule, value, callback) => (value === form.password ? callback() : callback(new Error('两次输入的密码不一致'))),
      trigger: 'blur'
    }
  ]
};

watch(
  () => form.activationCode,
  () => {
    preview.value = undefined;
    previewedCode.value = '';
    confirmed.value = false;
  }
);

const previewProfile = async () => {
  await formRef.value?.validateField('activationCode');
  previewing.value = true;
  try {
    const { data } = await previewParticipantActivation(form.activationCode);
    preview.value = data;
    previewedCode.value = form.activationCode;
  } finally {
    previewing.value = false;
  }
};

const submit = async () => {
  await formRef.value?.validate();
  if (!preview.value || previewedCode.value !== form.activationCode) {
    ElMessage.warning('请先使用当前激活码核对本人资料');
    return;
  }
  if (!confirmed.value) {
    ElMessage.warning('请先确认由本人激活并核对资料');
    return;
  }
  submitting.value = true;
  try {
    await activateParticipant({
      activationCode: form.activationCode,
      password: form.password,
      profileConfirmation: 'confirmed'
    });
    ElMessage.success('账号激活成功，请登录');
    await router.replace('/login');
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
.activation-page {
  min-height: 100%;
  display: grid;
  place-items: center;
  padding: 24px;
  background: linear-gradient(145deg, #edf5ff, #f9fbff 55%, #e8fff9);
}
.activation-card {
  width: min(460px, 100%);
  padding: 36px;
  border: 1px solid #dbe7f5;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 18px 60px rgb(39 88 145 / 12%);
}
.activation-brand {
  color: #2563eb;
  font-weight: 800;
  letter-spacing: 0.18em;
}
.activation-card h1 {
  margin: 10px 0;
}
.activation-card p {
  margin: 0 0 24px;
  color: #64748b;
}
.activation-submit {
  width: 100%;
  margin: 22px 0 16px;
}
.activation-preview-button {
  width: 100%;
  margin-bottom: 16px;
}
.activation-preview {
  margin-bottom: 20px;
}
</style>
