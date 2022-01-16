package com.example.sshdemo.service;

import com.example.sshdemo.common.util.SFTPUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * description: SFTPService <br>
 * date: 2022/1/15 17:52 <br>
 * author: 86130 <br>
 * version: 1.0 <br>
 */
public class SFTPService {

    public static void main(String[] args) throws Exception {
        SFTPUtil sftp = new SFTPUtil("192.168.59.154",22,"root","root");
        sftp.SFTPConnect();
        InputStream input = new FileInputStream(new File("C:\\Users\\86130\\Desktop\\test.jpg"));
        sftp.checkDirectoryExists("/usr/local","app/");
        sftp.upload("test1.jpg",input);
        System.out.println(sftp.listFiles("/usr/local/app"));
        sftp.rename("/usr/local/app","test1.jpg","test2.jpg");
        String command = "rm -rf /app/";
        sftp.execCommand(command);
        sftp.closeConnect();
    }
}
