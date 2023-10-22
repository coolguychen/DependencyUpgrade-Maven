package core;

import java.io.BufferedReader;
import java.io.File;
import java.util.Scanner;

/**
 * 初始化类
 */
public class Init {
    // 文件路径
    private String filePath;
    private int type;
    private BufferedReader result;

    public void inputPath(){
        Scanner scanner = new Scanner(System.in);
        System.out.println("请输入项目根目录:");
        while (scanner.hasNextLine()) {
            filePath = scanner.nextLine();
//            System.out.println(filePath);
            if(!filePath.equals(""))
                break;
        }
        File file = new File(filePath + "/pom.xml");
        if(!file.exists()){
            System.out.println("该路径下无pom.xml， 重试");
            inputPath();
        }
    }

    public void inputType() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("请输入推荐类型：");
        type = scanner.nextInt();
        if(type != 0 && type != 1 && type !=2) {
            System.out.println("输入错误类型！");
            inputType();
        }
    }

    public void inputPath(String path){
        this.filePath = path;
        File file = new File(path + "/pom.xml");
        if(!file.exists()){
            System.out.println("该路径下无pom.xml");
            inputPath();
        }
    }

    public void inputType(int type) {
        this.type = type;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getType() {
        return type;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setResult(BufferedReader result) {
        this.result = result;
    }

    public BufferedReader getResult() {
        return result;
    }
}
