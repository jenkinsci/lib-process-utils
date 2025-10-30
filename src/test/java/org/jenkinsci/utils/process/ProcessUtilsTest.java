package org.jenkinsci.utils.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class ProcessUtilsTest {

    @Test
    void testToString() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("java", "-version");
        Process process = pb.start();
        long pid = ProcessUtils.getPid(process);
        assertNotEquals(-1, pid);
        assertEquals(0, process.waitFor(), "Process " + pid + " failed to terminate at the end of the test");
    }
}
