import database.JDBC;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import util.HttpUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class crawlAllVersionDetails {
    private static final String libUrl = "https://mvnrepository.com/artifact/";

    public static void main(String[] args) {
        JDBC jdbc = new JDBC();
        HashMap<Integer, String> libLinks = jdbc.getAllUnAccessedLinksFromT_lib();
        for (Map.Entry<Integer, String> libLink : libLinks.entrySet()) {
            int libId = libLink.getKey();
            System.out.println("爬取库：" + libId);
            String link = libLink.getValue();
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
                // TODO: 2023/10/9 在每个weblink下获取其vulnerability，写入t_lib_vul & t_vul
                // vul不为空的话 加入libid_vulid对应列表
                if (doc.body().getElementsByClass("vuln").size() != 0) {
                    Elements vuls = doc.body().getElementsByClass("vuln");
                    for (Element v : vuls) {
                        CVE_id = v.text();
                        if (CVE_id.contains("CVE")) {
                            // TODO: 2023/10/10 若数据库中不存在该CVE, 将CVE信息插入t_vul
                            if (!jdbc.queryFromT_vul(CVE_id)) {
                                jdbc.insertIntoT_vul(CVE_id);
                            }
                            // TODO: libid和vno的对应
                            jdbc.insertIntoT_lib_vul(libId, CVE_id);
                        }
                    }
                }

                // TODO: 获取传递依赖，赋值给c_deps
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
                                String groupId = idInfo.get(0).text();
                                String artifactId = idInfo.get(1).text();
                                //version -- td[3]
                                Element verElement = tds.get(3);
                                String version = verElement.text();
                                // TODO: 2023/10/10 首先查一下这个库在t_lib中是否存在，如果不存在，爬取，存储
                                if (!jdbc.queryFromT_lib(groupId, artifactId, version)) {
                                    String artifactLink = libUrl + groupId + '/' + artifactId;
                                    // TODO: 2023/10/10 如果该artifact不存在 加入artifact
                                    if (!jdbc.isExistInT_artifact(artifactLink)) {
                                        jdbc.insertIntoT_artifact(groupId, artifactId, artifactLink);
                                        // TODO: 2023/10/10 加入Lib
                                        crawlAllVersions crawlAllVersions = new crawlAllVersions();
                                        crawlAllVersions.crawlLibAllVersionsLink(artifactLink, groupId, artifactId);
                                    }
                                }
                                // 获取依赖库的id
                                int dlibId = jdbc.getLibId(groupId, artifactId, version);
                                // TODO: 2023/10/9 在每个weblink下获取其直接依赖（一层），写入 t_lib_deplib
                                jdbc.insertIntoT_slib_tlib(libId, dlibId);
                            }

                        }
                    }
                }
                // TODO: 2023/10/10 解析完了之后，将对应lib标为accessed
                jdbc.updateLibAccess(libId, 1);
            } else {
                System.out.println("网页获取失败,404");
                jdbc.updateLibAccess(libId, 0);
            }
        }

    }

    //    public void crawlNewLib(String groupId,String artifactId, String version){
//        String link = libUrl + '/' +groupId + '/' + artifactId + '/' + version;
//        jdbc.insertLinkIntoT_lib(groupId, artifactId, version, usage, date, versionLink);
//
//    }
    // TODO: 2023/10/21 爬取groupId/artfactId/version下的详细信息
//    public void crawlDetail(String groupId, String artifactId, String version) {
//        JDBC jdbc = new JDBC();
//        String link = libUrl + groupId + '/' + artifactId + '/' + version;
//        // 爬取链接
//        Response response = HttpUtil.synGetHttp(link);
//        //得到html代码
//        String html = null;
//        //链接为null
//        if (response != null) {
//            try {
//                html = response.body().string();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            //格式化解析html
//            Document doc = Jsoup.parse(html);
//            String CVE_id = "";
//
//            // TODO: 2023/10/21 获取Date 和usage?
//
//            // vul不为空的话 加入libid_vulid对应列表
//            if (doc.body().getElementsByClass("vuln").size() != 0) {
//                Elements vuls = doc.body().getElementsByClass("vuln");
//                for (Element v : vuls) {
//                    CVE_id = v.text();
//                    if (CVE_id.contains("CVE")) {
//                        // TODO: 2023/10/10 若数据库中不存在该CVE, 将CVE信息插入t_vul
//                        if (!jdbc.queryFromT_vul(CVE_id)) {
//                            jdbc.insertIntoT_vul(CVE_id);
//                        }
//                        // TODO: libid和vno的对应
////                        jdbc.insertIntoT_lib_vul(libId, CVE_id);
//                    }
//                }
//            }
//
//            // TODO: 获取传递依赖，赋值给c_deps
//            String c_deps = "";
//            Elements tables = doc.body().getElementsByClass("version-section");
//            String compile = tables.get(0).getElementsByTag("h2").text();
//            int compile_num = Integer.parseInt(compile.substring(compile.indexOf('(') + 1, compile.indexOf(')')));
//            if (compile_num == 0) { //没有传递依赖
//                c_deps = null;
//            } else {
//                Elements trs = tables.get(0).getElementsByTag("tr");
//                for (int i = 1; i < trs.size(); i++) {
//                    Element td = trs.get(i);
//                    Elements tds = td.getElementsByTag("td");
//                    //groupId & artifactId在td[2]
//                    Element info = tds.get(2);
//                    if (info.select("a[href]").size() != 0) {
//                        Elements idInfo = info.select("a[href]");
//                        if (idInfo.size() < 2) {
//                            break; // artifact不是href，说明optional，屏蔽掉
//                        } else {
//                            String groupId = idInfo.get(0).text();
//                            String artifactId = idInfo.get(1).text();
//                            //version -- td[3]
//                            Element verElement = tds.get(3);
//                            String version = verElement.text();
//                            // TODO: 2023/10/10 首先查一下这个库在t_lib中是否存在，如果不存在，爬取，存储
//                            if (!jdbc.queryFromT_lib(groupId, artifactId, version)) {
//                                String artifactLink = libUrl + groupId + '/' + artifactId;
//                                // TODO: 2023/10/10 如果该artifact不存在 加入artifact
//                                if (!jdbc.isExistInT_artifact(artifactLink)) {
//                                    jdbc.insertIntoT_artifact(groupId, artifactId, artifactLink);
//                                    // TODO: 2023/10/10 加入Lib
//                                    crawlAllVersions crawlAllVersions = new crawlAllVersions();
//                                    crawlAllVersions.crawlLibAllVersionsLink(artifactLink, groupId, artifactId);
//                                }
//                            }
//                            // 获取依赖库的id
//                            int dlibId = jdbc.getLibId(groupId, artifactId, version);
//                            // TODO: 2023/10/9 在每个weblink下获取其直接依赖（一层），写入 t_lib_deplib
//                            jdbc.insertIntoT_slib_tlib(libId, dlibId);
//                        }
//
//                    }
//                }
//            }
//            // TODO: 2023/10/10 解析完了之后，将对应lib标为accessed
//            jdbc.updateLibAccess(libId, 1);
//        } else {
//            System.out.println("网页获取失败,404");
//            jdbc.updateLibAccess(libId, 0);
//        }
//    }
}
