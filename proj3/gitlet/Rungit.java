package gitlet;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

/**CS 61B 2017 fall project3-gitlet.
 * @author Fansheng Cheng.
 * The class where git runs different command. */
public class Rungit {

    /** the command that stores all the possible commands. */
    private Command _command;

    /** the condition that used to check if a commit of merge is made. */
    private boolean _mergecommited = true;

    /** the constructor of Rungit.
     * @param com the command that user inputs.
     */
    Rungit(Command com) {
        _command = com;
    }

    /** The process of running git, including different
     * cases of wrong command. */
    void process() {
        if (!_commands.containsKey(_command.getCommand())) {
            System.out.println("No command with that name exists.");
            return;
        }
        if (!_command.getCommand().equals("init")
                && !(new File(".gitlet")).isDirectory()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }
        if (this.checkCommand()) {
            _commands.get(_command.getCommand()).accept(_command.getOperands());
        }
    }

    /** Initialize a gitlet repository.
     * operands should be null, start with one commit with single branch
     * @param operands null
     */
    void doInit(String[] operands) {
        if (operands.length == 0) {
            File gitletDirectory = new File(".gitlet");
            gitletDirectory.mkdir();
            File index = new File(".gitlet/index");
            index.mkdir();
            File object = new File(".gitlet/object");
            object.mkdir();
            File refs = new File(".gitlet/refs");
            refs.mkdir();
            File objectCommit = new File(".gitlet/object/commit");
            objectCommit.mkdir();
            File objectTrees = new File(".gitlet/object/trees");
            objectTrees.mkdir();
            File removal = new File(".gitlet/removal");
            removal.mkdir();

            if (Utils.plainFilenamesIn(".gitlet/object/commit").size() != 0) {
                System.out.println("A Gitlet version-control system already "
                        + "exists in the current directory.");
                return;
            }

            Trees initialtree = new Trees();
            String initialtreeSha =
                    serialShaname(".gitlet/object/trees/", initialtree);
            Commit startcommit = new Commit(initialtreeSha, "initial commit");

            String startcommitSha =
                    serialShaname(".gitlet/object/commit/", startcommit);
            File head = new File(".gitlet/Head");
            File master = new File(".gitlet/refs/master");

            Utils.writeContents(head, ".gitlet/refs/master".getBytes());
            Utils.writeContents(master, startcommitSha.getBytes());
        } else {
            System.out.println("wrong input, nothing should follow by 'init'");
        }

    }

    /** The adding of files to gitlet staging area.
     * @param operands the file to be added, maximum size is 1.
     */
    void doAdd(String[] operands) {
        if (operands.length != 1) {
            System.out.println("wrong format after add");
            return;
        }
        String addname = operands[0];
        boolean validOperand = false;
        List<String> currentfiles = Utils.plainFilenamesIn(".");
        for (String i : currentfiles) {
            if (i.equals(addname)) {
                validOperand = true;
                break;
            }
        }
        if (!validOperand) {
            System.out.println("File does not exist.");
        } else {
            File toadd = new File(addname);
            byte[] content = Utils.readContents(toadd);
            String shacode = Utils.sha1((Object) content);
            String headTreesha = headCommit().gettreeSha();
            Trees headTree = (Trees)
                    Utils.deserialize(".gitlet/object/trees/" + headTreesha);
            String oldtreeSha1 = headTree.checksha1code(addname);

            if (oldtreeSha1 == null || !oldtreeSha1.equals(shacode)) {
                File copyToadd = new File(".gitlet/index/" + addname);
                Utils.writeContents(copyToadd, content);
            }
        }
        if (Utils.plainFilenamesIn(".gitlet/removal").contains(addname)) {
            (new File(".gitlet/removal/" + addname)).delete();
        }
    }

    /** The commit that a user can make to store a snap shoot of current files.
     * @param operands the message that user can give to this commit.
     */
    void doCommit(String[] operands) {
        if (operands.length == 0 || operands[0].equals("")) {
            System.out.println("Please enter a commit message.");
            return;
        }
        if (Utils.plainFilenamesIn(".gitlet/index").size() == 0
                && Utils.plainFilenamesIn(".gitlet/removal").size() == 0) {
            System.out.println("No changes added to the commit.");
            if (operands[0].equals("Merged")) {
                _mergecommited = false;
            }
            return;
        }
        Trees tocommit = new Trees();
        String treeSha = serialShaname(".gitlet/object/trees/", tocommit);
        for (String toRemove : Utils.plainFilenamesIn(".gitlet/removal")) {
            doRm(new String[]{toRemove});
        }
        String headContent = new String(Utils.readContents
                (new File(".gitlet/Head")));
        String headParentSha = new String(
                Utils.readContents(new File(headContent)));

        String message = arraytostring(operands);
        Commit com;
        if (operands[0].equals("Merged")) {
            String mergeParentname = operands[1];
            String mergeParentsha = new String(Utils.readContents
                    (new File(".gitlet/refs/" + mergeParentname)));
            com = new Commit(mergeParentsha,
                    headParentSha, treeSha, message);
        } else {
            com = new Commit(headParentSha, treeSha, message);
        }
        String newSha = serialShaname(".gitlet/object/commit/", com);
        Utils.writeContents(new File(headContent), newSha.getBytes());
        removalClear();
    }

    /** clear all the files in removal directory. (Usually after a commit) */
    void removalClear() {
        for (String removalFile : Utils.plainFilenamesIn(".gitlet/removal")) {
            (new File(".gitlet/removal/" + removalFile)).delete();
        }
    }

    /** Git remove command: remove the files in either
     * staging area or working area.
     * @param operands file's name.
     */
    void doRm(String[] operands) {
        String rmName = operands[0];
        File staged = new File(".gitlet/index/" + rmName);
        if (staged.isFile()) {
            staged.delete();
            return;
        }
        if (headTrees().checkfilename(rmName)) {
            File rmWorking = new File(rmName);
            if (rmWorking.isFile()) {
                rmWorking.delete();
            }
            File rmUntrack = new File(".gitlet/removal/" + rmName);
            Utils.writeContents(rmUntrack, "for untrack use".getBytes());
            return;
        }
        System.out.println("No reason to remove the file.");
    }

    /** The log information that includes all the history
     * from current commit to initial commit.
     * @param operands null
     */
    void doLog(String[] operands) {
        Commit head = headCommit();
        File headFile = new File(".gitlet/Head");
        String headBranchPath = new String(Utils.readContents(headFile));
        File headBranch = new File(headBranchPath);
        String headSha1 = new String(Utils.readContents(headBranch));

        while (head.getParentSHA1() != null) {
            System.out.println("===");
            System.out.print("commit ");
            System.out.println(headSha1);

            if (head.getMergedparentSha() != null) {
                System.out.println("Merge: "
                        + head.getParentSHA1().substring(0, 7)
                        + " " + head.getMergedparentSha().substring(0, 7));
            }

            System.out.println("Date: " + head.getDate());
            System.out.println(head.getMessage());
            System.out.println();
            headSha1 = head.getParentSHA1();
            head = (Commit) Utils.deserialize
                    (".gitlet/object/commit/" + head.getParentSHA1());
        }
        System.out.println("===");
        System.out.print("commit ");
        System.out.println(headSha1);
        System.out.println("Date: " + head.initialtime());
        System.out.println(head.getMessage());
    }

    /** print out all the log information made so far.
     * @param operands null
     */
    void doGloballog(String[] operands) {
        List<String> allfile = Utils.plainFilenamesIn(".gitlet/object/commit");
        int num = 0;
        for (String i : allfile) {
            File commitFile = new File(".gitlet/object/commit/" + i);
            Commit commitObj = (Commit)
                    Utils.deserialize(commitFile.getPath());
            System.out.println("===");
            System.out.print("commit ");
            System.out.println(i);
            if (commitObj.getMergedparentSha() != null) {
                System.out.println("Merge: "
                        + commitObj.getParentSHA1().substring(0, 7)
                        + " "
                        + commitObj.getMergedparentSha().substring(0, 7));
            }
            if (commitObj.getParentSHA1() != null) {
                System.out.println("Date: " + commitObj.getDate());
            } else {
                System.out.println("Date: " + commitObj.initialtime());
            }
            System.out.println(commitObj.getMessage());
            num += 1;
            if (num != allfile.size()) {
                System.out.println(" ");
            }
        }
    }

    /** Find the commit ID from the message input by user.
     * @param operands message.
     */
    void doFind(String[] operands) {
        if (operands.length == 0) {
            System.out.println("please write message");
        }
        String message = operands[0];
        boolean findId = false;
        for (String i : Utils.plainFilenamesIn
                (".gitlet/object/commit")) {
            Commit com = (Commit)
                    Utils.deserialize(".gitlet/object/commit/" + i);
            if (message.equals(com.getMessage())) {
                System.out.println(i);
                findId = true;
            }
        }
        if (!findId) {
            System.out.println("Found no commit with that message.");
        }
    }

    /** Show all the status including staging area, removal area, modified files
     * and untracked files.
     * @param operands null
     */
    void doStatus(String[] operands) {
        System.out.println("=== Branches ===");
        String headPath = new String(
                Utils.readContents(new File(".gitlet/Head")));
        File headBranch = new File(headPath);
        System.out.println("*" + headBranch.getName());
        for (String i : Utils.plainFilenamesIn(".gitlet/refs")) {
            if (!i.equals(headBranch.getName())) {
                System.out.println(i);
            }
        }
        System.out.println("\n" + "=== Staged Files ===");
        for (String stage : Utils.plainFilenamesIn(".gitlet/index")) {
            System.out.println(stage);
        }
        System.out.println("\n" + "=== Removed Files ===");
        for (String removed : Utils.plainFilenamesIn(".gitlet/removal")) {
            System.out.println(removed);
        }
        System.out.println("\n"
                + "=== Modifications Not Staged For Commit ===");
        List<String> result = statusHelper();
        for (String i : result) {
            System.out.println(i);
        }
        System.out.println("\n" + "=== Untracked Files ===");
        for (String i : untrackedfile()) {
            System.out.println(i);
        }
    }

    /** Designed for 'Modifications Not Staged For Commit' case.
     * @return all the possible case's result list.
     */
    List<String> statusHelper() {
        List<String> result = new ArrayList<>();
        for (String workingFile
                : Utils.plainFilenamesIn(".")) {
            String fileSha = headTrees().getfile(workingFile);
            String workingSha = Utils.sha1(
                    (Object) Utils.readContents(new File(workingFile)));
            List<String> staging = Utils.plainFilenamesIn(".gitlet/index");
            if (fileSha != null && !fileSha.equals(workingSha)) {
                if (staging == null || !staging.contains(workingFile)) {
                    result.add(workingFile + " (modified)");
                }
            }
            if (staging != null && staging.contains(workingFile)) {
                String stagingSha = Utils.sha1((Object)
                        Utils.readContents(new File(
                                ".gitlet/index/" + workingFile)));
                if (!stagingSha.equals(workingSha)) {
                    result.add(workingFile + " (modified)");
                }
            }
        }
        for (String stagingFile
                : Utils.plainFilenamesIn(".gitlet/index")) {
            if (!Utils.plainFilenamesIn(".").contains(stagingFile)) {
                result.add(stagingFile + " (deleted)");
            }
        }
        for (String trackedfile : headTrees().getfiles()) {
            if (!Utils.plainFilenamesIn(".gitlet/removal").contains(trackedfile)
                    && !Utils.plainFilenamesIn(".").contains(trackedfile)) {
                result.add(trackedfile + " (deleted)");
            }
        }
        Collections.sort(result);
        return result;
    }

    /** The command of checkout: 3 different cases
     * checkout a file of current commit,
     *          a file of a given commit,
     *          all files from a given commit.
     * @param operands file name
     *                 or commit id and file name
     *                 or branch name
     */
    void doCheckout(String[] operands) {
        if (operands.length == 3 && !operands[1].equals("--")) {
            System.out.println("Incorrect operands.");
            return;
        }
        if (operands[0].equals("--")) {
            String checkoutFile = operands[1];
            Commit headCommit = headCommit();
            checkoutHelper(headCommit, checkoutFile);
        } else if (operands.length == 3 && operands[1].equals("--")) {
            String commitID = operands[0];
            String filename = operands[2];
            Commit foundCommit = null;
            String existId = idfoundhelper(Utils.plainFilenamesIn
                    (".gitlet/object/commit"), commitID);
            if (existId == null) {
                System.out.println("No commit with that id exists.");
            } else {
                foundCommit = (Commit)
                        Utils.deserialize(".gitlet/object/commit/" + existId);
                checkoutHelper(foundCommit, filename);
            }
        } else {
            if (untrackedfile().size() > 0) {
                System.out.println("There is an untracked "
                        + "file in the way; delete it or add it first.");
                return;
            }
            String branchName = operands[0];
            if (branchName.equals(headBranchName())) {
                System.out.println("No need to checkout the current branch.");
                return;
            } else {
                for (String stagefile
                        : Utils.plainFilenamesIn(".gitlet/index")) {
                    (new File(".gitlet/index/" + stagefile)).delete();
                }
            }
            checkoutBranch(branchName);
        }
    }

    /** A checkout branch case.
     *
     * @param branchName the branch's name to be checkout.
     */
    void checkoutBranch(String branchName) {
        boolean branchExist = false;
        Commit branchCommit = null;
        for (String branch : Utils.plainFilenamesIn(".gitlet/refs")) {
            if (branchName.equals(branch)) {
                File branchFile = new File(".gitlet/refs/" + branchName);
                String branchCommitSha = new String(
                        Utils.readContents(branchFile));
                branchCommit = (Commit) Utils.deserialize(
                        ".gitlet/object/commit/" + branchCommitSha);
                branchExist = true;
                break;
            }
        }
        if (branchExist) {
            String treeSha = branchCommit.gettreeSha();
            Trees tree = (Trees) Utils.deserialize
                    (".gitlet/object/trees/" + treeSha);
            for (String filename : tree.getfiles()) {
                checkoutHelper(branchCommit, filename);
            }
            for (String workareaFile : Utils.plainFilenamesIn(".")) {
                if (!tree.getfiles().contains(workareaFile)) {
                    (new File(workareaFile)).delete();
                }
            }
        } else {
            System.out.println("No such branch exists.");
            return;
        }
        File head = new File(".gitlet/Head");
        Utils.writeContents(head,
                (".gitlet/refs/" + branchName).getBytes());
    }

    /**
     * @param allID the ID list as source(all commit's sha, 40 length)
     * @param searchID the target ID(>=6 length)
     * @return the list of ID that at least has same 6 digit as searchID.
     */
    String idfoundhelper(List<String> allID, String searchID) {
        for (String id : allID) {
            if (id.substring(0, searchID.length()).equals(searchID)) {
                return id;
            }
        }
        return null;
    }

    /** helper for doCheckout: find the file in commit COM,
     *  and file name CHECKOUT_FILE.
     * then make a copy of that file to working directory(proj3)
     * if CHECKOUT_FILE doesn't exist in com's tree, print DNE message.
     * @param com the commit that you want to find the file.
     * @param checkoutFile the filename that you are going to check out.
     */
    void checkoutHelper(Commit com, String checkoutFile) {
        String treeSha = com.gettreeSha();
        Trees tree = (Trees) Utils.deserialize(
                ".gitlet/object/trees/" + treeSha);
        String filesha = tree.getfile(checkoutFile);

        if (filesha != null) {
            File underTree = new File(".gitlet/object/" + filesha);

            File fileTocheckout = new File(checkoutFile);
            Utils.writeContents(fileTocheckout, Utils.readContents(underTree));
        } else {
            System.out.println("File does not exist in that commit.");
        }
    }

    /**(Untracked file: the file in working directory,
     * but not in current commit or staging area).
     * @return a list that contains all the untracked files
     */
    List<String> untrackedfile() {
        List<String> result = new ArrayList<>();
        for (String i : Utils.plainFilenamesIn(".")) {
            if (!headTrees().getfiles().contains(i)
                    && !Utils.plainFilenamesIn(".gitlet/index").contains(i)) {
                result.add(i);
            }
        }
        return result;
    }

    /** make a branch on current commit.
     * @param operands branch's name.
     */
    void doBranch(String[] operands) {
        String branchName = operands[0];
        for (String branch : Utils.plainFilenamesIn(".gitlet/refs")) {
            if (branch.equals(branchName)) {
                System.out.println("A branch with that name already exists.");
                return;
            }
        }
        File branchFile = new File(".gitlet/refs/" + branchName);
        String headBranch = headBranchName();
        Utils.writeContents(branchFile, Utils.readContents
                (new File(".gitlet/refs/" + headBranch)));
    }

    /** Remove the branch that exists.
     * @param operands branch's name.
     */
    void doRmBranch(String[] operands) {
        String rmbranchName = operands[0];
        List<String> branchList = Utils.plainFilenamesIn(".gitlet/refs");
        if (!branchList.contains(rmbranchName)) {
            System.out.println("A branch with that name does not exist.");
        }
        if (headBranchName().equals(rmbranchName)) {
            System.out.println("Cannot remove the current branch.");
        }
        for (String branch : branchList) {
            if (branch.equals(rmbranchName)) {
                (new File(".gitlet/refs/" + rmbranchName)).delete();
                return;
            }
        }
    }

    /** Reset a commit to be in working area.
     * also move current head branch to be that commit
     * @param operands commit ID, can be short(>=6)
     */
    void doReset(String[] operands) {
        String commitID = operands[0];
        if (untrackedfile().size() != 0) {
            System.out.println("There is an untracked "
                    + "file in the way; delete it or add it first.");
            return;
        }
        int i = commitID.length();
        String existID = "";
        for (String id : Utils.plainFilenamesIn(".gitlet/object/commit")) {
            if (id.substring(0, i).equals(commitID)) {
                existID = id;
                break;
            }
        }
        if (existID.equals("")) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Utils.writeContents(new File(".gitlet/refs/"
                + headBranchName()), existID.getBytes());

        for (String workingFile : Utils.plainFilenamesIn(".")) {
            if (!headTrees().getfiles().contains(workingFile)) {
                (new File(workingFile)).delete();
            }
        }
        for (String staged : Utils.plainFilenamesIn(".gitlet/index")) {
            (new File(".gitlet/index/" + staged)).delete();
        }

        for (String trackedFile : headTrees().getfiles()) {
            byte[] content = Utils.readContents(new File(
                    ".gitlet/object/" + headTrees().getfile(trackedFile)));
            Utils.writeContents(new File(trackedFile), content);
        }

    }

    /** Merge a branch with current branch.
     *
     * @param operands the branch that current branch is merged from.
     */
    void doMerge(String[] operands) {
        if (untrackedfile().size() != 0) {
            System.out.println("There is an untracked"
                    + " file in the way; delete it or add it first.");
            return;
        }
        String branchName = operands[0];
        if (failure(branchName)) {
            return;
        }
        String splitCommitSha = splitCommitSha(branchName);
        File headBranch = new File(".gitlet/refs/" + headBranchName());
        File otherBranch = new File(".gitlet/refs/" + branchName);
        String branchSha = new String(Utils.readContents(otherBranch));
        String headSha = new String(Utils.readContents(headBranch));
        if (splitCommitSha.equals(branchSha)) {
            System.out.println("Given branch is "
                    + "an ancestor of the current branch.");
            return;
        }
        if (splitCommitSha.equals(headSha)) {
            Utils.writeContents(headBranch, Utils.readContents(otherBranch));
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        Commit splitCommit = (Commit) Utils.deserialize(
                ".gitlet/object/commit/" + splitCommitSha);
        Commit otherCommit = branchToCommit(branchName);
        String otherSha = new String(Utils.readContents(new File(
                ".gitlet/refs/" + branchName)));
        boolean isConflict = mergeHelper(splitCommit, otherCommit);
        doCommit(new String[]{"Merged",
            branchName, "into", headBranchName()});
        if (!_mergecommited) {
            for (String untrack : Utils.plainFilenamesIn(".gitlet/removal")) {
                (new File(".gitlet/removal/" + untrack)).delete();
            }
            return;
        }
        if (isConflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /** A merge helper to merge the files, in different cases.
     * @param splitCommit the commit at split point.
     * @param otherCommit the commit of branch point.
     * @return true if it is conflict case.
     */
    boolean mergeHelper(Commit splitCommit, Commit otherCommit) {
        boolean result = false;
        for (String filename : unionfiles(splitCommit, otherCommit)) {
            boolean headSplit = comparefile(
                    headCommit(), splitCommit, filename);
            boolean headOther = comparefile(
                    headCommit(), otherCommit, filename);
            boolean otherSplit = comparefile(
                    otherCommit, splitCommit, filename);
            if (headSplit && !otherSplit) {
                if (commitToTrees(otherCommit).getfile(filename) == null) {
                    File removal = new File(".gitlet/removal/" + filename);
                    Utils.writeContents(removal, ("for removal use, "
                            + "only filename required").getBytes());
                } else {
                    simpleCheckout(filename, otherCommit);
                }
            } else if (!headSplit && !otherSplit && !headOther) {
                conflictOverwrite(filename, headCommit(), otherCommit);
                result = true;
            }
        }
        return result;
    }

    /**@param branchName the branch's name that searching starts.
     * @return all the commits' sha1 code on the path
     * from Head commit to initial commit.
     */
    List<String> parentsSha(String branchName) {
        List<String> result = new ArrayList<>();
        String commitSha = new String(Utils.readContents(
                new File(".gitlet/refs/" + branchName)));
        Commit com;
        while (commitSha != null) {
            result.add(commitSha);
            com = (Commit) Utils.deserialize(".gitlet/object/commit/"
                    + commitSha);
            commitSha = com.getParentSHA1();
        }
        return result;
    }

    /** helper method for do Merge.
     * @param branchName the branch name of checkout.
     * @return the split point's commit's sha code.
     *         'error' if split point is current branch.
     */
    String splitCommitSha(String branchName) {
        for (String headParentSha : parentsSha(headBranchName())) {
            for (String branchParent : parentsSha(branchName)) {
                if (headParentSha.equals(branchParent)) {
                    return branchParent;
                }
            }
        }
        return null;
    }

    /** All the possible failure cases.
     * @param branchName the branch's name of after merge
     * @return true if failure happen, false otherwise.
     */
    boolean failure(String branchName) {
        if (Utils.plainFilenamesIn(".gitlet/index").size() != 0
                || Utils.plainFilenamesIn(".gitlet/removal").size() != 0) {
            System.out.println("You have uncommitted changes.");
            return true;
        }
        if (!Utils.plainFilenamesIn(".gitlet/refs").contains(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return true;
        }
        if (headBranchName().equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            return true;
        }
        return false;
    }

    /** Assume FILENAME file is included in both commit COM1 and COM2,
     * and file in com1 is different from the file in com2.
     * Stage the new version of file.
     * @param filename the file to be overwrite
     * @param com1 1st commit where the file comes from(head commit)
     * @param com2 2nd commit where the file comes from(branch commit)
     */
    void conflictOverwrite(String filename, Commit com1, Commit com2) {
        String file1Sha = commitToTrees(com1).getfile(filename);
        String file2Sha = commitToTrees(com2).getfile(filename);
        File file1 = new File(".gitlet/object/" + file1Sha);
        File file2 = new File(".gitlet/object/" + file2Sha);
        StringBuilder builder = new StringBuilder();
        builder.append("<<<<<<< HEAD" + System.lineSeparator());
        if (file1Sha != null) {
            builder.append(new String(Utils.readContents(file1)));
        }
        builder.append("=======" + System.lineSeparator());
        if (file2Sha != null) {
            builder.append(new String(Utils.readContents(file2)));
        }
        builder.append(">>>>>>>" + System.lineSeparator());
        File toStage = new File(".gitlet/index/" + filename);
        Utils.writeContents(toStage, builder.toString().getBytes());
        if (Utils.plainFilenamesIn(".").contains(filename)) {
            Utils.writeContents(new File(filename) ,
                    builder.toString().getBytes());
        }
    }

    /** Checkout a file and Stage it:
     * Checkout: overide the file under working directory(if exist)
     *          or create the file under working directory.
     * Stage: add it to staging area.
     * @param filename the filename to checkout
     * @param com the commit that the file comes from
     */
    void simpleCheckout(String filename, Commit com) {
        File tocheckout = new File(filename);
        File tostage = new File(".gitlet/index/" + filename);
        String filesha = commitToTrees(com).getfile(filename);
        Utils.writeContents(tocheckout, Utils.readContents(
                new File(".gitlet/object/" + filesha)));
        Utils.writeContents(tostage, Utils.readContents(tocheckout));
    }

    /** Assume com1 and com2 both contain file with FILENAME.
     * @param com1 Commit 1
     * @param com2 Commit 2
     * @param filename the file name that used to compare between com1 and com2
     * @return true if FILE in com1 has same content(same sha1)
     *                  with FILE in com2
     *              or FILE in both com1 and com2 are removed.
     *         false if FILE in com1 has different content
     *                  with FILE in com2 or
     *               FILE in one of com1 or com2 has been removed.
     */
    boolean comparefile(Commit com1, Commit com2, String filename) {
        Trees tree1 = commitToTrees(com1);
        Trees tree2 = commitToTrees(com2);
        if (tree1.getfile(filename) == null
                && tree2.getfile(filename) == null) {
            return true;
        }
        if (tree1.getfile(filename) == null
                || tree2.getfile(filename) == null) {
            return false;
        }
        return tree1.getfile(filename).equals(tree2.getfile(filename));
    }

    /**
     * @param split the commit 1 (split point)
     * @param branch the commit 2 (branch point)
     * @return a union list of all files in commit split, branch and head.
     */
    List<String> unionfiles(Commit split, Commit branch) {
        ArrayList<String> result = new ArrayList<>();
        Trees splitTree = commitToTrees(split);
        Trees branchTree = commitToTrees(branch);
        Trees headTree = headTrees();
        result.addAll(splitTree.getfiles());
        for (String i : branchTree.getfiles()) {
            if (!result.contains(i)) {
                result.add(i);
            }
        }
        for (String i : headTree.getfiles()) {
            if (!result.contains(i)) {
                result.add(i);
            }
        }
        return result;
    }

    /**
     * @param otherBranch the branch's file name("b1, master..")
     *                    that is going to merge with head branch
     * @return a list of files' name that are contained in both
     *         head commit and other branch's commits.
     */
    List<String> samefiles(String otherBranch) {
        Commit otherCommit = branchToCommit(otherBranch);
        Trees otherTree = commitToTrees(otherCommit);
        List<String> headList = new ArrayList<>();
        List<String> branchList = new ArrayList<>();
        headList.addAll(headTrees().getfiles());
        branchList.addAll(otherTree.getfiles());
        headList.retainAll(branchList);
        return headList;
    }

    /** Transfer a Commit from a branch.
     * @param branchName a branch's name.
     * @return the commit that branch is pointing to.
     */
    Commit branchToCommit(String branchName) {
        String  otherSha = new String(Utils.readContents(
                new File(".gitlet/refs/" + branchName)));
        return (Commit) Utils.deserialize(".gitlet/object/commit/" + otherSha);
    }

    /**
     * @param com the commit
     * @return the Trees under the commit.
     */
    Trees commitToTrees(Commit com) {
        String treeSha = com.gettreeSha();
        return (Trees) Utils.deserialize(".gitlet/object/trees/" + treeSha);
    }

    /** Helper Method that used to create a file with serialized content of OBJ
     * and file name is obj's sha1 code.
     * @param path the path that new created file should be stored at.
     * @param obj the object that to be stored in path.
     * @return return object's sha1 code.
     */
    String serialShaname(String path, Object obj) {
        byte[] objContent = Utils.serialize(path + "obj_temp", obj);
        String objSha = Utils.sha1((Object) objContent);
        new File(path + "obj_temp").renameTo(new File(path + objSha));
        return objSha;
    }

    /** Helper Method.
     * @return the trees object that head commit is pointing to.
     */
    static Trees headTrees() {
        String treeSha = headCommit().gettreeSha();
        return (Trees) Utils.deserialize(".gitlet/object/trees/" + treeSha);
    }

    /** Helper method.
     * @return the Commit object that head is currently pointing to.
     */
    static Commit headCommit() {
        String headBranch = new String(
                Utils.readContents(new File(".gitlet/Head")));
        String headCommitSha = new String(
                Utils.readContents(new File(headBranch)));
        Commit result = (Commit) Utils.deserialize(
                ".gitlet/object/commit/" + headCommitSha);
        return result;
    }

    /** @return head branch's name. (i.e. Master, b1..). */
    static String headBranchName() {
        String headBranchPath = new String(
                Utils.readContents(new File(".gitlet/Head")));
        File headBranchFile = new File(headBranchPath);
        return headBranchFile.getName();
    }

    /** Convert all the string variables in array to be a string.
     * @param array a string array
     * @return a string containing all variables in array.
     */
    String arraytostring(String[] array) {
        StringBuilder builder = new StringBuilder();
        for (String s : array) {
            builder.append(s + " ");
        }
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    /** Add the remote repository.
     * @param operands remote name.
     */
    void doAddRemote(String[] operands) {
        String remoteName = operands[0];
        String remoteAddress = operands[1];
        File root = new File(remoteAddress + "/.gitlet");
        root.mkdir();
    }

    /** check if a command exist or not.
     * @return ture if command exists.
     */
    boolean checkCommand() {
        if (_commands.get(_command.getCommand()) != null) {
            return true;
        }
        throw new Error(_command.getCommand() + " is not supported");
    }

    /** Hashmap that stores all the commands, and link to their method. */
    private final HashMap<String, Consumer<String[]>>
            _commands = new HashMap<>();
    {
        _commands.put("init", this::doInit);
        _commands.put("commit", this::doCommit);
        _commands.put("add", this::doAdd);
        _commands.put("rm", this::doRm);
        _commands.put("log", this::doLog);
        _commands.put("global-log", this::doGloballog);
        _commands.put("find", this::doFind);
        _commands.put("status", this::doStatus);
        _commands.put("checkout", this::doCheckout);
        _commands.put("branch", this::doBranch);
        _commands.put("rm-branch", this::doRmBranch);
        _commands.put("reset", this::doReset);
        _commands.put("merge", this::doMerge);
        _commands.put("add-remote", this::doAddRemote);
    }
}
