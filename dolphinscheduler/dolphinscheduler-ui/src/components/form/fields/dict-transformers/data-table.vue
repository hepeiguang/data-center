<template>
  <n-data-table
    :columns="data.columns"
    :data="data.data"
    
    :pagination="false"
    :bordered="false"
  />
  <!-- :row-class-name="rowClassName"  原未填写的显示粉红色 -->
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
    NDataTable
} from 'naive-ui'
import { PlusOutlined, DeleteOutlined, EditOutlined } from '@vicons/antd'
import { cloneDeep } from 'lodash'
import { queryDictInfoCfgById } from '@/service/modules/dict-info'

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
        }
    },
    setup(props, ctx) {
        var valueCopy: any = cloneDeep(props.value)
        const data = reactive(valueCopy)
        const fields = reactive(props.fields)
        const show = ref(false)
        if (data.columns == null) {
            data.columns = [
                {
                    title: '序号',
                    key: 'no',
                    width: 60,
                    render: (row, index) => index + 1
                },
                {
                    title: '字段来源',
                    key: 'table_name',
                    ellipsis: {
                        tooltip: true
                    }
                },
                {
                    title: '字段名称',
                    key: 'column_name',
                    ellipsis: {
                        tooltip: true
                    }
                },
                {
                    title: '字典标准',
                    key: 'dict_name',
                    ellipsis: {
                        tooltip: true
                    }
                },

                {
                    title: '操作',
                    key: 'actions',
                    width: 120,
                    render(row: any, index: number) {
                        return [
                            h(NSpace, null, () => [
                                row.dict_type !== "NAMESPACE" ? 
                                h(
                                    NButton,
                                    {
                                        strong: true,
                                        tertiary: false,
                                        circle: true,
                                        type: 'info',
                                        size: 'small',
                                        disabled: row.dict_type ==='NAMESPACE',
                                        onClick: () => setAttributes(row, index)
                                    },
                                    {
                                        icon: () =>
                                            row.function_param
                                                ? h(EditOutlined)
                                                : h(EditOutlined)
                                    }
                                ) : null,
                                h(
                                    NButton,
                                    {
                                        circle: true,
                                        type: 'error',
                                        size: 'small',
                                        onClick: () => delRow(row, index)
                                    },
                                    { icon: () => h(DeleteOutlined) }
                                )
                            ])
                        ]
                    }
                }
            ]
        }

        watch(
            () => props.value,
            (newValue) => {
                Object.assign(data.data, newValue.data)
            },
            { deep: true }
        )
        const rowClassName = (row: any) => {
            console.log("row"+JSON.stringify(row));
            if (!row.dict_code) {
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
        const setAttributes = async (row: any, index: number) => {
          console.log(row, 'row')
            ctx.emit(
                'showModel',
                index,
                row
            )
        }
        const delRow = (row: Object,index: number) => {
            data.data = data.data.filter(
                (item: any) =>
                    item.column_name != row.column_name
                    //  || item.dict_code != row.dict_code
            )
            // console.log(data.data, 'data.data')
            ctx.emit('dataChange', cloneDeep(data.data))
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
