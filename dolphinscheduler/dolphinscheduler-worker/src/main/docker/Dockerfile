#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#FROM openjdk:8-jdk
FROM registry.cn-beijing.aliyuncs.com/htzhpublic/openjdk:8-arm-sudo

ENV DOCKER true
ENV TZ Asia/Shanghai
ENV DOLPHINSCHEDULER_HOME /opt/dolphinscheduler/worker-server

RUN chmod 1777 /tmp ; \
    sed -i 's|deb.debian.org|mirrors.tuna.tsinghua.edu.cn|g' /etc/apt/sources.list ; \
    sed -i 's|security.debian.org|mirrors.tuna.tsinghua.edu.cn/debian-security|g' /etc/apt/sources.list ; \
    apt update ; \
    apt install -y --no-install-recommends sudo ; \
    rm -rf /var/lib/apt/lists/*

# 确保 java 命令全局可用
RUN ln -s ${JAVA_HOME}/bin/java /usr/bin/java ; \
    ln -s ${JAVA_HOME}/bin/javac /usr/bin/javac

WORKDIR $DOLPHINSCHEDULER_HOME

ADD ./worker-server $DOLPHINSCHEDULER_HOME
COPY ./bin/env/dolphinscheduler_env.sh $DOLPHINSCHEDULER_HOME/conf/dolphinscheduler_env.sh

EXPOSE 1235

CMD [ "/bin/bash", "./bin/start.sh" ]
