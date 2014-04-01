package com.opersys.processexplorer.node;

import com.opersys.processexplorer.ProcessExplorerServiceBinder;
import com.opersys.processexplorer.node.NodeThreadEvent;
import com.opersys.processexplorer.node.NodeThreadEventData;

public interface NodeThreadListener {

    void onProcessServiceConnected(ProcessExplorerServiceBinder service);

    void onProcessServiceDisconnected();

    void ProcessExplorerServiceEvent(NodeThreadEvent ev, NodeThreadEventData evData);
}
