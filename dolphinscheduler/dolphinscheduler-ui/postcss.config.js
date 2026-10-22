// vite-project/postcss.config.js
module.exports = {
  plugins: {
    autoprefixer: {}, // 自动添加浏览器前缀（已过时，由 postcss-preset-env 代替）
    'postcss-preset-env': {}, // 支持未来的 CSS 特性，并自动加前缀
    cssnano: {}, // 压缩 CSS
  },
};