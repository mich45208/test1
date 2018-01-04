package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


/** The commit class that can be serialized.
 * @author Fansheng Cheng
 */
public class Commit implements Serializable {
    /** Constructor of commit, this will point to a parent
     *  commit that input as parameter.
     *
     * @param parentSha PARENT'S SHA
     * @param treeSha TREE'S SHA
     * @param message message that user input.
     */

    Commit(String parentSha, String treeSha, String message) {
        _date = createtime();
        _parentSHA1 = parentSha;
        _trees = treeSha;
        _message = message;
    }

    /** constructor of commit, this is used for git init,
     *  when there is no commit, this will be the first commit.
     *
     * @param treeSha tree's sha
     * @param message message
     */

    Commit(String treeSha, String message) {
        _date = createtime();
        _trees = treeSha;
        _message = message;
    }

    /**
     * @param mergedParentSha 2nd parent from merge case
     * @param parentSha normal parent
     * @param treeSha normal tree
     * @param message normal message
     */
    Commit(String mergedParentSha, String parentSha,
           String treeSha, String message) {
        _date = createtime();
        _mergedparentSha = mergedParentSha;
        _parentSHA1 = parentSha;
        _trees = treeSha;
        _message = message + ".";
    }

    /** @return trees stored in this commit. */
    public String gettreeSha() {
        return _trees;
    }

    /** @return parent commit's sha1 code, */
    public String getParentSHA1() {
        return _parentSHA1;
    }

    /** @return mergeparent's sha. */
    public String getMergedparentSha() {
        return _mergedparentSha;
    }

    /** @return the date of creating this commit. */
    public String getDate() {
        return _date;
    }

    /** @return the message of this commit. */
    public String getMessage() {
        return _message;
    }

    /** The method that create time for this commit.
     * @return string of created time.
     */
    static String createtime() {
        SimpleDateFormat sdf = new SimpleDateFormat(
                "EEE MMM d kk:mm:ss yyyy Z");
        sdf.setTimeZone(TimeZone.getTimeZone("PST"));
        Calendar cal = Calendar.getInstance();
        return sdf.format(cal.getTime());
    }

    /**
     * @return the time of initial commit's created time.
     */
    public String initialtime() {
        SimpleDateFormat sdf = new SimpleDateFormat(
                "EEE MMM d kk:mm:ss yyyy Z");
        sdf.setTimeZone(TimeZone.getTimeZone("PST"));
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.set(INITIALYEAR, Calendar.JANUARY, 1, 0, 0, 0);
        Date date = cal.getTime();
        return sdf.format(date);
    }

    /** The parent's sha code. */
    private String _parentSHA1;

    /** The tree's sha code. */
    private String _trees;

    /** The message of current commit. */
    private String _message;

    /** The date of creating this date. */
    private String _date;

    /** The merged parent's sha. */
    private String _mergedparentSha;

    /** The initial year of java's creating. */
    private static final int INITIALYEAR = 1970;
}
