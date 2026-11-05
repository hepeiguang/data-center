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

import { defineComponent, onMounted, watch, toRefs, computed } from 'vue'
import { NLayout, NLayoutContent, useMessage } from 'naive-ui'
import SideBar from './components/sidebar'
import { useDataList } from './use-dataList'
import { useLocalesStore } from '@/store/locales/locales'
import { useRouteStore } from '@/store/route/route'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'

const Content = defineComponent({
  name: 'DSContent',
  setup() {
    window.$message = useMessage()

    const route = useRoute()
    const { locale } = useI18n()
    const localesStore = useLocalesStore()
    const routeStore = useRouteStore()
    const { state, changeMenuOption, changeUserDropdown } = useDataList()

    const activeKey = computed(() => {
      const activeSide = route.meta.activeSide as string | undefined
      const key = activeSide || route.path
      return key.replace(
        ':projectCode',
        (route.params.projectCode as string) || ''
      )
    })

    const refresh = () => {
      changeMenuOption(state)
      changeUserDropdown(state)
    }

    onMounted(() => {
      locale.value = localesStore.getLocales
      refresh()
    })

    watch(locale, refresh)

    watch(
      () => route.path,
      (path) => {
        if (path !== '/login') {
          routeStore.setLastRoute(path)
          refresh()
        }
      }
    )

    return {
      ...toRefs(state),
      activeKey,
      currentRoute: route
    }
  },
  render() {
    return (
      <NLayout style='height: 100%' has-sider>
        <SideBar
          menuOptions={this.menuOptions}
          activeKey={this.activeKey}
          localesOptions={this.localesOptions}
          timezoneOptions={this.timezoneOptions}
          userDropdownOptions={this.userDropdownOptions}
        />
        <NLayoutContent
          native-scrollbar={false}
          style='padding: 16px 22px'
          contentStyle={'height: 100%'}
        >
          <router-view key={this.currentRoute.fullPath} />
        </NLayoutContent>
      </NLayout>
    )
  }
})

export default Content
