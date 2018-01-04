package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/** the tree class that used as a hash map to store all the files of a commit.
 * @author Fansheng Cheng
 */
public class Trees implements Serializable {
    /** A tree class constructor. */
    Trees() {
        tree = new HashMap<>();
        addStage();
        if (Utils.plainFilenamesIn(".gitlet/object/trees").size() != 0) {
            addLastcommit();
        }
    }

    /** Add whatever is in staging area(index directory).
     *  after add the files, the files in staging area should be deleted.
     *  Possible error: file address need to include 'gitlet/', otherwise
     *  it's not valid, and will be treated as not normal file(directory).
     */
    void addStage() {
        List<String> filenames = Utils.plainFilenamesIn(".gitlet/index");
        for (String name : filenames) {
            File temp = new File(".gitlet/index/" + name);
            String shacode = Utils.sha1((Object) Utils.readContents(temp));
            tree.put(name, shacode);
            File object = new File(".gitlet/object/" + shacode);
            Utils.writeContents(object, Utils.readContents(temp));
            if (!temp.delete()) {
                System.out.println("deletion fails");
            }
        }
    }

    /** add the files from last commit's tree.
     *  this will take all the files from previous commit's tree
     *  except for the files that already been added by add_stage()
     */
    void addLastcommit() {
        Trees preTree = Rungit.headTrees();
        boolean add = true;
        for (String key : preTree.tree.keySet()) {
            for (String currentkey : tree.keySet()) {
                if (currentkey.equals(key)) {
                    add = false;
                }
            }
            for (String untrack : Utils.plainFilenamesIn(".gitlet/removal")) {
                if (untrack.equals(key)) {
                    add = false;
                }
            }
            if (add) {
                tree.put(key, preTree.tree.get(key));
            } else {
                add = true;
            }
        }
        for (String i : Utils.plainFilenamesIn(".gitlet/removal")) {
            (new File(".gitlet/removal" + i)).delete();
        }
    }

    /** Given a file's name, check its sha1 code that stored in this tree.
     * @param key File's name.
     * @return  FIle's sha1 code.
     */
    public String checksha1code(String key) {
        return tree.containsKey(key) ? tree.get(key) : null;
    }

    /** Check if a file's name exists in this tree.
     * @param name file's name to be checked.
     * @return true if this tree contains this file name, false otherwise.
     */
    public boolean checkfilename(String name) {
        return tree.containsKey(name);
    }

    /**
     * @param filename is key of hash map.
     * @return the sha code of key filename.
     */
    public String getfile(String filename) {
        return tree.get(filename);
    }

    /** @return return all the keys(file names).
     */
    public Set<String> getfiles() {
        return tree.keySet();
    }
    /** Check whether a file in this tree, has anything different to
     * a file going to be added to staging area.
     * @param filename input file's name as a key of hash map.
     * @param shacode input file's sha1 code to compare with sha1 code
     *                that already stored in hash map.
     * @return true if a same input sha1 code is the same as exist sha1 code,
     *         false otherwise.
     */
    boolean checkIdenticalfile(String filename, String shacode) {
        return tree.get(filename).equals(shacode);
    }

    /** A Hash map recorded file's name and its sha1 code in this tree. */
    private HashMap<String, String> tree;
}
