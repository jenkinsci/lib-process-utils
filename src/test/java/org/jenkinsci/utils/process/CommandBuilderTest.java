/*
 * The MIT License
 *
 * Copyright (c) 2014 Red Hat, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.utils.process;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import org.junit.Test;

public class CommandBuilderTest {

    @Test
    public void testToString() {
        assertEquals("JAVA_OPTS=\"a value\" java -jar a.jar", builder().toString());
    }

    @Test
    public void testAdd() {
        CommandBuilder cb = new CommandBuilder().add("some", "more", "args");
        cb.env.put("OTHER_OPTS", "42");

        HashMap<String, String> expectedEnv = new HashMap<String, String>();
        expectedEnv.put("OTHER_OPTS", "42");
        expectedEnv.put("JAVA_OPTS", "a value");

        CommandBuilder builder = builder().add(cb);
        assertEquals(Arrays.asList("java", "-jar", "a.jar", "some", "more", "args"), builder.toList());
        assertEquals(expectedEnv, builder.env);
    }

    @Test
    public void testClone() {
        CommandBuilder b = builder();
        CommandBuilder clone = b.clone();
        assertEquals(b, clone);
        assertEquals(b.toList(), clone.toList());
        assertEquals(b.env, clone.env);
    }

    private CommandBuilder builder() {
        CommandBuilder cb = new CommandBuilder().pwd(new File("/tmp")).add("java", "-jar", "a.jar");
        cb.env.put("JAVA_OPTS", "a value");
        return cb;
    }
}
