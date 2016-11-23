package net.floodlightcontroller.crana;

import org.projectfloodlight.openflow.types.OFPort;

public class Demand {
	private int id;
	private int src;
	private OFPort sp = OFPort.of(1); //src port
	private int dst;
	private OFPort dp = OFPort.of(1);
	private int tcpSrcPort;
	private int tcpDstPort;
	private double flow;
	
	public Demand(){}
	
	public Demand(int id,int src,OFPort sp,int dst,OFPort dp, int tsp, int tdp, double flow){
		this.id=id;
		this.src=src;
		this.sp=sp;
		this.dst=dst;
		this.dp=dp;
		this.tcpSrcPort = tsp;
		this.tcpDstPort = tdp;
		this.flow=flow;
	}
	
	public Demand(int id,int src,OFPort sp,int dst,OFPort dp, double flow){
		this.id=id;
		this.src=src;
		this.sp= sp;
		this.dst=dst;
		this.dp= sp;
		this.flow=flow;
	}
	
	public Demand(int id,int src,int sp,int dst,int dp, double flow){
		this.id=id;
		this.src=src;
		this.sp= OFPort.of(sp);
		this.dst=dst;
		this.dp= OFPort.of(sp);
		this.flow=flow;
	}
	
	public Demand(int id,int src, int dst, double flow){
		this.id=id;
		this.src=src;
		this.dst=dst;
		this.flow=flow;
	}
	
	public String printDem(){
		String s  = this.id + " " + this.src + " " + this.dst + " "  + this.flow;
		//System.out.println(s);
		return s;
	}
	
	public int getId(){
		return this.id;
	}
	public int getSrc(){
		return this.src;
	}
	public int getDst(){
		return this.dst;
	}
	public OFPort getSp(){
		return this.sp;
	}
	public OFPort getDp(){
		return this.dp;
	}
	public int getTcpSrcPort(){
		return tcpSrcPort;
	}
	public int getTcpDstPort(){
		return tcpDstPort;
	}
	public double getFlow(){
		return this.flow;
	}
}
