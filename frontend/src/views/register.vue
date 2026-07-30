<template>
  <div class="register">
    <el-form ref="registerRef" :model="registerForm" :rules="registerRules" class="register-form">
      <div class="title-box">
        <h3 class="title">{{ title }}</h3>
        <lang-select />
      </div>
      <el-form-item prop="userType">
        <el-select v-model="registerForm.userType" size="large" placeholder="请选择注册类型" style="width: 100%">
          <el-option label="学校账号" value="school" />
          <el-option label="专家账号" value="expert" />
          <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon" /></template>
        </el-select>
      </el-form-item>
      <el-form-item prop="username">
        <el-input v-model="registerForm.username" type="text" size="large" auto-complete="off" :placeholder="proxy.$t('register.username')">
          <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon" /></template>
        </el-input>
      </el-form-item>
      <el-form-item prop="phonenumber">
        <el-input v-model="registerForm.phonenumber" type="text" size="large" auto-complete="off" placeholder="手机号">
          <template #prefix><svg-icon icon-class="phone" class="el-input__icon input-icon" /></template>
        </el-input>
      </el-form-item>
      <el-form-item prop="registrationCode">
        <el-input v-model="registerForm.registrationCode" type="text" size="large" auto-complete="off" placeholder="注册码">
          <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
        </el-input>
      </el-form-item>
      <template v-if="registerForm.userType === 'school'">
        <el-form-item>
          <el-radio-group v-model="schoolMode" size="large" class="school-mode">
            <el-radio-button label="select">选择已有学校</el-radio-button>
            <el-radio-button label="create">新增学校</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="schoolMode === 'select'">
          <el-select v-model="registerForm.schoolId" filterable clearable size="large" placeholder="选择学校名称" style="width: 100%">
            <el-option v-for="item in schoolOptions" :key="item.id" :label="item.schoolName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-else>
          <el-input v-model="registerForm.schoolName" type="text" size="large" auto-complete="off" placeholder="填写学校名称，不能与已有学校重复">
            <template #prefix><svg-icon icon-class="company" class="el-input__icon input-icon" /></template>
          </el-input>
        </el-form-item>
      </template>
      <el-form-item prop="password">
        <el-input
          v-model="registerForm.password"
          type="password"
          size="large"
          auto-complete="off"
          :placeholder="proxy.$t('register.password')"
          @keyup.enter="handleRegister"
        >
          <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
        </el-input>
      </el-form-item>
      <el-form-item prop="confirmPassword">
        <el-input
          v-model="registerForm.confirmPassword"
          type="password"
          size="large"
          auto-complete="off"
          :placeholder="proxy.$t('register.confirmPassword')"
          @keyup.enter="handleRegister"
        >
          <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
        </el-input>
      </el-form-item>
      <el-form-item v-if="captchaEnabled" prop="code">
        <el-input
          v-model="registerForm.code"
          size="large"
          auto-complete="off"
          :placeholder="proxy.$t('register.code')"
          style="width: 63%"
          @keyup.enter="handleRegister"
        >
          <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
        </el-input>
        <div class="register-code">
          <img :src="codeUrl" class="register-code-img" @click="getCode" />
        </div>
      </el-form-item>
      <el-form-item style="width: 100%">
        <el-button :loading="loading" size="large" type="primary" style="width: 100%" @click.prevent="handleRegister">
          <span v-if="!loading">{{ proxy.$t('register.register') }}</span>
          <span v-else>{{ proxy.$t('register.registering') }}</span>
        </el-button>
        <div style="float: right">
          <router-link class="link-type" :to="'/login'">{{ proxy.$t('register.switchLoginPage') }}</router-link>
        </div>
      </el-form-item>
    </el-form>
    <!--  底部  -->
    <div class="el-register-footer">
      <span>Copyright © 2018-2026 疯狂的狮子Li All Rights Reserved.</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { getCodeImg, getPublicSchoolOptions, getRegisterNotice, register } from '@/api/login';
import { RegisterForm } from '@/api/types';
import { to } from 'await-to-js';
import { useI18n } from 'vue-i18n';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const title = import.meta.env.VITE_APP_TITLE;
const router = useRouter();

const { t } = useI18n();

const registerForm = ref<RegisterForm>({
  tenantId: '000000',
  username: '',
  password: '',
  confirmPassword: '',
  code: '',
  uuid: '',
  userType: 'school',
  phonenumber: '',
  registrationCode: '',
  schoolId: undefined,
  schoolName: ''
});

const schoolMode = ref<'select' | 'create'>('select');
const schoolOptions = ref<any[]>([]);

const equalToPassword = (rule: any, value: string, callback: any) => {
  if (registerForm.value.password !== value) {
    callback(new Error(t('register.rule.confirmPassword.equalToPassword')));
  } else {
    callback();
  }
};

const registerRules: ElFormRules = {
  userType: [{ required: true, trigger: 'change', message: '请选择注册类型' }],
  username: [
    { required: true, trigger: 'blur', message: t('register.rule.username.required') },
    { min: 2, max: 20, message: t('register.rule.username.length', { min: 2, max: 20 }), trigger: 'blur' }
  ],
  password: [
    { required: true, trigger: 'blur', message: t('register.rule.password.required') },
    { min: 5, max: 20, message: t('register.rule.password.length', { min: 5, max: 20 }), trigger: 'blur' },
    { pattern: /^[^<>"'|\\]+$/, message: t('register.rule.password.pattern', { strings: '< > " \' \\ |' }), trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, trigger: 'blur', message: t('register.rule.confirmPassword.required') },
    { required: true, validator: equalToPassword, trigger: 'blur' }
  ],
  phonenumber: [
    { required: true, trigger: 'blur', message: '请输入手机号' },
    { pattern: /^1[3456789][0-9]\d{8}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  registrationCode: [{ required: true, trigger: 'blur', message: '请输入注册码' }],
  code: [{ required: true, trigger: 'change', message: t('register.rule.code.required') }]
};
const codeUrl = ref('');
const loading = ref(false);
const captchaEnabled = ref(true);
const registerRef = ref<ElFormInstance>();

const handleRegister = () => {
  registerRef.value?.validate(async (valid: boolean) => {
    if (valid) {
      if (registerForm.value.userType === 'school') {
        if (schoolMode.value === 'select') {
          registerForm.value.schoolName = '';
        } else {
          registerForm.value.schoolId = undefined;
        }
        if (!registerForm.value.schoolId && !registerForm.value.schoolName) {
          ElMessage.error('请选择已有学校或填写新学校名称');
          return;
        }
      }
      loading.value = true;
      const [err] = await to(register(registerForm.value));
      if (!err) {
        const username = registerForm.value.username;
        const noticeRes = await getRegisterNotice({ userType: registerForm.value.userType });
        const notices = noticeRes.data || [];
        const noticeHtml = notices.length
          ? notices.map((item: any) => `<style>${safeCss(item.customCss)}</style><h3 class="notice-title">${escapeHtml(item.noticeTitle)}</h3><div class="notice-content">${safeContent(item.noticeContent)}</div>`).join('<hr/>')
          : '<span style="color: red; ">' + t('register.registerSuccess', { username }) + '</span>';
        await ElMessageBox.alert(noticeHtml, notices.length ? '注册公告' : '系统提示', {
          confirmButtonText: '确定',
          dangerouslyUseHTMLString: true,
          type: 'success'
        });
        await router.push('/login');
      } else {
        loading.value = false;
        if (captchaEnabled.value) {
          getCode();
        }
      }
    }
  });
};

const getCode = async () => {
  const res = await getCodeImg();
  const { data } = res;
  captchaEnabled.value = data.captchaEnabled === undefined ? true : data.captchaEnabled;
  if (captchaEnabled.value) {
    codeUrl.value = 'data:image/png;base64,' + data.img;
    registerForm.value.uuid = data.uuid;
  }
};

const initSchoolOptions = async () => {
  const { data } = await getPublicSchoolOptions({ status: 'enabled' });
  schoolOptions.value = data || [];
};

const escapeHtml = (value?: string) => String(value || '').replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char] || char));
const safeContent = (value?: string) => escapeHtml(value).replace(/\n/g, '<br/>');
const safeCss = (value?: string) => String(value || '').replace(/<\/?script[\s\S]*?>/gi, '');

onMounted(() => {
  getCode();
  initSchoolOptions();
});
</script>

<style lang="scss" scoped>
.register {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  background-image: url('../assets/images/login-background.jpg');
  background-size: cover;
}

.title-box {
  display: flex;

  .title {
    margin: 0px auto 30px auto;
    text-align: center;
    color: #707070;
  }

  :deep(.lang-select--style) {
    line-height: 0;
    color: #7483a3;
  }
}

.register-form {
  border-radius: 6px;
  background: #ffffff;
  width: 400px;
  padding: 25px 25px 5px 25px;

  .el-input {
    height: 40px;

    input {
      height: 40px;
    }
  }

  .input-icon {
    height: 39px;
    width: 14px;
    margin-left: 0;
  }
}

.register-tip {
  font-size: 13px;
  text-align: center;
  color: #bfbfbf;
}

.register-code {
  width: 33%;
  height: 40px;
  float: right;

  img {
    cursor: pointer;
    vertical-align: middle;
  }
}

.el-register-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: #fff;
  font-family: Arial, serif;
  font-size: 12px;
  letter-spacing: 1px;
}

.register-code-img {
  height: 40px;
  padding-left: 12px;
}

.school-mode {
  width: 100%;

  :deep(.el-radio-button) {
    width: 50%;
  }

  :deep(.el-radio-button__inner) {
    width: 100%;
  }
}
</style>
