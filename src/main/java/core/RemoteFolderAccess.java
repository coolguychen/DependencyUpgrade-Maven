package core;

import java.io.*;
import java.net.*;

public class RemoteFolderAccess {
    public static void main(String[] args) {
        String remoteServerIP = "124.220.14.106";
        String remoteFolderPath = "/mavenRepo";
        
        try {
            // 创建 URL 对象
            URL url = new URL("ftp://" + remoteServerIP + remoteFolderPath);
            
            // 打开连接
            URLConnection connection = url.openConnection();
            
            // 读取远程文件夹中的内容
            InputStream inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            
            // 关闭流
            reader.close();
            
            System.out.println("远程文件夹内容读取完成！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}