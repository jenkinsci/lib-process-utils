package org.jenkinsci.utils.process;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class ProcessUtilsTest {

    @Test
    public void testToString() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("java");
        pb.inheritIO();
        Process process = pb.start();
        long pid = ProcessUtils.getPid(process);
        assertNotEquals(-1, pid);
        assertTrue("Process " + pid + " failed to terminate at the end of the test", process.waitFor(1, TimeUnit.SECONDS));
    }
}
