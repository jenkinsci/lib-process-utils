package org.jenkinsci.utils.process;

import java.lang.reflect.Field;

/**
 * @author Kohsuke Kawaguchi
 */
@SuppressWarnings("Since15")
public class ProcessUtils {
    /**
     * Figure out the UNIX process ID from {@link Process}
     */
    public static int getPid(Process p) {
        try {
            return (int) p.pid();
        } catch (UnsupportedOperationException e) {
            return -1;
        }
    }
}