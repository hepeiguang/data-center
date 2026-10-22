#!/bin/bash

# ============================================
# 多目标JAR包部署脚本
# 使用方法: ./deploy_jars.sh <jar_file> <target1> [target2] [target3] ...
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
  echo "使用方法: $0 <jar_file> <target1> [target2] [target3] ..."
  echo ""
  echo "参数说明:"
  echo "  jar_file  : 要部署的JAR文件路径"
  echo "  target    : 目标路径，可以是:"
  echo "        1. 本地目录 (如: /opt/apps)"
  echo "        2. 远程主机 (如: user@host:/remote/path)"
  echo "        3. 多个远程主机"
  echo ""
  echo "示例:"
  echo "  # 部署到本地和两个远程主机"
  echo "  $0 app.jar /opt/apps root@node1:/apps root@node2:/apps"
  echo ""
  echo "  # 部署到多个远程主机"
  echo "  $0 app.jar root@192.168.1.10:/apps root@192.168.1.11:/apps"
  echo ""
  echo "  # 使用配置文件中的主机列表"
  echo "  $0 app.jar -c cluster.conf"
  exit 1
}

# 检查参数
if [ $# -lt 2 ]; then
  log_error "参数不足!"
  usage
fi

source ${workDir}/env/install_env.sh

HOSTS=(${ips//,/ })
BASE_DIR="/services/etl"

# 获取是否重启参数
RESTART_SERVICE="$1"
# 验证重启参数
if [ "$RESTART_SERVICE" != "true" ] && [ "$RESTART_SERVICE" != "false" ]; then
  log_error "重启参数必须为 'true' 或 'false'"
  usage
fi

# 获取JAR文件
JAR_FILE="$2"
shift 2  # 移除前两个参数
JAR_FILE=$installPath/$JAR_FILE
# 检查JAR文件是否存在
if [ ! -f "$JAR_FILE" ]; then
  log_error "JAR文件不存在: $JAR_FILE"
  exit 1
fi

# 获取文件名
FILENAME=$(basename "$JAR_FILE")
log_info "开始部署: $FILENAME"
# 处理所有目标路径
TARGETS=("$@")
log_info "目标路径数量: ${#TARGETS[@]}"

# 备份文件
backupPath=$installPath/backup/$(date '+%Y%m%d%H%M')
mkdir -p $backupPath
cp -rf $installPath/${TARGETS[0]}/libs/$FILENAME $backupPath

SUCCESS="true"
# 遍历所有目标路径
for TARGET in "${TARGETS[@]}"; do
  echo ""
  log_info "处理目标: $TARGET"
  for HOST in "${HOSTS[@]}"; do
    # 使用scp拷贝文件
    log_info "正在拷贝到远程主机..."
    scp "$JAR_FILE" "$HOST:$installPath/$TARGET/libs/"
    if [ $? -eq 0 ]; then
      log_info "✓ 成功部署到 $HOST:$installPath/$TARGET/libs/"
    else
	  SUCCESS="false"
      log_error "✗ 部署到 $HOST 失败"
    fi
  done
done

rm -rf $installPath/$FILENAME
echo "Delete file： $installPath/$FILENAME"

# 根据参数决定是否重启服务
if [ "$SUCCESS" = "true" ] && [ "$RESTART_SERVICE" = "true" ]; then
  echo ""
  log_info "所有部署成功，开始重启服务..."
  
  cd $installPath
  ./bin/stop-all.sh
  ./bin/start-all.sh
  
  log_info "✓ 服务重启完成"
elif [ "$SUCCESS" = "true" ] && [ "$RESTART_SERVICE" = "false" ]; then
  echo ""
  log_info "所有部署成功，但重启服务设置为 false，跳过重启"
elif [ "$SUCCESS" = "false" ]; then
  echo ""
  log_warn "部署失败，跳过服务重启"
fi

echo ""
log_info "部署完成!"
log_info "已部署文件: $FILENAME"
log_info "部署目标数: ${#TARGETS[@]}"

