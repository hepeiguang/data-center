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
  ref,
} from 'vue'
import {
  NImage
} from 'naive-ui'
import { useThemeStore } from '@/store/theme/theme'
import styles from './index.module.scss'

const Logo = defineComponent({
  name: 'Logo',
  setup() {
    const themeStore = useThemeStore()
    const logo = ref(`${import.meta.env.BASE_URL}images/imgs/logo1.png`)

    return { themeStore, logo }
  },
  render() {
    return (
      <div
        class={[
          styles.logo,
        ]}
        >
        <NImage style={{marginRight: '10px'}} width='56' height='56' preview-disabled={true} src={this.logo} />
        <span>数据资产管控平台</span>
      </div>
    )
  }
})

export default Logo
