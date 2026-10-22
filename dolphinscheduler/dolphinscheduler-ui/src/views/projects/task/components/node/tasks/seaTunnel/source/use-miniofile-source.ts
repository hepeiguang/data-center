import { reactive } from 'vue'
import * as Fields from '../../../fields/index'
import type { IJsonItem, INodeData, ITaskData } from '../../../types'

export function useMinioFileSource({
   projectCode,
   from = 0,
   readonly,
   data
 }: {
  projectCode: number
  from?: number
  readonly?: boolean
  data?: ITaskData
}) {
  const model = reactive({
    taskType: 'MINIOFILE_SOURCE',
    datasourceId: '',
    bucket: '',
    path: '',
    file_format_type: 'text',
    parse_partition_from_path: true,
    xml_row_tag: 'RECORD',
    xml_use_attr_format: false
  } as INodeData)

  return {
    json: [
      Fields.useName(from),
      ...Fields.useMinioFileSource(model),
    ] as IJsonItem[],
    model
  }
}