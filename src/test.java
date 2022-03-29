import ir.PostingsList;

import java.lang.reflect.Array;
import java.util.*;

public class test {

    public static String getFileName(String path) {
        String result = "";
        StringTokenizer tok = new StringTokenizer( path, "\\/" );
        while ( tok.hasMoreTokens() ) {
            result = tok.nextToken();
        }
        return result;
    }

    public static void main(String[] args) {
        String str = getFileName("/Users/zhangziheng/OneDrive/KTH/SEandIR_ZihengZhang/src/assignment2/pagerank/davis_top_30.txt");
        System.out.println(str);

        HashMap<Integer, Double> hashMap = new HashMap<>();
        hashMap.put(1, 1.2);
        hashMap.put(2, 1.5);

        HashMap<Integer, Double> test = new HashMap<>(hashMap);

        System.out.println(test);
    }
}
