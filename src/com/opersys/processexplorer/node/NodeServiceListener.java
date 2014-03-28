package com.opersys.processexplorer.node;

public interface NodeServiceListener {

    void onNodeServiceConnected(NodeServiceBinder service);

    void onNodeServiceDisconnected();

    void NodeServiceEvent(NodeServiceEvent ev, NodeServiceEventData evData);
}
