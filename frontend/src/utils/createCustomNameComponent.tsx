/**
 * 后台返回的路由动态生成name 解决缓存问题
 * 感谢 @fourteendp
 * 详见 https://github.com/vbenjs/vue-vben-admin/issues/3927
 */
import { Component, defineComponent, h } from 'vue';

interface Options {
  name?: string;
}

const createComponentLoadError = (name?: string) =>
  defineComponent({
    name: `${name || 'DynamicPage'}LoadError`,
    setup() {
      const reloadPage = () => window.location.reload();
      return () =>
        h(
          'div',
          {
            class: 'app-container',
            role: 'alert',
            style: {
              display: 'flex',
              minHeight: 'calc(100vh - 150px)',
              alignItems: 'center',
              justifyContent: 'center'
            }
          },
          [
            h(
              'div',
              {
                style: {
                  width: 'min(520px, 100%)',
                  padding: '28px',
                  border: '1px solid #e5e7eb',
                  borderRadius: '10px',
                  background: '#ffffff',
                  textAlign: 'center',
                  boxShadow: '0 8px 24px rgb(15 23 42 / 6%)'
                }
              },
              [
                h('div', { style: { marginBottom: '10px', color: '#1f2937', fontSize: '18px', fontWeight: '600' } }, '页面加载失败'),
                h(
                  'div',
                  { style: { marginBottom: '20px', color: '#64748b', fontSize: '14px', lineHeight: '1.7' } },
                  '页面资源未能正常加载，请刷新后重试。若仍然失败，请联系管理员。'
                ),
                h(
                  'button',
                  {
                    type: 'button',
                    class: 'el-button el-button--primary',
                    onClick: reloadPage
                  },
                  '刷新页面'
                )
              ]
            )
          ]
        );
    }
  });

export function createCustomNameComponent(loader: () => Promise<any>, options: Options = {}): () => Promise<Component> {
  const { name } = options;
  let component: Component | null = null;

  const load = async () => {
    try {
      const { default: loadedComponent } = await loader();
      if (!loadedComponent) {
        throw new Error('The loaded module does not export a default component');
      }
      component = loadedComponent;
    } catch (error) {
      console.error(`Cannot resolve component ${name}, error:`, error);
    }
  };

  return async () => {
    if (!component) {
      await load();
    }

    if (!component) {
      return createComponentLoadError(name);
    }

    return Promise.resolve(
      defineComponent({
        name,
        render() {
          return h(component);
        }
      })
    );
  };
}
