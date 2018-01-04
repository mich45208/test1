package gitlet;

import ucb.junit.textui;
import org.junit.Test;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;


/** The suite of all JUnit tests for the gitlet package.
 *  @author Fansheng Cheng
 */
public class UnitTest {

    /** Run the JUnit tests in the loa package. Add xxxTest.class entries to
     *  the arguments of runClasses to run other JUnit tests. */
    public static void main(String[] ignored) {
        textui.runClasses(UnitTest.class);
    }

    /** A dummy test to avoid complaint. */
    @Test
    public void placeholderTest() {

    }


    @Test
    public void fileuseTest() {
        File copy1 = new File("gitlet" + File.separator
                + "index" + File.separator + "copy.txt");
        File test = new File("gitlet/.gitlet");
        test.mkdir();
    }


    @Test
    public void testHead() {
        File head = new File("gitlet/Head");
        List<String> names = Utils.plainFilenamesIn("gitlet/object/commit");
        File commitfile = new File("gitlet" + File.separator
                + "object" + File.separator
                + "commit" + File.separator + names.get(0));
        byte[] t = "gitlet/refs/master".getBytes();
        String k = new String(t);
        System.out.println(k);
        Commit recover = (Commit) Utils.deserialize(commitfile.getPath());
    }

    @Test
    public void messagetest() {
        String[] mic = new String[]{"i", "am", "mic"};
        StringBuilder builder = new StringBuilder();
        for (String s : mic) {
            builder.append(s + " ");
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append(".");
        System.out.println(builder);
    }

    @Test
    public void datetest() {
        Calendar cal = Calendar.getInstance();
        ZonedDateTime zd = ZonedDateTime.now();
        System.out.println(zd.getHour() + ":" + zd.getMinute()
                + ":" + zd.getDayOfWeek());
        System.out.println(cal.getTimeZone().getOffset
                (new Date().getTime()) / 1000 / 60 / 60);
        System.out.println(String.format("%02d", 8));
        System.out.println(Commit.createtime());
    }

    @Test
    public void datetest2() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d k:mm:ss yyyy Z");
        sdf.setTimeZone(TimeZone.getDefault());
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.set(1970, Calendar.JANUARY, 1, 0, 0, 0);
        Date date = cal.getTime();
        System.out.println(sdf.format(date));
        sdf.setTimeZone(TimeZone.getDefault());
        System.out.println(sdf.format(calendar.getTime()));
    }

    @Test
    public void deleterecord() {
        File folder = new File(".gitlet");
        deletehelper(folder);
    }

    void deletehelper(File file) {
        if (file.isFile()) {
            file.delete();
        } else {
            if (file.listFiles().length == 0) {
                file.delete();
                return;
            }
            for (File i : file.listFiles()) {
                deletehelper(i);
            }
            file.delete();
        }
    }

    @Test
    public void copyfiletest() {
        File source = new File("temp_repository/source.txt");
        Utils.serialize("temp_repository/testcopy" + "serialed", source);
        File target = new File("temp_repository/testcopy/target.txt");
        Utils.writeContents(target, Utils.readContents(source));
    }

    @Test
    public void hashTest() {
        HashMap<Integer, String> hash = new HashMap<>();
        hash.put(1, "lol");
        hash.put(2, "haha");
        System.out.println(hash.get(3));
        for (int i : hash.keySet()) {
            System.out.println(i);
        }
    }

    @Test
    public void setRetainTest() {
        HashMap<Integer, Integer> h1 = new HashMap<>();
        HashMap<Integer, Integer> h2 = new HashMap<>();
        h1.put(1, 11);
        h1.put(2, 12);
        h1.put(3, 13);
        h2.put(2, 22);
        h2.put(4, 24);
        h2.put(1, 21);
        h1.keySet().retainAll(h2.keySet());
        System.out.println(h1);
        System.out.println(h1.get(5) == h2.get(5));
    }

    @Test
    public void writtenOverideTest() {
        File foo = new File("foo.txt");
        StringBuilder builder = new StringBuilder();
        builder.append(new String(Utils.readContents(foo)));
        System.out.println(builder);
    }

    @Test
    public void collectionsortTest() {
        List<String> result = new ArrayList<>();
        result.add("cat");
        result.add("high");
        result.add("combo");
        result.add("juice");
        Collections.sort(result);
        System.out.println(result);
    }

    @Test
    public void fileTest() {
        File delete = new File("todelete");
        delete.delete();
    }
}


