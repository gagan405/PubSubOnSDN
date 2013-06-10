package net.floodlightcontroller.pubsub;

import java.util.HashSet;
import java.util.Set;

public class PubSubFlowUtil {
	
	/**
	 * Check if the given flow is already existing in the switch
	 * Either equal or covered by any other flow
	 * @param fl
	 * @param existingFlow
	 * @return
	 */
	
	public static boolean isFlowCovered(PubSubFlow fl, Set<PubSubFlow> existingFlow){
		for(PubSubFlow f : existingFlow){
			if(f.contains(fl)) return true;
		}
		return false;
	}
	
	/**
	 * Get the flows in the switch which will become irrelevant after adding this new flow
	 * @param fl
	 * @param existingFlow
	 * @return
	 */
	
	public static Set<PubSubFlow> getFullyCoveredFlows(PubSubFlow fl, Set<PubSubFlow> existingFlow){
		
		Set<PubSubFlow> coveredFlows = new HashSet<PubSubFlow>();
		
		for(PubSubFlow f : existingFlow){
			if(fl.contains(f)) coveredFlows.add(f);
		}
		
		return coveredFlows;
		
	}
	
	/**
	 * Get existing flow which is equivalent (not equal) to the given flow
	 * i.e. they differ in output ports
	 * @param fl
	 * @param existingFlow
	 * @return
	 */
	
	public static PubSubFlow getEquivalentFlow(PubSubFlow fl, Set<PubSubFlow> existingFlow){
		
		for(PubSubFlow f : existingFlow){
			if(f.dzEquals(fl)) return f;
		}
		
		return null;
	}
	
	/**
	 * Get Max DZ that is being delivered to the given port.
	 * @param port
	 * @param existingFlow
	 * @return
	 */

	public static dz getMaxDzRequired(Short port, Set<PubSubFlow> existingFlow ){
		dz d = null;
		
		for(PubSubFlow f : existingFlow){
			if(f.getOutport().contains(port)){
				if(d == null){
					d = f.getDZ();
					continue;
				}
				if(f.getDZ().contains(d)) d = f.getDZ();
			}
		}
		return d;
	}
	
	/**
	 * Get the Set of ports to which no Higher DZ is being sent.
	 * Used during Un-Advertisement.
	 * @param fl
	 * @param existingFlow
	 * @return
	 */
	
	public static Set<Short> getFilteredPorts(PubSubFlow fl, Set<PubSubFlow> existingFlow ){
		Set<Short> s = new HashSet<Short>();
		s.addAll(fl.getOutport());
		
		if(existingFlow.isEmpty()) return s;
		
		for(PubSubFlow f : existingFlow){
			if(f.getDZ().contains(fl.getDZ())){
				for(Short t : f.getOutport()){
					if(s.contains(t)){
						s.remove(t);
						continue;
					}
				}
			}
		}
		return s;
	}
	
	/**
	 * Get the set of flows which are candidates for deletion during unadvertisement.
	 * @param d
	 * @param inPort
	 * @param existingFlow
	 * @param newDz
	 * @return
	 */
	
	public static Set<PubSubFlow> getFlowTobeDeleted(dz d, Short inPort, Set<PubSubFlow> existingFlow, dz newDz){
		
		Set<PubSubFlow> fl = new HashSet<PubSubFlow>();
		
		for(PubSubFlow f : existingFlow){
			if((d.contains(f.getDZ())) && (inPort == f.getInport())){
				if(newDz != null){
					if(newDz.contains(f.getDZ())) continue;
				}
				fl.add(f); 
			}
		}
		
		return fl;
	}
	
	/**
	 * Get the max DZ that is incoming on a given Port.
	 * Used during UnSubscription.
	 * @param s
	 * @param existingFlow
	 * @return
	 */

	public static dz getMaxIncomingDz(Short s, Set<PubSubFlow> existingFlow){ 
		
		dz d = null;
		for(PubSubFlow f : existingFlow){
			if(f.getInport() != s) continue;
			if(d == null){
				d = f.getDZ();
			}
			else{
				if(f.getDZ().contains(d)) d = f.getDZ();
			}
		}
		return d;
	}
	
	/**
	 * Get a set of flows which are candidates for deletion/updates during unsubscription.
	 * @param d
	 * @param outPort
	 * @param existingFlow
	 * @param newDz
	 * @return
	 */
	
	public static Set<PubSubFlow> getFlowsForUnsubscription(dz d, Short outPort, Set<PubSubFlow> existingFlow, dz newDz){
		
		Set<PubSubFlow> fl = new HashSet<PubSubFlow>();
		
		for(PubSubFlow f : existingFlow){
			if((d.contains(f.getDZ())) && (f.getOutport().contains(outPort))){
				if(newDz != null){
					if(newDz.contains(f.getDZ())) continue;
				}
				fl.add(f); 
			}
		}
		
		return fl;
	}
	
	/**
	 * Get the flows which are covered by the new flow, but have different set of output ports
	 * @param fl
	 * @param existingFlow
	 * @return
	 */
	
	public static Set<PubSubFlow> getPartiallyCoveredFlows(PubSubFlow fl, Set<PubSubFlow> existingFlow){
		
		Set<PubSubFlow> coveredFlows = new HashSet<PubSubFlow>();
		
		for(PubSubFlow f : existingFlow){
			if(fl.dzCovers(f)) coveredFlows.add(f);
		}
		
		return coveredFlows;
		
	}
	
	/**
	 * Get existing flows which cover the new flow, but differ in output ports
	 * @param fl
	 * @param existingFlow
	 * @return
	 */
	
	public static Set<PubSubFlow> getPartiallyCoveringFlows(PubSubFlow fl, Set<PubSubFlow> existingFlow){
		
		Set<PubSubFlow> coveringFlows = new HashSet<PubSubFlow>();
		
		for(PubSubFlow f : existingFlow){
			
			if(f.dzCovers(fl)) coveringFlows.add(f);
		}
		
		return coveringFlows;
		
	}

}
