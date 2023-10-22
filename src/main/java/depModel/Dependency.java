package depModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 依赖/第三方包的结构
 */
public class Dependency {
    // lib_id
    private int libId;
    private int id;
    private String groupId; //该依赖的groupId
    private String artifactId; //该依赖的artifactId
    private String version; //该依赖的当前版本

    private int vulNum; //该依赖的漏洞数目

    private String usage; //该依赖的使用量

    private String publishDate; // 该依赖的发布日期
    private boolean conflict;

    private int depth;
    private Dependency parentDependency;

    private List<Dependency> exclusionDependency = new ArrayList<>(); // 是否存在被排除的依赖
    private List<String> higherVersions; //该依赖对应的更高版本
    private List<Dependency> higherList; //该依赖对应的更高依赖的集合
    //间接依赖
    private List<Dependency> subDependency;

    public Dependency(int libId, int depth) {
        this.libId = libId;
        this.depth = depth;
    }

    public Dependency(int id, String groupId, String artifactId, String version, int depth, Dependency parentDependency) {
        this.id = id;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.depth = depth;
        this.parentDependency = parentDependency;
    }

    public Dependency(String groupId, String artifactId, String version, int vulNum, String usage, String publishDate) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.vulNum = vulNum;
        this.usage = usage;
        this.publishDate = publishDate;
    }

    public Dependency(int id, String groupId, String artifactId, String version, int vulNum, String usage, String publishDate, int depth, Dependency parentDependency) {
        this.id = id;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.vulNum = vulNum;
        this.usage = usage;
        this.publishDate = publishDate;
        this.depth = depth;
        this.parentDependency = parentDependency;
    }

    public List<String> getHigherVersions() {
        return higherVersions;
    }

    public int getLibId() {
        return this.libId;
    }

    public void setLibId(int libId) {
        this.libId = libId;
    }

    public int getVulNum() {
        return vulNum;
    }

    public void setVulNum(int vulNum) {
        this.vulNum = vulNum;
    }

    public String getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(String publishDate) {
        this.publishDate = publishDate;
    }

    public List<Dependency> getExclusionDependency() {
        return exclusionDependency;
    }

    public void setExclusionDependency(List<Dependency> exclusionDependency) {
        this.exclusionDependency = exclusionDependency;
    }

    public void addExclusionDependency(Dependency exclusion) {
        this.exclusionDependency.add(exclusion);
    }

    /**
     * 构造函数
     *
     * @param id               记为出现的顺序
     * @param groupId
     * @param artifactId
     * @param version
     * @param depth            深度
     * @param parentDependency 父依赖是谁
     */
//    public Dependency(int id, String groupId, String artifactId, String version, int depth, Dependency parentDependency) {
//        this.id = id;
//        this.groupId = groupId;
//        this.artifactId = artifactId;
//        this.version = version;
//        this.depth = depth;
//        this.parentDependency = parentDependency;
//    }


    public Dependency(int libId, String groupId, String artifactId, String version, int vulNum, String usage, String publishDate) {
        this.libId = libId;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.vulNum = vulNum;
        this.usage = usage;
        this.publishDate = publishDate;
    }

    /**
     * 构造函数
     *
     * @param groupId
     * @param artifactId
     * @param version
     */
    public Dependency(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.higherList = new ArrayList<>();
        this.higherVersions = new ArrayList<>();
        //把当前依赖的版本加进去
        this.higherVersions.add(version);
    }

    public Dependency() {

    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public List<Dependency> getSubDependency() {
        return subDependency;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getUsage() {
        return usage;
    }

    public void setUsage(String usage) {
        this.usage = usage;
    }

    public Dependency getParentDependency() {
        return parentDependency;
    }

    public boolean isConflict() {
        return conflict;
    }

    public void printDependency() {
        System.out.print(getGroupId() + ":" + getArtifactId() + ":" + getVersion() + ", ");
        if(exclusionDependency.size()!=0) {
            System.out.print("同时需要屏蔽依赖：");
            for (Dependency ex: exclusionDependency) {
                ex.printDependency();
            }
        }
        System.out.println("----------------------------");
    }

}
