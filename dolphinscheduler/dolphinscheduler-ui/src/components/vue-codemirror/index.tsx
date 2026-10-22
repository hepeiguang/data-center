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
// src/components/codemirror-editor/index.tsx
import { computed, defineComponent, onMounted, onUnmounted, PropType, ref, watch, nextTick, toDisplayString } from 'vue'
import { basicSetup } from 'codemirror'
import { EditorState, Compartment, Extension, StateEffect, StateField } from '@codemirror/state'
import { javascript } from '@codemirror/lang-javascript'
import { oneDark } from '@codemirror/theme-one-dark'
import { useFormItem } from 'naive-ui/es/_mixins'
import { call } from 'naive-ui/es/_utils'
import { useThemeStore } from '@/store/theme/theme'
import type { MaybeArray, OnUpdateValue, OnUpdateValueImpl } from './types'
import { useResizeObserver } from '@vueuse/core'
import { sql, SQLConfig, StandardSQL } from '@codemirror/lang-sql'
import * as sqlFormatter from 'sql-formatter'
import { autocompletion, completionStatus } from '@codemirror/autocomplete'
import { keymap, EditorView, placeholder } from '@codemirror/view';
import { java, javaLanguage} from '@codemirror/lang-java'
import * as prettier from 'prettier'
import * as parserBabel from 'prettier/plugins/babel'
import { LanguageSupport } from "@codemirror/language"
import { CompletionContext } from '@codemirror/autocomplete'



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
    type: Object as PropType<{
      readOnly?: boolean
      language?: string
      placeholder?: string
    }>,
    default: () => ({
      readOnly: false,
      language: 'shell',
      placeholder:"请输入源代码"
    })
  }
}

export default defineComponent({
  name: 'CodeMirrorEditor',
  props,
  emits: ['update:value', 'change', 'focus', 'blur'],
  setup(props, ctx) {
    const themeStore = useThemeStore()
    const editorRef = ref<HTMLElement>()
    let editor: EditorView | null = null
    const formItem = useFormItem({})
    let currentTheme = themeStore.darkTheme ? oneDark : []
    const isFullscreen = ref(false)
    const readOnlyCompartment = new Compartment()
    const themeCompartment = new Compartment()
    const editorHeight = computed(() => isFullscreen.value ? '100vh' : '300px')
    const btncontent = computed(() => {
      return isFullscreen.value ? `${import.meta.env.BASE_URL}images/task-icons/smallp.png`: `${import.meta.env.BASE_URL}images/task-icons/bigp.png`
    })
    const gshIcon = `${import.meta.env.BASE_URL}images/task-icons/gsh.png`
    const checkFullscreen = () => {
      isFullscreen.value = !!document.fullscreenElement
    }


    onMounted(() => {
      document.addEventListener('fullscreenchange', checkFullscreen)
      document.addEventListener('keydown', (e) => {
        if (e.key === 'F11') checkFullscreen()
      })
    })

    // 在onUnmounted生命周期移除监听
    onUnmounted(() => {
      document.removeEventListener('fullscreenchange', checkFullscreen)
    })
    const toggleFullscreen = () => {
      const el = editorRef.value
      if (!el) return

      if (!document.fullscreenElement) {
        el.requestFullscreen()
          .then(() => {
            isFullscreen.value = true
            document.documentElement.style.overflow = 'hidden'
          })
      } else {
        document.exitFullscreen()
          .then(() => {
            isFullscreen.value = false
            document.documentElement.style.overflow = ''
          })
      }
    }
    
    const javaCompletion = (context: any) => {
      const word = context.matchBefore(/[\w@]*/)
      const triggerChars = ['.', ' ','@']
      if (
        !word ||
        (word.from === word.to &&
          !context.explicit &&
          !triggerChars.includes(context.char))
      ) return null
      return {
        from: word.from,
        options: [
          // 访问修饰符
          { label: 'private', type: 'keyword' },
          { label: 'protected', type: 'keyword' },
          
          // 数据类型
          { label: 'int', type: 'type' },
          { label: 'boolean', type: 'type' },
          { label: 'double', type: 'type' },
          
          // 流程控制
          { label: 'if', type: 'keyword' },
          { label: 'else', type: 'keyword' },
          { label: 'for', type: 'keyword' },
          { label: 'while', type: 'keyword' },
          
          // 类相关
          { label: 'this', type: 'keyword' },
          { label: 'new', type: 'keyword' },
          { label: 'extends', type: 'keyword' },
          { label: 'implements', type: 'keyword' },
          
          // 常用类
          { label: 'ArrayList', type: 'class' },
          { label: 'HashMap', type: 'class' },
          { label: 'Math', type: 'class' },
          
          // 常用方法
          { label: 'length()', type: 'method' },
          { label: 'add()', type: 'method' },
          { label: 'put()', type: 'method' },
          
          // 异常处理
          { label: 'try', type: 'keyword' },
          { label: 'catch', type: 'keyword' },
          { label: 'finally', type: 'keyword' },
          
          // 保留原始条目
          { label: 'public', type: 'keyword' },
          { label: 'class', type: 'keyword' },
          { label: 'void', type: 'keyword' },
          { label: 'String', type: 'class' },
          { label: 'System', type: 'class' },
          { label: 'println', type: 'method' },
          { 
            label: '@Override', 
            type: 'annotation',
            detail: '标记方法重写'
          },
          {
            label: '@Deprecated',
            type: 'annotation',
            detail: '标记已弃用'
          },
          {
            label: '@SuppressWarnings',
            type: 'annotation',
            detail: '抑制编译器警告'
          },
          {
            label: '@FunctionalInterface',
            type: 'annotation',
            detail: '函数式接口标记'
          },
          // 常用框架注解
          {
            label: '@Autowired',
            type: 'annotation',
            detail: 'Spring依赖注入'
          },
          {
            label: '@Service',
            type: 'annotation',
            detail: 'Spring服务层组件'
          },
          {
            label: '@RestController',
            type: 'annotation',
            detail: 'Spring REST控制器'
          },

        ],
        validFor: /^[\w@]*$/
      }
    }

    const javaKeywords = [
      // 基本关键字
      "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", 
      "const", "continue", "default", "do", "double", "else", "enum", "extends", "final", 
      "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", 
      "interface", "long", "native", "new", "package", "private", "protected", "public", 
      "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", 
      "throw", "throws", "transient", "try", "void", "volatile", "while",
      
      // 常用类和方法
      "System.out.println", "System.out.print", "System.err.println", "String", "Integer", 
      "Double", "Boolean", "Character", "ArrayList", "HashMap", "HashSet", "LinkedList", 
      "TreeMap", "TreeSet", "Iterator", "List", "Map", "Set", "Math.abs", "Math.sqrt", 
      "Math.pow", "Math.random", "Object", "Exception", "RuntimeException", "Thread", 
      "Runnable", "StringBuilder", "StringBuffer", "Arrays", "Collections",
      
      // 常用方法和字段
      "length()", "substring()", "equals()", "equalsIgnoreCase()", "toString()", 
      "valueOf()", "parseInt()", "parseDouble()", "get()", "put()", "size()", 
      "isEmpty()", "add()", "remove()", "contains()", "iterator()", "next()", 
      "hasNext()", "clear()", "toCharArray()", "charAt()", "indexOf()", "trim()", 
      "toUpperCase()", "toLowerCase()", "startsWith()", "endsWith()", "replace()",
      "split()", "join()", "sort()", "reverse()", "clone()", "hashCode()", 
      "wait()", "notify()", "notifyAll()", "sleep()", "start()", "run()", 
      "join()", "interrupt()", "isAlive()", "currentThread()", "isInterrupted()", 

      "@Override", "@Deprecated", "@SuppressWarnings", "@FunctionalInterface",
      "@Autowired", "@Service", "@RestController", "@Component", "@Repository", "@Bean",
      "@Configuration", "@Controller", "@RequestMapping", "@GetMapping", 
      "@PostMapping", "@PutMapping", "@DeleteMapping", "@PathVariable", 
      "@RequestParam", "@ResponseBody", "@RequestMapping.method", "@RequestBody", 
      "@RequestHeader", "@CookieValue", "@SessionAttribute", "@RequestPart", 
      "@RequestAttribute", "@CrossOrigin", "@Valid", "@Validated", "@ExceptionHandler", 
      "@ControllerAdvice", "@ExceptionHandler.value", "@ExceptionHandler.exception", 
      "@ExceptionHandler.responseStatus", "@RestControllerAdvice", "@EnableWebMvc", 
      "@EnableAspectJAutoProxy", "@ConfigurationProperties", "@PropertySource", 
      "@Configuration.value", "@Configuration.proxyBeanMethods"
    ];
    
    // 自定义 Java 自动提示函数
    const javaCompletionSource = (context: CompletionContext): CompletionResult | null => {
      const before = context.matchBefore(/[\w$@]+/);
      if (!before) return null;
      
      const prefix = before.text;
      const candidates = javaKeywords.filter(item => 
        item.toLowerCase().startsWith(prefix.toLowerCase())
      );
      
      // 简化的"."后方法提示逻辑
      const textBeforeCursor = context.state.doc.sliceString(0, context.pos);
      const lastDotIndex = textBeforeCursor.lastIndexOf('.');
      
      if (lastDotIndex > -1 && lastDotIndex > textBeforeCursor.lastIndexOf('\n')) {
        const precedingText = textBeforeCursor.slice(lastDotIndex + 1).trim();
        if (precedingText === prefix) {
          // 过滤只显示方法
          const methodCandidates = candidates.filter(item => item.includes('('));
          if (methodCandidates.length > 0) {
            return {
              from: before.from,
              options: methodCandidates.map(label => ({ label }))
            };
          }
        }
      }
      
      return {
        from: before.from,
        options: candidates.map(label => ({ label }))
      };
    };

    const formatSQL = async() => {
      if (!editor) return
      const code = editor.state.doc.toString()
      const formatted = await sqlFormatter.format(code, {
        language: 'sql',
        tabWidth: 2,
        keywordCase: 'upper',
        indentStyle: 'standard',
        linesBetweenQueries: 2

      })
      editor.dispatch({
        changes: {
          from: 0,
          to: editor.state.doc.length,
          insert: formatted
        }
      })
    }
    const formatJs = async() => {
      if (!editor) return
      const code = editor.state.doc.toString()
      const formatted = await prettier.format(code, {
        parser: 'babel',
        plugins: [
          (await import('prettier/plugins/babel')).default,
          (await import('prettier/plugins/estree')).default
        ],
        semi: true,
        singleQuote: true,
        tabWidth: 2,
        trailingComma: 'none',
        arrowParens: 'avoid'
      })
      editor.dispatch({
        changes: {
          from: 0,
          to: editor.state.doc.length,
          insert: formatted
        }
      })
    }
    const formatJava = async () => {
      if (!editor) return;
      try {
        const code = editor.state.doc.toString();
        const javaPlugin = await import('prettier-plugin-java');
        const formatted = await prettier.format(code, {
          parser: 'java',
          plugins: [javaPlugin.default], 
          tabWidth: 4,
          semi: true
        });
        editor.dispatch({
          changes: { from: 0, to: editor.state.doc.length, insert: formatted }
        });
      } catch (error) {
        console.error('Java格式化失败:', error);
      }
    }

    const createEditor = () => {
      if (!editorRef.value) return
      const extensions = [
        basicSetup,
        themeCompartment.of(currentTheme),
        readOnlyCompartment.of(EditorState.readOnly.of(!!formItem.mergedDisabledRef.value)),
        EditorView.theme({
          '&': { height: '100%', minHeight: '300px' },
          '.cm-scroller': { overflow: 'auto' }
        }),
        EditorView.updateListener.of(update => {
          if (update.docChanged) {
            const value = update.state.doc.toString()
            // 添加类型安全校验
            if (Array.isArray(props.onUpdateValue)) {
              props.onUpdateValue.forEach(fn => fn?.(value))
            } else if (typeof props.onUpdateValue === 'function') {
              props.onUpdateValue(value)
            }
            // 处理 v-model 双向绑定
            console.log(value)
            ctx.emit('update:value', value)
            ctx.emit('change', value)
            formItem.nTriggerFormChange()
            formItem.nTriggerFormInput()
          }
        }),
        currentTheme,
        autocompletion(),
        placeholder(props.options.placeholder || ''),
        props.options.language === 'java' && [
          new LanguageSupport(java()), 
          autocompletion({     override: [javaCompletionSource],
            activateOnTyping: true,
            selectOnOpen: false // 不自动选择第一个选项
         }) 
        ],
        props.options.language === 'javascript' && javascript(),
        props.options.language === 'sql' && sql({
          dialect: StandardSQL,
          upperCaseKeywords: true,
          schema: {
            // 可配置数据库schema提示
            users: {
              columns: ['id', 'name']  // 列名直接使用字符串数组
            }
          }
        }),
        props.options.language === 'sql' && EditorView.domEventHandlers({
          keydown: (e, view) => {
            if ((e.ctrlKey || e.metaKey) && e.key === 's') {
              e.preventDefault()
              formatSQL()
            }
          }
        }),
        props.options.language === 'javascript' && EditorView.domEventHandlers({
          keydown: (e, view) => {
            if ((e.ctrlKey || e.metaKey) && e.key === 's') {
              e.preventDefault()
              formatJs()
            }
          }
        }),
        props.options.language === 'java' && EditorView.domEventHandlers({
          keydown: (e, view) => {
            if ((e.ctrlKey || e.metaKey) && e.key === 's') {
              e.preventDefault()
              formatJava()
            }
          }
        })

      ]
      editor = new EditorView({
        state: EditorState.create({
          doc: props.defaultValue ?? props.value,
          extensions: extensions.filter((e): e is Extension => !!e) // 类型断言过滤false值
        }),
        parent: editorRef.value
      })
    }


    onMounted(() => {
      createEditor()
      useResizeObserver(editorRef, () => {
        editor?.requestMeasure()
      })
    })

    onUnmounted(() => {
      editor?.destroy()
    })

    watch(() => props.value, (newVal) => {
      if (newVal !== editor?.state.doc.toString()) {
        editor?.dispatch({
          changes: { from: 0, to: editor.state.doc.length, insert: newVal }
        })
      }
    })

    watch(() => formItem.mergedDisabledRef.value, (readOnly) => {
      editor?.dispatch({
        effects: readOnlyCompartment.reconfigure(
          EditorState.readOnly.of(readOnly)
        )
      })
    })

    watch(() => themeStore.darkTheme, (isDark) => {
      currentTheme = isDark ? oneDark : []
      editor?.dispatch({
        effects: [
          StateEffect.reconfigure.of(themeCompartment.of(currentTheme))
        ]
      })
    })

    ctx.expose({
      getValue: () => editor?.state.doc.toString() || ''
    })

    return () => (
      <div style={{ position: 'relative', width: '100%' }}>
        <div
          ref={editorRef}
          style={{
            width: '100%',
            border: '1px solid #eee',
            borderRadius: '3px',
            background: '#fff',
            height: editorHeight.value,
            transition: 'height 0.3s',
            position: 'relative',
            // paddingRight: '40px',
            boxSizing: 'border-box'
          }}
        >
          <span style={{
            display: 'inline-block',
            position: 'absolute',
            right: '0px',
            top: '0px',
            zIndex: 100,
            background: '#fff',
            // padding: '4px 8px',
            // borderRadius: '4px',
            // border: '1px solid #ddd',
            // cursor: 'pointer'
          }}>
            <button
              onClick={props.options.language == 'sql' ? formatSQL : props.options.language == 'java'?formatJava : formatJs}
              style={{
                width: '24px',
                height: '24px',
                border: '1px solid #ddd',
                borderRadius: '4px',
                cursor: 'pointer',
                // backgroundImage: "url('/images/task-icons/gsh.png')",
                backgroundImage: `url(${gshIcon})`,
                backgroundSize: '16px',
                backgroundRepeat: 'no-repeat',
                backgroundPosition: 'center',
                padding: 0
              }}
            >
            </button>
            <button
              onClick={toggleFullscreen}
              style={{
                width: '24px',
                height: '24px',
                border: '1px solid #ddd',
                borderRadius: '4px',
                cursor: 'pointer',
                backgroundImage: `url(${btncontent.value})`,
                backgroundSize: '16px',
                backgroundRepeat: 'no-repeat',
                backgroundPosition: 'center',
                padding: 0
              }}
            >
            </button>
          </span>
        </div>
      </div>
    )
  }
})