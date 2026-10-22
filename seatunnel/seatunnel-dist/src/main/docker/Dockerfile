# 使用基础镜像：JDK 8
FROM openjdk:8-jdk

# 设置环境变量
ENV SEATUNNEL_VERSION=2.3.10
ENV SEATUNNEL_HOME=/opt/seatunnel
ENV PATH=$SEATUNNEL_HOME/bin:$PATH

WORKDIR $SEATUNNEL_HOME

ADD ./bin $SEATUNNEL_HOME/bin
ADD ./lib $SEATUNNEL_HOME/lib
ADD ./connectors $SEATUNNEL_HOME/connectors
ADD ./plugins $SEATUNNEL_HOME/plugins
ADD ./config $SEATUNNEL_HOME/config
ADD ./licenses $SEATUNNEL_HOME/licenses
ADD ./starter $SEATUNNEL_HOME/starter

RUN mkdir -p $SEATUNNEL_HOME/logs/
# 暴露 Hazelcast 端口和 HTTP 接口端口
EXPOSE 5801 8080

CMD ["/bin/bash", "-c", "./bin/seatunnel-cluster.sh -r $SEATUNNEL_ROLE"]
