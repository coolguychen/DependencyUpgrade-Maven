package core;

import depModel.Dependency;
import database.JDBC;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DependencyTreeGenerator {
    // 生成libId的依赖树
    public static List<Dependency> generateDependencyTree(int libId, int depth) {
        List<Dependency> dependencyTreeList = new ArrayList<>();
        // 加入本身的依赖
        Dependency dependency = new Dependency(libId, depth);
        dependencyTreeList.add(dependency);
        // 查询指定库的记录并加入依赖树列表
        List<Integer> depLibs = queryLibraryByLibId(libId);
//        dependencyTreeList.add(lib);
        // 递归查询依赖库记录
        for (int dependencyId : depLibs) {
            // 查询依赖库的记录并加入依赖树列表
//            List<Integer> deps = queryLibraryByLibId(dependencyId);
            //如果存在传递依赖
            // 递归调用生成依赖树函数
            List<Dependency> childDependencyTree = generateDependencyTree(dependencyId, depth + 1);
            dependencyTreeList.addAll(childDependencyTree);
        }
        // TODO: 2023/10/18 跳出循环时，uodate t_Lib中的depTree 
        return dependencyTreeList;
    }

    //    public static List<Dependency> generateConflictDependencyTree(int libId, Connection connection) throws SQLException {
//        List<Dependency> dependencyTree = generateDependencyTree(libId, connection);
//        List<Dependency> conflictDependencyList = new ArrayList<>();
//
//        for (Dependency lib : dependencyTree) {
//            // 查询冲突的依赖库
//            List<Dependency> conflictDependency = queryConflictDependency(lib.getGroupId(), lib.getArtifactId(), lib.getVersion(), connection);
//
//            if (!conflictDependency.isEmpty()) {
//                conflictDependencyList.addAll(conflictDependency);
//            }
//        }
//
//        return conflictDependencyList;
//    }
//
    private static List<Integer> queryLibraryByLibId(int libId) {
        JDBC jdbc = new JDBC();
        List<Integer> deps = jdbc.getLibDeps(libId);
        return deps;
    }

//    private static List<Library> queryConflictDependency(String groupId, String artifactId, String version, Connection connection) throws SQLException {
//        List<Library> conflictDependencyList = new ArrayList<>();
//        String query = "SELECT * FROM t_lib WHERE groupId = ? AND artifactId = ? AND version != ?";
//
//        try (PreparedStatement statement = connection.prepareStatement(query)) {
//            statement.setString(1, groupId);
//            statement.setString(2, artifactId);
//            statement.setString(3, version);
//
//            try (ResultSet resultSet = statement.executeQuery()) {
//                while (resultSet.next()) {
//                    Library lib = new Library();
//                    lib.setLibId(resultSet.getInt("libId"));
//                    lib.setGroupId(resultSet.getString("groupId"));
//                    lib.setArtifactId(resultSet.getString("artifactId"));
//                    lib.setVersion(resultSet.getString("version"));
//                    // 设置其他属性
//                    conflictDependencyList.add(lib);
//                }
//            }
//        }
//
//        return conflictDependencyList;
//    }

    // 定义Library类，包含对应的属性和getter/setter方法
    public static void main(String[] args) {
        // 指定库的ID
        int libId = 32;

        List<Dependency> dependencyTree = generateDependencyTree(libId, 0);
        System.out.println(dependencyTree);
        JSONArray dependencies = new JSONArray();
        for (Dependency d: dependencyTree) {
            // 创建第一个dependency对象
            JSONObject dependency1 = new JSONObject();
            int depLibId = d.getLibId();
            int depth = d.getDepth();
            dependency1.put("libId", depLibId);
            dependency1.put("depth", depth);
            dependencies.put(dependency1);
        }

        // 打印输出
        String jsonString = dependencies.toString();
        System.out.println(jsonString);
        // TODO: 2023/10/16 upDATE 数据库对应列

//        List<Dependency> conflictDependencyTree = generateConflictDependencyTree(libId);

        // 打印依赖树
        System.out.println("Dependency Tree:");
        for (Dependency lib : dependencyTree) {
            System.out.println(lib);
        }

        // 打印冲突依赖关系
        System.out.println("Conflict Dependency Tree:");
//        for (Dependency lib : conflictDependencyTree) {
//            System.out.println(lib);
//        }

    }

}