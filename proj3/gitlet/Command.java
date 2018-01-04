package gitlet;

/** The command that was used for running git.
 * @author Fansheng Cheng
 */
public class Command {
    /** The command constructor.
     * @param command the command input by user.
     * @param operands the operands input by user.
     */
    Command(String command, String[] operands) {
        _command = command;
        _operands = operands;
    }

    /**
     * @return the command
     */
    String getCommand() {
        return _command;
    }

    /**
     * @return the operands
     */
    String[] getOperands() {
        return _operands;
    }

    /** The command. */
    private String _command;

    /** The operands followed by command. */
    private String[] _operands;
}
