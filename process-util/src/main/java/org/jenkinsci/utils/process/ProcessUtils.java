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
            Field f = p.getClass().getDeclaredField("pid");
            f.setAccessible(true);
            return (int)f.get(p);
        } catch (ReflectiveOperationException e) {
            return -1;
        }
    }
}