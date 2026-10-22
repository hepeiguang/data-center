# DS 3.4.3 UDF Resource Patch

Official DolphinScheduler **3.4.3** keeps `ResourceType.UDF` in SPI but removed the
`UDF` branch from `AbstractStorageOperator`, so requests with `type=UDF` fail with:

`Resource type: UDF not supported`

Mono UI / SeaTunnel UDF pages still send `type=UDF`. This patch restores the
`UDF` branch (files under `{tenant}/udfs/`).

## Mono source

The same fix lives in tree:

- `dolphinscheduler-spi/.../ResourceType.java` — `UDF(1, "udf")`
- `dolphinscheduler-storage-api/.../StorageOperator.java` — `UDF_FOLDER_NAME = "udfs"`
- `dolphinscheduler-storage-api/.../AbstractStorageOperator.java` — `case UDF`

When you run a self-built DS from mono, you do not need this binary patch.

## Apply (official 3.4.3 bin)

```bat
cd dolphinscheduler\patches\ds-3.4.3-udf-resource
apply-to-ds-bin.cmd
```

Requires `JAVA_HOME` (JDK 11) and a **stopped** DS process (jars are locked while
running). Default install: `D:\ds\apache-dolphinscheduler-3.4.3-bin`.
