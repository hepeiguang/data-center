import {
  defineComponent,
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
import log from '@/utils/log'

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
    options: {
      type: Object
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
    watch(
      () => props.value,
      () => {
        console.log('props.value', props.value)
        valueCopy = cloneDeep(props.value)
        // const { elementsRef, rulesRef, model }
        formData.value = useTaskRemote({
          projectCode,
          model: reactive(valueCopy.model),
          json: valueCopy.json,
          hint: valueCopy.hint
        })
      }
    )
    let fields = props.form
    const cancelModal = () => {
      ctx.emit('cancel')
    }

    const confirmModal = async () => {
      await formRef.value.validate()
      const data = formRef.value.getValues()
      ctx.emit('confirm', data)
    }
    return () => (
      <Modal
        show={props.show}
        mask-closable={false}
        title={'函数属性设置'}
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
            hint: formData.value.hint
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
