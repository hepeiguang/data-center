/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.plugin.task.extract;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.AbstractTask;
import org.apache.dolphinscheduler.plugin.task.api.TaskCallBack;
import org.apache.dolphinscheduler.plugin.task.api.TaskException;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.api.enums.ResourceType;
import org.apache.dolphinscheduler.plugin.task.api.parameters.AbstractParameters;
import org.apache.dolphinscheduler.plugin.task.api.parameters.resource.DataSourceParameters;
import org.apache.dolphinscheduler.plugin.task.extract.dto.ConnectParamDTO;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.EXIT_CODE_FAILURE;
import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.EXIT_CODE_SUCCESS;

@Slf4j
public class ExtractTask extends AbstractTask {
    /**
     * shell parameters
     */
    private ExtractParameters extractParameters;
    /**
     * taskExecutionContext
     */
    protected TaskExecutionContext taskExecutionContext;

    private Session session;

    /**
     * constructor
     *
     * @param taskExecutionContext taskExecutionContext
     */
    public ExtractTask(TaskExecutionContext taskExecutionContext) {
        super(taskExecutionContext);
        this.taskExecutionContext = taskExecutionContext;
    }

    @Override
    public void init() {
        extractParameters = JSONUtils.parseObject(taskExecutionContext.getTaskParams(), ExtractParameters.class);
        log.info("Init Extract task params {}", JSONUtils.toPrettyJsonString(extractParameters));
        if (extractParameters == null || !extractParameters.checkParameters()) {
            throw new TaskException("Extract task params is not valid");
        }
    }

    @Override
    public void handle(TaskCallBack taskCallBack) throws TaskException {
        try {
            DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                    .getResourceParameters(ResourceType.DATASOURCE, extractParameters.getSourceId());
            log.info("source jdbc datasource:{}", JSONUtils.toJsonString(dbSource));
            String connectionParams = dbSource.getConnectionParams();
            ConnectParamDTO connectParamDTO = JSONUtils.parseObject(connectionParams, ConnectParamDTO.class);
            String host = connectParamDTO.getHost();
            // 连接服务器
            connect(connectParamDTO);
            log.info("start connect to server host:{}: ", host);
            // 解压文件或目录
            decompress(extractParameters.getSourceFile(), extractParameters.getTargetDir());
            setExitStatusCode(EXIT_CODE_SUCCESS);
        } catch (Exception e) {
            log.error("Extract task error", e);
            setExitStatusCode(EXIT_CODE_FAILURE);
            throw new TaskException("Execute Extract task failed", e);
        } finally {
            // 断开连接
            disconnect();
        }
    }

    /**
     * 连接到服务器
     *
     * @param connectParamDTO 连接信息
     * @throws JSchException 连接异常
     */
    public void connect(ConnectParamDTO connectParamDTO) throws JSchException {
        log.info("start connect to server host:{}: ", connectParamDTO.getHost());
        if (StringUtils.isEmpty(connectParamDTO.getHost()) || StringUtils.isEmpty(connectParamDTO.getPort()) || StringUtils.isEmpty(connectParamDTO.getUser())) {
            log.error("Incomplete parameters");
            throw new JSchException("Incomplete parameters ");
        }

        JSch jsch = new JSch();

//        // 1. 配置JSch重试机制
//        JSch.setConfig("MaxAuthTries", "3");
//        JSch.setConfig("ConnectionAttempts", "3");

        session = jsch.getSession(connectParamDTO.getUser(), connectParamDTO.getHost(), Integer.parseInt(connectParamDTO.getPort()));
        // 1. 优先设置密码认证
        if (StringUtils.isNotBlank(connectParamDTO.getPassword())) {
            session.setPassword(connectParamDTO.getPassword());
            log.debug("Password authentication configured");
        }

        // 2. 添加公钥认证（如果有提供私钥路径）
        if (StringUtils.isNotEmpty(connectParamDTO.getPublicKey())) {
            byte[] privateKeyBytes = connectParamDTO.getPublicKey().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            jsch.addIdentity("identity-from-string", privateKeyBytes, null, null); // 第四个参数为 passphrase，如果私钥没有加密则传null
        }
        session.setConfig("StrictHostKeyChecking", "no");
        session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");

//        // 3. 设置超时和重试参数
//        session.setTimeout(30000); // 30秒超时
//        session.setServerAliveInterval(60000); // 60秒发送keepalive
        session.connect();
        log.info("connect to server host:{}: ", connectParamDTO.getHost());
    }

    /**
     * 断开服务器连接
     */
    public void disconnect() {
        if (session != null && session.isConnected()) {
            session.disconnect();
            log.info("disconnect server");
        }
    }

    /**
     * 解压文件或文件夹
     *
     * @param sourcePath     源路径
     * @param destinationDir 目标目录
     * @throws Exception 解压异常
     */
    public void decompress(String sourcePath, String destinationDir) throws Exception {
        log.info("start to decompress");
        ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
        sftpChannel.connect();

        try {
            // 确保目标目录存在
            createRemoteDirectory(sftpChannel, destinationDir);

            SftpATTRS attrs = sftpChannel.lstat(sourcePath);
            if (attrs.isDir()) {
                log.info("source path is dir");
                decompressDirectory(sftpChannel, sourcePath, destinationDir);
            } else {
                log.info("source path is file");
                decompressSingleFile(sftpChannel, sourcePath, destinationDir);
            }
        } finally {
            sftpChannel.disconnect();
        }
    }

    /**
     * 在远程服务器上创建目录
     *
     * @param sftpChannel SFTP通道
     * @param dirPath     目录路径
     * @throws SftpException SFTP异常
     */
    private void createRemoteDirectory(ChannelSftp sftpChannel, String dirPath) throws SftpException {
        try {
            sftpChannel.stat(dirPath);
        } catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                String[] folders = dirPath.split("/");
                String path = "";
                for (String folder : folders) {
                    if (folder.isEmpty()) continue;
                    path += "/" + folder;
                    try {
                        sftpChannel.stat(path);
                    } catch (SftpException ex) {
                        if (ex.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                            log.info("create dir path: {} ", path);
                            sftpChannel.mkdir(path);
                        }
                    }
                }
            }
        }
    }

    /**
     * 解压目录下的所有文件
     *
     * @param sftpChannel    SFTP通道
     * @param directoryPath  目录路径
     * @param destinationDir 目标目录
     * @throws Exception 解压异常
     */
    private void decompressDirectory(ChannelSftp sftpChannel, String directoryPath, String destinationDir) throws Exception {
        Vector<ChannelSftp.LsEntry> files = sftpChannel.ls(directoryPath);
        List<String> filesToDecompress = new ArrayList<>();

        // 收集所有需要解压的文件
        for (ChannelSftp.LsEntry entry : files) {
            String filename = entry.getFilename();
            if (!filename.equals(".") && !filename.equals("..")) {
                String fullPath = directoryPath + "/" + filename;
                if (!entry.getAttrs().isDir()) {
                    filesToDecompress.add(fullPath);
                }
            }
        }

        // 解压所有文件
        for (String filePath : filesToDecompress) {
            decompressSingleFile(sftpChannel, filePath, destinationDir);
        }
    }

    /**
     * 解压单个文件
     *
     * @param sftpChannel    SFTP通道
     * @param filePath       文件路径
     * @param destinationDir 目标目录
     * @throws Exception 解压异常
     */
    private void decompressSingleFile(ChannelSftp sftpChannel, String filePath, String destinationDir) throws Exception {
        String filename = new File(filePath).getName();
        log.info("dealing file: {}", filename);

        // 根据文件扩展名判断解压方式
        if (filename.endsWith(".zip")) {
            executeRemoteCommand("unzip -o \"" + filePath + "\" -d \"" + destinationDir + "\"");
        } else if (filename.endsWith(".tar.gz") || filename.endsWith(".tgz")) {
            executeRemoteCommand("tar -xzf \"" + filePath + "\" -C \"" + destinationDir + "\"");
        } else if (filename.endsWith(".tar")) {
            executeRemoteCommand("tar -xf \"" + filePath + "\" -C \"" + destinationDir + "\"");
        } else if (filename.endsWith(".gz") && !filename.endsWith(".tar.gz")) {
            String outputFilename = filename.replace(".gz", "");
            executeRemoteCommand("gzip -dc \"" + filePath + "\" > \"" + destinationDir + "/" + outputFilename + "\"");
        } else {
            log.info("not found file Type: " + filename);
        }
    }

    /**
     * 在远程服务器上执行命令
     *
     * @param command 要执行的命令
     * @throws Exception 执行异常
     */
    private void executeRemoteCommand(String command) throws Exception {
        log.info("executeRemoteCommand: {}", command);
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        channel.setInputStream(null);
        channel.setErrStream(System.err);

        try (InputStream in = channel.getInputStream();
             InputStream err = channel.getErrStream()) {
            channel.connect();

            // 读取命令输出
            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    log.info(new String(tmp, 0, i));
                }
                while (err.available() > 0) {
                    int i = err.read(tmp, 0, 1024);
                    if (i < 0) break;
                    log.info(new String(tmp, 0, i));
                }
                if (channel.isClosed()) {
                    if (in.available() > 0 || err.available() > 0) continue;
                    log.info("close status : " + channel.getExitStatus());
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ee) {
                    ee.printStackTrace();
                }
            }
        } finally {
            channel.disconnect();
        }
    }

    @Override
    public void cancel() throws TaskException {
        // cancel process
    }

    @Override
    public AbstractParameters getParameters() {
        return extractParameters;
    }
}
