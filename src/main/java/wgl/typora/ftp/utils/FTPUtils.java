package wgl.typora.ftp.utils;

import cn.hutool.setting.dialect.Props;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Calendar;

public class FTPUtils {
    public static FTPClient ftpClient = null;
    private static Props props = new Props("ftp.properties");
    private static final String host = props.getStr("ftp.url");
    private static final String username = props.getStr("ftp.username");
    private static final String password = props.getStr("ftp.password");
    private static final String port = props.getStr("ftp.port");
    private static final String resultPath = props.getStr("ftp.resPath");
    private static String path = props.getStr("ftp.path");
    private static final String urlPath = props.getStr("ftp.urlPath");
    private static String categoryFolder = "";

    /**
     * 初始化ftp服务器
     */
    public static void initFtpClient(String hostname, String username, String password, String port) {
        ftpClient = new FTPClient();
        ftpClient.setControlEncoding("utf-8");
        try {
            //连接ftp服务器
            ftpClient.connect(hostname, Integer.parseInt(port));
            //登录ftp服务器
            ftpClient.login(username, password);
            //是否成功登录服务器
            int replyCode = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                System.out.println("replyCode:" + replyCode);
                System.out.println("ftp服务器登录失败!");
            } else {
                System.out.println("ftp服务器登录成功！");
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 上传文件
     *
     * @param loadUrl 本地文件路径
     * @return
     */
    public static String uploadFile(String loadUrl) {
        boolean flag = false;
        InputStream inputStream = null;
        String imageName = "";
        try {
            File file = new File(loadUrl);
            imageName = file.getName();
            System.out.println("imageName:" + imageName);
            initFtpClient(host, username, password, port);
            System.out.println("开始上传文件");
            ftpClient.setControlEncoding("utf-8");
            ftpClient.setRemoteVerificationEnabled(false);
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
//            ftpClient.enterLocalActiveMode();
            ftpClient.enterLocalPassiveMode();
            imageName = updatePath(imageName);
            CreateDirecroty(path, imageName);
            ftpClient.makeDirectory(path);
            ftpClient.changeWorkingDirectory(path);
            ftpClient.setBufferSize(1024);

            inputStream = Files.newInputStream(Paths.get(loadUrl));
            ftpClient.storeFile(imageName, inputStream);
            System.out.println("上传结束");
            inputStream.close();
            ftpClient.logout();
            flag = true;
            System.out.println("上传文件成功");

        } catch (Exception e) {
            System.out.println("上传文件失败:" + e.getMessage());
            e.printStackTrace();
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return resultPath + "/" + categoryFolder + imageName;
    }

    /**
     * 更新path并返回imageName
     *
     * @param imageName
     * @return
     */
    private static String updatePath(String imageName) {
        categoryFolder = urlPath + "/";
       /* if (imageName.contains("_")) {
            String[] strs = imageName.split("_");
            for (int i = 0; i < strs.length; i++) {
                if (i != strs.length - 1) {
                    path = path + "/" + strs[i];
                    categoryFolder = categoryFolder + strs[i] + "/";
                }
            }
            return strs[strs.length - 1];
        }
        return imageName;*/

        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = (calendar.get(Calendar.MONTH) + 1) % 12;
        int date = calendar.get(Calendar.DATE);

        String strMonth;
        if (month<10){
             strMonth="0"+month;
        }else {
            strMonth=""+month;
        }

        String strDate;
        if (date<10){
            strDate="0"+date;
        }else {
            strDate=""+date;
        }

        String folder = "" + year + "/" + strMonth + "/" + strDate;
        path = path + "/"+folder;
        categoryFolder = categoryFolder + folder + "/";

        return imageName;
    }

    //改变目录路径
    public static boolean changeWorkingDirectory(String directory) {
        boolean flag = true;
        try {
            flag = ftpClient.changeWorkingDirectory(directory);
            if (flag) {
                System.out.println("进入文件夹" + directory + " 成功！");
            } else {
                System.out.println("进入文件夹" + directory + " 失败！开始创建文件夹");
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return flag;
    }

    //创建多层目录文件，如果有ftp服务器已存在该文件，则不创建，如果无，则创建
    public static boolean CreateDirecroty(String remote, String imageName) throws IOException {
        boolean success = true;
        String directory = remote + "/";

        // 如果远程目录不存在，则递归创建远程服务器目录
        if (!directory.equalsIgnoreCase("/") && !changeWorkingDirectory(directory)) {
            int start = 0;
            int end = 0;
            if (directory.startsWith("/")) {
                start = 1;
            } else {
                start = 0;
            }
            end = directory.indexOf("/", start);
            String path = "";
            StringBuilder paths = new StringBuilder();
            while (true) {
                String subDirectory = new String(remote.substring(start, end).getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
                path = path + "/" + subDirectory;
                if (!existFile(path)) {

                    if (makeDirectory(subDirectory)) {
                        changeWorkingDirectory(subDirectory);
                    } else {
                        System.out.println("创建目录[" + subDirectory + "]失败");
                        changeWorkingDirectory(subDirectory);
                    }
                } else {
                    changeWorkingDirectory(subDirectory);
                }

                paths.append("/").append(subDirectory);
                start = end + 1;
                end = directory.indexOf("/", start);
                // 检查所有目录是否创建完毕
                if (end <= start) {
                    break;
                }
            }
        }
        return success;
    }

    //判断ftp服务器文件是否存在
    public static boolean existFile(String path) throws IOException {
        boolean flag = false;
        ftpClient.enterLocalPassiveMode();
        FTPFile[] ftpFileArr = ftpClient.listFiles(path);
        if (ftpFileArr.length > 0) {
            flag = true;
        }
        return flag;
    }

    //创建目录
    public static boolean makeDirectory(String dir) {
        boolean flag = true;
        try {
            flag = ftpClient.makeDirectory(dir);
            if (flag) {
                System.out.println("创建文件夹" + dir + " 成功！");

            } else {
                System.out.println("创建文件夹" + dir + " 失败！");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    /**
     * 下载文件 *
     *
     * @param pathname  FTP服务器文件目录 *
     * @param filename  文件名称 *
     * @param localpath 下载后的文件路径 *
     * @return
     */
    public static boolean downloadFile(String hostname, String username, String password, String port, String pathname, String filename, String localpath) {
        boolean flag = false;
        OutputStream os = null;
        try {
            System.out.println("开始下载文件");
            initFtpClient(hostname, username, password, port);
            //切换FTP目录
            ftpClient.changeWorkingDirectory(pathname);
            FTPFile[] ftpFiles = ftpClient.listFiles();
            for (FTPFile file : ftpFiles) {
                if (filename.equalsIgnoreCase(file.getName())) {
                    File localFile = new File(localpath + "/" + file.getName());
                    os = new FileOutputStream(localFile);
                    ftpClient.retrieveFile(file.getName(), os);
                    os.close();
                }
            }
            ftpClient.logout();
            flag = true;
            System.out.println("下载文件成功");
        } catch (Exception e) {
            System.out.println("下载文件失败");
            e.printStackTrace();
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != os) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return flag;
    }

    /**
     * 删除文件 *
     *
     * @param pathname FTP服务器保存目录 *
     * @param filename 要删除的文件名称 *
     * @return
     */
    public static boolean deleteFile(String hostname, String username, String password, String port, String pathname, String filename) {
        boolean flag = false;
        try {
            System.out.println("开始删除文件");
            initFtpClient(hostname, username, password, port);
            //切换FTP目录
            ftpClient.changeWorkingDirectory(pathname);
            ftpClient.dele(filename);
            ftpClient.logout();
            flag = true;
            System.out.println("删除文件成功");
        } catch (Exception e) {
            System.out.println("删除文件失败");
            e.printStackTrace();
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return flag;
    }
}