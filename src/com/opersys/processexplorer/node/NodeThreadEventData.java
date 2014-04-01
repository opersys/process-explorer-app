package com.opersys.processexplorer.node;

public class NodeThreadEventData {

    private String stdout;

    private String stderr;

    public String getStdout() {
        return this.stdout;
    }

    public String getStderr() {
        return this.stderr;
    }

    public NodeThreadEventData(String stdout, String stderr) {
        this.stdout = stdout;
        this.stderr = stderr;
    }

    public NodeThreadEventData() {}
}
