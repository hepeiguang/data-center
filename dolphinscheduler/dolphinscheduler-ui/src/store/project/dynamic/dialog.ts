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

import { defineStore } from 'pinia'
import { DialogStore } from './types'

export const useDialogStore = defineStore({
    id: 'dialog-store',
    state: (): DialogStore => ({
        step: 1,
        canNextStep: false,
    }),
    persist: true,
    getters: {
        getStep(): number {
            return this.step
        },
        getDialogCanNext(): Boolean {
            return this.canNextStep
        },
    },
    actions: {
        setStep(step: number): void {
            this.step = step
        },
        setDialogCanNext(canNextStep: Boolean): void {
            this.canNextStep = canNextStep
        },
    }
})
