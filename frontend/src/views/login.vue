<template>
  <div class="login-page" :class="`login-page--${homeVersion}`">
    <div class="art-motion" aria-hidden="true">
      <span class="art-wash wash-a"></span>
      <span class="art-wash wash-b"></span>
      <span class="art-ribbon ribbon-a"></span>
      <span class="art-ribbon ribbon-b"></span>
      <span class="art-ribbon ribbon-c"></span>
      <span class="art-glass glass-a"></span>
      <span class="art-glass glass-b"></span>
      <span class="blur-shape blur-dome-left"></span>
      <span class="blur-shape blur-dome-right"></span>
      <span class="blur-shape blur-sun"></span>
      <span class="blur-shape blur-prism"></span>
      <span class="blur-shape blur-slate"></span>
      <span class="blur-spark spark-a"></span>
      <span class="blur-spark spark-b"></span>
      <span class="art-shine"></span>
      <span class="art-strokes"></span>
      <span class="art-particles"></span>
    </div>

    <div class="login-line-art" aria-hidden="true">
      <span class="line line-a"></span>
      <span class="line line-b"></span>
      <span class="line line-c"></span>
    </div>

    <header class="login-header">
      <button class="brand" type="button" @click="handleHomeRequest">
        <span class="brand-mark" aria-hidden="true">CH</span>
        <span class="brand-name">{{ loginBrandName }}</span>
      </button>
    </header>

    <main class="login-shell">
      <section class="hero-copy" :aria-label="text.ariaLabels.heroInfo">
        <span class="section-eyebrow">{{ text.brandKicker }}</span>
        <h1 class="slogan">
          <span v-for="(line, lineIndex) in text.sloganLines" :key="line" class="slogan-line">
            <span
              v-for="(char, charIndex) in line.split('')"
              :key="`${line}-${charIndex}`"
              class="slogan-char"
              :class="sloganColorClass(lineIndex, charIndex)"
              :data-char="char"
            >
              {{ char }}
            </span>
          </span>
        </h1>
        <p v-if="text.subtitle" class="hero-subtitle">{{ text.subtitle }}</p>
        <p v-if="text.description" class="hero-desc">{{ text.description }}</p>
        <div v-if="text.meta.length" class="hero-meta">
          <span v-for="item in text.meta" :key="item">{{ item }}</span>
        </div>
      </section>

      <section class="login-panel" :aria-label="text.formTitle">
          <el-form
            v-if="!showRegisterCard"
            key="form"
            ref="loginRef"
            :model="loginForm"
            :rules="loginRules"
            class="login-form"
          >
            <div class="form-topline">
              <span>{{ text.formKicker }}</span>
              <strong>{{ text.formTitle }}</strong>
            </div>

            <el-form-item prop="username">
              <el-input v-model="loginForm.username" type="text" size="large" auto-complete="off" :placeholder="text.usernamePlaceholder" />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                show-password
                size="large"
                auto-complete="off"
                :placeholder="text.passwordPlaceholder"
                @keyup.enter="handleLogin"
              />
            </el-form-item>

            <el-form-item v-if="captchaEnabled" prop="code" class="captcha-item">
              <el-input
                v-model="loginForm.code"
                class="captcha-input"
                size="large"
                auto-complete="off"
                :placeholder="text.codePlaceholder"
                @keyup.enter="handleLogin"
              />
              <button class="login-code" type="button" :title="text.captchaRefreshTitle" @click="getCode">
                <img v-if="codeUrl && !codeLoadFailed" :src="codeUrl" class="login-code-img" :alt="text.captchaAlt" @error="codeLoadFailed = true" />
                <span v-else class="login-code-fallback">{{ text.captchaRefreshTitle }}</span>
              </button>
            </el-form-item>

            <div class="form-options">
              <el-checkbox v-model="loginForm.rememberMe">{{ text.rememberPassword }}</el-checkbox>
              <button v-if="registerEnabled" class="register-link" type="button" @click="openRegisterCard">{{ text.registerText }}</button>
            </div>

            <div class="form-actions">
              <el-form-item class="submit-item">
                <el-button :loading="loading" size="large" type="primary" @click.prevent="handleLogin">
                  <span v-if="!loading">{{ text.loginButton }}</span>
                  <span v-else>{{ text.loggingButton }}</span>
                </el-button>
              </el-form-item>

            </div>
          </el-form>

          <el-form
            v-else
            key="register"
            ref="registerRef"
            :model="registerForm"
            :rules="registerRules"
            class="login-form register-card"
          >
            <div class="form-topline">
              <span>{{ text.registerCard.currentLabel }}</span>
              <strong>{{ text.registerCard.title }}</strong>
            </div>

            <div class="title-box">
              <div>
                <p class="form-kicker">{{ text.registerCard.kicker }}</p>
                <h2 class="title">{{ text.registerCard.title }}</h2>
              </div>
            </div>
            <p class="form-subtitle">{{ text.registerCard.subtitle }}</p>

            <div class="register-scroll">
              <el-form-item prop="userType">
                <el-select v-model="registerForm.userType" size="large" :placeholder="text.registerCard.userTypePlaceholder" style="width: 100%">
                  <el-option :label="text.registerCard.schoolUserType" value="school" />
                  <el-option :label="text.registerCard.expertUserType" value="expert" />
                </el-select>
              </el-form-item>

              <el-form-item prop="username">
                <el-input v-model="registerForm.username" type="text" size="large" auto-complete="off" :placeholder="text.registerCard.usernamePlaceholder" />
              </el-form-item>

              <el-form-item prop="phonenumber">
                <el-input v-model="registerForm.phonenumber" type="text" size="large" auto-complete="off" :placeholder="text.registerCard.phonePlaceholder" />
              </el-form-item>

              <el-form-item prop="registrationCode">
                <el-input
                  v-model="registerForm.registrationCode"
                  type="text"
                  size="large"
                  auto-complete="off"
                  :placeholder="text.registerCard.registrationCodePlaceholder"
                />
              </el-form-item>

              <template v-if="registerForm.userType === 'school'">
                <el-form-item>
                  <el-radio-group v-model="schoolMode" size="large" class="school-mode">
                    <el-radio-button label="select">{{ text.registerCard.schoolModeSelect }}</el-radio-button>
                    <el-radio-button label="create">{{ text.registerCard.schoolModeCreate }}</el-radio-button>
                  </el-radio-group>
                </el-form-item>
                <el-form-item v-if="schoolMode === 'select'">
                  <el-select
                    v-model="registerForm.schoolId"
                    filterable
                    clearable
                    size="large"
                    :placeholder="text.registerCard.schoolSelectPlaceholder"
                    style="width: 100%"
                  >
                    <el-option v-for="item in schoolOptions" :key="item.id" :label="item.schoolName" :value="item.id" />
                  </el-select>
                </el-form-item>
                <el-form-item v-else>
                  <el-input
                    v-model="registerForm.schoolName"
                    type="text"
                    size="large"
                    auto-complete="off"
                    :placeholder="text.registerCard.schoolNamePlaceholder"
                  />
                </el-form-item>
              </template>

              <el-form-item prop="password">
                <el-input
                  v-model="registerForm.password"
                  type="password"
                  size="large"
                  auto-complete="off"
                  :placeholder="text.registerCard.passwordPlaceholder"
                  @keyup.enter="handleRegister"
                />
              </el-form-item>

              <el-form-item prop="confirmPassword">
                <el-input
                  v-model="registerForm.confirmPassword"
                  type="password"
                  size="large"
                  auto-complete="off"
                  :placeholder="text.registerCard.confirmPasswordPlaceholder"
                  @keyup.enter="handleRegister"
                />
              </el-form-item>

              <el-form-item v-if="registerCaptchaEnabled" prop="code" class="captcha-item">
                <el-input
                  v-model="registerForm.code"
                  class="captcha-input"
                  size="large"
                  auto-complete="off"
                  :placeholder="text.registerCard.codePlaceholder"
                  @keyup.enter="handleRegister"
                />
                <button class="login-code" type="button" :title="text.captchaRefreshTitle" @click="getRegisterCode">
                  <img
                    v-if="registerCodeUrl && !registerCodeLoadFailed"
                    :src="registerCodeUrl"
                    class="login-code-img"
                    :alt="text.captchaAlt"
                    @error="registerCodeLoadFailed = true"
                  />
                  <span v-else class="login-code-fallback">{{ text.captchaRefreshTitle }}</span>
                </button>
              </el-form-item>
            </div>

            <div class="form-actions">
              <el-form-item class="submit-item">
                <el-button :loading="registerLoading" size="large" type="primary" @click.prevent="handleRegister">
                  <span v-if="!registerLoading">{{ text.registerCard.submitButton }}</span>
                  <span v-else>{{ text.registerCard.submittingButton }}</span>
                </el-button>
              </el-form-item>

              <button class="back-entry" type="button" @click="backToLogin">
                {{ text.registerCard.backToLoginText }}
              </button>
            </div>
          </el-form>
      </section>
    </main>

    <footer class="login-footer">
      <span>{{ text.footer }}</span>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { getCodeImg, getPublicSchoolOptions, getRegisterNotice, getTenantList, register as registerAccount } from '@/api/login';
import { useUserStore } from '@/store/modules/user';
import { LoginData, RegisterForm, TenantVO } from '@/api/types';
import { to } from 'await-to-js';
import { loginPageText } from '@/config/loginPage';

const text = loginPageText;
const registerIntentKey = 'CREHN_OPEN_REGISTER_CARD';
const portalHomePath = '/crehn/';
const sloganColorClasses = ['c-blue', 'c-blue', 'c-teal', 'c-green', 'c-green', 'c-lime', 'c-gold', 'c-coral'];
const sloganColorClass = (lineIndex: number, charIndex: number) => sloganColorClasses[(lineIndex * 4 + charIndex) % sloganColorClasses.length];
const loginBrandName = computed(() => text.brandName);
const userStore = useUserStore();
const router = useRouter();
const homeVersion = computed<'v1' | 'v2'>(() => router.currentRoute.value.query.homeVersion === 'v2' ? 'v2' : 'v1');

const loginForm = ref<LoginData>({
  tenantId: '000000',
  username: '',
  password: '',
  rememberMe: false,
  code: '',
  uuid: ''
} as LoginData);

const loginRules: ElFormRules = {
  tenantId: [{ required: true, trigger: 'blur', message: text.validation.tenantIdRequired }],
  username: [{ required: true, trigger: 'blur', message: text.validation.usernameRequired }],
  password: [{ required: true, trigger: 'blur', message: text.validation.passwordRequired }],
  code: [{ required: true, trigger: 'change', message: text.validation.codeRequired }]
};

const codeUrl = ref('');
const codeLoadFailed = ref(false);
const loading = ref(false);
const captchaEnabled = ref(true);
const tenantEnabled = ref(true);
const registerEnabled = ref(true);
const redirect = ref('/');
const loginRef = ref<ElFormInstance>();
const tenantList = ref<TenantVO[]>([]);
const showRegisterCard = ref(false);

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
const registerRef = ref<ElFormInstance>();
const registerCodeUrl = ref('');
const registerCodeLoadFailed = ref(false);
const registerLoading = ref(false);
const registerCaptchaEnabled = ref(true);
const schoolMode = ref<'select' | 'create'>('select');
const schoolOptions = ref<any[]>([]);

const equalToPassword = (rule: any, value: string, callback: any) => {
  if (registerForm.value.password !== value) {
    callback(new Error(text.validation.confirmPasswordEqual));
  } else {
    callback();
  }
};

const registerRules: ElFormRules = {
  userType: [{ required: true, trigger: 'change', message: text.validation.userTypeRequired }],
  username: [
    { required: true, trigger: 'blur', message: text.validation.usernameRequired },
    { min: 2, max: 20, message: text.validation.usernameLength, trigger: 'blur' }
  ],
  password: [
    { required: true, trigger: 'blur', message: text.validation.passwordRequired },
    { min: 5, max: 20, message: text.validation.passwordLength, trigger: 'blur' },
    { pattern: /^[^<>"'|\\]+$/, message: text.validation.passwordPattern, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, trigger: 'blur', message: text.validation.confirmPasswordRequired },
    { validator: equalToPassword, trigger: 'blur' }
  ],
  phonenumber: [
    { required: true, trigger: 'blur', message: text.validation.phoneRequired },
    { pattern: /^1[3456789][0-9]\d{8}$/, message: text.validation.phonePattern, trigger: 'blur' }
  ],
  registrationCode: [{ required: true, trigger: 'blur', message: text.validation.registrationCodeRequired }],
  code: [{ required: true, trigger: 'change', message: text.validation.codeRequired }]
};

const prepareRegisterCard = async () => {
  if (registerCaptchaEnabled.value && !registerCodeUrl.value) {
    await getRegisterCode();
  }
  if (schoolOptions.value.length === 0) {
    await initSchoolOptions();
  }
};

const openRegisterCard = async () => {
  showRegisterCard.value = true;
  await prepareRegisterCard();
};

const backToLogin = async () => {
  showRegisterCard.value = false;
  if (captchaEnabled.value && !codeUrl.value) {
    await getCode();
  }
};

const shouldOpenRegisterCardOnLoad = () => {
  const query = router.currentRoute.value.query || {};
  const requestedByQuery = query.register === '1' || query.mode === 'register';
  let requestedByStorage = false;
  try {
    requestedByStorage = sessionStorage.getItem(registerIntentKey) === '1';
    sessionStorage.removeItem(registerIntentKey);
  } catch (error) {
    requestedByStorage = localStorage.getItem(registerIntentKey) === '1';
    localStorage.removeItem(registerIntentKey);
  }
  return requestedByQuery || requestedByStorage;
};

const clearRegisterOpenQuery = async () => {
  const route = router.currentRoute.value;
  const query = { ...route.query };
  const hasRegisterQuery = query.register !== undefined || query.mode === 'register' || query.t !== undefined;
  if (!hasRegisterQuery) return;
  delete query.register;
  delete query.mode;
  delete query.t;
  await router.replace({ path: route.path, query, hash: route.hash });
};

const openRegisterCardFromIntent = async () => {
  if (!shouldOpenRegisterCardOnLoad()) return;
  await openRegisterCard();
  await clearRegisterOpenQuery();
};

const hasDirtyRegisterForm = () => {
  const form = registerForm.value;
  return [
    form.username,
    form.phonenumber,
    form.registrationCode,
    form.schoolId,
    form.schoolName,
    form.password,
    form.confirmPassword,
    form.code
  ].some((value) => String(value ?? '').trim().length > 0);
};

const confirmDiscardRegister = async () => {
  if (!showRegisterCard.value || !hasDirtyRegisterForm()) {
    return true;
  }
  try {
    await ElMessageBox.confirm('当前注册信息尚未提交，确定关闭并返回首页吗？', '确认关闭', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    return true;
  } catch (error) {
    return false;
  }
};

const goPortalHome = () => {
  window.location.href = `${portalHomePath}?version=${homeVersion.value}`;
};

const handleHomeRequest = async () => {
  if (await confirmDiscardRegister()) {
    goPortalHome();
  }
};

const handlePageShow = async () => {
  await openRegisterCardFromIntent();
};

const safeInternalRedirect = (value: unknown) => {
  if (typeof value !== 'string' || !value.startsWith('/') || value.startsWith('//') || value.includes('\\')) {
    return '/';
  }
  return value;
};

watch(
  () => router.currentRoute.value,
  (newRoute: any) => {
    redirect.value = safeInternalRedirect(newRoute.query?.redirect);
  },
  { immediate: true }
);

const handleLogin = () => {
  loginRef.value?.validate(async (valid: boolean, fields: any) => {
    if (valid) {
      loading.value = true;
      if (loginForm.value.rememberMe) {
        localStorage.setItem('tenantId', String(loginForm.value.tenantId));
        localStorage.setItem('username', String(loginForm.value.username));
        localStorage.setItem('rememberMe', String(loginForm.value.rememberMe));
      } else {
        localStorage.removeItem('tenantId');
        localStorage.removeItem('username');
        localStorage.removeItem('rememberMe');
      }
      localStorage.removeItem('password');
      const [err] = await to(userStore.login(loginForm.value));
      if (!err) {
        await router.push(redirect.value);
        loading.value = false;
      } else {
        loading.value = false;
        if (captchaEnabled.value) {
          await getCode();
        }
      }
    } else {
      console.warn(text.submitErrorLog, fields);
    }
  });
};

const getCode = async () => {
  const res = await getCodeImg();
  const { data } = res;
  captchaEnabled.value = data.captchaEnabled === undefined ? true : data.captchaEnabled;
  if (captchaEnabled.value) {
    loginForm.value.code = '';
    codeLoadFailed.value = false;
    codeUrl.value = 'data:image/png;base64,' + data.img;
    loginForm.value.uuid = data.uuid;
  }
};

const getRegisterCode = async () => {
  const res = await getCodeImg();
  const { data } = res;
  registerCaptchaEnabled.value = data.captchaEnabled === undefined ? true : data.captchaEnabled;
  if (registerCaptchaEnabled.value) {
    registerForm.value.code = '';
    registerCodeLoadFailed.value = false;
    registerCodeUrl.value = 'data:image/png;base64,' + data.img;
    registerForm.value.uuid = data.uuid;
  }
};

const getLoginData = () => {
  localStorage.removeItem('password');
  const tenantId = localStorage.getItem('tenantId');
  const username = localStorage.getItem('username');
  const rememberMe = localStorage.getItem('rememberMe');
  loginForm.value = {
    tenantId: tenantId === null ? String(loginForm.value.tenantId) : tenantId,
    username: username === null ? String(loginForm.value.username) : username,
    password: '',
    rememberMe: rememberMe === 'true'
  } as LoginData;
};

const initTenantList = async () => {
  const { data } = await getTenantList(false);
  tenantEnabled.value = data.tenantEnabled === undefined ? true : data.tenantEnabled;
  if (tenantEnabled.value) {
    tenantList.value = data.voList;
    if (tenantList.value != null && tenantList.value.length !== 0) {
      loginForm.value.tenantId = tenantList.value[0].tenantId;
    }
  }
};

const initSchoolOptions = async () => {
  const { data } = await getPublicSchoolOptions({ status: 'enabled' });
  schoolOptions.value = data || [];
};

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
          ElMessage.error(text.registerCard.schoolRequiredMessage);
          return;
        }
      }
      registerLoading.value = true;
      const [err] = await to(registerAccount(registerForm.value));
      if (!err) {
        const username = registerForm.value.username;
        const noticeRes = await getRegisterNotice({ userType: registerForm.value.userType });
        const notices = noticeRes.data || [];
        const noticeHtml = notices.length
          ? notices
              .map(
                (item: any) =>
                  `<style>${safeCss(item.customCss)}</style><h3 class="notice-title">${escapeHtml(item.noticeTitle)}</h3><div class="notice-content">${safeContent(item.noticeContent)}</div>`
              )
              .join('<hr/>')
          : `<span style="color: red; ">${text.registerCard.successMessage.replace('{username}', username)}</span>`;
        await ElMessageBox.alert(noticeHtml, notices.length ? text.registerCard.noticeTitle : text.registerCard.successTitle, {
          confirmButtonText: '确定',
          dangerouslyUseHTMLString: true,
          type: 'success'
        });
        registerLoading.value = false;
        showRegisterCard.value = false;
        await getCode();
      } else {
        registerLoading.value = false;
        if (registerCaptchaEnabled.value) {
          await getRegisterCode();
        }
      }
    }
  });
};

const escapeHtml = (value?: string) =>
  String(value || '').replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char] || char);
const safeContent = (value?: string) => escapeHtml(value).replace(/\n/g, '<br/>');
const safeCss = (value?: string) => String(value || '').replace(/<\/?script[\s\S]*?>/gi, '');

onMounted(async () => {
  document.title = text.pageTitle;
  getCode();
  initTenantList();
  getLoginData();
  window.addEventListener('pageshow', handlePageShow);
  await openRegisterCardFromIntent();
});

onUnmounted(() => {
  window.removeEventListener('pageshow', handlePageShow);
});
</script>

<style lang="scss" scoped>
.login-page {
  --login-white: #ffffff;
  --login-paper: #f8f5ff;
  --login-ink: #33245d;
  --login-text: #645b7d;
  --login-muted: #8d82a4;
  --login-blue-deep: #5743b7;
  --login-blue: #7b65dc;
  --login-green: #ed7b9c;
  --login-line: rgba(102, 79, 194, 0.16);
  --login-shadow: 0 24px 60px rgba(80, 56, 155, 0.14);

  position: relative;
  min-height: 100%;
  overflow: hidden;
  color: var(--login-ink);
  background:
    radial-gradient(circle at 74% 16%, rgba(243, 133, 161, 0.22), transparent 26%),
    radial-gradient(circle at 18% 22%, rgba(124, 99, 222, 0.18), transparent 30%),
    linear-gradient(150deg, #fcfaff 0%, #f6f2ff 52%, #fff7fa 100%);
}

.login-page--v2 {
  --login-paper: #f7f8ff;
  --login-ink: #172b78;
  --login-text: #4d5f93;
  --login-muted: #7787b6;
  --login-blue-deep: #2346c9;
  --login-blue: #4e6cf5;
  --login-green: #913fe7;
  --login-line: rgba(57, 85, 227, 0.18);
  --login-shadow: 0 24px 60px rgba(35, 70, 201, 0.14);
  background-color: #f7f8ff;
  background-image:
    linear-gradient(rgba(84, 104, 236, 0.1) 1px, transparent 1px),
    linear-gradient(90deg, rgba(84, 104, 236, 0.1) 1px, transparent 1px),
    radial-gradient(circle at 76% 16%, rgba(141, 74, 241, 0.18), transparent 27%),
    radial-gradient(circle at 18% 74%, rgba(67, 133, 255, 0.16), transparent 28%);
  background-size: 36px 36px, 36px 36px, auto, auto;
}

.login-page--v1 .wash-a {
  background:
    radial-gradient(circle at 44% 42%, rgba(128, 96, 224, 0.28), transparent 48%),
    radial-gradient(circle at 62% 62%, rgba(238, 121, 155, 0.22), transparent 58%);
}

.login-page--v1 .wash-b {
  background:
    radial-gradient(circle at 42% 38%, rgba(239, 132, 160, 0.26), transparent 50%),
    radial-gradient(circle at 58% 62%, rgba(114, 88, 211, 0.18), transparent 60%);
}

.login-page--v2 .wash-a,
.login-page--v2 .wash-b {
  background:
    radial-gradient(circle at 44% 42%, rgba(69, 103, 248, 0.26), transparent 48%),
    radial-gradient(circle at 62% 62%, rgba(145, 63, 231, 0.2), transparent 58%);
}

.login-page--v2 .art-ribbon,
.login-page--v2 .line {
  border-color: rgba(54, 82, 222, 0.16);
}

.login-page,
.login-page * {
  box-sizing: border-box;
}

.art-motion {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
  isolation: isolate;
}

.art-motion span {
  position: absolute;
  display: block;
  pointer-events: none;
}

.art-wash {
  width: 44vw;
  height: 44vw;
  min-width: 420px;
  min-height: 420px;
  border-radius: 50%;
  opacity: 0.42;
  filter: blur(34px);
  mix-blend-mode: multiply;
  animation: artWashDrift 18s ease-in-out infinite alternate;
}

.wash-a {
  left: -12vw;
  top: 18vh;
  background:
    radial-gradient(circle at 44% 42%, rgba(0, 191, 166, 0.22), transparent 48%),
    radial-gradient(circle at 62% 62%, rgba(10, 142, 219, 0.18), transparent 58%);
}

.wash-b {
  right: -14vw;
  bottom: -16vh;
  background:
    radial-gradient(circle at 42% 38%, rgba(10, 142, 219, 0.18), transparent 50%),
    radial-gradient(circle at 58% 62%, rgba(0, 191, 166, 0.16), transparent 60%);
  animation-delay: -7s;
}

.art-ribbon {
  width: 58vw;
  height: 160px;
  border: 1px solid transparent;
  border-top-color: rgba(0, 94, 168, 0.12);
  border-bottom-color: rgba(0, 191, 166, 0.1);
  border-radius: 50%;
  opacity: 0.72;
  filter: blur(0.2px);
  transform-origin: center;
  animation: artRibbonFlow 24s ease-in-out infinite alternate;
}

.ribbon-a {
  left: -10vw;
  top: 34vh;
  transform: rotate(-16deg);
}

.ribbon-b {
  right: -8vw;
  top: 20vh;
  width: 54vw;
  border-top-color: rgba(0, 191, 166, 0.12);
  border-bottom-color: rgba(10, 142, 219, 0.1);
  transform: rotate(-12deg);
  animation-delay: -8s;
}

.ribbon-c {
  right: 4vw;
  bottom: 8vh;
  width: 62vw;
  height: 190px;
  border-top-color: rgba(0, 94, 168, 0.1);
  border-bottom-color: rgba(0, 191, 166, 0.08);
  transform: rotate(8deg);
  animation-delay: -14s;
}

.art-glass {
  width: 360px;
  height: 190px;
  border: 1px solid rgba(255, 255, 255, 0.68);
  border-radius: 34px;
  opacity: 0.34;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.44), rgba(255, 255, 255, 0.08) 54%, rgba(0, 191, 166, 0.1)),
    linear-gradient(116deg, transparent 0%, rgba(255, 255, 255, 0.44) 44%, transparent 57%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.72),
    inset 0 -18px 32px rgba(10, 142, 219, 0.06),
    0 24px 70px rgba(5, 86, 148, 0.1);
  backdrop-filter: blur(16px) saturate(1.1);
  -webkit-backdrop-filter: blur(16px) saturate(1.1);
  transform: rotate(-14deg);
  animation: artGlassFloat 20s ease-in-out infinite alternate;
}

.glass-a {
  left: 7vw;
  top: 19vh;
}

.glass-b {
  right: 9vw;
  top: 46vh;
  width: 420px;
  height: 220px;
  opacity: 0.28;
  transform: rotate(12deg);
  animation-delay: -9s;
}

.blur-shape {
  opacity: 0.52;
  filter: blur(18px);
  transform-origin: center;
  animation: blurShapeFloat 18s ease-in-out infinite alternate;
}

.blur-dome-left {
  left: -13vw;
  top: -18vh;
  width: 42vw;
  height: 42vw;
  min-width: 520px;
  min-height: 520px;
  border-radius: 50%;
  background:
    radial-gradient(circle at 68% 66%, rgba(255, 255, 255, 0.86) 0 32%, transparent 33%),
    radial-gradient(circle at 42% 58%, rgba(73, 144, 255, 0.4), rgba(73, 144, 255, 0.1) 58%, transparent 72%);
  box-shadow: 0 40px 90px rgba(42, 125, 226, 0.14);
}

.blur-dome-right {
  right: -11vw;
  top: -12vh;
  width: 31vw;
  height: 31vw;
  min-width: 390px;
  min-height: 390px;
  border-radius: 50%;
  opacity: 0.46;
  background:
    radial-gradient(circle at 35% 54%, rgba(255, 255, 255, 0.9) 0 29%, transparent 30%),
    radial-gradient(circle, rgba(82, 127, 242, 0.34), rgba(82, 127, 242, 0.08) 62%, transparent 76%);
  animation-delay: -8s;
}

.blur-sun {
  right: 27vw;
  top: -16px;
  width: 112px;
  height: 112px;
  border-radius: 50%;
  opacity: 0.74;
  filter: blur(10px);
  background:
    radial-gradient(circle at 34% 30%, rgba(255, 255, 255, 0.52), transparent 22%),
    radial-gradient(circle, rgba(255, 198, 55, 0.88), rgba(255, 218, 102, 0.4) 60%, transparent 76%);
  box-shadow: 0 22px 70px rgba(255, 187, 42, 0.24);
  animation-delay: -3s;
}

.blur-prism {
  left: 24vw;
  top: 22vh;
  width: 128px;
  height: 128px;
  opacity: 0.58;
  filter: blur(12px);
  background: linear-gradient(150deg, rgba(53, 229, 203, 0.78), rgba(77, 215, 173, 0.24));
  clip-path: polygon(16% 18%, 92% 0, 48% 96%);
  box-shadow: 18px 20px 44px rgba(0, 191, 166, 0.18);
  animation-delay: -11s;
}

.blur-slate {
  right: 11vw;
  top: 28vh;
  width: 168px;
  height: 148px;
  opacity: 0.48;
  filter: blur(15px);
  border-radius: 28px;
  background: linear-gradient(135deg, rgba(112, 84, 245, 0.66), rgba(48, 129, 243, 0.28));
  box-shadow: 0 28px 70px rgba(65, 95, 236, 0.24);
  transform: rotate(34deg);
  animation-delay: -15s;
}

.blur-spark {
  width: 34px;
  height: 34px;
  opacity: 0.62;
  filter: blur(3px);
  background:
    linear-gradient(90deg, transparent 43%, rgba(158, 45, 232, 0.86) 45% 55%, transparent 57%),
    linear-gradient(0deg, transparent 43%, rgba(158, 45, 232, 0.86) 45% 55%, transparent 57%);
  clip-path: polygon(50% 0, 60% 38%, 100% 50%, 60% 62%, 50% 100%, 40% 62%, 0 50%, 40% 38%);
  animation: sparkPulse 4.8s ease-in-out infinite;
}

.spark-a {
  left: 29vw;
  top: 16vh;
}

.spark-b {
  right: 25vw;
  top: 15vh;
  width: 22px;
  height: 22px;
  opacity: 0.42;
  background:
    linear-gradient(90deg, transparent 43%, rgba(0, 159, 219, 0.82) 45% 55%, transparent 57%),
    linear-gradient(0deg, transparent 43%, rgba(0, 159, 219, 0.82) 45% 55%, transparent 57%);
  animation-delay: -2.2s;
}

.art-shine {
  inset: 82px -20% 0;
  opacity: 0.38;
  background: linear-gradient(
    110deg,
    transparent 0%,
    transparent 38%,
    rgba(255, 255, 255, 0.42) 48%,
    rgba(178, 234, 246, 0.16) 53%,
    transparent 64%,
    transparent 100%
  );
  mix-blend-mode: screen;
  animation: artLightSweep 9s ease-in-out infinite;
}

.art-strokes {
  inset: 82px 0 0;
  opacity: 0.38;
  background:
    linear-gradient(112deg, transparent 8%, rgba(0, 142, 219, 0.1) 8.6%, transparent 9.2%),
    linear-gradient(118deg, transparent 26%, rgba(0, 191, 166, 0.08) 26.6%, transparent 27.2%),
    linear-gradient(104deg, transparent 66%, rgba(0, 94, 168, 0.08) 66.5%, transparent 67.1%);
  mask-image: linear-gradient(90deg, transparent 0%, #000 14%, #000 86%, transparent 100%);
  animation: artStrokeGlide 22s ease-in-out infinite alternate;
}

.art-particles {
  inset: 82px 0 0;
  opacity: 0.64;
  background-image:
    radial-gradient(circle, rgba(0, 142, 219, 0.32) 0 1.2px, transparent 1.7px),
    radial-gradient(circle, rgba(0, 191, 166, 0.26) 0 1px, transparent 1.6px),
    radial-gradient(circle, rgba(0, 94, 168, 0.16) 0 1px, transparent 1.5px);
  background-position:
    12% 30%,
    78% 24%,
    60% 78%;
  background-size:
    260px 220px,
    340px 280px,
    460px 360px;
  animation: artParticleFloat 28s linear infinite;
}

.login-header {
  position: relative;
  z-index: 3;
  height: 82px;
  border-bottom: 1px solid var(--login-line);
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
}

.brand {
  width: auto;
  max-width: calc(100% - 48px);
  height: 100%;
  margin: 0 24px 0 clamp(24px, 4vw, 64px);
  padding: 0;
  display: flex;
  align-items: center;
  gap: 14px;
  border: 0;
  color: var(--login-blue-deep);
  background: transparent;
  font-family: inherit;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: 0.04em;
  text-align: left;
  cursor: pointer;
  appearance: none;
}

.brand-name {
  white-space: nowrap;
}

.brand-mark {
  width: 54px;
  height: 54px;
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 18px 18px 18px 4px;
  color: #ffffff;
  background: linear-gradient(135deg, var(--login-blue-deep), var(--login-blue) 58%, var(--login-green));
  box-shadow: 0 12px 26px var(--login-shadow);
  font-size: 16px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.login-line-art {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  overflow: hidden;
}

.line {
  position: absolute;
  border: 1px solid var(--login-line);
  border-radius: 50%;
}

.line-a {
  width: 720px;
  height: 230px;
  right: -220px;
  top: 108px;
  transform: rotate(-10deg);
}

.line-b {
  width: 900px;
  height: 260px;
  right: -280px;
  bottom: 110px;
  transform: rotate(9deg);
}

.line-c {
  width: 680px;
  height: 180px;
  left: -220px;
  bottom: 64px;
  transform: rotate(-8deg);
}

@keyframes artWashDrift {
  from {
    transform: translate3d(0, 0, 0) scale(1);
  }

  to {
    transform: translate3d(5vw, -4vh, 0) scale(1.08);
  }
}

@keyframes artRibbonFlow {
  from {
    translate: 0 0;
    scale: 1;
  }

  to {
    translate: 4vw -2vh;
    scale: 1.04;
  }
}

@keyframes artGlassFloat {
  from {
    translate: 0 0;
  }

  to {
    translate: 3vw -2vh;
  }
}

@keyframes artLightSweep {
  from {
    transform: translateX(-28%);
  }

  45%,
  100% {
    transform: translateX(28%);
  }
}

@keyframes blurShapeFloat {
  from {
    translate: 0 0;
    scale: 1;
  }

  to {
    translate: 2.2vw -1.4vh;
    scale: 1.04;
  }
}

@keyframes sparkPulse {
  0%,
  100% {
    transform: scale(0.88) rotate(0deg);
    opacity: 0.42;
  }

  50% {
    transform: scale(1.12) rotate(12deg);
    opacity: 0.78;
  }
}

@keyframes artStrokeGlide {
  from {
    transform: translate3d(-2vw, 0, 0);
  }

  to {
    transform: translate3d(3vw, -1vh, 0);
  }
}

@keyframes artParticleFloat {
  from {
    background-position:
      12% 30%,
      78% 24%,
      60% 78%;
  }

  to {
    background-position:
      20% 24%,
      70% 32%,
      54% 70%;
  }
}

@keyframes sloganIn {
  from {
    opacity: 0;
    transform: translateY(16px) skewX(-7deg);
  }

  to {
    opacity: 1;
    transform: translateY(0) skewX(-7deg);
  }
}

.login-shell {
  position: relative;
  z-index: 2;
  width: min(1220px, calc(100% - 56px));
  min-height: calc(100vh - 132px);
  margin: 0 auto;
  padding: 70px 0 86px;
  display: grid;
  grid-template-columns: minmax(0, 0.94fr) minmax(480px, 0.86fr);
  gap: 64px;
  align-items: center;
}

.hero-copy {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  transition:
    opacity 0.18s ease,
    filter 0.18s ease;
}

.section-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--login-blue-deep);
}

.section-eyebrow::before {
  content: '';
  width: 30px;
  height: 2px;
  border-radius: 2px;
  background: linear-gradient(90deg, var(--login-blue), var(--login-green));
}

.slogan {
  margin: 22px 0 12px;
  font-size: clamp(62px, 7.4vw, 112px);
  line-height: 0.94;
  font-weight: 950;
  letter-spacing: 0.12em;
}

.slogan-line {
  display: block;
  width: fit-content;
  white-space: nowrap;
}

.slogan-char {
  position: relative;
  display: inline-block;
  padding-right: 0.025em;
  text-shadow: 0 14px 32px rgba(0, 94, 168, 0.14);
  transform: skewX(-7deg);
  animation: sloganIn 0.9s ease both;
}

.slogan-line:nth-child(2) .slogan-char {
  animation-delay: 0.12s;
}

.slogan-char.c-blue {
  color: var(--login-blue-deep);
}

.slogan-char.c-teal {
  color: var(--login-blue);
}

.slogan-char.c-green {
  color: #9a5bc8;
}

.slogan-char.c-lime {
  color: #c76cb2;
}

.slogan-char.c-gold {
  color: #e083a1;
}

.slogan-char.c-coral {
  color: var(--login-green);
}

.slogan-char::after {
  content: attr(data-char);
  position: absolute;
  inset: 0;
  z-index: -1;
  color: #005ea8;
  opacity: 0.08;
  transform: translate(2px, 2px);
}

.hero-subtitle {
  margin: 22px 0 0;
  font-size: clamp(22px, 2.1vw, 32px);
  font-weight: 900;
  letter-spacing: 0.24em;
  color: #2d8cb2;
}

.hero-desc {
  max-width: 560px;
  margin: 22px 0 0;
  color: var(--login-text);
  line-height: 1.9;
  font-size: 16px;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 26px;
}

.hero-meta span {
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 13px;
  color: var(--login-text);
  background: rgba(255, 255, 255, 0.62);
  border: 1px solid var(--login-line);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.login-panel {
  position: relative;
  min-height: 540px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.emblem-stage {
  position: absolute;
  top: 0;
  left: 50%;
  width: min(360px, 92%);
  aspect-ratio: 1;
  display: grid;
  place-items: center;
  transform: translateX(-50%);
  pointer-events: none;
}

.glass-disc {
  position: absolute;
  width: 84%;
  height: 84%;
  border-radius: 50%;
  background:
    radial-gradient(circle at 34% 25%, rgba(255, 255, 255, 0.94), rgba(255, 255, 255, 0.2) 30%, transparent 62%),
    linear-gradient(145deg, rgba(0, 130, 220, 0.12), rgba(0, 191, 166, 0.12));
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow:
    inset 0 12px 35px rgba(255, 255, 255, 0.58),
    0 26px 72px rgba(0, 94, 168, 0.15);
}

.emblem-logo {
  position: relative;
  z-index: 2;
  width: 58%;
  height: 58%;
  object-fit: contain;
  filter: drop-shadow(0 18px 28px rgba(0, 94, 168, 0.20));
}

.orbit {
  position: absolute;
  border-radius: 50%;
  border: 2px solid rgba(0, 142, 219, 0.22);
}

.orbit-a {
  width: 94%;
  height: 38%;
  transform: rotate(-14deg);
  border-left-color: rgba(0, 191, 166, 0.26);
}

.orbit-b {
  width: 78%;
  height: 32%;
  transform: rotate(36deg);
  border-right-color: rgba(229, 109, 82, 0.24);
}

.emblem-label {
  position: absolute;
  z-index: 3;
  right: 18px;
  bottom: 70px;
  padding: 8px 14px;
  border-radius: 999px;
  color: #0a6d9d;
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(0, 142, 219, 0.14);
  font-size: 13px;
  font-weight: 800;
  box-shadow: 0 12px 26px rgba(0, 94, 168, 0.08);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.login-form {
  position: relative;
  z-index: 3;
  width: 100%;
  max-height: none;
  padding: 34px 30px 30px;
  display: flex;
  flex-direction: column;
  overflow: visible;
  border-radius: 30px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(255, 255, 255, 0.78)),
    rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(87, 67, 183, 0.18);
  box-shadow:
    0 26px 72px rgba(80, 56, 155, 0.16),
    inset 0 1px 0 rgba(255, 255, 255, 0.76);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  animation: loginCardFloatIn 0.08s cubic-bezier(0.18, 0.9, 0.28, 1) both;

  .el-input {
    height: 46px;

    input {
      height: 46px;
    }
  }
}

@keyframes loginCardFloatIn {
  from {
    opacity: 0;
    transform: translateY(3px) scale(0.992);
    box-shadow:
      0 12px 32px rgba(80, 56, 155, 0.1),
      inset 0 1px 0 rgba(255, 255, 255, 0.64);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
    box-shadow:
      0 26px 72px rgba(80, 56, 155, 0.16),
      inset 0 1px 0 rgba(255, 255, 255, 0.76);
  }
}

.form-topline {
  margin-bottom: 18px;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-radius: 999px;
  color: var(--login-text);
  background: rgba(255, 255, 255, 0.62);
  border: 1px solid var(--login-line);
}

.form-topline span {
  font-size: 12px;
  font-weight: 700;
}

.form-topline strong {
  color: var(--login-blue-deep);
  font-size: 13px;
}

.title-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 8px;
}

.form-kicker {
  margin: 0 0 6px;
  color: var(--login-blue-deep);
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.12em;
}

.title {
  margin: 0;
  color: var(--login-ink);
  font-size: 28px;
  font-weight: 900;
}

.form-subtitle {
  margin: 0 0 20px;
  color: var(--login-text);
  line-height: 1.6;
}

.login-form :deep(.el-input__wrapper),
.login-form :deep(.el-select__wrapper) {
  min-height: 46px;
  border-radius: 18px;
  background-color: rgba(255, 255, 255, 0.78);
  box-shadow: 0 0 0 1px rgba(87, 67, 183, 0.16);
}

.login-form :deep(.el-input__wrapper.is-focus),
.login-form :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 2px rgba(123, 101, 220, 0.26);
}

.login-page--v2 .login-form {
  border-color: rgba(35, 70, 201, 0.2);
  box-shadow:
    0 26px 72px rgba(35, 70, 201, 0.16),
    inset 0 1px 0 rgba(255, 255, 255, 0.76);
}

.login-page--v2 .login-form :deep(.el-input__wrapper),
.login-page--v2 .login-form :deep(.el-select__wrapper) {
  box-shadow: 0 0 0 1px rgba(35, 70, 201, 0.16);
}

.login-page--v2 .login-form :deep(.el-input__wrapper.is-focus),
.login-page--v2 .login-form :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 2px rgba(78, 108, 245, 0.28);
}

.captcha-item :deep(.el-form-item__content) {
  display: grid;
  grid-template-columns: 1fr 132px;
  gap: 10px;
}

.captcha-input {
  width: 100%;
}

.login-code {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 132px;
  height: 46px;
  padding: 0;
  overflow: hidden;
  border: 1px solid var(--login-line);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
  cursor: pointer;
}

.login-code-fallback {
  color: var(--login-blue-deep);
  font-size: 13px;
}

.login-code-img {
  display: block;
  width: 100%;
  height: 46px;
  object-fit: cover;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin: 0 0 16px;
}

.register-link {
  flex: 0 0 auto;
  max-width: 42%;
  padding: 0;
  border: none;
  color: var(--login-blue-deep);
  background: transparent;
  appearance: none;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
  text-decoration: none;
  text-align: right;
  white-space: normal;
  cursor: pointer;
}

.register-link:hover {
  color: var(--login-blue);
}

.form-actions {
  position: relative;
  z-index: 1;
  margin-top: 2px;
  padding-top: 6px;
  display: grid;
  gap: 10px;
  background: transparent;
}

.submit-item {
  margin-bottom: 0;
}

.submit-item :deep(.el-form-item__content) {
  display: block;
  width: 100%;
}

.login-form :deep(.el-button--primary) {
  width: 100%;
  height: 46px;
  border: none;
  border-radius: 999px;
  font-weight: 800;
  background: linear-gradient(135deg, var(--login-blue-deep), var(--login-blue) 62%, var(--login-green));
  box-shadow: 0 18px 36px rgba(0, 94, 168, 0.24);
}

.register-card {
  padding-bottom: 30px;
}

.register-scroll {
  flex: 0 0 auto;
  margin: 0 0 10px;
  padding-right: 0;
  overflow: visible;
}

.school-mode {
  width: 100%;
}

.school-mode :deep(.el-radio-button) {
  flex: 1;
}

.school-mode :deep(.el-radio-button__inner) {
  width: 100%;
  border-radius: 16px;
}

.back-entry {
  display: block;
  width: 100%;
  height: 36px;
  border: none;
  border-radius: 999px;
  color: var(--login-blue-deep);
  background: rgba(255, 255, 255, 0.55);
  box-shadow: inset 0 0 0 1px var(--login-line);
  font-weight: 700;
  cursor: pointer;
  transition:
    color 0.18s ease,
    background 0.18s ease;
}

.back-entry:hover {
  color: var(--login-blue);
  background: rgba(255, 255, 255, 0.78);
}

.login-footer {
  position: fixed;
  z-index: 4;
  right: 0;
  bottom: 0;
  left: 0;
  height: 40px;
  line-height: 40px;
  text-align: center;
  color: var(--login-muted);
  font-family: Arial, serif;
  font-size: 12px;
}

@media (max-width: 1100px) {
  .login-shell {
    grid-template-columns: 1fr;
    gap: 34px;
    text-align: center;
  }

  .section-eyebrow,
  .hero-meta {
    justify-content: center;
  }

  .slogan-line {
    margin: 0 auto;
  }

  .hero-desc {
    margin-left: auto;
    margin-right: auto;
  }

  .login-panel {
    width: min(620px, 100%);
    margin: 0 auto;
  }

  .art-ribbon {
    width: 82vw;
  }
}

@media (max-width: 720px) {
  .login-header {
    height: 72px;
  }

  .brand {
    max-width: calc(100% - 28px);
    margin: 0 14px;
    font-size: 16px;
    line-height: 1.25;
  }

  .brand-name {
    max-width: 220px;
    white-space: normal;
  }

  .brand-mark {
    width: 46px;
    height: 46px;
  }

  .login-shell {
    width: min(100% - 32px, 640px);
    min-height: calc(100vh - 112px);
    padding: 44px 0 64px;
  }

  .slogan {
    font-size: clamp(54px, 15vw, 78px);
    letter-spacing: 0.06em;
  }

  .hero-subtitle {
    font-size: 20px;
    letter-spacing: 0.14em;
  }

  .login-panel {
    min-height: 660px;
    height: auto;
  }

  .login-form {
    padding: 28px 20px 26px;
  }

  .art-wash {
    opacity: 0.28;
  }

  .art-ribbon {
    opacity: 0.42;
  }

  .art-glass,
  .art-shine {
    opacity: 0.2;
  }

  .blur-shape {
    opacity: 0.26;
  }

  .blur-dome-left {
    left: -42vw;
    top: -16vh;
  }

  .blur-dome-right {
    right: -40vw;
    top: -10vh;
  }

  .blur-prism,
  .blur-slate,
  .blur-sun {
    transform: scale(0.72);
  }

  .art-strokes,
  .art-particles {
    opacity: 0.32;
  }
}

@media (max-width: 520px) {
  .captcha-item :deep(.el-form-item__content) {
    grid-template-columns: 1fr;
  }

  .login-code {
    width: 100%;
  }

  .form-options {
    align-items: flex-start;
    flex-direction: column;
  }

  .login-footer {
    position: static;
    padding: 0 16px 12px;
    height: auto;
    line-height: 1.6;
  }
}

@media (prefers-reduced-motion: reduce) {
  .art-wash,
  .art-ribbon,
  .art-glass,
  .art-shine,
  .blur-shape,
  .blur-spark,
  .art-strokes,
  .art-particles,
  .slogan-char,
  .login-form {
    animation: none !important;
  }
}
</style>
