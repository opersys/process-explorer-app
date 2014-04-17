package com.opersys.processexplorer.node;

public class NodeThreadEventData {

    private String stdout;
    private String stderr;

    private Exception ex;

    public String getStdout() {
        return this.stdout;
    }

    public String getStderr() {
        return this.stderr;
    }

    public Exception getException() {
        return this.ex;
    }

    public NodeThreadEventData(String stdout, String stderr) {
        this.stdout = stdout;
        this.stderr = stderr;
    }

    public NodeThreadEventData(Exception ex) {
        this.ex = ex;
    }

    public NodeThreadEventData() {}
}
