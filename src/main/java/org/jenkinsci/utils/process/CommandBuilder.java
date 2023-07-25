package org.jenkinsci.utils.process;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Used to build up arguments for a process invocation.
 *
 * <p>
 * Most of the methods are for adding arguments. To simplify this, we
 * have all kinds of overloaded {@link #add(String)} methods. These methods
 * are null safe, in that if null is passed as an argument they get ignored.
 *
 * <p>
 * Configuring other parts of the process invocation isn't complete, except
 * {@link #pwd(File)}. When you find a need to add more, please add them.
 *
 * <p>
 * Once enough details are configured, use {@link #system()} or {@link #popen()}
 * to run them. Alternatively, use {@link #build()} to get a configured
 * {@link ProcessBuilder} and then handle the output yourself.
 *
 * @author Kohsuke Kawaguchi
 */
public class CommandBuilder implements Serializable, Cloneable {
    private final List<String> args = new ArrayList<String>();
    public final Map<String, String> env = new HashMap<>();

    private File pwd;

    public CommandBuilder() {}

    public CommandBuilder(Collection<String> args) {
        addAll(args);
    }

    public CommandBuilder(String... args) {
        add(args);
    }

    public ProcessBuilder build() {
        return new ProcessBuilder(args).directory(pwd);
    }

    /**
     * Executes a process and waits for that to complete. stdin/out/err will be the same
     * with that of the calling process.
     *
     * See http://linux.die.net/man/3/system
     */
    public int system() throws IOException, InterruptedException {
        ProcessBuilder pb = build();
        pb.environment().putAll(Collections.unmodifiableMap(env));
        pb.redirectOutput(Redirect.INHERIT);
        pb.redirectError(Redirect.INHERIT);
        pb.redirectInput(Redirect.INHERIT);
        Process p = pb.start();
        return p.waitFor();
    }

    /**
     * Executes a process and read its output.
     *
     * See http://linux.die.net/man/3/popen
     */
    public ProcessInputStream popen() throws IOException {
        ProcessBuilder pb = build();
        pb.environment().putAll(Collections.unmodifiableMap(env));
        pb.redirectErrorStream(true);
        LOGGER.fine("Executing: " + toStringWithQuote());
        Process p = pb.start();
        p.getOutputStream().close();
        return new ProcessInputStream(p).withDisplayName(toStringWithQuote());
    }

    public CommandBuilder pwd(File dir) {
        this.pwd = dir;
        return this;
    }

    public CommandBuilder add(Object a) {
        return add(a.toString());
    }

    /**
     * @since 1.378
     */
    public CommandBuilder add(Object a, boolean mask) {
        return add(a.toString());
    }

    public CommandBuilder add(File f) {
        return add(f.getAbsolutePath());
    }

    public CommandBuilder add(String a) {
        if (a != null) {
            args.add(a);
        }
        return this;
    }

    public CommandBuilder prepend(String... args) {
        this.args.addAll(0, Arrays.asList(args));
        return this;
    }

    /**
     * Adds an argument by quoting it.
     * This is necessary only in a rare circumstance,
     * such as when adding argument for ssh and rsh.
     *
     * Normal process invocations don't need it, because each
     * argument is treated as its own string and never merged into one.
     */
    public CommandBuilder addQuoted(String a) {
        return add('"' + a + '"');
    }

    /**
     * @since 1.378
     */
    public CommandBuilder addQuoted(String a, boolean mask) {
        return add('"' + a + '"');
    }

    public CommandBuilder add(Object... args) {
        if (args != null) {
            for (Object arg : args) {
                add(arg);
            }
        }
        return this;
    }

    public CommandBuilder add(String... args) {
        if (args != null) {
            for (String arg : args) {
                add(arg);
            }
        }
        return this;
    }

    public CommandBuilder add(CommandBuilder cmds) {
        if (cmds != null) {
            addAll(cmds.args);
            env.putAll(cmds.env);
        }
        return this;
    }

    public CommandBuilder addAll(Collection<String> args) {
        if (args != null) {
            for (String arg : args) {
                add(arg);
            }
        }
        return this;
    }

    //    /**
    //     * Decomposes the given token into multiple arguments by splitting via whitespace.
    //     */
    //    public CommandBuilder addTokenized(String s) {
    //        if(s==null) return this;
    //        add(Util.tokenize(s));
    //        return this;
    //    }

    /**
     * @since 1.378
     */
    public CommandBuilder addKeyValuePair(String prefix, String key, String value) {
        if (key == null) {
            return this;
        }
        add(((prefix == null) ? "-D" : prefix) + key + '=' + value);
        return this;
    }

    /**
     * Adds key value pairs as "-Dkey=value -Dkey=value ..."
     *
     * {@code -D} portion is configurable as the 'prefix' parameter.
     * @since 1.114
     */
    public CommandBuilder addKeyValuePairs(String prefix, Map<String, String> props) {
        for (Entry<String, String> e : props.entrySet()) {
            addKeyValuePair(prefix, e.getKey(), e.getValue());
        }
        return this;
    }

    /**
     * Adds key value pairs as "-Dkey=value -Dkey=value ..." with masking.
     *
     * @param prefix
     *      Configures the -D portion of the example. Defaults to -D if null.
     * @param props
     *      The map of key/value pairs to add
     * @param propsToMask
     *      Set containing key names to mark as masked in the argument list. Key
     *      names that do not exist in the set will be added unmasked.
     * @since 1.378
     */
    public CommandBuilder addKeyValuePairs(String prefix, Map<String, String> props, Set<String> propsToMask) {
        for (Entry<String, String> e : props.entrySet()) {
            addKeyValuePair(prefix, e.getKey(), e.getValue());
        }
        return this;
    }

    public String[] toCommandArray() {
        return args.toArray(new String[args.size()]);
    }

    @Override
    public CommandBuilder clone() {
        CommandBuilder r = new CommandBuilder();
        r.args.addAll(this.args);
        r.env.putAll(env);
        r.pwd = pwd;
        return r;
    }

    /**
     * Re-initializes the arguments list.
     */
    public void clear() {
        args.clear();
        env.clear();
    }

    public List<String> toList() {
        return args;
    }

    /**
     * Just adds quotes around args containing spaces, but no other special characters,
     * so this method should generally be used only for informational/logging purposes.
     */
    public String toStringWithQuote() {
        StringBuilder buf = new StringBuilder();
        for (String arg : args) {
            if (buf.length() > 0) {
                buf.append(' ');
            }

            if (arg.indexOf(' ') >= 0 || arg.length() == 0) {
                buf.append('"').append(arg).append('"');
            } else {
                buf.append(arg);
            }
        }
        return buf.toString();
    }

    /**
     * Wrap command in a CMD.EXE call so we can return the exit code (ERRORLEVEL).
     * This method takes care of escaping special characters in the command, which
     * is needed since the command is now passed as a string to the CMD.EXE shell.
     * This is done as follows:
     * Wrap arguments in double quotes if they contain any of:
     *   space *?,;^&amp;&lt;&gt;|"
     *   and if escapeVars is true, % followed by a letter.
     * <br> When testing from command prompt, these characters also need to be
     * prepended with a ^ character: ^&amp;&lt;&gt;|  -- however, invoking cmd.exe from
     * Jenkins does not seem to require this extra escaping so it is not added by
     * this method.
     * <br> A " is prepended with another " character.  Note: Windows has issues
     * escaping some combinations of quotes and spaces.  Quotes should be avoided.
     * <br> If escapeVars is true, a % followed by a letter has that letter wrapped
     * in double quotes, to avoid possible variable expansion.
     * ie, %foo% becomes "%"f"oo%".  The second % does not need special handling
     * because it is not followed by a letter. <br>
     * Example: "-Dfoo=*abc?def;ghi^jkl&amp;mno&lt;pqr&gt;stu|vwx""yz%"e"nd"
     * @param escapeVars True to escape %VAR% references; false to leave these alone
     *                   so they may be expanded when the command is run
     * @return new CommandBuilder that runs given command through cmd.exe /C
     * @since 1.386
     */
    public CommandBuilder toWindowsCommand(boolean escapeVars) {
        StringBuilder quotedArgs = new StringBuilder();
        boolean quoted, percent;
        for (String arg : args) {
            quoted = percent = false;
            for (int i = 0; i < arg.length(); i++) {
                char c = arg.charAt(i);
                if (!quoted && (c == ' ' || c == '*' || c == '?' || c == ',' || c == ';')) {
                    quoted = startQuoting(quotedArgs, arg, i);
                } else if (c == '^' || c == '&' || c == '<' || c == '>' || c == '|') {
                    if (!quoted) {
                        quoted = startQuoting(quotedArgs, arg, i);
                        // quotedArgs.append('^'); See note in javadoc above
                    }
                } else if (c == '"') {
                    if (!quoted) {
                        quoted = startQuoting(quotedArgs, arg, i);
                    }
                    quotedArgs.append('"');
                } else if (percent && escapeVars && ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'))) {
                    if (!quoted) {
                        quoted = startQuoting(quotedArgs, arg, i);
                    }
                    quotedArgs.append('"').append(c);
                    c = '"';
                }
                percent = (c == '%');
                if (quoted) {
                    quotedArgs.append(c);
                }
            }
            if (quoted) {
                quotedArgs.append('"');
            } else {
                quotedArgs.append(arg);
            }
            quotedArgs.append(' ');
        }
        // (comment copied from old code in hudson.tasks.Ant)
        // on Windows, executing batch file can't return the correct error code,
        // so we need to wrap it into cmd.exe.
        // double %% is needed because we want ERRORLEVEL to be expanded after
        // batch file executed, not before. This alone shows how broken Windows is...
        quotedArgs.append("&& exit %%ERRORLEVEL%%");
        return new CommandBuilder().add("cmd.exe", "/C").addQuoted(quotedArgs.toString());
    }

    /**
     * Calls toWindowsCommand(false)
     * @see #toWindowsCommand(boolean)
     */
    public CommandBuilder toWindowsCommand() {
        return toWindowsCommand(false);
    }

    private static boolean startQuoting(StringBuilder buf, String arg, int atIndex) {
        buf.append('"').append(arg.substring(0, atIndex));
        return true;
    }

    /**
     * Debug/error message friendly output.
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (Entry<String, String> envvar : env.entrySet()) {
            buf.append(envvar.getKey()).append('=');
            if (envvar.getValue().indexOf(' ') >= 0) {
                buf.append('"').append(envvar.getValue()).append('"');
            } else {
                buf.append(envvar.getValue());
            }

            buf.append(' ');
        }

        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);

            if (i > 0) {
                buf.append(' ');
            }

            if (arg.indexOf(' ') >= 0 || arg.length() == 0) {
                buf.append('"').append(arg).append('"');
            } else {
                buf.append(arg);
            }
        }
        return buf.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CommandBuilder that = (CommandBuilder) o;

        if (!args.equals(that.args)) {
            return false;
        }
        if (!env.equals(that.env)) {
            return false;
        }
        if (pwd != null ? !pwd.equals(that.pwd) : that.pwd != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = args.hashCode();
        result = 31 * env.hashCode();
        result = 31 * result + (pwd != null ? pwd.hashCode() : 0);
        return result;
    }

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(CommandBuilder.class.getName());
}
