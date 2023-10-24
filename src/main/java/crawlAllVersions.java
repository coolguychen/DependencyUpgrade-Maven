import database.JDBC;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import util.HttpUtil;

import java.io.IOException;
import java.util.List;

public class crawlAllVersions {
    public static void main(String[] args) {
        JDBC jdbc = new JDBC();
        List<String[]> artifactLinks = jdbc.queryFromT_artifact();
        // 获取已经爬过的
        List<String> groupIds = jdbc.queryGroupIdFromT_lib();
        for (String[] info : artifactLinks) {
            // 访问该链接下的所有版本信息
            String groupId = info[0];
            String artifactId = info[1];
            String link = info[2];
            if (groupIds.contains(groupId)) {
                //
            } else {
                crawlLibAllVersionsLink(link, groupId, artifactId);
            }
        }

    }

    /**
     * 爬取所有artifact下前30个版本的链接
     *
     * @param libLink
     * @param groupId
     * @param artifactId
     */
    public static void crawlLibAllVersionsLink(String libLink, String groupId, String artifactId) {
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
                        JDBC jdbc = new JDBC();
                        jdbc.insertLinkIntoT_lib(groupId, artifactId, version, usage, date, versionLink);
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
