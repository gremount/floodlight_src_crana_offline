package net.floodlightcontroller.crana;

import java.io.*;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.Executors;  
import java.util.concurrent.ScheduledExecutorService;   

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowModFlags;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;


import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.AppCookie;
import net.floodlightcontroller.linkdiscovery.ILinkDiscovery.LDUpdate;
import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.routing.Route;
import net.floodlightcontroller.sflowcollector.ISflowListener;
import net.floodlightcontroller.sflowcollector.InterfaceStatistics;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import net.floodlightcontroller.topology.ITopologyListener;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.topology.NodePortTuple;
import net.floodlightcontroller.util.FlowModUtils;
import net.floodlightcontroller.util.MatchUtils;
import net.floodlightcontroller.util.OFMessageDamper;
import net.floodlightcontroller.sflowcollector.ISflowCollectionService;
import net.floodlightcontroller.crana.Edge;
import net.floodlightcontroller.crana.Demand;


import net.floodlightcontroller.crana.selfishrouting.*;
import net.floodlightcontroller.crana.trafficengineering.*;
import net.floodlightcontroller.crana.voting.Voting;
import net.floodlightcontroller.crana.cheat_off.Cheating;

public class Coordinator implements IFloodlightModule, ITopologyListener, ISflowListener{
	private static final Logger log = LoggerFactory.getLogger(Coordinator.class);
	private static boolean isEnabled = false;
	private static final String ENABLED_STR = "enable";
	
	protected static String ROUTING_CHOICE = "Voting";
	protected static int OFMESSAGE_DAMPER_CAPACITY = 10000; // 
	protected static int OFMESSAGE_DAMPER_TIMEOUT = 250; // ms
	public static final int FLOW_DURATION = 6;//6min 的打流时间
	public static final int INITIAL_DEALY = 20;
	public static final int PERIOD = 400;
	public static final int BG_DEMAND_NUM = 20; //background demand num
	public static final int APP_DEMAND_NUM = 15; //app demand num
	//public static final int UTILTESTNUM=20;//测试链路利用率的次数的参数
	public static Random rand;

	protected static boolean FLOWMOD_DEFAULT_MATCH_TRANSPORT = true;
	public static int FLOWMOD_DEFAULT_IDLE_TIMEOUT = 300; // in seconds
	public static int FLOWMOD_DEFAULT_HARD_TIMEOUT = 310; // infinite
	public static int FLOWMOD_DEFAULT_PRIORITY = 233; // 0 is the default table-miss flow in OF1.3+, so we need to use 1
	public static final int MAX_DEMAND = 20; 
	public static final int DEMAND_NUM = 30; 
	public static final int Coordinator_APP_ID = 3; 
	static {
		AppCookie.registerApp(Coordinator_APP_ID, "Coordinator");
	}
	public static final U64 appCookie = AppCookie.makeCookie(Coordinator_APP_ID, 0);
	
	private static ITopologyService topologyService;
	private static ISflowCollectionService sflowCollectionService;
	private static IOFSwitchService switchService;
	private OFMessageDamper messageDamper;
	
	private int numDpid;
	private int numEdge;
	private static Set<Link>  allLinks;
	private static Map<NodePortTuple,InterfaceStatistics > statisticsMap;
	private static List<Edge> incL;
	static ArrayList<Demand> req;
	public static long topoTS = 0;
	
	private int utilTestNum;//测试链路利用率的次数
	private double utilSum;//多次测试链路利用率结果的和，为了后面求多次平均
	
	//after computing route
	public void PushAllRoute(){
		Map<Integer, List<Integer>> paths = readPath("inputFile//path.txt");
		
		for(Demand dm : req){
			DatapathId srcDpid = DatapathId.of(dm.getSrc() + 1);
			OFPort sPort = dm.getSp();
			DatapathId dstDpid = DatapathId.of(dm.getDst() + 1);
			OFPort dPort = dm.getDp();
			//System.out.println("********demand is coming*******");
			//System.out.println("demand " + dm.getId() +  ": " +srcDpid + " -> " + dstDpid);
			
			List<Integer> path = paths.get(dm.getId());
			
			Route route = getRoute(path, srcDpid, sPort, dstDpid, dPort);
			
			pushBiRoute(route, dm.getSrc(), dm.getDst(), dm.getTcpSrcPort(), dm.getTcpDstPort());
		    
		}
	}
	
	protected Match createMatch(int src, int dst, int tsp, int tdp) {
		DatapathId srcDpid = DatapathId.of(src + 1);
		//DatapathId dstDpid = DatapathId.of(dst + 1);
		
		IOFSwitch srcMac = switchService.getSwitch(srcDpid);
		//IOFSwitch dstMac = switchService.getSwitch(dstDpid);
		
		Match.Builder mb = srcMac.getOFFactory().buildMatch();
		
		IPv4Address sip = IPv4Address.of(167772160 + src + 1);
		IPv4Address dip = IPv4Address.of(167772160 + dst + 1);
	    
		TransportPort udpSrcPort = TransportPort.of(tsp);
		TransportPort udpDstPort = TransportPort.of(tdp);
		
		boolean MATCH_UDP_PORT = false;
		if(MATCH_UDP_PORT){
			mb.setExact(MatchField.ETH_TYPE, EthType.IPv4)
			.setExact(MatchField.IPV4_SRC, sip)
			.setExact(MatchField.IPV4_DST, dip)
			.setExact(MatchField.IP_PROTO, IpProtocol.UDP)
			.setExact(MatchField.UDP_SRC, udpSrcPort)
			.setExact(MatchField.UDP_DST, udpDstPort);
		}else{
			mb.setExact(MatchField.ETH_TYPE, EthType.IPv4)
			.setExact(MatchField.IPV4_SRC, sip)
			.setExact(MatchField.IPV4_DST, dip);
		}
		
		return mb.build();
	}
	
	public void pushBiRoute(Route route, int src, int dst, int tsp, int tdp){
		List<NodePortTuple> switchPortList = route.getPath();
		List<NodePortTuple> reSwitchPortList = new ArrayList<>();
		
		for(int i = switchPortList.size() - 1; i >= 0; i--){
			reSwitchPortList.add(switchPortList.get(i));
		}
		
		//System.out.println("push forward route");
		Match match = createMatch(src, dst, tsp, tdp);
		pushRoute(switchPortList, match);
		
		//System.out.println("push reverse route");
		Match reMatch = createMatch(dst, src, tdp, tsp);
		pushRoute(reSwitchPortList, reMatch);
		
		
	}
	
	public void pushRoute(List<NodePortTuple> switchPortList , Match match) {
		
		for (int indx = switchPortList.size() - 1; indx > 0; indx -= 2) {
			// indx and indx-1 will always have the same switch DPID.
			DatapathId switchDPID = switchPortList.get(indx).getNodeId();
			IOFSwitch sw = switchService.getSwitch(switchDPID);

			if (sw == null) {
				System.out.println("sw is null");
				return;
			}
			
			// need to build flow mod based on what type it is. Cannot set command later
			OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
			
			OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
			List<OFAction> actions = new ArrayList<OFAction>();	
			
			Match.Builder mb = MatchUtils.convertToVersion(match, sw.getOFFactory().getVersion());
 			
			// set input and output ports on the switch
			OFPort outPort = switchPortList.get(indx).getPortId();
			OFPort inPort = switchPortList.get(indx - 1).getPortId();
			
			mb.setExact(MatchField.IN_PORT, inPort);
			
			aob.setPort(outPort);
			aob.setMaxLen(Integer.MAX_VALUE);
			actions.add(aob.build());
			
			//requestFlowRemovedNotification
			Set<OFFlowModFlags> flags = new HashSet<>();
			flags.add(OFFlowModFlags.SEND_FLOW_REM);
			fmb.setFlags(flags);
		    
			//U64 cookie = U64.of(0);
			U64 cookie = AppCookie.makeCookie(Coordinator_APP_ID, 0);
			
			fmb.setMatch(mb.build())
			.setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
			.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
			.setBufferId(OFBufferId.NO_BUFFER)
			.setCookie(cookie)
			.setOutPort(outPort)
			.setPriority(FLOWMOD_DEFAULT_PRIORITY);
			
			FlowModUtils.setActions(fmb, actions, sw);
			
			
			try {				
				
				messageDamper.write(sw, fmb.build());
				/*
				log.info("Pushing Route flowmod routeIndx={} \n               " +
						"sw={}  srcIp = {} dstIp = {} inPort={} outPort={} ",
						new Object[] {indx,
						sw,
						fmb.getMatch().get(MatchField.IPV4_SRC),
						fmb.getMatch().get(MatchField.IPV4_DST),
						fmb.getMatch().get(MatchField.IN_PORT),
						outPort });
				*/

			} catch (IOException e) {
				log.error("Failure writing flow mod", e);
			}
		}
	}
	
	//convert a path to route
	public Route getRoute(List<Integer> path, DatapathId srcId, OFPort srcPort, DatapathId dstId, OFPort dstPort) {
		Route route = new Route(srcId, dstId);
		List<NodePortTuple> switchPorts = new ArrayList<>();
		switchPorts.add(new NodePortTuple(srcId,srcPort));
		if(path !=null)
			for(Integer id : path){
				if(incL!=null && (incL.size() -1 ) >= id ){
					switchPorts.add(incL.get(id).srcPort);
					switchPorts.add(incL.get(id).dstPort);
				}
		}
		switchPorts.add(new NodePortTuple(dstId,dstPort));
		
		route.setPath(switchPorts);	
		
		return route;
	}
	
	//read path computed by our algorithm
	public Map<Integer, List<Integer>> readPath(String filename){
		File file = new File(filename);
		if(!file.exists())
			System.out.println("path file doesn't exist.");
		
		Map<Integer, List<Integer>> paths = new HashMap<Integer, List<Integer>>();
		
		try{
			Scanner input = new Scanner(file);
			while(input.hasNext()){
				String line = input.nextLine();
				String [] strAry = line.split(" ");
				Integer id  = Integer.parseInt(strAry[0]);
				List<Integer> path = new ArrayList<>();
			    for (int i = 1; i < strAry.length; i++)
			    	path.add(Integer.parseInt(strAry[i]));
			    if(paths.get(id) == null)
			    	paths.put(id, path);
			}
			
			input.close();
		}
		catch(IOException e){
			System.out.println(e);
		}
		return paths;
	}
	
	// called when topo changed or sflow get new statistics
	public void updateTopo() {
		
		File file = new File("inputFile//topo.txt");
		try{
			PrintWriter output = new PrintWriter(file);
			incL.clear();
			int id = 0;
			output.println(numDpid);
			output.println(numEdge);
			
			if(allLinks != null)
				for(Link lk: allLinks)
					if(lk != null){
						if(statisticsMap != null
							&& statisticsMap.containsKey(new NodePortTuple(lk.getSrc(),lk.getSrcPort()))
							&& statisticsMap.get(new NodePortTuple(lk.getSrc(),lk.getSrcPort()))!=null){	
							Edge edge = new Edge(id++, lk, statisticsMap.get(new NodePortTuple(lk.getSrc(),lk.getSrcPort())).getIfOutOctets().doubleValue());
							output.println(edge.printEdge());
							incL.add(edge);
                        }else{
                        	Edge edge = new Edge(id++, lk, 0);
                        	output.println(edge.printEdge());
                        	incL.add(edge);	
                        }
				    }
			     
		output.close();
		}
		catch(IOException e){
			System.out.println(e);
		}
	}

	/**
	 * 功能： 产生背景流和app流
	 */
	public void GenerateDemand() throws IOException{
		//int numDem = rand.nextInt(numDpid*(numDpid-1)/2) + 1;
		//int numAppDem = rand.nextInt(numDem) + 1;
		req.clear();
		GenDemand(numDpid,numDpid,"inputFile//req.txt", "inputFile//gt.txt");// 根据图的点数产生背景流需求
	}
	
	/**
	 * 功能：产生整个网络的背景流
	 * host和交换机之间的入端口号假定再1-5之间，在构建mininet网络时，交换机之间的端口号不能为1-5之间
	 * @param n 点数
	 * @param num :需求数
	 * @param name :文件名
	 * @throws IOException
	 */
	public  void GenDemand(int numDpid,int numDem, String reqName, String trName) throws IOException {
		File reqFile = new File(reqName);
		File trFile = new File(trName);
		PrintWriter outReq = new PrintWriter(reqFile);
		PrintWriter outTr = new PrintWriter(trFile);

		outReq.println(numDem);
	    
		/*
		int k = 0;
		for (int i = 0; i < numDpid; i++)
			for(int j = i+1;  j < numDpid ; j++)
			{
				int flow = 409600;
				Demand dem = new Demand(k  , i, j, flow);
				req.add(dem);			
				outReq.println(dem.printDem());
				outTr.print(genTraffic(k++, i, j, flow));
			}
		*/
		
		for (int i = 0; i < numDem; i++) {
			int s = rand.nextInt(numDpid), t;
			do{
				t=rand.nextInt(numDpid);
			}while(s==t);
			//int flow = rand.nextInt(15)+2;
			int flow = 409600*10;//*(rand.nextInt(5)+1);
			Demand dem = new Demand(i, s, t, flow);
			req.add(dem);			
			outReq.println(dem.printDem());
			outTr.print(genTraffic( i, s, t, flow));
		}
		
		
		outReq.close();
		outTr.close();
	}
	
	public String genTraffic(int i, int s, int t, int flow){
		return "time x h" + (t+1) + " xterm -title d" + i + "_h" + (t+1) + "_recv -e ITGRecv -l log" + i + "\r\n"
				+ "py time.sleep(0.5)\r\n"
				+ "time x h" + (s+1) + " xterm -title d" + i + "_h" + (s+1) 
				+ "_send -e ITGSend -a 10.0.0." + (t+1) + " -T UDP -C " + 100 +" -c 5120 -t "+ FLOW_DURATION*60*1000 +" \r\n"
				+ "py time.sleep(0.5)\r\n";
	}
	
	@Override
	public void sflowCollected(Map<Integer, InterfaceStatistics> ifIndexIfStatMap){
		statisticsMap = sflowCollectionService.getStatisticsMap();
		updateTopo();
		//System.out.println("*********** sflow get new statistics: print all Edge **********");
		//System.out.println(incL);
		
	}

	//get all links.
	@Override
	public void topologyChanged(List<LDUpdate> linkUpdates) {
		long cur = System.currentTimeMillis();
		if(topoTS == 0){
			System.out.print("first update");
			topoTS = cur;
		}else{
			long interval = (cur - topoTS);
			System.out.print("topo update interval " + interval + " ms");
			topoTS = cur;
		}
		Map<DatapathId, Set<Link>>  dpidLinks = topologyService.getAllLinks();
		Set<DatapathId> dpidSet = dpidLinks.keySet();
		allLinks.clear();
		if(dpidSet.size() != 0)
			numDpid = dpidSet.size();
	
		System.out.println(", this time has " + numDpid + " sw");
		
		if(dpidSet != null){
			for(DatapathId dpid : dpidSet){
				Set<Link> linkSet = dpidLinks.get(dpid);
				if (linkSet == null) continue;
				//System.out.println(i++ + " " + dpid.toString() +  " : " + linkSet);
				allLinks.addAll(linkSet);
			}
		}
		numEdge = allLinks.size();
		
		updateTopo();
		
		//System.out.println("************* topologyChanged: print all links ***********");
		//System.out.println(allLinks);
	}
	
	double calUtil(){
		double util_temp=0.0;
		for(Edge lk:incL){
			if(util_temp<lk.bw/lk.capacity)
				util_temp=lk.bw/lk.capacity;
		}
		System.err.println("-----------  now utilization = "+util_temp);
		return util_temp;
	}
	
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> m =
				new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(ITopologyService.class);
		l.add(ISflowCollectionService.class);
		l.add(IThreadPoolService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		topologyService  = context.getServiceImpl(ITopologyService.class);
		sflowCollectionService = context.getServiceImpl(ISflowCollectionService.class);
		switchService = context.getServiceImpl(IOFSwitchService.class);
		//threadPoolService = context.getServiceImpl(IThreadPoolService.class);
		
		messageDamper = new OFMessageDamper(OFMESSAGE_DAMPER_CAPACITY,
				EnumSet.of(OFType.FLOW_MOD),
				OFMESSAGE_DAMPER_TIMEOUT);
		
		allLinks = new HashSet<Link>();
		incL = new ArrayList<Edge>();
		req = new ArrayList<Demand>();
		rand = new Random(System.currentTimeMillis());
		
		utilTestNum=0;
		utilSum=0.0;
		
		Map<String, String> config = context.getConfigParams(this);
		if (config.containsKey(ENABLED_STR)) {
			try {
				isEnabled = Boolean.parseBoolean(config.get(ENABLED_STR).trim());
			} catch (Exception e) {
				log.error("Could not parse '{}'. Using default of {}", ENABLED_STR, isEnabled);
			}
		}
		log.info("Coordinator {}", isEnabled ? "enabled" : "disabled");
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		if (isEnabled) {
			topologyService.addListener(this); 
			sflowCollectionService.addSflowListener(this);
			log.info("\n*****Coordinator starts*****");
			Runnable test = new TestTask();
			ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();  
			service.scheduleAtFixedRate(test, INITIAL_DEALY, PERIOD, TimeUnit.SECONDS); 
			
			Runnable test2 = new TestUtil();
			ScheduledExecutorService service2 =Executors.newSingleThreadScheduledExecutor();
			service2.scheduleAtFixedRate(test2, FLOW_DURATION*60/2, 5, TimeUnit.SECONDS);
		}
	}
	
	class TestUtil implements Runnable{
		public void run(){
			try{
				double util_temp=calUtil();
				utilTestNum++;
				utilSum+=util_temp;
				System.err.println("************ 最大链路利用率 是  " + utilSum/utilTestNum + "  ***************");
			}
			catch(Exception e){
    			e.printStackTrace();
    			return;
    		}
		}
	}
	
	
	class TestTask implements Runnable{
    	public void run(){
    		
    		int exitid = 1;
    		try{
    			GenerateDemand();
    			
    			long t0 = System.currentTimeMillis();
    			
    			switch(ROUTING_CHOICE){
    				case "SR": 
    					exitid = SelfishRouting.callSR();
    					break;
    				case "TE":
    					exitid = TrafficEngineering.callTE();
    					break;
    				case "Cheating":
    					exitid = Cheating.callCheat();
    					break;		
    				case "Voting":
    					exitid = Voting.callVoting();
    					break;
    				default:
    					System.out.println("No this routing service. Stay tuned.");
    			}
    			
    			long consuming = System.currentTimeMillis() - t0;
    			
    			if(exitid == 0)
    				System.out.println("routing success, takes " + consuming + " ms" );
    			else{
    				System.out.println("routing failed.");
    				return;
    			}
    		}
    		catch(Exception e){
    			e.printStackTrace();
    			return;
    		}
    		System.out.println("read path");
    		PushAllRoute();
    		System.out.println("算法完成计算 \n It's time to generate traffic.");
        }
    	
    }

}
