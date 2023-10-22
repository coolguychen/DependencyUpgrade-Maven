package core;

import database.JDBC;
import depModel.Dependency;
import depModel.DependencyTree;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import util.IOUtil;

import java.util.*;

public class SingleModule {
    //单模块处理方案

    //输入的项目路径
    // TODO: 2023/10/21 project path, 远程服务器上的目录 
    private static String projectPath;
    //项目路径下的pom文件路径
    private static String pomPath;
    private static int type;
    JDBC jdbc = new JDBC();

    //构造函数
    SingleModule() {

    }

    SingleModule(String path) {
        projectPath = path;
        pomPath = path + "/pom.xml";
    }

    //解析出来的项目的依赖的集合
    private List<Dependency> dependencySet = new ArrayList<>();

    public List<Dependency> getDependencySet() {
        return dependencySet;
    }

    public void setDependencySet(List<Dependency> dependencySet) {
        this.dependencySet = dependencySet;
    }

    //所有依赖对应的更高版本的集合
    private List<List<Dependency>> higherSet = new ArrayList<>();

    //得到的依赖升级版本的结果集合
    private List<List<Dependency>> resultSet = new ArrayList<>();

    //无冲突的结果集
    private List<List<Dependency>> resWithoutConflict = new ArrayList<>();


    //需要调解/升级的结果集
    private List<DependencyTree> resToMediate = new ArrayList<>();

    private List<DependencyTree> resAfterMediate = new ArrayList<>();

    public List<DependencyTree> getResAfterMediate() {
        return resAfterMediate;
    }

    public void setResAfterMediate(List<DependencyTree> resAfterMediate) {
        this.resAfterMediate = resAfterMediate;
    }

    public List<List<Dependency>> getResWithoutConflict() {
        return resWithoutConflict;
    }

    public void setResWithoutConflict(List<List<Dependency>> resWithoutConflict) {
        this.resWithoutConflict = resWithoutConflict;
    }

    public void addIntoResWithoutConflict(List<Dependency> res) {
        this.resWithoutConflict.add(res);
    }

    //propertyMap
    private static Map<String, String> propertyMap = new HashMap<>();

    public static List<List<Dependency>> getRecommendDepSet() {
        return recommendDepSet;
    }

    public static void setRecommendDepSet(List<List<Dependency>> recommendDepSet) {
        SingleModule.recommendDepSet = recommendDepSet;
    }

    // 推荐的结果
    private static List<List<Dependency>> recommendDepSet = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException {
        type = 0;
        SingleModule singleModule = new SingleModule("D:\\1javawork\\Third Party Libraries\\TestDemo");
        singleModule.parsePom();
//        singleModule.setDependencySet(list1);
        // TODO: 2023/10/19 先对原项目 进行冲突判断
        boolean isConflictBefore = singleModule.conflictDetectBefore();
        if (!isConflictBefore) {
            //如果原项目没有冲突，加入无冲突集合
            System.out.println("原项目无冲突");
            singleModule.resWithoutConflict.add(singleModule.getDependencySet());
        }

        // 如果没有冲突，可以告诉用户没有冲突，但可以给出升级的无冲突版本
        // 有冲突，推荐无冲突版本
        singleModule.singleModuleUpgrade(singleModule.getDependencySet());
        // 对升级后的结果集进行有无冲突的判断
        singleModule.conflictDetectAfter();

        // 如果没有无冲突的升级版本
        if (singleModule.resWithoutConflict.size() != 0) {
            System.out.println("以下是推荐版本");
            recommendDepSet = singleModule.resWithoutConflict;
        } else {
            System.out.println("以下是调解后的版本");
//            recommend ;
//            recommend =
        }

        int id = 0;
        for (List<Dependency> dplist : recommendDepSet) {
            System.out.println(id++);
            for (Dependency d : dplist) {
                d.printDependency();
            }
        }

    }


    /**
     * 通过项目的pom文件得到依赖。
     */
    public void parsePom() throws InterruptedException {
//        dependencySet = new DependencySet();
        System.out.println("解析" + pomPath + "结果中...");
        SAXReader sr = new SAXReader();
        try {
            //pom.xml文件
            Document document = sr.read(projectPath + "/pom.xml");
            Element root = document.getRootElement();
            addPropertyMap(root);
            Element dependencies = root.element("dependencies"); //获取到dependencies的字段
            List<Element> list = dependencies.elements(); //dependencies下的子元素
            for (Element dependency : list) { //循环输出全部dependency的相关信息
                Element e = dependency.element("scope");
                if (e != null) {
                    String scope = dependency.element("scope").getText();
                    if (scope.equals("test") || scope.equals("runtime"))
                        System.out.println("排除范围为" + scope + "的包");
                } else {
                    String groupId = dependency.element("groupId").getText();
//                System.out.println("groupId为：" + groupId);
                    String artifactId = dependency.element("artifactId").getText();
//                System.out.println("artifactId为："+artifactId);
                    // TODO: 4/2/2023 关于${version}的解析
                    Element version_ele = dependency.element("version");
//                System.out.println("版本号为：" + version);
                    if (version_ele != null) {
                        String version = dependency.element("version").getText();
                        if (version.contains("${project.version}")) {
//                                System.out.println("为本地模块，不考虑");
                        } else if (version.contains("$")) {
                            // TODO: 4/2/2023 关于${version}的解析
                            // 获取{}中间的元素，在propertyMap中寻找对应
                            version = version.substring(version.indexOf("{"), version.indexOf("}"));
                            version = propertyMap.get(version);
                            //新建一个Dependency
                            Dependency d = new Dependency(groupId, artifactId, version);
                            //添加到项目依赖列表里面
                            dependencySet.add(d);
                        } else {
                            //加入待升级集合。
                            //新建一个Dependency
                            Dependency d = new Dependency(groupId, artifactId, version);
                            //添加到项目依赖列表里面
                            dependencySet.add(d);
                        }
                    }
                    //版本号为空 默认latest / 父模块管理
                    else {
                        System.out.println("版本号为空。默认最新版本/在父模块进行管理.");
                    }
                }
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    /**
     * 解析properties字段，应对出现引用版本号的情况。
     *
     * @param root
     */
    public void addPropertyMap(Element root) {
        Element properties = root.element("properties"); //获取到properties的字段
        List<Element> list = properties.elements();
        for (Element property : list) {
            String key = property.getName();
            String value = property.getText();
            propertyMap.put(key, value);
        }
    }

    /**
     * @param dependencySet pom文件解析后得到的dependencySet
     */
    public void singleModuleUpgrade(List<Dependency> dependencySet) {
        RecommendSolution solution = new RecommendSolution();
        // TODO: 2023/10/18 根据传入的参数选择如何推荐
        List<List<Dependency>> resultToBeSorted = new ArrayList<>();
        // 遍历pom文件的所有依赖：
        for (Dependency d : dependencySet) {
            String groupId = d.getGroupId();
            String artifactId = d.getArtifactId();
            //在t_lib中找到这个依赖的组合
            List<Dependency> allVersions = jdbc.getLibAllVersions(groupId, artifactId);
//            resultTo.add(allVersions);
            List<Dependency> sortedVersions = new ArrayList<>();
            if (type == 0) {
                // 按照 发行时间排序
                sortedVersions = solution.sortByDate(allVersions);
            } else if (type == 1) {
                // 按照usage
                sortedVersions = solution.sortByUsage(allVersions);
            } else if (type == 2) {
                // 按照vulNum
                sortedVersions = solution.sortByVulNums(allVersions);
            }
            List<Dependency> optimalList = new ArrayList<>();
            // TODO: 2023/10/19 选前两个版本 可以直接将isConflict的libVersion排除掉？
            if (sortedVersions.size() >= 2 && dependencySet.size() < 10) {
                Dependency d1 = sortedVersions.get(0);
                Dependency d2 = sortedVersions.get(1);
                optimalList = Arrays.asList(d1, d2); // 高的两个版本 加入list
            } else if (sortedVersions.size() < 2 || dependencySet.size() > 10) {
                // 如果只有一个版本，或者直接依赖数目过多
                Dependency d1 = sortedVersions.get(0);
                optimalList = Arrays.asList(d1);
            }
            resultToBeSorted.add(optimalList);
        }
        // 对结果集进行笛卡尔积 返回的放在resultSet里面
        descartes(resultToBeSorted, resultSet, 0, new ArrayList<>());
    }


    /**
     * Discription: 笛卡尔乘积算法
     * 把一个List{[1,2],[A,B],[a,b]} 转化成
     * List{[1,A,a],[1,A,b],[1,B,a],[1,B,b],[2,A,a],[2,A,b],[2,B,a],[2,B,b]} 数组输出
     *
     * @param dimensionValue 原List
     * @param result         通过乘积转化后的数组
     * @param layer          中间参数
     * @param currentList    中间参数
     */
    public void descartes(List<List<Dependency>> dimensionValue, List<List<Dependency>> result, int layer, List<Dependency> currentList) {
        //中间参数小于列表
        if (layer < dimensionValue.size() - 1) {
            if (dimensionValue.get(layer).size() == 0) {
                //递归
                descartes(dimensionValue, result, layer + 1, currentList);
            } else {
                for (int i = 0; i < dimensionValue.get(layer).size(); i++) {
                    List<Dependency> list = new ArrayList<Dependency>(currentList);
                    list.add(dimensionValue.get(layer).get(i));
                    //递归 层数+1
                    descartes(dimensionValue, result, layer + 1, list);
                }
            }
        } else if (layer == dimensionValue.size() - 1) {
            if (dimensionValue.get(layer).size() == 0) {
                result.add(currentList);
            } else {
                for (int i = 0; i < dimensionValue.get(layer).size(); i++) {
                    List<Dependency> list = new ArrayList<Dependency>(currentList);
                    list.add(dimensionValue.get(layer).get(i));
                    result.add(list);
                }
            }
        }
    }

    /**
     * 获得最终结果集
     * 多列表笛卡尔积
     */
    public void getResults() {
        List<List<Dependency>> dimensionValue = higherSet;    // 原来的List
        List<List<Dependency>> res = new ArrayList<>(); //返回集合
        descartes(dimensionValue, res, 0, new ArrayList<>());
        //打印结果集信息
        for (List<Dependency> dp : res) {
            List<Dependency> list = new ArrayList<>();
//            System.out.println(dp.size()); //dp.size()为依赖数目
            for (Dependency d : dp) {
//                System.out.print(d.getGroupId() + ":" + d.getArtifactId() + ":"+ d.getVersion() + " ");
                list.add(d);
            }
            //加入结果集
            resultSet.add(list);
        }
    }


    public boolean conflictDetectBefore() {
        DependencyTree dependencyTree = new DependencyTree();
        IOUtil ioUtil = new IOUtil();
        String backUpPath = projectPath + "/backUpPom.xml";
        //先备份一下原有的pom文件
        ioUtil.copyFile(pomPath, backUpPath);
        dependencyTree.constructTree(projectPath);
//        dependencyTree.constructTree(projectPath);
        // TODO: 2023/10/20 生成pom文件的路径，能否设置为远程？
        dependencyTree.parseTree(projectPath + "/tree.txt");
        //恢复原来的pom文件
        ioUtil.copyFile(backUpPath, pomPath);
        //如果树存在conflict
        if (dependencyTree.isConflict()) {
            return true;
        } else {
            //否则加入无冲突结果集
            resWithoutConflict.add(dependencySet);
            System.out.println("无冲突，继续");
            return false;
        }
    }

    /**
     * 对result结果集中的结果进行冲突检测
     */
    public void conflictDetectAfter() {
        IOUtil ioUtil = new IOUtil();
        String backUpPath = projectPath + "/backUpPom.xml";
        //先备份一下原有的pom文件
        ioUtil.copyFile(pomPath, backUpPath);
        for (List<Dependency> dependencyList : resultSet) {
            DependencyTree dependencyTree = new DependencyTree();
            //对于结果集中的每一项，重写pom文件并调用mvn dependency:tree
            ioUtil.modifyDependenciesXml(pomPath, dependencyList);
            //根据生成的pom文件，执行mvn命令行 解析出依赖树
            dependencyTree.constructTree(projectPath);
            dependencyTree.parseTree(projectPath + "/tree.txt");
            //如果树存在conflict 加入待调解列表
            if (dependencyTree.isConflict()) {
                resToMediate.add(dependencyTree);
                System.out.println("加入待调解列表！");
            } else {
                //否则加入无冲突结果集
                resWithoutConflict.add(dependencyList);
                System.out.println("无冲突，继续");
            }
        }
        //恢复原来的pom文件
        ioUtil.copyFile(backUpPath, pomPath);
        //如果无冲突的结果集不存在 进入冲突调解程序
        if (resWithoutConflict.size() == 0) {
            conflictMediation();
        }
    }

    public void conflictMediation() {
        RecommendSolution solution = new RecommendSolution();
        //遍历待冲突调解的结果集合
        for (DependencyTree tree : resToMediate) {
            // TODO: 2023/10/21 初始化一个List
            List<Dependency> directDeps = new ArrayList<>();
            //获取冲突依赖的集合
            HashMap<String[], List<Dependency>> conflictMap = tree.getConflictMap();
            //遍历conflictMap
            for (Map.Entry<String[], List<Dependency>> entry : conflictMap.entrySet()) {
                List<Dependency> conflictDepList = entry.getValue(); //获取冲突的集合
                String groupId = conflictDepList.get(0).getGroupId();
                String artifactId = conflictDepList.get(0).getArtifactId();
                //编写比较器 对象按照version从小到大
                if (type == 0) {
                    // TODO: 2023/10/20 按照date
                    solution.sortByDate(conflictDepList);

                } else if (type == 1) {
                    // TODO: 2023/10/20 按照usage
                    solution.sortByUsage(conflictDepList);

                } else if (type == 2) {
                    // TODO: 2023/10/20 按照vulNum
                    solution.sortByVulNums(conflictDepList);

                }
                Dependency latestDep = conflictDepList.get(conflictDepList.size() - 1);
                System.out.print("最后获得最最优版本的依赖为：");
//                latestDep.printDependency();
                // 获取resList: 原项目实际加载的依赖
                List<Dependency> resList = tree.getResList();
                // 遍历resList(加载的依赖)
                for (Dependency dependency : resList) {
                    //定位到与冲突的依赖
                    if (dependency.getGroupId().equals(groupId) && dependency.getArtifactId().equals(artifactId)) {
                        System.out.println("与实际加载的依赖版本进行比较");
                        if (type == 0) {
                            // 原来加载的 比 冲突的 更新 保留
                            if (dependency.getVersion().compareTo(latestDep.getVersion()) > 0) {
//                                System.out.println("保留原来加载的版本");
                                Dependency parent = dependency.getParentDependency();
                                addParentDepIntoDirectList(directDeps, parent);
                            }
                            //否则需要exclude实际加载的依赖
                            else {
                                System.out.print("exclusion实际加载的依赖：");
                                // 首先将项目原本加载的依赖屏蔽
                                Dependency parent = dependency.getParentDependency();
                                parent.addExclusionDependency(dependency);
                                addParentDepIntoDirectList(directDeps, parent);
                                if (conflictDepList.size() == 1) {
                                    //如果map里面只有一个特殊处理？
//                                    System.out.println("自动加载这个依赖");
                                } else {
                                    for (int i = 0; i < conflictDepList.size() - 1; i++) {
                                        Dependency unLoadDependency = conflictDepList.get(i);
                                        parent = unLoadDependency.getParentDependency();
                                        parent.addExclusionDependency(unLoadDependency);
//                                        System.out.print("建议父依赖：");
//                                        parent.printDependency();
//                                        System.out.print("需要exclusion子依赖：");
//                                        unLoadDependency.printDependency();
                                        addParentDepIntoDirectList(directDeps, parent);
                                    }
                                }
                            }
                        } else if (type == 1) {
                            // 如果原来加载的比冲突的使用量更大
                            if (dependency.getUsage().compareTo(latestDep.getUsage()) > 0) {
//                                System.out.println("保留原来加载的版本");
                                Dependency parent = dependency.getParentDependency();
                                addParentDepIntoDirectList(directDeps, parent);
                            } else {
//                                System.out.print("exclusion实际加载的依赖：");
                                // 首先将项目原本加载的依赖屏蔽
                                Dependency parent = dependency.getParentDependency();
                                parent.addExclusionDependency(dependency);
                                addParentDepIntoDirectList(directDeps, parent);
                                if (conflictDepList.size() == 1) {
                                    //如果map里面只有一个特殊处理？
//                                    System.out.println("自动加载这个依赖");
                                } else {
                                    for (int i = 0; i < conflictDepList.size() - 1; i++) {
                                        Dependency unLoadDependency = conflictDepList.get(i);
                                        parent = unLoadDependency.getParentDependency();
                                        parent.addExclusionDependency(unLoadDependency);
//                                        System.out.print("建议父依赖：");
//                                        parent.printDependency();
//                                        System.out.print("需要exclusion子依赖：");
//                                        unLoadDependency.printDependency();
                                        addParentDepIntoDirectList(directDeps, parent);
                                    }
                                }
                            }
                        } else if (type == 2) {
                            if (dependency.getVulNum() < dependency.getVulNum()) {
                                System.out.println("保留原来加载的版本");
                            } else {
                                System.out.print("exclusion实际加载的依赖：");
                                // 首先将项目原本加载的依赖屏蔽
                                Dependency parent = dependency.getParentDependency();
                                parent.addExclusionDependency(dependency);
                                addParentDepIntoDirectList(directDeps, parent);
                                if (conflictDepList.size() == 1) {
                                    //如果map里面只有一个特殊处理？
//                                    System.out.println("自动加载这个依赖");
                                } else {
                                    for (int i = 0; i < conflictDepList.size() - 1; i++) {
                                        Dependency unLoadDependency = conflictDepList.get(i);
                                        parent = unLoadDependency.getParentDependency();
                                        parent.addExclusionDependency(unLoadDependency);
//                                        System.out.print("建议父依赖：");
//                                        parent.printDependency();
//                                        System.out.print("需要exclusion子依赖：");
//                                        unLoadDependency.printDependency();
                                        addParentDepIntoDirectList(directDeps, parent);
                                    }
                                }
                            }
                        }
                    } else {
                        // 其他的 加入直接依赖即可
                        Dependency parent = dependency.getParentDependency();
                        addParentDepIntoDirectList(directDeps, parent);
                    }
                }
            }

            // 加入推荐的列表
            recommendDepSet.add(directDeps);
        }
    }

    public List<Dependency> addParentDepIntoDirectList(List<Dependency> directDeps, Dependency parent) {
        if (directDeps.size() != 0) {
            boolean exist = false;
            for (Dependency d : directDeps) {
                // directDeps里面已经存在这个直接依赖了
                if (d.getGroupId().equals(parent.getGroupId()) && d.getArtifactId().equals(parent.getArtifactId())
                        && d.getVersion().equals(parent.getVersion())) {
                    exist = true; //标为存在
                    break;
                }
            }
            if (!exist) {
                // 说明不存在 加入
                directDeps.add(parent);
            }
        } else { // 为0 直接加入
            directDeps.add(parent);
        }
        return directDeps;
    }


}
