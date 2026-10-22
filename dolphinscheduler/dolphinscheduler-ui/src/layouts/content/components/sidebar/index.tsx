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

import { defineComponent, ref, PropType, computed } from 'vue'
import { NLayoutSider, NMenu } from 'naive-ui'
import { useMenuClick } from './use-menuClick'
import { useParentAppProps } from '@/store/parentAppProps/parentAppProps'

const Sidebar = defineComponent({
  name: 'Sidebar',
  props: {
    sideMenuOptions: {
      type: Array as PropType<any>,
      default: []
    },
    sideKey: {
      type: String as PropType<string>,
      default: ''
    }
  },
  setup(props) {
    const parentAppProps = useParentAppProps()
    const collapsedRef = ref(false)
    const defaultExpandedKeys = [
      'workflow',
      'task',
      'udf-manage',
      'service-manage',
      'statistical-manage',
      'task-group-manage'
    ]
    const { handleMenuClick } = useMenuClick()
    const filteredTaskMenuOptions = computed(() => {
      return props.sideMenuOptions.filter((item: any) => item.label !== '任务')
    })
    const finalFilteredMenuOptions = computed(() => {
      if (parentAppProps.filterMenu) {
        return filteredTaskMenuOptions.value.map((item: any) => ({
          ...item,
          children: item.children.filter((ele: any) => ele.label === '项目概览'||ele.label === '工作流定义'||ele.label === '工作流实例')
        }))
      }
      return props.sideMenuOptions
    })
    return { collapsedRef, defaultExpandedKeys, handleMenuClick,parentAppProps,finalFilteredMenuOptions }
  },
  render() {
    return (
      <NLayoutSider
        bordered
        nativeScrollbar={false}
        show-trigger='bar'
        collapse-mode='width'
        collapsed={this.collapsedRef}
        onCollapse={() => (this.collapsedRef = true)}
        onExpand={() => (this.collapsedRef = false)}
      >
        <NMenu
          class='tab-vertical'
          value={this.sideKey}
          options={this.finalFilteredMenuOptions}
          defaultExpandedKeys={this.defaultExpandedKeys}
          onUpdateValue={this.handleMenuClick}
        />
      </NLayoutSider>
    )
  }
})

export default Sidebar
