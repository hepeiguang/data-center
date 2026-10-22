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

import { defineComponent, computed, ref, nextTick, provide, toRefs } from 'vue'
import {
  zhCN,
  enUS,
  dateZhCN,
  dateEnUS,
  NConfigProvider,
  darkTheme,
  GlobalThemeOverrides,
  NMessageProvider
} from 'naive-ui'
import { useThemeStore } from '@/store/theme/theme'
import { useLocalesStore } from '@/store/locales/locales'
import themeList from '@/themes'
// 是不是为第三方的子应用集成
import { useParentAppProps } from '@/store/parentAppProps/parentAppProps'
const App = defineComponent({
  name: 'App',
  setup() {
    const isRouterAlive = ref(true)
    const themeStore = useThemeStore()
    const parentAppProps = useParentAppProps()
    const currentTheme = computed(() =>
      themeStore.darkTheme ? darkTheme : undefined
    )
    const localesStore = useLocalesStore()
    /*refresh page when router params change*/
    const reload = () => {
      isRouterAlive.value = false
      nextTick(() => {
        isRouterAlive.value = true
      })
    }

    provide('reload', reload)
    parentAppProps.setLayoutStyles(window.$wujie && window.$wujie.props && window.$wujie.props.layoutStyles)
    parentAppProps.setIsAside(window.$wujie && window.$wujie.props && window.$wujie.props.isAside || false)
    parentAppProps.setfilterMenu(window.$wujie && window.$wujie.props && window.$wujie.props.filterMenu || false)
    const { layoutHeight } = toRefs(window.$wujie && window.$wujie.props && window.$wujie.props.layoutStyles || {})
    return {
      reload,
      isRouterAlive,
      currentTheme,
      localesStore,
      layoutHeight
    }
  },
  render() {
    const themeOverrides: GlobalThemeOverrides =
      themeList[this.currentTheme ? 'dark' : 'light']

    return (
      <NConfigProvider
        theme={this.currentTheme}
        theme-overrides={themeOverrides}
        style={{ width: '100%', height: this.layoutHeight ? this.layoutHeight : '100vh' }}
        date-locale={
          String(this.localesStore.getLocales) === 'zh_CN' ? dateZhCN : dateEnUS
        }
        locale={String(this.localesStore.getLocales) === 'zh_CN' ? zhCN : enUS}
      >
        <NMessageProvider>
          {this.isRouterAlive ? <router-view /> : ''}
        </NMessageProvider>
      </NConfigProvider>
    )
  }
})

export default App
