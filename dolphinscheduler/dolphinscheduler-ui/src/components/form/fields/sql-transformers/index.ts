import {
  defineComponent,
  h,
  ref,
  PropType,
  reactive,
  toRefs,
  onMounted,
  watch,
  unref,
  inject
} from 'vue'
import { FormItemRule } from 'naive-ui'
import { isFunction, values, cloneDeep } from 'lodash'
import { useI18n } from 'vue-i18n'
import { IJsonItem } from '@/components/form/types'
import DataTable from './data-table.vue'
import DataModal from './data-modal'
// import {getDependency } from '@/service/modules/data-source'

const props: any = {
  fields: {
    type: Object
  },
  field: {
    type: String
  }
} as const

const DefinitionTable = defineComponent({
  name: 'DefinitionTable',
  props: {
    show: {
      type: Boolean as PropType<boolean>
      // default: false
    },
    value: {
      type: Object
    },
    form: {
      type: Object
    }
  },
  emits: ['cancel', 'confirm', 'dataChange', 'updateValue'],
  setup(props, ctx) {
    const { t } = useI18n()
    const readonly = unref(inject('readonly'))
    const table = ref()
    //将传递的值变为响应式
    const valueCopy = reactive(cloneDeep(props.value))
    const fields = props.form
    const variables = reactive({
      show: ref(false),
      task: ref({})
    })
    const setIndex = ref(0)
    watch(
      () => props.value,
      (newVal) => {
        Object.assign(valueCopy, cloneDeep(newVal)) // 使用 Object.assign 避免丢失响应性
      },
      { deep: true }
    )

    const dataChange = (data: any) => {
      valueCopy.data = data
      ctx.emit('dataChange', valueCopy.data)
    }

    //点击
    const showModel = (json: any, index: number, param: any, hint: string) => {
      setIndex.value = index

      const model = json.map((config: any) => {
        return { [config.field]: '' }
      })

      variables.task = {
        model: param ? param : Object.assign({}, ...model),
        json: json,
        hint
      }
      variables.show = true
    }
    const cancelModal = () => {
      variables.show = false
    }

    const confirmModal = async (data: any) => {
      valueCopy.data[setIndex.value].function_param = data
      ctx.emit('dataChange', valueCopy.data)
      variables.show = false
    }
    return {
      t,
      variables,
      dataChange,
      showModel,
      cancelModal,
      confirmModal,
      valueCopy,
      fields,
      readonly,
      table
    }
  },
  render() {
    const {
      variables,
      dataChange,
      showModel,
      cancelModal,
      confirmModal,
      valueCopy,
      fields,
      readonly
    } = this
    return [


      // 显示表格；
      // 接收 value, fields, readonly 等属性；
      // 用户点击某行会触发 onShowModel 打开弹窗。
      h(DataTable, {
        ref: 'table',
        value: valueCopy,
        fields: fields,
        readonly: readonly,
        onShowModel: showModel,
        onDataChange: dataChange
      }),

      // 显示模态框；
      // 接收 value, form, show；
      // 提交后调用 onConfirm，取消调用 onCancel。
      h(DataModal, {
        value: variables.task,
        onUpdateValue: (value: object) => (this.variables.task = value),
        show: variables.show,
        onConfirm: confirmModal,
        onCancel: cancelModal,
        form: this.form
      })
    ]
  }
})

export function renderSqlTransformers(
  item: IJsonItem,
  fields: { [field: string]: any },
  unused: { [key: string]: FormItemRule }[]
) {
  const { field, children = [] } = isFunction(item) ? item() : item
  const tableData = reactive({
    data: fields[field]
  })
  const form: any = fields
  const onDataChange = (data: any) => {
    fields[field] = data
  }

  return h(DefinitionTable, {
    value: tableData,
    onUpdateValue: (value: object) => true,
    show: true,
    onDataChange: onDataChange,
    form: form
  })
}
