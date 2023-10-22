package core;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import java.io.FileReader;
import java.util.*;

/**
 * 获取pom.xml的直接依赖
 */
public class PomParser {
    private List<Dependency> dependencies;
    public PomParser() {
        dependencies = new ArrayList<>();
    }

    /**
     * 输入一个pom文件路径，解析得到依赖列表
     * @param pomFilePath
     * @return
     */
    public List<Dependency> parsePom(String pomFilePath) {
        List<Dependency> directDependencies = new ArrayList<>();
        try {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new FileReader(pomFilePath));
            directDependencies = model.getDependencies();
            for (Dependency dependency : directDependencies) {
                String groupId = dependency.getGroupId();
                String artifactId = dependency.getArtifactId();
                String version = dependency.getVersion();
                String formattedDependency = groupId + ":" + artifactId + ":" + version;
            }
            printDependencies();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return directDependencies;
    }

    private void printDependencies() {
        System.out.println("Dependencies:");
        for (Dependency dependency : dependencies) {
        }
    }

    public static void main(String[] args) {
        String pomFilePath = "D:\\1javawork\\Third Party Libraries\\UpgradeTPL\\pom.xml";
        PomParser parser = new PomParser();
        parser.parsePom(pomFilePath);
    }
}