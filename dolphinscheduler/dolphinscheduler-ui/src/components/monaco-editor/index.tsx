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

import {
  defineComponent,
  onMounted,
  onUnmounted,
  PropType,
  ref,
  watch,
  computed
} from 'vue'
import {
  NImage
} from 'naive-ui'
import { useFormItem } from 'naive-ui/es/_mixins'
import { call } from 'naive-ui/es/_utils'
import { useThemeStore } from '@/store/theme/theme'
// import * as monaco from 'monaco-editor'
// import editorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker'
// import jsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker'
// import cssWorker from 'monaco-editor/esm/vs/language/css/css.worker?worker'
// import htmlWorker from 'monaco-editor/esm/vs/language/html/html.worker?worker'
// import tsWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker'
import type {
  MaybeArray,
  OnUpdateValue,
  OnUpdateValueImpl,
  // monaco as Monaco
} from './types'

const props = {
  value: {
    type: String as PropType<string>,
    default: ''
  },
  defaultValue: {
    type: String as PropType<string>
  },
  'onUpdate:value': [Function, Array] as PropType<MaybeArray<OnUpdateValue>>,
  onUpdateValue: [Function, Array] as PropType<MaybeArray<OnUpdateValue>>,
  options: {
    type: Object as any,// PropType<Monaco.editor.IStandaloneEditorConstructionOptions>,
    default: () => ({
      readOnly: false,
      language: 'shell'
    })
  }
}

// @ts-ignore
window.MonacoEnvironment = {
  // getWorker(_: any, label: string) {
    // if (label === 'json') {
    //   return new jsonWorker()
    // }
    // if (['css', 'scss', 'less'].includes(label)) {
    //   return new cssWorker()
    // }
    // if (['html', 'handlebars', 'razor'].includes(label)) {
    //   return new htmlWorker()
    // }
    // if (['typescript', 'javascript'].includes(label)) {
    //   return new tsWorker()
    // }
    // return new editorWorker()
  // }
}

export default defineComponent({
  name: 'MonacoEditor',
  props,
  emits: ['change', 'focus', 'blur'],
  setup(props, ctx) {
    let editor = null as any // monaco.editor.IStandaloneCodeEditor | null
    const themeStore = useThemeStore()
    const monacoEditorThemeRef = ref(themeStore.darkTheme ? 'vs-dark' : 'vs')
    const editorRef = ref()
    const editorBaseRef = ref()
    const getValue = () => editor?.getValue()
    const formItem = useFormItem({})

    const initMonacoEditor = () => {
      const dom = editorRef.value
      // if (dom) {
      //   editor = monaco.editor.create(dom, {
      //     ...props.options,
      //     readOnly: formItem.mergedDisabledRef.value || props.options?.readOnly,
      //     value: props.defaultValue ?? props.value,
      //     automaticLayout: true,
      //     theme: monacoEditorThemeRef.value,
      //     scrollbar: {
      //       alwaysConsumeMouseWheel: false
      //     }
      //   })
      //   editor.onDidChangeModelContent(() => {
      //     const { onUpdateValue, 'onUpdate:value': _onUpdateValue } = props
      //     const value = editor?.getValue() || ''

      //     if (onUpdateValue) call(onUpdateValue as OnUpdateValueImpl, value)
      //     if (_onUpdateValue) call(_onUpdateValue as OnUpdateValueImpl, value)
      //     ctx.emit('change', value)

      //     formItem.nTriggerFormChange()
      //     formItem.nTriggerFormInput()
      //   })
      //   editor.onDidBlurEditorWidget(() => {
      //     ctx.emit('blur')
      //     formItem.nTriggerFormBlur()
      //   })
      //   editor.onDidFocusEditorWidget(() => {
      //     ctx.emit('focus')
      //     formItem.nTriggerFormFocus()
      //   })
      // }
    }

    const isFullScreen = ref(false)
    const baseStyle = computed(() => {
      return isFullScreen.value? {position :'fixed', height: '100%', width: '100%', top: 0, left: 0, zIndex: 1000}: {position :'relative', height: '300px', width: '100%'}
    })

    const fullscreenImg = computed(() => {
      return isFullScreen.value ? `${import.meta.env.BASE_URL}images/imgs/unfullScreen.png`: `${import.meta.env.BASE_URL}images/imgs/fullScreen.png`
    })
    const changeFullScreen = () => {
      isFullScreen.value = !isFullScreen.value
      console.log('isFullScreen', isFullScreen.value)
      // if (isFullScreen.value) {
      //   editorBaseRef.value.style.position = 'fixed'
      //   editorBaseRef.value.style.height = '100%'
      //   editorBaseRef.value.style.top = '0'
      //   editorBaseRef.value.style.left = '0'
      //   editorBaseRef.value.style.zIndex = '1000'
      // }else{
      //   editorBaseRef.value.style.position = 'relative'
      //   editorBaseRef.value.style.height = '300px'
      // }
    }

    onMounted(() => initMonacoEditor())

    onUnmounted(() => {
      editor?.dispose()
    })

    watch(
      () => props.value,
      (val) => {
        if (val !== getValue()) {
          editor?.setValue(val)
        }
      }
    )

    watch(
      () => formItem.mergedDisabledRef.value,
      (value) => {
        editor?.updateOptions({ readOnly: value })
      }
    )

    watch(
      () => themeStore.darkTheme,
      () => {
        editor?.dispose()
        monacoEditorThemeRef.value = themeStore.darkTheme ? 'vs-dark' : 'vs'
        initMonacoEditor()
      }
    )

    ctx.expose({ getValue })

    return { editorRef, editorBaseRef, changeFullScreen, isFullScreen, baseStyle, fullscreenImg }
  },
  render() {
    return (
      <div  ref='editorBaseRef' style={this.baseStyle}>
        <div
          ref='editorRef'
          style={{
            height: '100%',
            width: '100%',
            border: '1px solid #eee'
          }}
        />
      <NImage style={{position: 'absolute', top: '10px', right: '10px', zIndex: 100, width: '30px', height: '30px'}} width='30' height='30' preview-disabled={true} src={this.fullscreenImg} onClick={this.changeFullScreen} />
      </div>
    )
  }
})
