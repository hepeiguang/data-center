import { ref, onMounted, onUnmounted, computed, watch, watchEffect, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import type { IJsonItem } from '../../../types'
export function useHiveSink(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()

  const advancedSpan = computed(() => (model.advanced == true ? 24 : 0))

  return [
    {
      type: 'input',
      field: 'metastore_uri',
      name: t('seaTunnel.metastore_uri'),
      props: {
        placeholder: t('thrift://metastore:9083'),
        maxLength: 256
      },
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('seaTunnel.metastore_uri') + t('project.node.required'))
          }
        }
      }
    },
    {
      type: 'input',
      field: 'table_name',
      name: t('seaTunnel.table_name'),
      props: {
        placeholder: t('default.test_hive_sink_on_hdfs_with_kerberos'),
        maxLength: 256
      },
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('seaTunnel.table_name') + t('project.node.required'))
          }
        }
      }
    },
    {
      type: 'input',
      field: 'hdfs_site_path',
      name: t('seaTunnel.hdfs_site_path'),
      tooltip: '用于加载Namenode的高可用配置',
      props: {
        placeholder: t('')
      }
    },
    {
      type: 'input',
      field: 'hive_site_path',
      name: t('seaTunnel.hive_site_path'),
      props: {
        placeholder: t('')
      }
    },
    {
      type: 'switch',
      field: 'advanced',
      name: t('seaTunnel.advanced')
    },
    {
      type: 'mirror',
      field: 'hiveHadoopConfVo',
      name: t('seaTunnel.hiveHadoopConfVo'),
      tooltip: 'Hadoop 配置中的属性（core-site.xml、hdfs-site.xml、hive-site.xml），请输入标准json,例如：{ "key": "value" }',
      span: advancedSpan,
      validate: {
        trigger: ['input', 'trigger'],
        validator(validate: any, value: string) {
          if (value) {
            try {
              JSON.parse(value)
            } catch (error) {
              return new Error('输入的内容不是合法的 JSON 格式')
            }
          }
        }
      },
      props: {
        language: 'sql'
      }
    },
    {
      type: 'input',
      field: 'hive.hadoop.conf-path',
      name: t('seaTunnel.hive_hadoop_conf_path'),
      tooltip: '指定加载core-site.xml、hdfs-site.xml、hive-site.xml文件的路径',
      span: advancedSpan,
      props: {
        placeholder: t('')
      }
    },
    {
      type: 'input',
      field: 'krb5_path',
      name: t('seaTunnel.krb5_path'),
      tooltip: '用于Kerberos认证',
      span: advancedSpan,
      props: {
        placeholder: t('/etc/krb5.conf')
      }
    },
    {
      type: 'input',
      field: 'kerberos_principal',
      name: t('seaTunnel.kerberos_principal'),
      span: advancedSpan,
      props: {
        placeholder: t('')
      }
    },
    {
      type: 'input',
      field: 'kerberos_keytab_path',
      name: t('seaTunnel.kerberos_keytab_path'),
      span: advancedSpan,
      props: {
        placeholder: t('')
      }
    },
    {
      type: 'switch',
      field: 'abort_drop_partition_metadata',
      name: t('seaTunnel.abort_drop_partition_metadata'),
      tooltip: '注意：这只影响元存储中的元数据，分区中的数据将始终被删除（同步过程中生成的数据）',
      span: advancedSpan
    },
    {
      type: 'input',
      field: 'compress_codec',
      name: t('seaTunnel.compress_codec'),
      span: advancedSpan,
      props: {
        placeholder: t('lzo/none')
      }
    }
  ]
}