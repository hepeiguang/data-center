package org.apache.dolphinscheduler.plugin.storage.api;

import org.apache.dolphinscheduler.common.utils.FileUtils;
import org.apache.dolphinscheduler.spi.enums.ResourceType;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Objects;

/**
 * Patched for ResourceType.UDF (official 3.4.3 removed UDF case and throws
 * "Resource type: UDF not supported"). UDF files live under tenant/udfs/.
 */
public abstract class AbstractStorageOperator implements StorageOperator {

    /** Kept local so we do not need to change StorageOperator interface bytecode. */
    public static final String UDF_FOLDER_NAME = "udfs";

    protected final String resourceBaseAbsolutePath;

    public AbstractStorageOperator(String resourceBaseAbsolutePath) {
        this.resourceBaseAbsolutePath =
                Objects.requireNonNull(resourceBaseAbsolutePath, "Resource upload path should not be null");
    }

    @Override
    public ResourceMetadata getResourceMetaData(String resourceAbsolutePath) {
        String storageBaseDirectory = getStorageBaseDirectory();
        String resourceSegment = StringUtils.substringAfter(resourceAbsolutePath, storageBaseDirectory);
        String[] segments = StringUtils.split(resourceSegment, File.separator, 3);
        if (segments.length == 0) {
            throw new IllegalArgumentException("Invalid resource path: " + resourceAbsolutePath);
        }
        ResourceType detectedType = ResourceType.FILE;
        if (segments.length >= 2 && UDF_FOLDER_NAME.equals(segments[1])) {
            detectedType = ResourceType.UDF;
        }
        String name = resourceAbsolutePath;
        int slash = Math.max(resourceAbsolutePath.lastIndexOf('/'), resourceAbsolutePath.lastIndexOf('\\'));
        if (slash >= 0 && slash < resourceAbsolutePath.length() - 1) {
            name = resourceAbsolutePath.substring(slash + 1);
        }
        boolean isDirectory = !name.contains(".");
        return ResourceMetadata.builder()
                .resourceAbsolutePath(resourceAbsolutePath)
                .resourceBaseDirectory(storageBaseDirectory)
                .isDirectory(isDirectory)
                .tenant(segments[0])
                .resourceType(detectedType)
                .resourceRelativePath(segments.length == 2 ? "/" : segments[2])
                .resourceParentAbsolutePath(StringUtils.substringBeforeLast(resourceAbsolutePath, File.separator))
                .build();
    }

    @Override
    public String getStorageBaseDirectory() {
        return resourceBaseAbsolutePath;
    }

    @Override
    public String getStorageBaseDirectory(String tenantCode) {
        if (StringUtils.isEmpty(tenantCode)) {
            throw new IllegalArgumentException("Tenant code should not be empty");
        }
        return FileUtils.concatFilePath(getStorageBaseDirectory(), tenantCode);
    }

    @Override
    public String getStorageBaseDirectory(String tenantCode, ResourceType resourceType) {
        String tenantBaseDirectory = getStorageBaseDirectory(tenantCode);
        if (resourceType == null) {
            throw new IllegalArgumentException("Resource type should not be null");
        }
        String resourceBaseDirectory;
        switch (resourceType) {
            case FILE:
                resourceBaseDirectory =
                        FileUtils.concatFilePath(tenantBaseDirectory, StorageOperator.FILE_FOLDER_NAME);
                break;
            case UDF:
                resourceBaseDirectory = FileUtils.concatFilePath(tenantBaseDirectory, UDF_FOLDER_NAME);
                break;
            case ALL:
                resourceBaseDirectory = tenantBaseDirectory;
                break;
            default:
                throw new IllegalArgumentException("Resource type: " + resourceType + " not supported");
        }
        return resourceBaseDirectory;
    }

    @Override
    public String getStorageFileAbsolutePath(String tenantCode, String fileName) {
        return FileUtils.concatFilePath(getStorageBaseDirectory(tenantCode, ResourceType.FILE), fileName);
    }

    protected void exceptionIfPathEmpty(String resourceAbsolutePath) {
        if (StringUtils.isEmpty(resourceAbsolutePath)) {
            throw new IllegalArgumentException("Resource path should not be empty");
        }
    }

    protected void exceptionIfPathNotUnderStorageBaseDir(String resourceAbsolutePath) {
        String storageBaseDirectory = getStorageBaseDirectory();
        if (!resourceAbsolutePath.startsWith(storageBaseDirectory)) {
            throw new IllegalArgumentException(
                    "Resource path: " + resourceAbsolutePath + " is not under storage base directory: "
                            + storageBaseDirectory);
        }
    }
}
