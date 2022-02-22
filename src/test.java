import ir.PostingsList;

import java.lang.reflect.Array;
import java.util.*;

public class test {
    public static void main(String[] args) {
        String line = "1;2,3,4,";

        int index = line.indexOf( ";" );

        StringTokenizer tok = new StringTokenizer( line.substring(index+1), "," );

        HashMap<Integer, HashMap<Integer, Boolean>> link = new HashMap<>();
        int fromDoc = index;

        while (tok.hasMoreTokens()) {
//            System.out.println("123");
            String otherTitle = tok.nextToken();
            int otherDoc = Integer.parseInt(otherTitle);

            if (link.get(fromDoc) == null) {
                link.put(fromDoc, new HashMap<Integer,Boolean>());
            }

            if (link.get(fromDoc).get(otherDoc) == null) {
                link.get(fromDoc).put( otherDoc, true );
            }
        }

        for (Map.Entry<Integer, HashMap<Integer, Boolean>> entry : link.entrySet()) {
            System.out.println(entry.getKey());
            for (Map.Entry<Integer, Boolean> entry1 : entry.getValue().entrySet()) {
                System.out.println(entry1);
            }
            System.out.println("---");
        }
    }
}
