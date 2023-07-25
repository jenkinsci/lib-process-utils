package org.jenkinsci.utils.process;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

public class ProcessUtilsTest {

    @Test
    public void testToString() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("java", "-version");
        Process process = pb.start();
        long pid = ProcessUtils.getPid(process);
        assertNotEquals(-1, pid);
        assertEquals("Process " + pid + " failed to terminate at the end of the test", 0, process.waitFor());
    }
}
