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
    NDataTable
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
                    title: '表名',
                    key: 'table_name',
                    width: 90,
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
                    title: 'SQL函数',
                    key: 'function_code',
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
                },
                {
                    title: '操作',
                    key: 'actions',
                    render(row: any, index: number) {
                        return [
                            h(NSpace, null, () => [
                                h(
                                    NButton,
                                    {
                                        strong: true,
                                        tertiary: false,
                                        circle: true,
                                        type: 'info',
                                        size: 'small',
                                        onClick: () => setAttributes(row, index)
                                    },
                                    {
                                        icon: () =>
                                            row.function_param
                                                ? h(EditOutlined)
                                                : h(EditOutlined)
                                    }
                                ),
                                h(
                                    NButton,
                                    {
                                        circle: true,
                                        type: 'error',
                                        size: 'small',
                                        onClick: () => delRow(row)
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
            if (row.hasParam && !row.function_param) {
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
            const AttributeRes = await queryFunctionInfoCfgByCode(
                row.function_code
            )
            let functionRemark = AttributeRes.functionRemark
            if (functionRemark) {
                functionRemark = functionRemark.replace(/\\r/g, '&nbsp')
                functionRemark = functionRemark.replace(/\\n/g, '<br>')
                // functionRemark = functionRemark.replaceAll(/'/g, '“')
                // functionRemark = functionRemark.replace(/;/g, '')
            }
            console.log(functionRemark)
            ctx.emit(
                'showModel',
                JSON.parse(AttributeRes.functionAttribute || '[]'),
                index,
                row.function_param,
                functionRemark
            )
        }
        const delRow = (row: any) => {
            data.data = data.data.filter(
                (item: any) =>
                    item.column_name != row.column_name ||
                    item.function_code != row.function_code
            )

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
