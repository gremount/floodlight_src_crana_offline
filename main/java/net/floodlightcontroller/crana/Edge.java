package net.floodlightcontroller.crana;

import org.projectfloodlight.openflow.types.OFPort;

import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.topology.NodePortTuple;

public class Edge {
	public int id;
	public int src;
	public NodePortTuple srcPort;
	public int dst;
	public NodePortTuple dstPort;
	public double capacity = 100000000; //100Mbit/s
	public double bw;
	public int delay;
	
	public Edge(int id, Link link, double outOctets){
		this.id = id;
		this.src = (int)link.getSrc().getLong() - 1;
		this.srcPort = new NodePortTuple(link.getSrc(),link.getSrcPort());
		this.dst = (int)link.getDst().getLong() -1;
		this.dstPort = new NodePortTuple(link.getDst(),link.getDstPort());
		this.delay = (int) link.getLatency().getValue();
		this.bw = outOctets * 8;
	}
	
	public String printEdge(){
		String s = this.id + " " + this.src + " " + this.dst + " " + this.capacity + " " + this.bw +  "  " + this.delay;
		return s;
	}
	
	 @Override
	    public boolean equals(Object obj) {
	        if (this == obj)
	            return true;
	        if (obj == null)
	            return false;
	        if (getClass() != obj.getClass())
	            return false;
	        Edge other = (Edge) obj;
	        if (dst != other.dst)
	            return false;
	        if (!dstPort.equals(other.dstPort))
	            return false;
	        if (src != other.src)
	            return false;
	        if (!srcPort.equals(other.srcPort))
	            return false;
	        return true; /* do not include latency */
	    }
	 
	@Override
    public String toString() {
        return "Edge [id= " + this.id
        		+ ", src= " + this.src
                + " srcPort="
                + srcPort.toString()
                + ", dst= " + this.dst
                + ", inPort="
                + dstPort.toString()
                + ", capacity="
                + this.capacity
                + ", bw="
                + this.bw
                + ", delay =" 
                +this.delay
                + "]";
    }
	
}
