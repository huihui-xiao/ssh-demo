package com.example.sshdemo.common.util;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * date: 2022/1/15 17:25 <br>
 * author: 86130 <br>
 * version: 1.0 <br>
 */
@Slf4j
public class SFTPUtil {

    /**
     * host
     */
    private String host;

    /**
     * 端口
     */
    private Integer port;

    /**
     * 账号
     */
    private String username;

    /**
     * 密码
     */
    private String password;


    private Session session;

    private ChannelSftp sftp;

    private SFTPUtil sftpUtil;

    public SFTPUtil(){}

    public SFTPUtil(String host,Integer port,String username,String password){
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    /**
     * 上传文件
     * @param sftpFileName 文件名称
     * @param input
     * @return
     */
    public boolean upload(String sftpFileName, InputStream input){
        try {
            sftp.put(input, sftpFileName);
            log.info("上传成功：{}",true);
            return Boolean.TRUE;
        } catch (SftpException e) {
            log.error("上传失败:{}",e);
            closeConnect();
        }
        return Boolean.FALSE;
    }

    /**
     * 删除文件
     * @param directory 要删除文件所在目录
     * @param deleteFile 要删除的文件
     */
    public void delete(String directory, String deleteFile){
        try {
            sftp.cd(directory);
            sftp.rm(deleteFile);
        } catch (SftpException e) {
            log.error("文件删除失败：{}",e);
            closeConnect();
        }
    }

    /**
     * 查看当前路径
     * @return String
     */
    public String pwd(){
        try {
            return sftp.pwd();
        } catch (SftpException e) {
            log.error("查看文件路径失败：{}",e);
        }
        return null;
    }

    public String inDirectory(String directory){
        try {
            sftp.cd(directory);
            return sftp.pwd();
        } catch (SftpException e) {
            log.error("查看文件路径失败：{}",e);
        }
        return null;
    }

    /**
     * 列出目录下的文件
     * @param directory 要列出的目录
     */
    public List<String> listFiles(String directory) {
        List<String> list = new ArrayList<>();
        try {
            Vector ls = sftp.ls(directory);
            for (Object obj : ls){
                if(obj instanceof com.jcraft.jsch.ChannelSftp.LsEntry){
                    ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) obj;
                    if(!entry.getAttrs().isDir()){
                        list.add(entry.getFilename());
                    }
                }
            }
            return list;
        } catch (SftpException e) {
            log.error("查看当前目录文件：{}",e);
            closeConnect();
        }
        return null;
    }

    /**
     * 更改文件名
     *
     * @param directory 文件所在目录
     * @param oldFileNm 原文件名
     * @param newFileNm 新文件名
     * @throws Exception

     */
    public void rename(String directory, String oldFileNm, String newFileNm){
        try {
            sftp.cd(directory);
            sftp.rename(oldFileNm, newFileNm);
            log.info("重命名成功 ok");
        } catch (SftpException e) {
            log.error("重命名文件失败：{}",e);
            closeConnect();
        }

    }

    /**
     * 检查目录是否存在
     * @param basePath  服务器的基础路径
     * @param directory  上传到该目录
     */
    public void checkDirectoryExists(String basePath,String directory){
        try {
            sftp.cd(basePath);
            sftp.cd(directory);
        } catch (SftpException e) {
            //目录不存在，则创建文件夹
            String [] dirs = directory.split("/");
            String tempPath = basePath;
            for(String dir:dirs){
                if(null== dir || "".equals(dir)) continue;
                tempPath += "/" + dir;
                try{
                    sftp.cd(tempPath);
                }catch(SftpException ex){
                    log.info("目录不存在，开始创建目录：{}",tempPath);
                    try {
                        sftp.mkdir(tempPath);
                        sftp.cd(tempPath);
                        log.info("目录创建成功：{}",tempPath);
                    } catch (SftpException e1) {
                        log.error("创建目录异常：{}",e1);
                        closeConnect();
                    }
                }
            }
        }
    }

    /**
     * 执行相关的命令
     *
     * @param command
     * @throws IOException
     */
    public void execCommand(String command) {
        InputStream in = null;// 输入流(读)
        Channel channel = null;// 定义channel变量
        try {
            // 如果命令command不等于null
            if (command != null) {
                // 打开channel
                //说明：exec用于执行命令;sftp用于文件处理
                channel = session.openChannel("exec");
                // 设置command
                ((ChannelExec) channel).setCommand(command);
                // channel进行连接
                channel.connect();
                // 获取到输入流
                in = channel.getInputStream();
                // 执行相关的命令
                String processDataStream = processDataStream(in);
                // 打印相关的命令
                log.info("1、打印相关返回的命令: " + processDataStream);
            }
        } catch (Exception e) {
            log.info("操作失败：{}",e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.info("文件流关闭失败：{}",e);
                }
            }
            if (channel != null) {
                channel.disconnect();
            }
        }
    }


    /**
     * 对将要执行的linux的命令进行遍历
     *
     * @param in
     * @return
     * @throws Exception
     */
    public String processDataStream(InputStream in) throws Exception {
        StringBuffer sb = new StringBuffer();
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String result = "";
        try {
            while ((result = br.readLine()) != null) {
                sb.append(result);
                // System.out.println(sb.toString());
            }
        } catch (Exception e) {
            log.error("获取数据流失败:{}",e);
        } finally {
            br.close();
        }
        return sb.toString();
    }

    public void SSHConnect() {
        try {
            // 创建JSch对象
            JSch jsch = new JSch();
            // 根据用户名，主机ip，端口获取一个Session对象
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            Properties configTemp = new Properties();
            configTemp.put("StrictHostKeyChecking", "no");
            // 为Session对象设置properties
            session.setConfig(configTemp);
            // 设置timeout时间
            session.setTimeout(60000);
            session.connect();
        }catch (JSchException e){
            log.error("连接SSH异常:{}",e);
        }
    }

    public void SFTPConnect(){
        SSHConnect();
        // 通过Session建立链接
        // 打开SFTP通道
        Channel channel = null;
        try {
            channel = session.openChannel("sftp");
            channel.connect();

            sftp = (ChannelSftp) channel;
        } catch (JSchException e) {
            log.error("连接SFTP异常:{}",e);
        }

    }



    /**
     * 断开SFTP Channel、Session连接
     */
    public void closeConnect() {
        try {
            if (sftp != null) {
                sftp.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        } catch (Exception e) {
            log.error("sftp close error:{}", e);
        }
    }

}
