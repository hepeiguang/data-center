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
  inject,
  computed
} from 'vue'
import { dateArDZ, FormItemRule } from 'naive-ui'
import { isFunction, values, cloneDeep } from 'lodash'
import { useI18n } from 'vue-i18n'
import { IJsonItem } from '@/components/form/types'
import DataTable from './data-table.vue'
import DataModal from './data-modal'
import { queryDictInfo } from '@/service/modules/dict-info'
import functionModal from '@/views/resource/udf/function/components/function-modal'
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
    const valueCopy = reactive(cloneDeep(props.value))
    const fields = props.form
    const variables = reactive({
      show: ref(false),
      task: ref({})
    })
    const setIndex = ref(0)
    const functionOptions = ref([] as any)
    functionOptions.value = []
    const dictCodeArr = computed(() => (Array.isArray(functionOptions.value) && functionOptions.value.find((item:any) => item.value === 'NAMESPACE')?.children ))
    const typeCodeArr = computed(() => (Array.isArray(functionOptions.value) && functionOptions.value.map((item:any) => {return {'label': item.label, 'value': item.value, 'apiUrl': item.apiUrl }} ) ))
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

    const showModel = (index: number, param: any) => {
      const json = arrangementJson(param)
      const model = json.map((config: any) => {
        return { [config.field]: '' }
      })
      variables.task = {
        model: param ? param : Object.assign({}, ...model),
        json: json,
      }
      variables.show = true
    }
    // 整理json
    const arrangementJson = (param) => {
      const TIME_TYPE = [
        {
          label: 'yyyy',
          value: 'yyyy'
        },
        {
          label: 'yyyy-MM',
          value: 'yyyy-MM'
        },
        {
          label: 'yyyy-MM-dd',
          value: 'yyyy-MM-dd'
        },
        {
          label: 'yyyy-MM-dd HH',
          value: 'yyyy-MM-dd HH'
        },
        {
          label: 'yyyy-MM-dd HH:mm',
          value: 'yyyy-MM-dd HH:mm'
        },
        {
          label: 'yyyy-MM-dd HH:mm:ss',
          value: 'yyyy-MM-dd HH:mm:ss'
        },
      ]
      const model:any = param
      const minMaxSpan = computed(() => (model.dict_type === 'RANGE' ? 12 : 0))
      const timeTypeSpan = computed(() => ( model.dict_type === 'TIMESTAMP' ? 24 : 0))
      const numAccuracySpan = computed(() => (model.dict_type === 'NUMACCURACY' ? 24 : 0))
      const dictCodeSpan = computed(() =>  0)
      return [
        {
          type: 'select',
          field: 'dict_code',
          name: '标准类型',
          span: 24,
          required: true,
          options: typeCodeArr,
          props: {
            disabled: false
          }
        },
        {
          type: 'input-number',
          field: 'min_num',
          name: t('最小值域'),
          span: minMaxSpan,
          value: model.min_num ? model.min_num : 0,
          props: { min: 1, placeholder: '10000' },
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('请输入最小值域'))
              } else if (model.max_num < value) {
                return new Error(t('最小值域需小于最大值域'))
              }
            }
          }
        },
        {
          type: 'input-number',
          field: 'max_num',
          name: t('最大值域'),
          span: minMaxSpan,
          value: model.max_num ? model.max_num : 0,
          props: { min: 1, placeholder: '10000' },
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('请输入最大值域'))
              } else if (model.min_num > value) {
                return new Error(t('最大值域需大于最小值域'))
              }
            }
          }
        },
        {
          type: 'select',
          field: 'time_type',
          name: '时间戳',
          span: timeTypeSpan,
          options: TIME_TYPE,
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('请选择时间戳'))
              }
            }
          }
        },
        {
          type: 'input-number',
          field: 'num_accuracy',
          name: '数值精度',
          value: model.num_accuracy ? model.num_accuracy : 0,
          span: numAccuracySpan,
          props: {
            min: 0
          },
          slots: {
            suffix: () => '位小数'
          }
        },
        {
          type: 'tree-select',
          field: 'dict_code',
          name: t('字典选择'),
          span: dictCodeSpan,
          options: dictCodeArr,
          props: {
            overrideDefaultNodeClickBehavior: true,
            keyField: 'value',
            labelField: 'label'
          },
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('请选择字典'))
              }
            }
          }
        },
      ]
    }
    const cancelModal = () => {
      variables.show = false
    }

    const confirmModal = async (data: any) => {
      // console.log(data, setIndex, 'datavalueCopy')
      // valueCopy.data[setIndex.value].function_param = data
      // console.log(valueCopy, 'valueCopy.data')
      console.log(data, 'datadata')
      let index = valueCopy.data.findIndex((item:any) => item.column_name === data.column_name)
      // 判断当前是否nameSpace类型
      if (data.dict_type === 'NAMESPACE') {
        data.dict_name = typeCodeArr.value.find((ele:any) => ele.value === data.dict_code)?.label
      }
      valueCopy.data[index] = data
      ctx.emit('dataChange', valueCopy.data)
      variables.show = false
    }
    const getDataSourceList = async () => {
      functionOptions.value = await queryDictInfo()
    }
    onMounted(() => {
      getDataSourceList()
    })
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
      functionOptions,
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
      readonly,
      functionOptions
    } = this
    return [
      
      h(DataTable, {
        ref: 'table',
        value: valueCopy,
        fields: fields,
        readonly: readonly,
        onShowModel: showModel,
        onDataChange: dataChange
      }),
      h(DataModal, {
        value: variables.task,
        onUpdateValue: (value: object) => (this.variables.task = value),
        show: variables.show,
        onConfirm: confirmModal,
        onCancel: cancelModal,
        form: this.form,
        functionOptions:functionOptions
      })
    ]
  }
})

export function renderDictTransformers(
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
