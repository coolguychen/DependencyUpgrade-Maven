package core;

import depModel.Dependency;
import depModel.DependencyTree;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class RecommendSolution {

    public static List<Dependency> sortByUsage(List<Dependency> dependencies) {
        Collections.sort(dependencies, new Comparator<Dependency>() {
            @Override
            public int compare(Dependency d1, Dependency d2) {
                if(d1.getUsage().equals(d2.getUsage())) {
                    // 如果相等 按照深度/声明顺序
                    if (d1.getDepth() == d2.getDepth()) {
                        return d1.getId() - d2.getDepth();
                    } else {
                        return d1.getDepth() - d2.getDepth();
                    }
                }else{
                    return d2.getUsage().compareTo(d1.getUsage());
                }
            }
        });
        return dependencies;
    }


    public static List<Dependency> sortByDate(List<Dependency> dependencies) {
        Collections.sort(dependencies, new Comparator<Dependency>() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);

            @Override
            public int compare(Dependency d1, Dependency d2) {
                try {
                    String date_1 = d1.getPublishDate();
                    String date_2 = d2.getPublishDate();
                    Date date1 = dateFormat.parse(date_1);
                    Date date2 = dateFormat.parse(date_2);
                    if (date1.equals(date2)) {
                        // 如果相等 按照深度/声明顺序
                        if (d1.getDepth() == d2.getDepth()) {
                            return d1.getId() - d2.getDepth();
                        } else {
                            return d1.getDepth() - d2.getDepth();
                        }
                    } else {
                        return date2.compareTo(date1);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                return 0;
            }
        });
        return dependencies;
    }

    public static List<Dependency> sortByVulNums(List<Dependency> dependencies) {
        Collections.sort(dependencies, new Comparator<Dependency>() {
            @Override
            public int compare(Dependency d1, Dependency d2) {
                if(d1.getVulNum() == d2.getVulNum()) {
                    if (d1.getDepth() == d2.getDepth()) {
                        return d1.getId() - d2.getDepth();
                    } else {
                        return d1.getDepth() - d2.getDepth();
                    }
                }
                else {
                    return d2.getVulNum() - d1.getVulNum();
                }
            }
        });
        return dependencies;
    }
}
