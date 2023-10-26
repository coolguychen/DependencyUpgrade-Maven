package database;

import depModel.Dependency;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import util.HttpUtil;

import java.io.IOException;
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
     *
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
                    " AND version = \"" + version + "\""; //accessed为1 才有vuls信息

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

    /**
     * 获取已经爬取了的groupIds
     *
     * @return
     */
    public List<String> queryGroupIdFromT_lib() {
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
        } catch (SQLException E) {

        }
        return groupIds;
    }

    public HashMap<Integer, String> getAllUnAccessedLinksFromT_lib() {
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
        } catch (SQLException E) {

        }
        return libInfo;
    }

    /**
     * 获取libId
     *
     * @param groupId
     * @param artifactId
     * @param version
     * @return
     */
    public int getLibId(String groupId, String artifactId, String version) {
        if(queryFromT_lib(groupId, artifactId, version)){
            try {
                String sql = "SELECT libId FROM t_lib WHERE groupId = \"" + groupId + "\"" +
                        " AND artifactId = \"" + artifactId + "\"" +
                        " AND version = \"" + version + "\"";
                stmt = conn.createStatement();
                int libId = 0;
                resultSet = stmt.executeQuery(sql);
                while (resultSet.next()) {
                    libId = resultSet.getInt("libId");
                }
                return libId;
            } catch (SQLException E) {

            }
        }
        else{
            //不存在
            crawlLib(groupId, artifactId, version);
        }
        return 0;
    }

    public void updateLibAccess(int libId, int status) {
        try {
            String sql = "UPDATE t_lib SET accessed =" + status + " WHERE libId = " + libId;
            stmt = conn.createStatement();
            int resultSet = stmt.executeUpdate(sql);
            if (resultSet > 0) {
                //如果插入成功，则打印success
                System.out.println("Update Library Access Success");
            } else {
                //如果插入失败，则打印Failure
                System.out.println("Failure");
            }
        } catch (SQLException e) {

        }
    }

    /**
     * 更新libID处的漏洞数量
     *
     * @param vulNum
     * @param libId
     */
    public void updateLibVuLNums(int vulNum, int libId) {
        try {
            String sql = "UPDATE t_lib SET vulCount =" + vulNum + " WHERE libId = " + libId;
            stmt = conn.createStatement();
            int resultSet = stmt.executeUpdate(sql);
            if (resultSet > 0) {
                //如果插入成功，则打印success
                System.out.println("Update Library Access Success");
            } else {
                //如果插入失败，则打印Failure
                System.out.println("Failure");
            }
        } catch (SQLException e) {

        }
    }

    /**
     * 依赖关系插入表t_dlib_slib
     *
     * @param dlib
     * @param slib
     */
    public void insertIntoT_slib_tlib(int dlib, int slib) {
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

    public void insertIntoT_lib_vul(int lib_id, String CVE_id) {
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

    /**
     * 返回该lib下对应的所有CVEs
     * @param libId
     * @return
     */
    public List<String> getCVEOfLib(int libId) {
        List<String> vuls = new ArrayList<>();
        try {
            String sql = "SELECT lDISTINCT(vno) FROM t_lib_vulnerability WHERE libId = " + libId;
            Statement stmt = conn.createStatement();
            resultSet = stmt.executeQuery(sql);
            while (resultSet.next()) {
                String CVE = resultSet.getString("vno");
                vuls.add(CVE);
            }
        } catch (SQLException e) {

        }
        return vuls;
    }


    public void insertLinkIntoT_lib(String groupId, String artifactId, String version, String usages, String publish_date, String libLink) {
        if (queryFromT_lib(groupId, artifactId, version) == true) {
            System.out.println("数据库已存在该条数据！");
        } else {
            try {
                String sql = "insert into t_lib (groupId, artifactId, version, usages, publishDate, libLink) values " +
                        "( \"" + groupId + "\", \"" + artifactId + "\", \"" + version + "\",\"" + usages + "\",\"" + publish_date + "\", \"" + libLink + "\")";
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


    public void insertIntoT_artifact(String groupId, String artifactId, String link) {
        if (isExistInT_artifact(link)) {
            System.out.println("已存在，跳过");
        } else {
            try {
                String sql = "insert into t_artifact (groupId, artifactId, link) values " +
                        "(\"" + groupId + "\", \"" + artifactId + "\", \"" + link + "\")";
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

    public void insertIntoT_artifact(String category, String groupId, String artifactId, String link) {
        if (isExistInT_artifact(link)) {
            System.out.println("已存在，跳过");
        } else {
            try {
                String sql = "insert into t_artifact (category, groupId, artifactId, link) values " +
                        "(\"" + category + "\", \"" + groupId + "\", \"" + artifactId + "\", \"" + link + "\")";
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
        try {
            String sql = "SELECT * from t_artifact where link = \"" + link + "\"";
            stmt = conn.createStatement();
            resultSet = stmt.executeQuery(sql);
            int res = 0;
            while (resultSet.next()) {
                res++;
            }
            if (res == 0) return false;
            else return true;
        } catch (SQLException e) {

        }
        return false;
    }

    /**
     * @return 返回artifact三元组的集合
     * {groupId, artfactId, link}
     */
    public List<String[]> queryFromT_artifact() {
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
        } else {
            try {
                String cveLink = CVElink + vno;
                String sql = "insert into t_vulnerability (vno, vlink) values" + "(\"" + vno + "\",\"" + cveLink + "\")";
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
        }
        return false;
    }

    /**
     * 获取libId下对应的所有依赖
     *
     * @param libId
     * @return
     */
    public List<Integer> getLibDeps(int libId) {
        List<Integer> c_deps = new ArrayList<>();
        try {
            String sql = "SELECT  DISTINCT dlibId FROM t_slib_dlib WHERE slibId =" + libId;
            Statement stmt = conn.createStatement();
            resultSet = stmt.executeQuery(sql);
            while (resultSet.next()) {
                int dlib = resultSet.getInt("dlibId");
                c_deps.add(dlib);
            }

        } catch (SQLException e) {

        }
        return c_deps;
    }


    public void updateVulNums(int libId) {
        try {
            String sql = "SELECT COUNT(DISTINCT(vno)) FROM t_lib_vulnerability WHERE libId = " + libId;
            stmt = conn.createStatement();
            //执行插入语句
            resultSet = stmt.executeQuery(sql);
            int num = 0;
            while (resultSet.next()) {
                num = resultSet.getInt(0);
            }
            // vuls 数量 更新在表中
            String updateSql = "UPDATE t_lib SET vulCount = " + num + " WHERE libId =" + libId;
            int res = stmt.executeUpdate(updateSql);
            if (res > 0) {
                //如果插入成功，则打印success
                System.out.println("Update vul nums Success");
            } else {
                System.out.println("Failure");
            }
        } catch (SQLException se) {
            // 处理 JDBC 错误
            se.printStackTrace();
        } finally {
        }
    }


    /**
     * @param groupId
     * @param artifactId
     * @return
     */
    public List<Dependency> getLibAllVersions(String groupId, String artifactId) {
        List<Dependency> depList = new ArrayList<>();
        String libLink = libUrl + groupId + "/" + artifactId;
        // 如果该artifact 不存在 就要爬取
        if (!isExistInT_artifact(libLink)) {
            crawlLibAllVersionsLink(libLink, groupId, artifactId);
        }
        // 取前30个
        try {
            String sql = "SELECT * FROM t_lib WHERE groupId = " + "\"" + groupId + "\"" + "AND " + "artifactId = " + "\"" + artifactId + "\"";
            stmt = conn.createStatement();
            //执行插入语句
            resultSet = stmt.executeQuery(sql);
            // 遍历结果集
            int cnt = 1;
            while (resultSet.next()) {
                String version = resultSet.getString("version");
                String usage = resultSet.getString("usages");
                int vulCount = resultSet.getInt("vulCount");
                String publishDate = resultSet.getString("publishDate");
                Dependency d = new Dependency(groupId, artifactId, version, vulCount, usage, publishDate);
                depList.add(d);
                cnt++;
                if (cnt > 30) break;
            }
        } catch (SQLException e) {

        }
        return depList;
    }

    public int getAccessState(int libId) {
        int state = 0; //表示库的状态码，0表示未访问或者访问失败，1表示访问成功，即vuls&compileDeps都已更新
        try {
            String sql = "SELECT accessed FROM t_lib WHERE libId = " + libId;
            stmt = conn.createStatement();
            //执行插入语句
            resultSet = stmt.executeQuery(sql);
            while (resultSet.next()) {
                state = resultSet.getInt("accessed");
            }
        } catch (SQLException e) {

        }
        return state;
    }


    // 2023/10/24 爬取不存在的库
    // 在解析pom的时候调用
    // 以及获取更高版本的时候调用
    // 构建dependencyTree的时候
    /**
     * 检查库是否存在于t_lib中
     * 不存在 爬取
     *
     * @param groupId
     * @param artifactId
     * @param version
     */
    public void crawlLib(String groupId, String artifactId, String version) {
        // 如果不存在 调用爬取逻辑
        // 1. 先爬groupId & artifactId页面
        // 2. 再爬version详细页面
        if (!queryFromT_lib(groupId, artifactId, version)) {
            String libLink = libUrl + groupId + "/" + artifactId;
            crawlLibAllVersionsLink(libLink, groupId, artifactId);
        }
        //如果存在 看下accessed的状态
        else {
            // 先获取libId
            int libId = getLibId(groupId, artifactId, version);
            if (getAccessState(libId) != 1) {
                // 如果accessed 不为1，说明漏洞信息还没有爬取
                crawlVersionDetails(groupId, artifactId, version);
            }
            //如果accessed 为1，那么没问题
        }
    }

    private static final String libUrl = "https://mvnrepository.com/artifact/";


    /**
     * 爬取artifact页，获取usages&date等信息，
     * 并且插入t_lib,生成libId
     * 并且继续爬取version页
     *
     * @param groupId
     * @param artifactId
     * @param version1
     */
    public void crawlArtifactLink(String groupId, String artifactId, String version1) {
        String libLink = libUrl + groupId + "/" + artifactId;
        Response response = HttpUtil.synGetHttp(libLink);
        if (response != null) {
            //得到html代码
            String html = null;
            try {
                html = response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //格式化解析html
            Document doc = Jsoup.parse(html);
            //根据class标签名获取到版本号
            Elements tbodys = doc.body().getElementsByTag("tbody");
            for (Element body : tbodys) {
                Elements b = body.getElementsByClass("vbtn");
                if (b.size() == 0) { //不存在vbtn，跳过
                    continue;
                } else {
                    Elements trs = body.getElementsByTag("tr");
                    for (Element e : trs) {
                        String version = e.getElementsByClass("vbtn").text();
                        // 定位到 该版本的号的位置
                        if (version.equals(version1)) {
                            Elements es = e.getElementsByClass("pbt");
                            Elements e_usage = es.get(0).select("a[href]");
                            // 获取使用量
                            String usage = e_usage.text();
                            // 获取日期
                            String date = e.getElementsByClass("date").text();
                            String versionLink = libLink + "/" + version;
                            // 插入usage & date信息
                            insertLinkIntoT_lib(groupId, artifactId, version, usage, date, versionLink);
                            // 2023/10/26 继续爬取详情页
                            crawlVersionDetails(groupId, artifactId, version1);
                            break;
                        }
                    }
                    // 获取完信息后跳过
                    break;
                }
            }
        }

    }

    /**
     * 爬取lib详情页，抓取vuls, deps信息
     * pre：这个lib已经在t_lib中
     *
     * @param groupId
     * @param artifactId
     * @param version
     */
    public void crawlVersionDetails(String groupId, String artifactId, String version) {
        String link = libUrl + groupId + '/' + artifactId + '/' + version;
        // 获取libId
        int libId = getLibId(groupId, artifactId, version);
        // 爬取链接
        Response response = HttpUtil.synGetHttp(link);
        //得到html代码
        String html = null;
        //链接为null
        if (response != null) {
            try {
                html = response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //格式化解析html
            Document doc = Jsoup.parse(html);
            String CVE_id = "";
            // vul不为空的话 加入libid_vulid对应列表
            int vulNum = 0;
            if (doc.body().getElementsByClass("vuln").size() != 0) {
                Elements vuls = doc.body().getElementsByClass("vuln");
                for (Element v : vuls) {
                    CVE_id = v.text();
                    if (CVE_id.contains("CVE")) {
                        if (!queryFromT_vul(CVE_id)) {
                            insertIntoT_vul(CVE_id);
                        }
                        insertIntoT_lib_vul(libId, CVE_id);
                        vulNum++;
                    }
                }
            }
            // 2023/10/26 更新t_lib中的vuls的数量
            updateLibVuLNums(vulNum, libId);
            String c_deps = "";
            Elements tables = doc.body().getElementsByClass("version-section");
            String compile = tables.get(0).getElementsByTag("h2").text();
            int compile_num = Integer.parseInt(compile.substring(compile.indexOf('(') + 1, compile.indexOf(')')));
            if (compile_num == 0) { //没有传递依赖
                c_deps = null;
            } else {
                Elements trs = tables.get(0).getElementsByTag("tr");
                for (int i = 1; i < trs.size(); i++) {
                    Element td = trs.get(i);
                    Elements tds = td.getElementsByTag("td");
                    //groupId & artifactId在td[2]
                    Element info = tds.get(2);
                    if (info.select("a[href]").size() != 0) {
                        Elements idInfo = info.select("a[href]");
                        if (idInfo.size() < 2) {
                            break; // artifact不是href，说明optional，屏蔽掉
                        } else {
                            String groupId1 = idInfo.get(0).text();
                            String artifactId1 = idInfo.get(1).text();
                            //version -- td[3]
                            Element verElement = tds.get(3);
                            String version1 = verElement.text();
                            if (!queryFromT_lib(groupId1, artifactId1, version1)) {
                                crawlArtifactLink(groupId1, artifactId1, version1);
                            }
                            // 获取依赖库的id
                            int dlibId = getLibId(groupId, artifactId, version);
                            insertIntoT_slib_tlib(libId, dlibId);
                        }

                    }
                }
            }
            updateLibAccess(libId, 1);
        } else {
            System.out.println("网页获取失败,404");
            updateLibAccess(libId, 0);
        }
    }

    /**
     * 爬取该artfact链接下所有库版本（限制30）
     * 加入t_lib
     *
     * @param libLink
     * @param groupId
     * @param artifactId
     */
    public void crawlLibAllVersionsLink(String libLink, String groupId, String artifactId) {
        // 不存在 爬取
        if(!isExistInT_artifact(libLink)) {
            Response response = HttpUtil.synGetHttp(libLink);
            if (response != null) {
                //得到html代码
                String html = null;
                try {
                    html = response.body().string();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //格式化解析html
                Document doc = Jsoup.parse(html);
                //根据class标签名获取到版本号
                Elements tbodys = doc.body().getElementsByTag("tbody");
                for (Element body : tbodys) {
                    Elements b = body.getElementsByClass("vbtn");
                    if (b.size() == 0) { //不存在vbtn，跳过
                        continue;
                    } else {
                        Elements trs = body.getElementsByTag("tr");
                        int num = 1;
                        for (Element e : trs) {
                            String version = e.getElementsByClass("vbtn").text();
                            Elements es = e.getElementsByClass("pbt");
                            Elements e_usage = es.get(0).select("a[href]");
                            String usage = e_usage.text();
                            if (usage == null) {
                                //如果没有人使用，看下一个版本
                                continue;
                            }
                            String date = e.getElementsByClass("date").text();
                            String versionLink = libLink + "/" + version;
//            crawlLibDetails(groupId, artifactId, version, usage, date);
                            // 插入usage & date信息
                            insertLinkIntoT_lib(groupId, artifactId, version, usage, date, versionLink);
                            // 获取详情
                            crawlVersionDetails(groupId, artifactId, version);
                            num++;
                            if (num > 30) break; //库版本超过30退出
                        }
                        // 获取完信息后跳过
                        break;
                    }
                }
            }
        }
    }


}
