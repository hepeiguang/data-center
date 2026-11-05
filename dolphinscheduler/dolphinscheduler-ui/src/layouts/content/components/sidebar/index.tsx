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

import { defineComponent, ref, PropType, watch } from 'vue'
import { NLayoutSider, NMenu, NButton, NIcon, NPopover } from 'naive-ui'
import { SettingOutlined } from '@vicons/antd'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import Logo from '../logo'
import Theme from '../theme'
import Locales from '../locales'
import Timezone from '../timezone'
import User from '../user'
import { useMenuClick } from './use-menuClick'
import styles from './index.module.scss'

const findAncestors = (
  options: any[],
  targetKey: string,
  trail: string[] = []
): string[] | null => {
  for (const option of options) {
    const key = String(option.key)
    if (key === targetKey) return trail
    if (option.children?.length) {
      const ancestors = findAncestors(option.children, targetKey, [
        ...trail,
        key
      ])
      if (ancestors) return ancestors
    }
  }
  return null
}

const Sidebar = defineComponent({
  name: 'Sidebar',
  props: {
    menuOptions: {
      type: Array as PropType<any[]>,
      default: () => []
    },
    activeKey: {
      type: String as PropType<string>,
      default: ''
    },
    localesOptions: {
      type: Array as PropType<any[]>,
      default: () => []
    },
    timezoneOptions: {
      type: Array as PropType<any[]>,
      default: () => []
    },
    userDropdownOptions: {
      type: Array as PropType<any[]>,
      default: () => []
    }
  },
  setup(props) {
    const router = useRouter()
    const { t } = useI18n()
    const { handleMenuClick } = useMenuClick()
    const collapsed = ref(false)
    const showSettings = ref(false)
    const expandedKeys = ref<string[]>([])

    const syncExpandedKeys = () => {
      const ancestors = findAncestors(props.menuOptions, props.activeKey) || []
      expandedKeys.value = Array.from(
        new Set([...expandedKeys.value, ...ancestors])
      )
    }

    watch(() => [props.activeKey, props.menuOptions], syncExpandedKeys, {
      immediate: true
    })

    const handleUpdateExpandedKeys = (keys: string[]) => {
      expandedKeys.value = keys
    }

    const handleUpdateSettings = (show: boolean) => {
      showSettings.value = show
    }

    const handleUISettingClick = () => {
      showSettings.value = false
      router.push({ path: '/ui-setting' })
    }

    return {
      t,
      collapsed,
      showSettings,
      expandedKeys,
      handleMenuClick,
      handleUpdateExpandedKeys,
      handleUpdateSettings,
      handleUISettingClick
    }
  },
  render() {
    return (
      <NLayoutSider
        bordered
        collapseMode='width'
        width={240}
        collapsedWidth={64}
        collapsed={this.collapsed}
        showTrigger='bar'
        onCollapse={() => (this.collapsed = true)}
        onExpand={() => (this.collapsed = false)}
        contentStyle='display: flex; flex-direction: column; overflow: hidden; height: 100%'
      >
        <div class={[styles.logo, this.collapsed && styles.logoCollapsed]}>
          {this.collapsed ? (
            <div class={styles.logoMark}>
              <Logo />
            </div>
          ) : (
            <Logo />
          )}
        </div>
        <div class={styles.menu}>
          <NMenu
            value={this.activeKey}
            options={this.menuOptions}
            indent={20}
            collapsed={this.collapsed}
            collapsedWidth={64}
            collapsedIconSize={22}
            expandedKeys={this.expandedKeys}
            onUpdateExpandedKeys={this.handleUpdateExpandedKeys}
            onUpdateValue={this.handleMenuClick}
          />
        </div>
        <div class={[styles.footer, this.collapsed && styles.footerCollapsed]}>
          <div
            class={[styles.account, this.collapsed && styles.accountCollapsed]}
          >
            <NPopover
              trigger='click'
              placement='right-end'
              showArrow
              show={this.showSettings}
              onUpdateShow={this.handleUpdateSettings}
            >
              {{
                trigger: () => (
                  <NButton
                    quaternary
                    size='small'
                    title={this.t('menu.settings')}
                  >
                    {{
                      icon: () => (
                        <NIcon size='16'>
                          <SettingOutlined />
                        </NIcon>
                      ),
                      default: this.collapsed
                        ? undefined
                        : () => this.t('menu.settings')
                    }}
                  </NButton>
                ),
                default: () => (
                  <div class={styles.panel}>
                    <NButton text onClick={this.handleUISettingClick}>
                      {{
                        icon: () => (
                          <NIcon size='16'>
                            <SettingOutlined />
                          </NIcon>
                        ),
                        default: () => this.t('menu.ui_setting')
                      }}
                    </NButton>
                    <div class={styles.panelDivider} />
                    <Theme />
                    <Locales localesOptions={this.localesOptions} />
                    <Timezone timezoneOptions={this.timezoneOptions} />
                  </div>
                )
              }}
            </NPopover>
            <User
              collapsed={this.collapsed}
              userDropdownOptions={this.userDropdownOptions}
            />
          </div>
        </div>
      </NLayoutSider>
    )
  }
})

export default Sidebar
