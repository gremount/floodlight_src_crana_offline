package net.floodlightcontroller.crana;

import net.floodlightcontroller.topology.NodePortTuple;

public class SrcDstTuple {
	private NodePortTuple srcPort;
	private NodePortTuple dstPort;
	
	public SrcDstTuple(NodePortTuple srcPort, NodePortTuple dstPort){
		this.srcPort = srcPort;
		this.dstPort = dstPort;		
	}
	
	public NodePortTuple getSrccPort(){
		return srcPort;
	}
	
	public NodePortTuple getDstPort(){
		return dstPort;
	}
	
	 @Override
     public boolean equals(Object obj) {
        if (this == obj)
        	return true;
	    if (obj == null)
	        return false;
	    if (getClass() != obj.getClass())
	        return false;
        SrcDstTuple other = (SrcDstTuple) obj;
        if (!dstPort.equals(other.dstPort))
	        return false;
        if (!srcPort.equals(other.srcPort))
        	return false;
	    return true; /* do not include latency */
	 }
	 
	 @Override
	 public String toString() {
        return "[ srcPort= "
	    + srcPort.toString()
	    +" -> dstPort= "
        + dstPort.toString()
        +" ]";
	 }
}
