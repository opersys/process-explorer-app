package com.opersys.processexplorer.node;

public interface NodeServiceListener {

    void onConnected(NodeService service);

    void onDisconnected();

    void NodeServiceEvent(NodeServiceEvent ev, NodeServiceEventData evData);
}
