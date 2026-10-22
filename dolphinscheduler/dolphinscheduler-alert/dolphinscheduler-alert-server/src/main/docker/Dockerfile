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
ENV DOLPHINSCHEDULER_HOME /opt/dolphinscheduler/alert-server

WORKDIR $DOLPHINSCHEDULER_HOME

ADD ./alert-server $DOLPHINSCHEDULER_HOME
COPY ./bin/env/dolphinscheduler_env.sh $DOLPHINSCHEDULER_HOME/conf/dolphinscheduler_env.sh

EXPOSE 50052 50053

CMD [ "/bin/bash", "./bin/start.sh" ]
