import {
  defineComponent,
  computed,
  ref,
  reactive,
  watch,
  PropType,
  unref,
  inject
} from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import Modal from '@/components/modal'
import Form from '@/components/form'
import { cloneDeep } from 'lodash'
import { useTaskRemote } from '@/views/projects/task/components/node/use-task-remote'

const DataModal = defineComponent({
  name: 'DataModal',
  emits: ['cancel', 'confirm'],
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
    functionOptions: {
      type: Array
    }
  },
  components: {},
  setup(props, ctx) {
    const { t } = useI18n()
    const route = useRoute()
    const readonly = unref(inject('readonly'))
    const table = ref()
    const formRef = ref()
    const formData = ref()
    const projectCode = Number(route.params.projectCode)
    const code = Number(route.params.code)
    var valueCopy: any = cloneDeep(props.value)
    // formData.value = useTaskRemote({
    //   projectCode,
    //   model: reactive(valueCopy.model),
    //   json: valueCopy.json,
    //   hint: valueCopy.hint
    // })
    const typeCodeArr = computed(
      () =>
        Array.isArray(props.functionOptions.value) &&
        props.functionOptions.value.map((item: any) => {
          return { label: item.label, value: item.value, apiUrl: item.apiUrl }
        })
    )

    watch(
      () => props.value,
      (value: any) => {
        // valueCopy = cloneDeep(props.value)

        console.log(
          '选择数据',
          reactive(value.model),
          props.functionOptions
        )

        let data = props.functionOptions.find((i) => {
          return reactive(value.model).dict_code === i.id
        })
        value = {...value,
          dict_name: data.dictName,
          dict_type: data.dictType
        }
        let json = [
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
          }
        ]
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
          }
        ]
        const model: any = value.model
        switch (model.dict_type) {
          case 'RANGE':
            json = [
              {
                type: 'select',
                field: 'dict_code',
                name: '标准类型',
                span: 24,
                required: true,
                options: props.functionOptions,
                value: model.dict_code,
                props: {
                  disabled: true
                }
              },
              {
                type: 'input-number',
                field: 'min_num',
                name: t('最小值域'),
                span: 12,
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
                span: 12,
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
              }
            ]
            break
          case 'TIMESTAMP':
            json = [
              {
                type: 'select',
                field: 'dict_code',
                name: '标准类型',
                span: 24,
                required: true,
                options: props.functionOptions,
                value: model.dict_code,
                props: {
                  disabled: true
                }
              },
              {
                type: 'select',
                field: 'time_type',
                name: '时间戳',
                options: TIME_TYPE,
                span: 24,
                value: model.time_type,
                validate: {
                  trigger: ['input', 'blur'],
                  required: true,
                  validator(validate: any, value: string) {
                    if (!value) {
                      return new Error(t('请选择时间戳'))
                    }
                  }
                }
              }
            ]
            break
          case 'NUMACCURACY':
            json = [
              {
                type: 'select',
                field: 'dict_code',
                name: '标准类型',
                span: 24,
                required: true,
                options: props.functionOptions,
                value: model.dict_code,
                props: {
                  disabled: true
                }
              },
              {
                type: 'input-number',
                field: 'num_accuracy',
                name: '数值精度',
                span: 24,
                value: model.num_accuracy ? model.num_accuracy : 0,
                props: {
                  min: 0
                },
                slots: {
                  suffix: () => '位小数'
                }
              }
            ]
            break
        }
        // const { elementsRef, rulesRef, model }
        formData.value = useTaskRemote({
          projectCode,
          model: data,
          json: json,
          hint: value.hint
        })
      },
      {
        deep: true
      }
    )
    let fields = props.form
    const cancelModal = () => {
      ctx.emit('cancel')
    }
    const confirmModal = async () => {
      await formRef.value.validate()
      const data = formRef.value.getValues()
      console.log(props.value.model, 'data123')
      const newData = props.value.model;
      switch(data.dictType) {
        case "RANGE":
          newData.min_num = data.min_num
          newData.max_num = data.max_num
          break;
        case "TIMESTAMP":
          newData.time_type = data.time_type
          break;
        case "NUMACCURACY":
          newData.num_accuracy = data.num_accuracy
          break;
      }
      ctx.emit('confirm', newData)
    }
    return () => (
      <Modal
        show={props.show}
        mask-closable={false}
        title={'字典属性设置'}
        onCancel={cancelModal}
        onConfirm={confirmModal}
        confirmDisabled={readonly}
      >
        <Form
          ref={formRef}
          meta={{
            model: formData.value.model,
            rules: formData.value.rulesRef,
            elements: formData.value.elementsRef,
            disabled: unref(readonly),
            // hint: formData.value.hint
          }}
          layout={{
            xGap: 10
          }}
        />
      </Modal>
    )
  }
})

export default DataModal
