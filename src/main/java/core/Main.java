package core;

import depModel.Dependency;

import java.util.List;

public class Main {
    // 程序入口
    public static void main(String[] args) {
        Init init = new Init();
        if(args.length != 0) {
            String str = args[0]; //执行jar包命令 传入参数
            init.inputPath(str);
        }
        if(args.length != 0) {
            String str = args[0]; //执行jar包命令 传入参数
            init.inputPath(str);
        }
        else {
            // 输入项目路径
            init.inputPath();
        }
        // 创建单模块解析
        SingleModule singleModule = new SingleModule(init.getFilePath());
        try {
            singleModule.parsePom();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
//        singleModule.setDependencySet(list1);
        // TODO: 2023/10/19 先对原项目 进行冲突判断
        boolean isConflictBefore = singleModule.conflictDetectBefore();
        if (!isConflictBefore) {
            //如果原项目没有冲突，加入无冲突集合
            System.out.println("原项目无冲突");
            singleModule.getResWithoutConflict().add(singleModule.getDependencySet());
        }

        // 如果没有冲突，可以告诉用户没有冲突，但可以给出升级的无冲突版本
        // 有冲突，推荐无冲突版本
        singleModule.singleModuleUpgrade(singleModule.getDependencySet());
        // 对升级后的结果集进行有无冲突的判断
        singleModule.conflictDetectAfter();

        // 如果没有无冲突的升级版本
        if (singleModule.getResWithoutConflict().size() != 0) {
            System.out.println("以下是推荐版本");
            singleModule.setRecommendDepSet(singleModule.getResWithoutConflict());
        } else {
            System.out.println("以下是调解后的版本");
        }

        int id = 0;
        for(List<Dependency> dplist : singleModule.getRecommendDepSet()) {
            System.out.println(id ++);
            for(Dependency d: dplist) {
                d.printDependency();
            }
        }

    }
}
