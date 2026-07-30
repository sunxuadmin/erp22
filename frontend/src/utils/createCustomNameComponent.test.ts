import { describe, expect, it, vi } from 'vitest';
import { createCustomNameComponent } from './createCustomNameComponent';

describe('createCustomNameComponent', () => {
  it('returns a visible error component when the page loader rejects', async () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    const componentFactory = createCustomNameComponent(() => Promise.reject(new Error('chunk load failed')), {
      name: 'SchoolCategoryPage'
    });

    const component = (await componentFactory()) as {
      name?: string;
      setup?: () => () => { children?: Array<{ children?: Array<{ children?: string }> }> };
    };
    const render = component.setup?.();
    const vnode = render?.();

    expect(component.name).toBe('SchoolCategoryPageLoadError');
    expect(vnode?.children?.[0]?.children?.[0]?.children).toBe('页面加载失败');
    expect(consoleError).toHaveBeenCalledWith('Cannot resolve component SchoolCategoryPage, error:', expect.any(Error));
    consoleError.mockRestore();
  });

  it('returns the named wrapper when the page loader succeeds', async () => {
    const loadedComponent = { name: 'LoadedPage', render: () => null };
    const componentFactory = createCustomNameComponent(() => Promise.resolve({ default: loadedComponent }), {
      name: 'SchoolCategoryPage'
    });

    const component = (await componentFactory()) as { name?: string; render?: () => { type?: unknown } };
    const vnode = component.render?.();

    expect(component.name).toBe('SchoolCategoryPage');
    expect(vnode?.type).toBe(loadedComponent);
  });
});
