package core;

import depModel.Dependency;

import java.util.List;

public class Main {
    // 程序入口
    public static void main(String[] args) {
        Init init = new Init();
        if(args.length == 2) {
            String str = args[0]; //执行jar包命令 传入参数
            String type = args[1];
            init.inputPath(str);
            init.inputType(Integer.parseInt(type));
        }
        else {
            // 输入项目路径
            init.inputPath();
            init.inputType();
        }
        // 创建单模块解析
        SingleModule singleModule = new SingleModule(init.getFilePath());
        try {
            singleModule.getSingleUpgradeSolutions(init.getFilePath(), init.getType());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
