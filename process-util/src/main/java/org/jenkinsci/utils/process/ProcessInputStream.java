package org.jenkinsci.utils.process;

import org.apache.commons.io.IOUtils;

import java.io.*;

/**
 * Represents a reader end of the pipe to a running process.
 *
 * @author Kohsuke Kawaguchi
 * @see CommandBuilder#popen()
 */
public class ProcessInputStream extends InputStream {
    private final Process process;
    private final InputStream base;

    /**
     * In case of error, used to describe this process
     */
    private String displayName;

    public ProcessInputStream(Process p) {
        this.process = p;
        this.base = process.getInputStream();
    }

    ProcessInputStream withDisplayName(String n) {
        this.displayName = n;
        return this;
    }

    @Override
    public int read() throws IOException {
        return base.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return base.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return base.read(b, off, len);
    }

    public InputStream getInputStream(){
        return base;
    }

    /**
     * Gives underlying {@link Process}
     */
    public Process getProcess(){
        return process;
    }

    @Override
    public long skip(long n) throws IOException {
        return base.skip(n);
    }

    @Override
    public int available() throws IOException {
        return base.available();
    }

    @Override
    public void close() throws IOException {
        base.close();
    }

    /**
     * Waits for the process to exit and returns its status code.
     */
    public int waitFor() throws InterruptedException {
        return process.waitFor();
    }

    /**
     * Reads the whole thing as a string.
     */
    public String asText() throws IOException, InterruptedException {
        String r = IOUtils.toString(this);
        waitFor();  // collect the finished child process
        return r;
    }

    /**
     * Waits for the process to complete. If the process returns a non-zero exit code,
     * throw {@link IOException} with all the output from the process for diagnosis.
     */
    public String verifyOrDieWith(String errorMessage) throws IOException, InterruptedException {
        String text = asText();
        if (waitFor()==0)   return text;
        throw new IOException(errorMessage+"\n"+text);
    }

    /**
     * Decorates the input stream by forcing an read error in case the process exits with
     * non-zero exit code.
     */
    public InputStream withErrorCheck() {
        return new FilterInputStream(this) {
            @Override
            public int read() throws IOException {
                return check(super.read());
            }

            @Override
            public int read(byte[] b) throws IOException {
                return check(super.read(b));
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return check(super.read(b, off, len));
            }

            private int check(int r) throws IOException {
                if (r<0) {
                    try {
                        int x = waitFor();
                        if (x !=0) {
                            String n = displayName;
                            if (n==null)    n=process.toString();
                            throw new IOException("Process '"+n+"' terminated with exit code="+x);
                        }
                    } catch (InterruptedException e) {
                        throw (IOException)new InterruptedIOException().initCause(e);
                    }
                }
                return r;
            }
        };
    }
}
