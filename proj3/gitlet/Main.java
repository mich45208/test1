package gitlet;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Fansheng Cheng
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        String[] operands = new String[args.length - 1];
        for (int i = 0; i < operands.length; i += 1) {
            operands[i] = args[i + 1];
        }
        Command com = new Command(args[0], operands);
        Rungit run = new Rungit(com);
        run.process();
    }
}
