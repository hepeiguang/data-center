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

import { useRouter } from 'vue-router'
import {
  defineComponent,
  onMounted,
  onUnmounted,
  ref,
  getCurrentInstance,
  PropType,
  toRefs,
  watch
} from 'vue'
import {
  NIcon,
  NSpace,
  NDataTable,
  NButtonGroup,
  NButton,
  NPagination,
  NBreadcrumb,
  NBreadcrumbItem
} from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { SearchOutlined } from '@vicons/antd'
import { useTable, useDetailPageStore } from './table/use-table'
import { useIsDetailPageStore, isEmpty } from './edit/use-edit'
import { useFileStore } from '@/store/file/file'
import Card from '@/components/card'
import ResourceFolderModal from './folder'
import ResourceUploadModal from './upload'
import ResourceRenameModal from './rename'
import styles from './index.module.scss'
import type { Router } from 'vue-router'
import Search from '@/components/input-search'
import { ResourceType } from '@/views/resource/components/resource/types'
import { useUserStore } from '@/store/user/user'

const props = {
  resourceType: {
    type: String as PropType<ResourceType>,
    default: undefined
  },
  customResourceType: {
    type: String as PropType<'HIVE' | 'SEATUNNEL' | 'FILE'>,
    default: undefined
  },
  isShowMutiSelect:{
    type: Boolean,
    default: true
  }
}

export default defineComponent({
  name: 'ResourceList',
  props,
  setup(props) {
    const router: Router = useRouter()
    const fileStore = useFileStore()
    const breadListRef = ref<Array<string>>()

    const {
      variables,
      tableWidth,
      requestData,
      updateList,
      createColumns,
      handleCreateFile
    } = useTable()

    const userStore = useUserStore()

    variables.resourceType = props.resourceType
    //传入 将父组件的默认值值交予variables
    variables.customResourceType = props.customResourceType
    //传入控制
    variables.isShowMutiSelect = props.isShowMutiSelect

    //点击某页
    const handleUpdatePage = (page: number) => {
      variables.pagination.page = page
      requestData()
    }

    //更改页面列表容纳大小数字
    const handleUpdatePageSize = (pageSize: number) => {
      variables.pagination.page = 1
      variables.pagination.pageSize = pageSize
      requestData()
    }

    const handleConditions = () => {
      //该方法从useTable定义 从useTable创建store并调用getter setter
      //接着从useFile里调用方法拿到分页的方法
      //最后返回相关数据
      requestData()
    }

    //此处方法存疑 点击创建文件夹
    const handleCreateFolder = () => {
      variables.folderShowRef = true
    }

    //创建文件 或者 创建udf资源
    const handleUploadFile = () => {
      variables.isReupload = false
      variables.uploadShowRef = true
    }

    const handleRenameFile = () => {
      variables.renameShowRef = true
    }
    const detailPageStore = useDetailPageStore()
    const isDetailPageStore = useIsDetailPageStore()

    //点击查询这段代码的作用是从详情页返回列表页时恢复之前的查询参数和页面状态。
    const handleDetailBackList = () => {
      if (isDetailPageStore.getIsDetailPage) {
        //增加代码
        variables.customResourceType = detailPageStore.getCustomResourceType
        variables.resourceType = detailPageStore.getResourceType
        variables.fullName = detailPageStore.getFullName
        variables.tenantCode = detailPageStore.getTenantCode
        variables.searchRef = detailPageStore.getSearchValue
        variables.pagination.page = detailPageStore.getPage
        variables.pagination.pageSize = detailPageStore.getPageSize
        if (!isEmpty(variables.searchRef)) {
          handleConditions()
        }
        detailPageStore.$reset()
        isDetailPageStore.$reset()
      } else {
        detailPageStore.$reset()
        isDetailPageStore.$reset()
      }
    }

    onUnmounted(() => {
      isDetailPageStore.$reset()
    })
    onMounted(() => {
      handleDetailBackList()
      createColumns(variables)
      fileStore.setCurrentDir(variables.fullName)
      breadListRef.value = fileStore.getCurrentDir
        .replace(/\/+$/g, '')
        .split('/')
        .slice(2) as Array<string>
      requestData()
    })

    //getCurrentInstance() 获取当前组件实例
    //appContext 是 Vue 应用上下文
    //config.globalProperties 包含 Vue 应用级别的全局属性/方法
    /*
    * 使用场景
    *
    // 插件开发：通过 appContext 访问应用级配置
    // 工具函数：全局注册的工具方法可通过上下文获取
    // 依赖共享：跨组件共享状态或服务
    * */
    const trim = getCurrentInstance()?.appContext.config.globalProperties.trim

    //  这里没理解是什么功能 点击文件夹?
    const handleBread = (index: number) => {
      const breadName =
        //variables.fullName：存储当前完整目录路径
        //http://localhost:5173/
        //resource/resource-manage?
        //prefix=file:/dolphinscheduler/default/udfs/file文件夹/&tenantCode=default
        //文件夹完整路径 /dolphinscheduler/default/udfs/file%E6%96%87%E4%BB%B6%E5%A4%B9/&tenantCode=default
        variables.fullName
          //将完整路径按'/'分割成数组  “”  dolphinscheduler  default    udfs   file%E6%96%87%E4%BB%B6%E5%A4%B9   "&tenantCode=default"
          .split('/')
          //取前index+3个元素    “”  dolphinscheduler  default
          .slice(0, index + 3)
          //重新拼接成路径字符串     /dolphinscheduler/default   +     /    =>   /dolphinscheduler/default/
          .join('/') + '/'
      goBread(breadName)
    }

    const goBread = (fullName: string) => {
      const { resourceType, tenantCode } = variables
      const baseDir =
        resourceType === 'UDF'
          ? userStore.getBaseUdfDir
          : userStore.getBaseResDir
      if (fullName === '' || !fullName.startsWith(baseDir)) {
        router.push({
          name: resourceType === 'UDF' ? 'resource-manage' : 'file-manage'
        })
      } else {
        router.push({
          name:
            resourceType === 'UDF'
              ? 'resource-sub-manage'
              : 'resource-file-subdirectory',
          query: { prefix: fullName, tenantCode: tenantCode }
        })
      }
    }

    watch(useI18n().locale, () => {
      createColumns(variables)
    })

    return {
      breadListRef,
      tableWidth,
      updateList,
      handleConditions,
      handleCreateFolder,
      handleCreateFile,
      handleUploadFile,
      handleRenameFile,
      handleUpdatePage,
      handleUpdatePageSize,
      handleBread,
      trim,
      ...toRefs(variables)
    }
  },
  render() {
    const { t } = useI18n()

    const {
      handleConditions,
      handleCreateFolder,
      handleCreateFile,
      handleUploadFile,
      tableWidth
    } = this
    const manageTitle =
      this.resourceType === 'UDF'
        ? t('resource.udf.udf_resources')
        : t('resource.file.file_manage')

    return (
      <NSpace vertical>
        <Card>
          <NSpace justify='space-between'>
            {/* <NButtonGroup size='small'>
              <NButton
                type='primary'
                onClick={handleCreateFolder}
                class='btn-create-directory'
              >
                {t('resource.file.create_folder')}
              </NButton>
              {this.resourceType !== 'UDF' && (
                <NButton onClick={handleCreateFile} class='btn-create-file'>
                  {t('resource.file.create_file')}
                </NButton>
              )}
              <NButton onClick={handleUploadFile} class='btn-upload-resource'>
                {this.resourceType === 'UDF'
                  ? t('resource.udf.upload_udf_resources')
                  : t('resource.file.upload_files')}
              </NButton>
            </NButtonGroup> */}
            <NSpace>
              <Search
                placeholder={t('resource.file.enter_keyword_tips')}
                v-model:value={this.searchRef}
                onSearch={handleConditions}
              />
              <NButton size='small' type='primary' onClick={handleConditions}>
                <NIcon>
                  <SearchOutlined />
                </NIcon>
              </NButton>
            </NSpace>
          </NSpace>
        </Card>
        <Card title={manageTitle}>
          {{
            header: () => (
              <NBreadcrumb separator='>'>
                {this.breadListRef?.map((item, index) => (
                  <NBreadcrumbItem>
                    <NButton
                      text
                      disabled={
                        index > 0 && index === this.breadListRef!.length - 1
                      }
                      onClick={() => this.handleBread(index)}
                    >
                      {index === 0 ? manageTitle : item}
                    </NButton>
                  </NBreadcrumbItem>
                ))}
              </NBreadcrumb>
            ),
            default: () => (
              <NSpace vertical>
                <NDataTable
                  remote
                  columns={this.columns}
                  data={this.resourceList?.table}
                  striped
                  size={'small'}
                  class={styles['table-box']}
                  row-class-name='items'
                  scrollX={tableWidth}
                />
                <NSpace justify='center'>
                  <NPagination
                    v-model:page={this.pagination.page}
                    v-model:pageSize={this.pagination.pageSize}
                    pageSizes={this.pagination.pageSizes}
                    item-count={this.pagination.itemCount}
                    onUpdatePage={this.handleUpdatePage}
                    onUpdatePageSize={this.handleUpdatePageSize}
                    show-quick-jumper
                    show-size-picker
                  />
                </NSpace>
              </NSpace>
            ),
            'header-extra': () => (
              <NButtonGroup size='small'>
                <NButton
                  type='primary'
                  onClick={handleCreateFolder}
                  class='btn-create-directory'
                >
                  {t('resource.file.create_folder')}
                </NButton>
                {this.resourceType !== 'UDF' && (
                  <NButton onClick={handleCreateFile} class='btn-create-file'>
                    {t('resource.file.create_file')}
                  </NButton>
                )}
                <NButton onClick={handleUploadFile} class='btn-upload-resource'>
                  {this.resourceType === 'UDF'
                    ? t('resource.udf.upload_udf_resources')
                    : t('resource.file.upload_files')}
                </NButton>
              </NButtonGroup>
            )
          }}
        </Card>
        <ResourceFolderModal
          v-model:show={this.folderShowRef}
          resourceType={this.resourceType}
          onUpdateList={this.updateList}
        />
        <ResourceUploadModal
          v-model:show={this.uploadShowRef}
          isReupload={this.isReupload}
          resourceType={this.resourceType}
          name={this.reuploadInfo.name}
          fullName={this.reuploadInfo.fullName}
          description={this.reuploadInfo.description}
          userName={this.reuploadInfo.user_name}
          onUpdateList={this.updateList}
          v-model:customResourceType={ this.reuploadInfo.customType}
          v-model:isShowMutiSelect={this.isShowMutiSelect}
        />

        <ResourceRenameModal
          v-model:show={this.renameShowRef}
          resourceType={this.resourceType}
          name={this.renameInfo.name}
          fullName={this.renameInfo.fullName}
          description={this.renameInfo.description}
          userName={this.renameInfo.user_name}
          onUpdateList={this.updateList}
        />
      </NSpace>
    )
  }
})
