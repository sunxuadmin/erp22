import { createSvgIconsPlugin } from 'vite-plugin-svg-icons-ng';

export default (path: any) => {
  return createSvgIconsPlugin({
    // 指定需要缓存的图标文件夹
    iconDirs: [path.resolve(process.cwd(), 'src', 'assets/icons/svg')],
    // 指定symbolId格式
    symbolId: 'icon-[dir]-[name]'
  });
};
