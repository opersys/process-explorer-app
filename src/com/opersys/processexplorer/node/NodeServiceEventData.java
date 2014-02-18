package com.opersys.processexplorer.node;

public class NodeServiceEventData {

    private String stdout;

    private String stderr;

    public String getStdout() {
        return this.stdout;
    }

    public String getStderr() {
        return this.stderr;
    }

    public NodeServiceEventData(String stdout, String stderr) {
        this.stdout = stdout;
        this.stderr = stderr;
    }

    public NodeServiceEventData() {}
}
