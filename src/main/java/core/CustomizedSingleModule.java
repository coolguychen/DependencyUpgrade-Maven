package core;

import depModel.Dependency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.versioning.ComparableVersion;

// 用户定制的单模块
// 用户可以对某一个第三方库的版本进行限制
// 比如 指定 httpclient 不等于 4.5.13
public class CustomizedSingleModule extends SingleModule {

    private String selectedGroupId;
    private String selectedArtifactId;
    private String selectedVersion;
    private String opr;    // 用户选择的运算符

    public String getSelectedGroupId() {
        return selectedGroupId;
    }

    public void setSelectedGroupId(String selectedGroupId) {
        this.selectedGroupId = selectedGroupId;
    }

    public String getSelectedArtifactId() {
        return selectedArtifactId;
    }

    public void setSelectedArtifactId(String selectedArtifactId) {
        this.selectedArtifactId = selectedArtifactId;
    }

    public String getOpr() {
        return opr;
    }

    public void setOpr(String opr) {
        this.opr = opr;
    }

    public String getSelectedVersion() {
        return selectedVersion;
    }

    public void setSelectedVersion(String selectedVersion) {
        this.selectedVersion = selectedVersion;
    }

    public CustomizedSingleModule() {
    }

    public CustomizedSingleModule(String path, int _type) {
        super(path, _type);
    }

    public static void main(String[] args) {
        CustomizedSingleModule singleModule = new CustomizedSingleModule();
        //先解析pom文件，得到getLibsFromPom
        singleModule.getLibsFromPom("D:\\1javawork\\singleModuleDemo");
        singleModule.getCustomizedUpgradeSolutions("D:\\1javawork\\singleModuleDemo",0,"log4j","log4j", "=","1.2.16");
        singleModule.getUpgradedPom(0);
    }

    /**
     * 用户选择第三方库，返回其全部版本（限制最多30个）
     *
     * @param groupId
     * @param artifactId
     * @return
     */
    public List<String> getAllVersions(String groupId, String artifactId) {
        List<String> versionList = new ArrayList<>();
        // 从数据库中获取
        List<Dependency> allDeps = jdbc.getLibAllVersions(groupId, artifactId);
        for (Dependency d : allDeps) {
            versionList.add(d.getVersion());
        }
        return versionList;
    }


    /**
     *
     * @param inputVersion
     * @param opr
     * @param dependencies
     * @return
     */
    public List<Dependency> getCustomizedDeps(String inputVersion, String opr, List<Dependency> dependencies) {
        List<Dependency> result = new ArrayList<>();
        // 在这里实现逻辑，根据输入版本号查找 opr 输入版本的依赖，并添加到结果列表中
        for (Dependency dependency : dependencies) {
            String dependencyVersion = dependency.getVersion();
            if (compareVersions(dependencyVersion, inputVersion, opr)) {
                result.add(dependency);
            }
        }
        return result;
    }

    private int versionCompareRes(String version1, String version2) {
        ComparableVersion v1 = new ComparableVersion(version1);
        ComparableVersion v2 = new ComparableVersion(version2);

        return v1.compareTo(v2);
    }

    /**
     * 按照定义的比较规则定义比较函数
     *
     * @param version1
     * @param version2
     * @param operator
     * @return
     */
    public boolean compareVersions(String version1, String version2, String operator) {
        int result = versionCompareRes(version1, version2);
        switch (operator) {
            case "<":
                return result < 0;
            case "<=":
                return result <= 0;
            case ">":
                return result > 0;
            case ">=":
                return result >= 0;
            case "=":
                return result == 0;
            case "!=":
                return result != 0;
            default:
                throw new IllegalArgumentException("Invalid operator: " + operator);
        }
    }


    /**
     * 版本约束的推荐的入口
     * @param projectPath
     * @param type
     * @param groupId
     * @param artifactId
     * @param opr
     * @param version
     * @return
     */
    public List<List<Dependency>> getCustomizedUpgradeSolutions(String projectPath, int type,
                                                                String groupId, String artifactId,
                                                                String opr, String version) {
        // 首先 set等等
        setProjectPath(projectPath);
        setType(type);
        setPomPath(projectPath + "/pom.xml");
        setSelectedGroupId(groupId);
        setSelectedArtifactId(artifactId);
        setSelectedVersion(version);
        setOpr(opr);
        boolean isConflictBefore = conflictDetectBefore();
        if (!isConflictBefore) {
            //如果原项目没有冲突，加入无冲突集合
            System.out.println("原项目无冲突");
            getResWithoutConflict().add(dependencySet);
        }

        // 如果没有冲突，可以告诉用户没有冲突，但可以给出升级的无冲突版本
        // 有冲突，推荐无冲突版本
        singleModuleUpgrade(dependencySet); //todo 重载了这个方法
        // 对升级后的结果集进行有无冲突的判断
        conflictDetectAfter();

        // 如果没有无冲突的升级版本
        if (resWithoutConflict.size() != 0) {
            System.out.println("以下是无冲突的推荐版本");
            // 将recommend设置为resWithoutConflict
            setRecommendDepSet(resWithoutConflict);
        } else {
            System.out.println("以下是调解后的版本");
        }
        int id = 0;
        for(List<Dependency> dplist : recommendDepSet) {
            System.out.println(id ++);
            for(Dependency d: dplist) {
                d.printDependency();
            }
        }
        return recommendDepSet;
    }

    @Override
    public void singleModuleUpgrade(List<Dependency> dependencySet) {
        RecommendSolution solution = new RecommendSolution();
        // TODO: 2023/10/18 根据传入的参数选择如何推荐
        List<List<Dependency>> resultToBeSorted = new ArrayList<>();
        // 遍历pom文件的所有依赖：
        for (Dependency d : dependencySet) {
            List<Dependency> allVersions = new ArrayList<>();
            String groupId = d.getGroupId();
            String artifactId = d.getArtifactId();
            // TODO: 2023/10/24 如果是用户自定义的偏好推荐
            if(groupId.equals(selectedGroupId) && artifactId.equals(selectedArtifactId)) {
                // 先获取groupId + artifactId 下的全部版本
                allVersions = jdbc.getLibAllVersions(selectedGroupId, selectedArtifactId);
                // 提取出遵守用户自定义(opr selectedVersion)的依赖列表
                allVersions = getCustomizedDeps(selectedVersion, opr, allVersions); //返回的是满足依赖约束的versions
            }
            else{
                //没有约束的，在t_lib中找到这个依赖的全部组合
                allVersions = jdbc.getLibAllVersions(groupId, artifactId);
            }
            List<Dependency> sortedVersions = new ArrayList<>();
            // 仍然按照选择的推荐
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
}
