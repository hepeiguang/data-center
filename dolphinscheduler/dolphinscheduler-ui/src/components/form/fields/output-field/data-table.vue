<template>
  <n-data-table
    :columns="data.columns"
    :data="data.data"
    :row-class-name="rowClassName"
    :pagination="false"
    :bordered="false"
  />
</template>

<script lang="ts">
  import {
    defineComponent,
    reactive,
    ref,
    h,
    watch,
    onMounted,
    PropType,
    VNode
  } from 'vue'
  import {
    useMessage,
    NSpace,
    NButton,
    NSelect,
    NInput,
    NGrid,
    NGi,
    NIcon,
    NTooltip,
    SelectOption,
    NDataTable,
    NCheckbox
  } from 'naive-ui'
  import { PlusOutlined, DeleteOutlined, EditOutlined } from '@vicons/antd'
  import { cloneDeep } from 'lodash'
  import { queryFunctionInfoCfgByCode } from '@/service/modules/sql-function'

  export default defineComponent({
    name: 'DataTable',
    components: {
      NSpace,
      NButton,
      NSelect,
      NInput,
      NGrid,
      NGi,
      NIcon,
      NTooltip,
      PlusOutlined,
      EditOutlined,
      DeleteOutlined,
      NDataTable
    },
    props: {
      value: {
        type: Object as PropType<any>
      },
      fields: {
        type: Object as PropType<any>
      },
      readonly: {
        type: Boolean
      },
      options: {
        type: Object 
        },
      isSpan:{
        type: Boolean
      }
    },
    setup(props, ctx) {
      var valueCopy: any = cloneDeep({ data: props.options })
      const data = reactive(valueCopy)
      const fields = reactive(props.fields)
      const show = ref(false)

      const setColumns = () => {
        // if (data.columns == null) {
        // }
        data.columns = [
          {
            title: () =>
              h(NCheckbox, {
                checked: data.data.every((row: any) => row.selected),
                indeterminate:
                  data.data.some((row: any) => row.selected) &&
                  !data.data.every((row: any) => row.selected),
                onUpdateChecked: (value: boolean) => {
                  data.data.forEach((row: any) => (row.selected = value))
                  ctx.emit('dataChange', cloneDeep(data.data))
                }
              }),
            key: 'selection',
            width: 60,
            render(row: any) {
              return h(NCheckbox, {
                checked: row.selected,
                onUpdateChecked: (value: boolean) => {
                  row.selected = value
                  ctx.emit('dataChange', cloneDeep(data.data))
                },
                disabled: props.readonly
              })
            }
          },
          {
            title: '表名',
            key: 'table_name',
            width: props.isSpan? 0 : 90,
            ellipsis: {
              tooltip: true
            }
          },
          {
            title: '字段',
            key: 'column_name',
            width: 90,
            ellipsis: {
              tooltip: true
            }
          },
          {
            title: '别名',
            key: 'new_name',
            width: 100,
            render(row, index) {
              return h(NInput, {
                value: row.new_name,
                onUpdateValue(v) {
                  data.data[index].new_name = v
                  ctx.emit('dataChange', cloneDeep(data.data))
                }
              })
            }
          }
        ]
      }

      setColumns()

      watch(
        () => props.options,
        (newValue) => {
          // Object.assign(
          //   data.data,
            // newValue.map((item: any) => ({
            //   ...item,
            //   selected: item.selected || false
            // }))
          // )
          data.data.splice(0, data.data.length)
          newValue.forEach(item => {
            data.data.push(item)
          })
          // setColumns()
        },
        { deep: true }
      )
      const rowClassName = (row: any) => {
        if (!row.function_param) {
          return 'empty-row'
        } else {
          for (const k in row.function_param) {
            if (row.function_param.hasOwnProperty(k)) {
              const value = row.function_param[k]
              if (!value && value !== 0) {
                return 'lack-row'
              }
            }
          }
        }
      }
      onMounted(() => {})

      return {
        data,
        fields,
        show,
        renderOption: ({
          node,
          option
        }: {
          node: VNode
          option: SelectOption
        }) =>
          h(NTooltip, null, {
            trigger: () => node,
            default: () => option.label
          }),
        rowClassName
      }
    }
  })
</script>

<style scoped>
  .current {
    margin: auto;
  }
</style>
