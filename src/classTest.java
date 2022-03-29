import java.util.ArrayList;

public class classTest {
    int docID = 0;
    double score = 0;
    ArrayList<Integer> offset = new ArrayList<Integer>();

    public classTest(int docID, double score, int offset) {
        this.docID = docID;
        this.score = score;
        this.offset.add(offset);
    }
}