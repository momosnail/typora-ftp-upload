package wgl.typora.ftp;
import wgl.typora.ftp.utils.FTPUtils;
import java.io.File;

public class Main {
    public static void main(String[] args) {
      /*  if (!checkArgs(args)) {
            return;
        }*/
//        for (String arg : args) {
//           String arg="C:\\Users\\55021\\Desktop\\b.jpg";
           String arg="/home/wgl/图片/截图/1.png";
            String hostImageUrl = FTPUtils.uploadFile(arg);
            System.out.println(hostImageUrl);
//        }
    }

    /**
     * 判断文件是否存在
     *
     * @param args
     * @return
     */
    public static Boolean checkArgs(String[] args) {
        boolean checkResult = true;
        if (args.length == 0) {
            System.out.println("输入参数不能为空");
            checkResult = false;
        }
        for (String filePath : args) {
            if (!new File(filePath).exists()) {
                System.out.println(filePath + " 文件不存在");
                checkResult = false;
            }
        }
        return checkResult;
    }
}
