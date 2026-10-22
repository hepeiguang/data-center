/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import viteCompression from 'vite-plugin-compression'
import path from 'path'

export default defineConfig({
  base: process.env.NODE_ENV === 'production' ? '/ds/' : '/',
  plugins: [
    vue(),
    vueJsx(),
    viteCompression({
      verbose: true,
      disable: false,
      threshold: 10240,
      algorithm: 'gzip',
      ext: '.gz',
      deleteOriginFile: false
    })
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
      // resolve vue-i18n warning: You are running the esm-bundler build of vue-i18n.
    }
  },
  server: {
    host: '0.0.0.0', // 监听所有地址
    hmr: {
      overlay: false
    },
    proxy: {
      '/dolphinscheduler/yg': {
        // target: loadEnv('development', './').VITE_APP_DEV_WEB_URL,
        target: loadEnv('development', './').VITE_APP_YG_WEB_URL,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/dolphinscheduler\/yg/, '/yg')
      },
      '/dolphinscheduler': {
        target: loadEnv('development', './').VITE_APP_DEV_WEB_URL,
        changeOrigin: true
      }
    }
  },
  //  build: {
  //   sourcemap: false,
  //   target: ['es2015'],
  //   minify: 'terser',
  //   terserOptions: {
  //     compress: {
  //       drop_console: true,
  //       drop_debugger: true
  //     }
  //   },
  //   rollupOptions: {
  //     output: {
  //       manualChunks(id) {
  //         if (id.includes('node_modules')) {
  //           // 将大依赖单独拆包
  //           if (id.includes('lodash')) return 'vendor-lodash'
  //           if (id.includes('axios')) return 'vendor-axios'
  //           if (id.includes('naive-ui')) return 'vendor-naive'
  //           if (id.includes('echarts')) return 'vendor-echarts'
            
  //           // 其余依赖分组打包
  //           return 'vendor'
  //         }
  //       }
  //     }
  //   }
  // },
  // optimizeDeps: {
  //   include: ['vue', 'vue-router', 'pinia', 'axios'],
  //   // exclude: ['your-lightweight-package']
  // }
})
  //  build: {
  //   sourcemap: false,
  //   target: ['es2015'],
  //   minify: 'terser',
  //   terserOptions: {
  //     compress: {
  //       drop_console: true,
  //       drop_debugger: true
  //     }
  //   },
  //   rollupOptions: {
  //     output: {
  //       manualChunks(id) {
  //         if (id.includes('node_modules')) {
  //           // 将大依赖单独拆包
  //           if (id.includes('lodash')) return 'vendor-lodash'
  //           if (id.includes('axios')) return 'vendor-axios'
  //           if (id.includes('naive-ui')) return 'vendor-naive'
  //           if (id.includes('echarts')) return 'vendor-echarts'
            
  //           // 其余依赖分组打包
  //           return 'vendor'
  //         }
  //       }

  //     }
  //   }
  // },
  // optimizeDeps: {
    // include: ['vue', 'vue-router', 'pinia', 'axios'],
    // exclude: ['your-lightweight-package']
  // }
// })
