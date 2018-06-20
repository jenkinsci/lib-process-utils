package org.jenkinsci.utils.process;

/**
 * @author Kohsuke Kawaguchi
 */
public class ProcessUtils {
    /**
     * Figure out the UNIX process ID from {@link Process}
     */
    public static long getPid(Process p) {
        try {
            ProcessHandle handle = p.toHandle();
            return handle.pid();
        } catch (UnsupportedOperationException e1) {
            return -1L;
        }
    }
}