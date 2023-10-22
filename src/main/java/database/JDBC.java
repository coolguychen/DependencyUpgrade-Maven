package database;

import depModel.Dependency;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JDBC {
    // test为数据库名称
    // MySQL 8.0 以上版本选择
    static final String JdbcDriver = "com.mysql.cj.jdbc.Driver";
    //连接服务器上的数据库
    static final String Url = "jdbc:mysql://124.220.14.106:3306/thirdlib?useSSL=false&serverTimezone=UTC";

    private static Connection conn = null;
    private static Statement stmt = null;
    private static ResultSet resultSet = null;
    static final String User = "thirdlib";//输入你的数据库库名
    static final String PassWord = "C8C061C3A18D8E63BBB811E47A67696C";//输入你的数据库连接密码

    public JDBC() {
        if (conn == null) startUp();
    }

    public void startUp() {
        // 注册 JDBC 驱动
        try {
            Class.forName(JdbcDriver);
            conn = DriverManager.getConnection(Url, User, PassWord);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 查询T_lib中是否存在该库
     * @param groupId
     * @param artifactId
     * @param version
     * @return
     */
    public boolean queryFromT_lib(String groupId, String artifactId, String version) {
//        startUp();
        try {
            //SELECT count(*) from library where groupId = "com.google.guava" AND artifactId = "guava";
            String sql = "SELECT * from t_lib where groupId = \"" + groupId + "\""
                    + " AND artifactId = \"" + artifactId + "\"" +
                    " AND version = \"" + version + "\"" + ";";

            stmt = conn.createStatement();
            resultSet = stmt.executeQuery(sql);
            int res = 0;
            while (resultSet.next()) {
                res++;
            }
            //获取结果集，如果为0说明不存在
            if (res == 0) return false;
            else return true;
        } catch (SQLException se) {
            // 处理 JDBC 错误
            se.printStackTrace();
        } catch (Exception e) {
            // 处理 Class.forName 错误
            e.printStackTrace();
        }
        return false;
    }

    public Dependency getLibDetail(int libId) {
        Dependency dependency = null;
        try {
            String sql = "SELECT * FROM t_lib WHERE libId = " + libId;
            stmt = conn.createStatement();
            resultSet = stmt.executeQuery(sql);
            // 遍历结果集
            while (resultSet.next()) {
                String groupId = resultSet.getString("groupId");
                String artifactId = resultSet.getString("artifactId");
                String version = resultSet.getString("version");
                String usage = resultSet.getString("usages");
                int vulCount = resultSet.getInt("vulCount");
                String publishDate = resultSet.getString("publishDate");
                dependency = new Dependency(libId, groupId, artifactId, version, vulCount, usage, publishDate);
            }
        } catch (SQLException E) {
            System.out.println(E);
        }
        return dependency;
    }

//    public int getLibId(String groupId, String artifactId, String version) {
//        // 如果存在
//        if(queryFromT_lib(groupId, artifactId, version)) {
//           try{
//               String sql = "SELECT libId from t_lib where groupId = \"" + groupId + "\""
//                       + " AND artifactId = \"" + artifactId + "\"" +
//                       " AND version = \"" + version + "\"" + ";";
//
//               stmt = conn.createStatement();
//               resultSet = stmt.executeQuery(sql);
//               int res = 0;
//               while (resultSet.next()) {
//                   return resultSet.getInt("libId");
//               }
//           } catch (SQLException e) {
//
//           }
//        } else{
//            // 如果不存在 爬取
//            return 0;
//        }
//    }

    /**
     * 获取已经爬取了的groupIds
     * @return
     */
    public List<String> queryGroupIdFromT_lib(){
        List<String> groupIds = new ArrayList<>();
        try {
            String sql = "SELECT DISTINCT groupId FROM t_lib";
            stmt = conn.createStatement();
            resultSet = stmt.executeQuery(sql);

            // 遍历结果集
            while (resultSet.next()) {
                String groupId = resultSet.getString("groupId");
                groupIds.add(groupId);
            }
        } catch (SQLException E){

        }
        return groupIds;
    }

    public HashMap<Integer, String> getAllUnAccessedLinksFromT_lib(){
//        List<String[]> lib = new ArrayList<>();
        HashMap<Integer, String> libInfo = new HashMap<>();
        try {
            // 获取未解析过的链接
            String sql = "SELECT libId, libLink FROM t_lib WHERE libId > 150000 AND libId < 200000 AND accessed is NULL";
            stmt = conn.createStatement();
            resultSet = stmt.executeQuery(sql);

            // 遍历结果集
            while (resultSet.next()) {
                int libId = resultSet.getInt("libId");
                String libLink = resultSet.getString("libLink");
//                groupIds.add(new String[]{libId, groupId});
                libInfo.put(libId, libLink);
            }
        } catch (SQLException E){

        }
        return libInfo;
    }

    public int getLibId(String groupId, String artifactId, String version){
        try {
            String sql = "SELECT libId FROM t_lib WHERE groupId = \"" +  groupId + "\""  +
                    " AND artifactId = \"" + artifactId + "\"" +
                    " AND version = \"" + version + "\"";
            stmt = conn.createStatement();
            int libId = 0;
            resultSet = stmt.executeQuery(sql);
            while (resultSet.next()) {
                libId = resultSet.getInt("libId");
            }
            return libId;
        }
        catch (SQLException E) {

        }
        return 0;
    }

    public void updateLibAccess(int libId,int status){
        try{
            String sql = "UPDATE t_lib SET accessed =" + status + " WHERE libId = " +libId;
            stmt = conn.createStatement();
            int resultSet = stmt.executeUpdate(sql);
            if (resultSet > 0) {
                //如果插入成功，则打印success
                System.out.println("Update Library Access Success");
            } else {
                //如果插入失败，则打印Failure
                System.out.println("Failure");
            }
        }catch (SQLException e) {

        }
    }

    /**
     * 依赖关系插入表t_dlib_slib
     * @param dlib
     * @param slib
     */
    public void insertIntoT_slib_tlib(int dlib, int slib){
        try {
            String sql = "insert into t_slib_dlib (slibId, dlibId) values " +
                    "(" + dlib + "," + slib + ")";
            stmt = conn.createStatement();
            //执行插入语句
            int resultSet = stmt.executeUpdate(sql);
            if (resultSet > 0) {
                //如果插入成功，则打印success
                System.out.println("Insert depLib Success");
            } else {
                //如果插入失败，则打印Failure
                System.out.println("Failure");
            }
        } catch (SQLException se) {
            // 处理 JDBC 错误
            se.printStackTrace();
        }
    }

    // TODO: 2023/10/9 将CVE_id与lib_id对应。 
    public void insertIntoT_lib_vul(int lib_id, String CVE_id){
        try {
            String sql = "insert into t_lib_vulnerability (libId, vno) values " +
                    "(" + lib_id + ",\"" + CVE_id + "\")";
            stmt = conn.createStatement();
            //执行插入语句
            int resultSet = stmt.executeUpdate(sql);
            if (resultSet > 0) {
                //如果插入成功，则打印success
                System.out.println("Insert Lib to CVE Success");
            } else {
                //如果插入失败，则打印Failure
                System.out.println("Failure");
            }

        } catch (SQLException se) {
            // 处理 JDBC 错误
            se.printStackTrace();
        }
    }

    public void insertLinkIntoT_lib(String groupId, String artifactId, String version, String usages, String publish_date, String libLink){
        if (queryFromT_lib(groupId, artifactId, version) == true) {
            System.out.println("数据库已存在该条数据！");
        } else {
            try {
                String sql = "insert into t_lib (groupId, artifactId, version, usages, publishDate, libLink) values " +
                        "( \"" + groupId + "\", \"" + artifactId + "\", \"" + version  + "\",\"" + usages + "\",\"" + publish_date + "\", \"" + libLink + "\")";
                stmt = conn.createStatement();
                //执行插入语句
                int resultSet = stmt.executeUpdate(sql);
                if (resultSet > 0) {
                    //如果插入成功，则打印success
                    System.out.println("Success");
                } else {
                    //如果插入失败，则打印Failure
                    System.out.println("Failure");
                }

            } catch (SQLException se) {
                // 处理 JDBC 错误
                se.printStackTrace();
            }
        }
    }


    public void insertIntoT_artifact(String groupId, String artifactId, String link){
        if(isExistInT_artifact(link)) {
            System.out.println("已存在，跳过");
        }
        else {
            try {
                String sql = "insert into t_artifact (groupId, artifactId, link) values " +
                        "(\"" +groupId + "\", \"" + artifactId + "\", \"" + link + "\")";
                stmt = conn.createStatement();
                //执行插入语句
                int resultSet = stmt.executeUpdate(sql);
                if (resultSet > 0) {
                    //如果插入成功，则打印success
                    System.out.println("Success");
                } else {
                    //如果插入失败，则打印Failure
                    System.out.println("Failure");
                }

            } catch (SQLException se) {
                // 处理 JDBC 错误
                se.printStackTrace();
            }
        }

    }

    public void insertIntoT_artifact(String category, String groupId, String artifactId, String link){
        if(isExistInT_artifact(link)) {
            System.out.println("已存在，跳过");
        }
        else {
            try {
                String sql = "insert into t_artifact (category, groupId, artifactId, link) values " +
                        "(\"" + category + "\", \"" +groupId + "\", \"" + artifactId + "\", \"" + link + "\")";
                stmt = conn.createStatement();
                //执行插入语句
                int resultSet = stmt.executeUpdate(sql);
                if (resultSet > 0) {
                    //如果插入成功，则打印success
                    System.out.println("Success");
                } else {
                    //如果插入失败，则打印Failure
                    System.out.println("Failure");
                }

            } catch (SQLException se) {
                // 处理 JDBC 错误
                se.printStackTrace();
            }
        }

    }

    public boolean isExistInT_artifact(String link) {
        try{
            String sql = "SELECT * from t_artifact where link = \"" + link + "\"";
            stmt = conn.createStatement();
            resultSet = stmt.executeQuery(sql);
            int res = 0;
            while (resultSet.next()) {
                res++;
            }
            if(res == 0) return false;
            else return true;
        }catch (SQLException e){

        }
        return false;
    }
    /**
     *
     * @return 返回artifact三元组的集合
     * {groupId, artfactId, link}
     */
    public List<String[]> queryFromT_artifact(){
        List<String[]> artifactInfo = new ArrayList<>();
        try {
            String sql = "SELECT groupId, artifactId, link from t_artifact";
            stmt = conn.createStatement();
            resultSet = stmt.executeQuery(sql);
            // 遍历结果集
            while (resultSet.next()) {
                String groupId = resultSet.getString("groupId");
                String artifactId = resultSet.getString("artifactId");
                String link = resultSet.getString("link");
                String[] info = new String[3];
                info[0] = groupId;
                info[1] = artifactId;
                info[2] = link;
                artifactInfo.add(info);
            }
            return artifactInfo;

        } catch (SQLException se) {
            // 处理 JDBC 错误
            se.printStackTrace();
        }
        return null;
    }

    //最后关闭数据库资源
    public void closeOff() {
        // 关闭资源
        try {
            if (stmt != null) stmt.close();
        } catch (SQLException se2) {
        }
        try {
            if (conn != null) conn.close();
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    // TODO：CVE信息的爬取和insert

    public static void main(String[] args) {
        JDBC jdbc = new JDBC();
        jdbc.queryFromT_artifact();
//        jdbc.insertIntoT_lib(0,"org.dom4j", "dom4j", "1.1.1", "", "", 110, "Sep 20, 2022","");

    }


    private static String CVElink = "https://cve.mitre.org/cgi-bin/cvename.cgi?name=";


    public void insertIntoT_vul(String vno) {
        if (queryFromT_vul(vno) == true) {
            System.out.println("数据库已存在该条数据！");
        }
        else {
            try {
                String cveLink = CVElink + vno;
                String sql = "insert into t_vulnerability (vno, vlink) values"  + "(\""+ vno + "\",\"" + cveLink + "\")";
                stmt = conn.createStatement();
                //执行插入语句
                int resultSet = stmt.executeUpdate(sql);
                if (resultSet > 0) {
                    //如果插入成功，则打印success
                    System.out.println("Insert CVE Success");
                } else {
                    //如果插入失败，则打印Failure
                    System.out.println("Failure");
                }

            } catch (SQLException se) {
                // 处理 JDBC 错误
                se.printStackTrace();
            } finally {
            }
        }

    }

    /**
     *
     * @param vno
     * @return
     */
    public boolean queryFromT_vul(String vno) {
        try {
            //SELECT count(*) from library where groupId = "com.google.guava" AND artifactId = "guava";
            String sql = "SELECT * from t_vulnerability where vno = \"" + vno + "\"" + ";";
            stmt = conn.createStatement();
            resultSet = stmt.executeQuery(sql);
            int res = 0;
            while (resultSet.next()) {
                res++;
            }
            //获取结果集，如果为0说明不存在
            if (res == 0) return false;
            else return true;
        } catch (SQLException se) {
            // 处理 JDBC 错误
            se.printStackTrace();
        } catch (Exception e) {
            // 处理 Class.forName 错误
            e.printStackTrace();
        } finally {
            // 关闭资源
//            try {
//                if (stmt != null) stmt.close();
//            } catch (SQLException se2) {
//            }
//            try {
//                if (conn != null) conn.close();
//            } catch (SQLException se) {
//                se.printStackTrace();
//            }
        }
        return false;
    }

    // TODO: 2023/10/11 获取libId下对应的所有依赖

    /**
     * 获取libId下对应的所有依赖
     * @param libId
     * @return
     */
    public List<Integer> getLibDeps(int libId){
        List<Integer> c_deps = new ArrayList<>();
        try{
            String sql = "SELECT  DISTINCT dlibId FROM t_slib_dlib WHERE slibId =" + libId;
            Statement stmt = conn.createStatement();
            resultSet = stmt.executeQuery(sql);
            while (resultSet.next()) {
                int dlib =  resultSet.getInt("dlibId");
                c_deps.add(dlib);
            }

        } catch (SQLException e) {

        }
        return c_deps;
    }


    public void updateVulNums (int libId) {
        try {
            String sql = "SELECT COUNT(DISTINCT(vno)) FROM t_lib_vulnerability WHERE libId = " + libId;
            stmt = conn.createStatement();
            //执行插入语句
            resultSet = stmt.executeQuery(sql);
            int num = 0;
            while(resultSet.next()){
                num = resultSet.getInt(0);
            }
            // vuls 数量 更新在表中
            String updateSql = "UPDATE t_lib SET vulCount = " + num + " WHERE libId =" + libId;
            int res = stmt.executeUpdate(updateSql);
            if(res > 0) {
                //如果插入成功，则打印success
                System.out.println("Update vul nums Success");
            }else{
                System.out.println("Failure");
            }
        } catch (SQLException se) {
            // 处理 JDBC 错误
            se.printStackTrace();
        } finally {
        }
    }


    /**
     *
     * @param groupId
     * @param artifactId
     * @return
     */
    public List<Dependency> getLibAllVersions(String groupId, String artifactId) {
        List<Dependency> depList = new ArrayList<>();
        // TODO: 2023/10/19 如果该lib 不存在 就要爬取

        // TODO: 2023/10/19 如果存在
        try{
            String sql = "SELECT * FROM t_lib WHERE groupId = " + "\"" + groupId + "\"" + "AND " + "artifactId = " + "\"" + artifactId +"\"";
            stmt = conn.createStatement();
            //执行插入语句
            resultSet = stmt.executeQuery(sql);
            // 遍历结果集
            while (resultSet.next()) {
                String version = resultSet.getString("version");
                String usage = resultSet.getString("usages");
                int vulCount = resultSet.getInt("vulCount");
                String publishDate = resultSet.getString("publishDate");
                Dependency d = new Dependency(groupId, artifactId, version, vulCount, usage, publishDate);
                depList.add(d);
            }
        }catch (SQLException e) {

        }
        return depList;
    }
}
