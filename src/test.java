import ir.PostingsList;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class test {
    public static void main(String[] args) {
        HashMap<String, ArrayList<Integer>> hashMap = new HashMap<String, ArrayList<Integer>>();
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        arrayList.add(123);
        arrayList.add(456);
        hashMap.put("abc", arrayList);

        System.out.println(hashMap.get("abc").contains(567));
    }
}
