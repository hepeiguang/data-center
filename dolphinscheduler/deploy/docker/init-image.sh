#!/bin/bash
# 获取DAM的根目录
DS_HOME=$(dirname "$(dirname "$(dirname "$(realpath "$0")")")")
cd ${DS_HOME}


# 检查并加载.env文件
if [ -f "${DS_HOME}/deploy/docker/.env" ]; then
    echo "Loading environment variables from .env file..."
    source "${DS_HOME}/deploy/docker/.env"
else
    echo "Warning: .env file not found in ${DS_HOME}"
fi

IMAGE_NAME=${IMAGE_NAME:-"zdkj/dam"}
IMAGE_TAG=${TAG:-"latest"}
ENABLE_PUSH_IMAGE=${ENABLE_PUSH_IMAGE:-"false"}
HUB=${HUB:-""}

# 检查是否需要推送镜像
if [ "${ENABLE_PUSH_IMAGE}" ]; then
    echo "Build image to local"
    docker build -t ${HUB}/zdkj/api-server:${IMAGE_TAG} -f ${DS_HOME}/deploy/dockerfile/api-server/Dockerfile .
    docker build -t ${HUB}/zdkj/master-server:${IMAGE_TAG} -f ${DS_HOME}/deploy/dockerfile/master-server/Dockerfile .
    docker build -t ${HUB}/zdkj/worker-server:${IMAGE_TAG} -f ${DS_HOME}/deploy/dockerfile/worker-server/Dockerfile .
    docker build -t ${HUB}/zdkj/alert-server:${IMAGE_TAG} -f ${DS_HOME}/deploy/dockerfile/alert-server/Dockerfile .

    echo "Pushing image to registry..."
    docker push ${HUB}/zdkj/api-server:${IMAGE_TAG}
    docker push ${HUB}/zdkj/master-server:${IMAGE_TAG}
    docker push ${HUB}/zdkj/worker-server:${IMAGE_TAG}
    docker push ${HUB}/zdkj/alert-server:${IMAGE_TAG}

    if [ $? -eq 0 ]; then
        echo "Image pushed successfully."
    else
        echo "Error: Failed to push image."
        exit 1
    fi
else
    echo "Build image to local"
    docker build -t zdkj/api-server:${IMAGE_TAG} -f ${DS_HOME}/deploy/dockerfile/api-server/Dockerfile .
    docker build -t zdkj/master-server:${IMAGE_TAG} -f ${DS_HOME}/deploy/dockerfile/master-server/Dockerfile .
    docker build -t zdkj/worker-server:${IMAGE_TAG} -f ${DS_HOME}/deploy/dockerfile/worker-server/Dockerfile .
    docker build -t zdkj/alert-server:${IMAGE_TAG} -f ${DS_HOME}/deploy/dockerfile/alert-server/Dockerfile .

    echo "ENABLE_PUSH_IMAGE is not true, skipping image push."
fi
