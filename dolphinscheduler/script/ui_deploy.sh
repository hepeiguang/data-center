#!/bin/bash

# ============================================
# 多目标JAR包部署脚本
# 使用方法: ./deploy_jars.sh <UI_FILE> <target1> [target2] [target3] ...
# ============================================

set -e

workDir=`dirname $0`
workDir=`cd ${workDir};pwd`

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
  echo -e "${GREEN}[INFO]${NC} $1"
}

log_error() {
  echo -e "${RED}[ERROR]${NC} $1"
}

log_warn() {
  echo -e "${YELLOW}[WARN]${NC} $1"
}

log_debug() {
  echo -e "${BLUE}[DEBUG]${NC} $1"
}

# 显示用法
usage() {
  echo "多目标JAR包部署脚本"
  echo "========================================"
  echo "使用方法: $0 <UI_ZIP_FILE> "
  echo ""
  echo "参数说明:"
  echo "  UI_ZIP_FILE  : 要部署的UI ZIP文件全名"
  echo ""
  echo "示例:"
  echo "  # 部署前端包到集群中所有节点"
  echo "  $0 dist.zip"
  echo ""
  echo ""
  exit 1
}

# 检查参数
if [ $# -lt 1 ]; then
  log_error "参数不足!"
  usage
fi

source ${workDir}/env/install_env.sh
HOSTS=(${ips//,/ })

# 获取JAR文件
UI_FILE="$1"
UI_FILE=$installPath/$UI_FILE

# 检查UI文件是否存在
if [ ! -f "$UI_FILE" ]; then
  log_error "UI文件不存在: $UI_FILE"
  exit 1
fi

# 获取文件名
FILENAME=$(basename "$UI_FILE")
log_info "开始部署: $FILENAME"
# 判断是否为压缩文件，如果是则解压
if [[ "$FILENAME" == *.zip ]] || [[ "$FILENAME" == *.tar.gz ]] || [[ "$FILENAME" == *.tgz ]] || [[ "$FILENAME" == *.tar ]]; then
  log_info "检测到压缩文件，正在进行解压..."
  TEMP_DIR="/tmp/ui_extract_$$"

  if [[ "$FILENAME" == *.zip ]]; then
    unzip -q "$UI_FILE" -d "$TEMP_DIR"
  elif [[ "$FILENAME" == *.tar.gz ]] || [[ "$FILENAME" == *.tgz ]]; then
    tar -xzf "$UI_FILE" -C "$TEMP_DIR"
  elif [[ "$FILENAME" == *.tar ]]; then
    tar -xf "$UI_FILE" -C "$TEMP_DIR"
  fi

  # 更新UI_FILE变量指向ui目录
  UI_FILE="$TEMP_DIR/dist"
  log_info "解压完成，使用解压后文件进行部署"
else
  # 判断是否为普通文件夹，如果是则重命名为ui
  if [ -d "$UI_FILE" ]; then
    log_info "检测到文件夹，正在重命名为ui..."
    TEMP_DIR="/tmp/ui_extract_$$"
    UI_DIR="$TEMP_DIR/ui"
    mkdir -p "$TEMP_DIR"
    mv "$UI_FILE" "$UI_DIR"  # 使用mv直接重命名
    UI_FILE="$UI_DIR"
    log_info "文件夹重命名完成，使用ui目录进行部署"
  fi
fi

# 备份文件
backupPath=$installPath/backup/$(date '+%Y%m%d%H%M')
mkdir -p $backupPath
cp -r $installPath/api-server/ui $backupPath/

SUCCESS="true"
# 遍历所有目标路径
for HOST in "${HOSTS[@]}"; do
  # 使用scp拷贝文件
  log_info "正在拷贝到远程主机..."
  ssh $HOST "rm -rf ${installPath}/api-server/ui"
  scp -q -r "$UI_FILE" "$HOST:$installPath/api-server/ui"
  if [ $? -eq 0 ]; then
    log_info "✓ 成功部署到 $HOST:$installPath/api-server/"
  else
  SUCCESS="false"
    log_error "✗ 部署到 $HOST 失败"
  fi
done

rm -rf $installPath/$FILENAME
echo "Delete file： $installPath/$FILENAME"
# 清理临时解压目录（如果存在）
if [[ -n "$TEMP_DIR" ]] && [[ -d "$TEMP_DIR" ]]; then
  rm -rf "$TEMP_DIR"
  log_debug "已清理临时目录: $TEMP_DIR"
fi

echo ""
log_info "部署完成!"
log_info "已部署文件: $FILENAME"
log_info "部署目标数: ${#HOSTS[@]}"

