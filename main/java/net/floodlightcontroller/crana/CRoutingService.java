package net.floodlightcontroller.crana;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U64;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.routing.Route;

public interface CRoutingService extends IFloodlightService {
	//public Route getRoute(DatapathId src, DatapathId dst, U64 cookie);
	
	public Route getRoute(DatapathId srcId, OFPort srcPort, DatapathId dstId, OFPort dstPort, U64 cookie);
}
