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
    },
    fields: {
      type: Object
    },
    field: {
      type: String
    },
    options: {
      type: Object
    },
    isSpan:{
      type: Boolean
    }
  },
  emits: ['cancel', 'confirm', 'dataChange', 'updateValue'],
  setup(props, ctx) {
    const { t } = useI18n()
    const readonly = unref(inject('readonly'))
    const table = ref()
    const valueCopy = reactive(cloneDeep(props.value))
    const optionsCopy = reactive(props.options)
    const fields = props.form
    const myIsSpan  = props.isSpan
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
    watch(
      () => props.options,
      (newVal) => {
        // Object.assign(optionsCopy, cloneDeep(newVal)) // 使用 Object.assign 避免丢失响应性
        newVal.map((item: any) => ({
          ...item,
          selected: item.selected || false
        }))
          // )
        optionsCopy.splice(0, optionsCopy.length)
        newVal.forEach(item => {
          optionsCopy.push(item)
        })
      },
      { deep: true }
    )

    const dataChange = (data: any) => {
      valueCopy.data = data
      let temp = data.filter(item => {
        return item.selected
      })
      ctx.emit('dataChange', temp)
    }

    const showModel = (json: any, index: number, param: any, hint: string) => {
      setIndex.value = index
      const model = json.map((config: any) => {
        return { [config.field]: '' }
      })
      //删除默认值
      delete json[0].value
      delete json[1].value
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
      optionsCopy,
      fields,
      readonly,
      table,
      myIsSpan
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
      readonly,
      optionsCopy,
      myIsSpan
    } = this
    return [
      h(DataTable, {
        ref: 'table',
        value: valueCopy,
        options: optionsCopy,
        fields: fields,
        readonly: readonly,
        onShowModel: showModel,
        onDataChange: dataChange,
        isSpan:myIsSpan
      }),
      h(DataModal, {
        value: variables.task,
        onUpdateValue: (value: object) => (this.variables.task = value),
        show: variables.show,
        onConfirm: confirmModal,
        onCancel: cancelModal,
        form: this.form,
        options: optionsCopy
      })
    ]
  }
})

export function renderOutputField(
  item: IJsonItem,
  fields: { [field: string]: any },
  unused: { [key: string]: FormItemRule }[]
) {
  const { field, children = [] , options=[],props} = isFunction(item) ? item() : item
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
    form: form,
    options: options,
    isSpan:props.isSpan
  })
}
