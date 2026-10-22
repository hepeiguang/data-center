#!/bin/bash
# 获取seatunnel的根目录
SEATUNNEL_HOME=$(dirname "$(dirname "$(dirname "$(realpath "$0")")")")
cd ${SEATUNNEL_HOME}

# 检查并加载.env文件
if [ -f "${SEATUNNEL_HOME}/deploy/docker/.env" ]; then
    echo "Loading environment variables from .env file..."
    source "${SEATUNNEL_HOME}/deploy/docker/.env"
else
    echo "Warning: .env file not found in ${SEATUNNEL_HOME}"
fi

IMAGE_NAME=${IMAGE_NAME:-"zdkj/seatunnel"}
IMAGE_TAG=${TAG:-"latest"}
ENABLE_PUSH_IMAGE=${ENABLE_PUSH_IMAGE:-"false"}
HUB=${HUB:-""}

# 检查是否需要推送镜像
if [ "${ENABLE_PUSH_IMAGE}" ]; then
    FULL_IMAGE_NAME="${HUB}/${IMAGE_NAME}:${IMAGE_TAG}"
    docker build -t ${FULL_IMAGE_NAME} -f ${SEATUNNEL_HOME}/deploy/docker/Dockerfile .

    echo "Pushing image to registry..."
    docker push ${FULL_IMAGE_NAME}

    if [ $? -eq 0 ]; then
        echo "Image pushed successfully."
    else
        echo "Error: Failed to push image."
        exit 1
    fi
else
    FULL_IMAGE_NAME="${IMAGE_NAME}:${IMAGE_TAG}"
    docker build -t ${FULL_IMAGE_NAME} -f ${SEATUNNEL_HOME}/deploy/docker/Dockerfile .
    echo "ENABLE_PUSH_IMAGE is not true, skipping image push."
fi
