package net.floodlightcontroller.delaymonitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.threadpool.IThreadPoolService;

public class DelayMonitor implements IFloodlightModule, IDelayService {
	private static final Logger log = LoggerFactory.getLogger(DelayMonitor.class);

	private static IOFSwitchService switchService;
	private static IThreadPoolService threadPoolService;
	
	private static boolean isEnabled = false;
	
	private static int delayInterval = 10; /* could be set by REST API, so not final */
	private static ScheduledFuture<?> delayMonitor;
	
	private static final String INTERVAL_DELAY_STR = "monitorIntervalDelaySeconds";
	private static final String ENABLED_STR = "enable";
	
	private class LinkDelayMonitor implements Runnable {
		
		@Override
		public void run() {
			
		}
	}
	
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l = 
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IDelayService.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> m =
				new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		m.put(IDelayService.class, this);
		return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IOFSwitchService.class);
		l.add(IThreadPoolService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		switchService = context.getServiceImpl(IOFSwitchService.class);
		threadPoolService = context.getServiceImpl(IThreadPoolService.class);
		
		Map<String, String> config = context.getConfigParams(this);
		if (config.containsKey(ENABLED_STR)) {
			try {
				isEnabled = Boolean.parseBoolean(config.get(ENABLED_STR).trim());
			} catch (Exception e) {
				log.error("Could not parse '{}'. Using default of {}", ENABLED_STR, isEnabled);
			}
		}
		log.info("Delay monitor {}", isEnabled ? "enabled" : "disabled");
		
		if (config.containsKey(INTERVAL_DELAY_STR)) {
			try {
				delayInterval = Integer.parseInt(config.get(INTERVAL_DELAY_STR).trim());
			} catch (Exception e) {
				log.error("Could not parse '{}'. Using default of {}", INTERVAL_DELAY_STR, delayInterval);
			}
		}
		log.info("Port statistics collection interval set to {}s", delayInterval);
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		if (isEnabled) {
			startDelayMonitor();
		}
	}
	
	private void startDelayMonitor(){
		delayMonitor = threadPoolService.getScheduledExecutor().scheduleAtFixedRate(new LinkDelayMonitor(), delayInterval, delayInterval, TimeUnit.SECONDS);
		//tentativePortStats.clear(); /* must clear out, otherwise might have huge BW result if present and wait a long time before re-enabling stats */
		log.warn("Delay monitor thread(s) started"); 
	}

}
