import ir.PostingsList;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class test {
    public static void main(String[] args) {
        StringBuilder str = new StringBuilder("");
        str.append("123 1,2,3,\n");
        str.append("456 4,5,6,\n");
        str.append("789 7,8,9,\n");


        String a = str.toString();

//        String[] postingsList = a.split("\n");
//
//        for (String item : postingsList) {
//            String[] docID = item.split(" ");
//            System.out.println(docID[0]); // docID;
//            String[] offset = docID[1].split(",");
//            for (String i : offset) {
//                System.out.println(i);
//            }
//
//        }

//        for (String s : postingsList) {
//            String[] arry = s.split(" ");
//            //System.out.println(arry[0]+","+arry[1]);
//            result.insert(Integer.parseInt(arry[0]), 0.0, Integer.parseInt(arry[1]));
//        }

    }
}
