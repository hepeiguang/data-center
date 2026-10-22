#!/bin/bash
# 安装目录
DS_HOME=$(dirname "$(dirname "$(dirname "$(realpath "$0")")")")

# 加载 .env 文件
set -a
source "${DS_HOME}/deploy/docker/.env"
set +a

# 检查是否传入了参数
if [ -z "$1" ]; then
  echo "请传入服务名参数，例如: $0 api-server 或 $0 all"
  exit 1
fi

SERVICE_NAME=$1

# 支持的服务列表
SUPPORTED_SERVICES=("api-server" "master-server" "worker-server" "alert-server")

if [ "$SERVICE_NAME" == "all" ]; then
  # 构建所有服务
  for image in "${SUPPORTED_SERVICES[@]}"; do
    echo "正在构建镜像：$image"
    docker build -t ds/$image:$TAG -f ${DS_HOME}/deploy/dockerfile/$image/Dockerfile .
    #  docker tag ds-${image}:$TAG ${HUB}/dolphinscheduler/ds-${image}:$TAG
    #  docker push ${HUB}/dolphinscheduler/ds-${image}:$TAG
  done
elif [[ " ${SUPPORTED_SERVICES[*]} " == *" $SERVICE_NAME "* ]]; then
  # 构建指定服务
  echo "正在构建镜像：$SERVICE_NAME"
  docker build -t ds/$SERVICE_NAME:$TAG -f ${DS_HOME}/deploy/dockerfile/$SERVICE_NAME/Dockerfile .
  #  docker tag ds-${image}:$TAG ${HUB}/dolphinscheduler/ds-${image}:$TAG
  #  docker push ${HUB}/dolphinscheduler/ds-${image}:$TAG
else
  # 参数错误
  echo "错误：'$SERVICE_NAME' 不是一个受支持的服务名。可选值: ${SUPPORTED_SERVICES[*]} 或 all"
  exit 1
fi
