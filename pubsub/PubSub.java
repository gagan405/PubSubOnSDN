package net.floodlightcontroller.pubsub;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;

import net.floodlightcontroller.core.IFloodlightProviderService;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.Set;

import net.floodlightcontroller.linkdiscovery.*;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.UDP;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFType;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPacketIn;

import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.packet.IPv4;

import net.floodlightcontroller.staticflowentry.IStaticFlowEntryPusherService;
import net.floodlightcontroller.topology.ITopologyListener;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.topology.NodePortTuple;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;

public class PubSub implements ITopologyListener, IOFMessageListener, IFloodlightModule {
	
	private static final int MAX_DZ_LENGTH = 23;
	
	protected IFloodlightProviderService floodlightProvider;
	protected IStaticFlowEntryPusherService staticFlowEntryPusher;
	protected IRoutingService routingProvider;
	protected ITopologyService topologyProvider;
	protected ILinkDiscoveryService linkDiscovery;
	
	//protected Map<Link, LinkInfo> switchPortLinks;
	protected Map<Long, Set<Link>> swlinks;
	
	protected Set<Long> macAddresses;
	protected static Logger logger;
	protected int packetCount_1;
	protected int flowCount;
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "PubSub";
	}
	
	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		// TODO Auto-generated method stub
		
		Collection<Class<? extends IFloodlightService>> l =
		        new ArrayList<Class<? extends IFloodlightService>>();
		    
			l.add(IFloodlightProviderService.class);
		    l.add(IStaticFlowEntryPusherService.class);
		    l.add(IRoutingService.class);
		    l.add(ITopologyService.class);
		    l.add(ILinkDiscoveryService.class);
		    
		    return l;	
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		// TODO Auto-generated method stub
		
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		routingProvider = context.getServiceImpl(IRoutingService.class);
		topologyProvider = context.getServiceImpl(ITopologyService.class);
		linkDiscovery = context.getServiceImpl(ILinkDiscoveryService.class);

		//switchPortLinks = new HashMap<Link, LinkInfo>();
		
	    macAddresses = new ConcurrentSkipListSet<Long>();
	    staticFlowEntryPusher = context.getServiceImpl(IStaticFlowEntryPusherService.class);
	    logger = LoggerFactory.getLogger(PubSub.class);
	    packetCount_1 = 0;
	    flowCount = 0;

	}

	@Override
	public void startUp(FloodlightModuleContext context) {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		topologyProvider.addListener(this);
	}
	
	@Override
	public void topologyChanged(){
		// Update shortest paths and flow entries
		System.out.println("Topology changed! ");
		
		Map<Long, Set<Link>> swlinks_temp = linkDiscovery.getSwitchLinks();
		PubSubTreeManager.updateSwitchLinks(swlinks_temp);
		PubSubTreeManager.printLinks();
		PubSubTreeManager.printGraph();
		
		return;
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(
			IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		
		Ethernet eth =  IFloodlightProviderService.bcStore.get(cntx,
                                            IFloodlightProviderService.CONTEXT_PI_PAYLOAD);

		OFPacketIn pi = (OFPacketIn) msg;

		OFMatch match = new OFMatch();
		match.loadFromPacket(pi.getPacketData(), (short) 0);

		if(eth.getEtherType() == Ethernet.TYPE_IPv4){
			
		//	System.out.println(IPv4.fromIPv4Address(match.getNetworkDestination()));
			
			//Sent to IP-fix -- Advertisement / Un-Advertisement / Subscription Request / Un-Subscription Request
			if (IPv4.fromIPv4Address(match.getNetworkDestination()).equals("225.37.0.0")){
				int type;
				int length;
				
				IPv4 ipPkt = (IPv4)eth.getPayload();
					if(ipPkt.getProtocol() == IPv4.PROTOCOL_UDP){

						UDP udpPkt = (UDP)ipPkt.getPayload();
						Data dataPkt = (Data)udpPkt.getPayload();

						byte[] arr = dataPkt.getData();    		
						
						if(arr.length != 8){
							System.out.println("Invalid request!");
							return Command.STOP;
						}
							
						long swID = sw.getId();
						short portNum = pi.getInPort();
						
						final NodePortTuple n = new NodePortTuple(swID, portNum);
						
						type = (arr[0] & 7);
						length = (arr[1] & 0b00111111);
						
						if((length > MAX_DZ_LENGTH) && (type != 4)){
							return Command.STOP;
						}
						
						if((length != 32) && (type == 4)){
							return Command.STOP;
						}
						
						/**
						 * Extract the dz from the byte array
						 */
						/* Easy but inefficient!
						boolean[] temp = new boolean[length];
						boolean[] temp1 = new boolean[40];
						int pos = 0;
						
					
						for(int i = 2; i < 7; i ++){
							for(int index = 0; index < 8; index++){
								byte mask = (byte) ((byte)(0 | 1) << (7-index));
								
								if((arr[i] & mask) != 0)
									temp1[((i-2) << 3) + index] = true;
								else
									temp1[((i-2) << 3) + index] = false;
							}
						}
						
						 */
						
						int bit_index = ((length & 7) == 0)? 7 : ((length & 7) -1);
						
						boolean[] arr1 = new boolean[length];	
						
						for(int byte_index = (3+ ((40 - length) >> 3)); byte_index < 8; byte_index++ ){
							while(bit_index >= 0){
								arr1[length - ((7-byte_index) << 3) - bit_index - 1] = 
										((arr[byte_index] & (byte)((byte)(0|1) << bit_index)) != 0);
								bit_index--;
							}
							bit_index = 7;
						}
							
//						temp = Arrays.copyOfRange(temp1, 40-length, 40);
//						dz d = new dz(temp);
						final dz d = new dz(arr1);
						
						switch(type){
						case 0:
							//New Publisher
							/*addPublisherThread = new Thread(new Runnable() {
					        @Override
					        public void run() {
					                  addPublisher(d,n);
					                  return;
					                    }
					        }, "Add Publisher");
					        addPublisherThread.start();
					       */ 
							addPublisher(d, n);
							/**
							 * For debug purposes
							 */
							
							PubSubTreeManager.PrintTree(d);
							
							break;
						case 1:
							//Unadvertisement -- removal of publisher
							removePublisher(n,d);
							break;
						case 2:
							//Subscription request
							addSubscriber(d, n);
							System.out.println("Added Subscriber for : " + d.toString());
						//	PubSubTreeManager.printSubscribers(d);
						//	PubSubTreeManager.printSubscribers(null);
							
							break;
						case 3:
							//UnSubscription -- removal of subscriber
							removeSubscriber(n,d);
							break;
							
						case 4:
							//Set IP for subscriber
							setIPforSubscriber(n, d);
							break;
							
						case 5:
							deleteAllSubs();
							break;
							
						case 6:
							deleteAllPubs();
							break;	
						}
						
					/*for (int i = 0; i < dataPkt.getData().length; i++){
							System.out.print((char)arr[i]);
						}
					*/
	//					PubSubTreeManager.PrintAllTrees();
					}			
			}
		}    
        return Command.STOP;
			
	}
	
	
	public void deleteAllPubs(){
		PubSubTreeManager.deleteTree();
	}
	
	public void deleteAllSubs(){
		Set<PubSubTree> t = PubSubTreeManager.getAllTrees();
		if(t == null) return;
		if(t.isEmpty()) return;
		
		for(PubSubTree tree : t){
			Map<Long, Set<PubSubFlow>> m = tree.getAllFlows();
			for(long l : m.keySet()){
				Set<PubSubFlow> fl = m.get(l);
				for(PubSubFlow f : fl){
					staticFlowEntryPusher.deleteFlow(f.getName());
				}
			}
			tree.deleteAllFlows();
		}
		
		PubSubTreeManager.removeSubscriber();
	}
	
	/**
	 * Method : addFlow
	 * Description : Adds flow for particular dz along a path (Src-Dst)
	 * @param dz
	 * @param path
	 */
	
	public void addFlow(dz d, List<NodePortTuple> path, PubSubTree tree, dz subDZ){
		
		//return if tree or path is null
		if(tree == null) return;
		if(path == null) return;
		
		dz key = null;
		
		//Choose the lowest dz
		if(tree.getDz().contains(d)) key = new dz(d);
		else key = new dz(tree.getDz());
		
		//dz to IP
    	String ip = key.toIP();
    	
    	if(ip == null){
    		System.out.println("ERROR: invalid dz (too long)");
    		return;
    	}
    	boolean flagSameHost = false;
    	
    	if(path.size() == 2){
    		if(path.get(0).getPortId() == path.get(1).getPortId())
    			flagSameHost = true;
    	}
    	//Travel along source to destination
    	for(int i = 0; i < path.size(); i+=2){
    		    		
    		//For each switch
    		NodePortTuple n = path.get(i+1);
    		
    		Long swid = path.get(i).getNodeId();
    		Short inPort = path.get(i).getPortId();
    		Short outPort = path.get(i+1).getPortId();
    		
    		if(flagSameHost) outPort = (short)0xfff8 ;
    		
    		IOFSwitch sw = floodlightProvider.getSwitches().get(swid);
    		
    		Set<PubSubFlow> existingFlow = tree.getFlowForSwitch(swid);
    		
    		Set<Short> out = new HashSet<Short>();
			out.add(outPort);
			
    		String flowName = "PS_" + swid + "_" + flowCount;
    		PubSubFlow fl = new PubSubFlow(flowName, key, swid, inPort, out);
    		fl.setPriority(16384);
    		
    		/**
    		 * 1: If no flow exists --> add this flow
    		 * 
    		 * 2: If existing flow covers new flow --> Do nothing
    		 * 
    		 * 3: If existing flow's dz is equal to the new flow but differs in port
    		 * 	  update the existing flow with new set of ports
    		 * 
    		 * 4: If existing flow partially covers new flow
    		 * 	  i.e. 00 -> 2, new 000 -> 3 (ports do not match)
    		 *    add ports of covering flows to the new flow and set priority appropriately
    		 *    push new flow
    		 * 
    		 * 5: If new flow covers some of existing flows --> delete covered flows
    		 * 
    		 * 6: If new flow partially covers existing flows (i.e. ports do not match)
    		 * 	  Update all covered flows with new output ports. (Delete and push new)
    		 *    Set priority accordingly
    		 *    push new flow     
    		 */
    		
    		//If this is the last switch, we have to add a field set-dst-ip in the actions of the flow
    	//	if(i == path.size() - 2){
    	//		fl.setDstIP(subDZ.toIP(), outPort);
    	//	}
    		
    		if(i == path.size() - 2){
    			fl.setDstIP(PubSubTreeManager.getIPForSub(n), outPort);
    		}
    		
    		//If no flow with any related DZ exist, add new flow
    		if(existingFlow == null){
    			//Tested
    			tree.addFlow(swid, fl);
    	   	
    	    	/**
    	    	 * The following method was not a part of Floodlight framework originally.
    	    	 * I created this as I couldn't figure out how to set network mask
    	    	 * even after months of discussion in the mailing list with the developers.
    	    	 * 
    	    	 */
    	    
    	    	staticFlowEntryPusher.addFlowFromJSON(flowName, fl.toJSON(sw.getStringId()), sw.getStringId());
    	    	flowCount++;
    	    	existingFlow = null;
    	    	
    		}
    		
    		else{
    
    			//Check if the new flow is already there, either equivalent flow
    			//Or contained by any other flow
    			
    			if(PubSubFlowUtil.isFlowCovered(fl, existingFlow)) continue;
    			
    			Set<PubSubFlow> coveredFlows = null;
    			
    			//get flows that are covered by the new flow
    			//Delete these flows as they are of no use now
    			
    			coveredFlows = PubSubFlowUtil.getFullyCoveredFlows(fl, existingFlow);
    			
    			if(coveredFlows != null)
    				if(!coveredFlows.isEmpty()){
    					for(PubSubFlow cf : coveredFlows){
    						staticFlowEntryPusher.deleteFlow(cf.getName());
    						tree.deleteFlow(swid, cf);
    					}
    				}
    			
    			boolean equivelntFlowSet = false;
    	
    			boolean prioritySet = false;
    			Set<PubSubFlow> partiallyCoveringFlows = new HashSet<PubSubFlow>();
    			partiallyCoveringFlows = PubSubFlowUtil.getPartiallyCoveringFlows(fl, existingFlow);
    			
    			//For each partially covering flows, add the outputs to this new flow
    			//Set priority if it is not already set
    			
    			if(partiallyCoveringFlows != null)
    				if(!partiallyCoveringFlows.isEmpty()){
    					int prio = 0;
    					int length = 0;
    					
    					for(PubSubFlow cf : partiallyCoveringFlows){
    						
    						if(cf.dzEquals(fl)) continue;
    						
    						fl.addOutPorts(cf.getOutport());
    						
    						for(Short p : fl.getOutport()){
    							if(cf.isDstIPSet(p))
    								fl.setDstIP(cf.getDstIP(p), p);
    						}
    						
    						//Get the highest priority of all covered flows
    						if(cf.getDZ().getLength() > length)
    							length = cf.getDZ().getLength();
					
    						if(cf.getPriority() > prio)
    							prio = cf.getPriority();		
    					}
    			
    					if(!prioritySet){
    						fl.setPriority(prio + fl.getDZ().getLength() - length);
    						prioritySet = true;
    					}
    				}
    			
    			Set<PubSubFlow> partiallyCoveredFlows = null;
    			partiallyCoveredFlows = PubSubFlowUtil.getPartiallyCoveredFlows(fl, existingFlow);
    			
    			//To each of these partially covered flows, add this output port
    			
    			if(partiallyCoveredFlows != null)
    				if(!partiallyCoveredFlows.isEmpty()){
    					int length = 32767;
    					int prio = 32767;
    					
    					for(PubSubFlow cf : partiallyCoveredFlows){
    						if(cf.getDZ().equals(fl.getDZ()))
    							equivelntFlowSet = true;
    						
    						PubSubFlow newFlow = new PubSubFlow(cf.getName(), cf.getDZ(), cf.getSWID(), cf.getInport(), cf.getOutport());
    						newFlow.setPriority(cf.getPriority());
    						newFlow.addOutPorts(fl.getOutport());
    								
    						newFlow.addPortIPMapping(cf.getPortIPMapping());
    						newFlow.addPortIPMapping(fl.getPortIPMapping());
    						
    						//Get the lowest priority of all covered flows
    						if(cf.getDZ().getLength() < length)
    							length = cf.getDZ().getLength();
    						
    						if(cf.getPriority() < prio)
    							prio = cf.getPriority();
    						
    						
    						staticFlowEntryPusher.deleteFlow(cf.getName());
    						tree.deleteFlow(swid, cf.getName());
    						
    						staticFlowEntryPusher.addFlowFromJSON(newFlow.getName(), newFlow.toJSON(sw.getStringId()), sw.getStringId());
    						tree.addFlow(swid, newFlow);
    					}
    			
    					if(!prioritySet)
    						fl.setPriority(prio - (length - fl.getDZ().getLength()));
    			
    				}
    			
    			
    			if(!equivelntFlowSet){
    				staticFlowEntryPusher.addFlowFromJSON(flowName, fl.toJSON(sw.getStringId()), sw.getStringId());
    				tree.addFlow(swid, fl);
    				flowCount++;
    			}
    			
    			existingFlow = null;
    			partiallyCoveredFlows = null;
    			partiallyCoveringFlows = null;
    			coveredFlows = null;
    		}
    	}
    		    
    }
	
	
	/**
	 * Set IP for a subscriber
	 */
	
	public void setIPforSubscriber(NodePortTuple n, dz d){
		PubSubTreeManager.setIPForSub(d.toIP(true), n);
	}
	/**
	 * Invoked during unSubscription request
	 */
	
	public void removeSubscriber(NodePortTuple sub, dz d){
		
		//Get relevant trees
		ArrayList<PubSubTree> t = PubSubTreeManager.getRelevantTrees(d);
		Host subscriber = new Host(sub, d, false);
				
		//For each tree
		for(PubSubTree tree : t){
			recursiveUnsub(d, sub, tree, null);
		}
		
		PubSubTreeManager.removeSubscriber(d, subscriber);
		
	}
	
	public void recursiveUnsub(dz d, NodePortTuple sub, PubSubTree tree, dz newDz){
		//Get existing flows for this switch
		Set<PubSubFlow> existingFlow = tree.getFlowForSwitch(sub.getNodeId());
		Long swid = sub.getNodeId();
		IOFSwitch sw = floodlightProvider.getSwitches().get(swid);
		
		if(existingFlow == null) return;
		if(existingFlow.isEmpty()) return;
				
		//Get the flows to be deleted -- covered flows with the same out-port having dz s.t. d >= dz > newDz
		Set<PubSubFlow> fl = PubSubFlowUtil.getFlowsForUnsubscription(d, sub.getPortId(), existingFlow, newDz);
		
		existingFlow.removeAll(fl);
		
		Set<Short> inPorts = new HashSet<Short>();
		
		//For each of these flows, remove this particular output port from the output list
		
		for(PubSubFlow f: fl){
			
			PubSubFlow newFlow = new PubSubFlow(f.getName(), f.getDZ(), f.getSWID(), f.getInport(), f.getOutport());
			newFlow.setPriority(f.getPriority());
			newFlow.addPortIPMapping(f.getPortIPMapping());
			
			newFlow.removeOutPorts(sub.getPortId());
			
			staticFlowEntryPusher.deleteFlow(f.getName());
			tree.deleteFlow(swid, f);
			inPorts.add(f.getInport());
			
			//If this was the only subscriber
			if(newFlow.getOutport().isEmpty()){
				//if newDZ is null, do nothing				
				if(newDz != null){
					newFlow.addOutPorts(sub.getPortId());
					newFlow.setPriority(newFlow.getPriority() + newDz.getLength() - f.getDZ().getLength());
					newFlow.setDZ(newDz);
					
					staticFlowEntryPusher.addFlowFromJSON(newFlow.getName(), newFlow.toJSON(sw.getStringId()), sw.getStringId());
					tree.addFlow(swid, newFlow);
				}
			}
		
			else{
				//There are other subscribers! with same DZ
				if(f.getDZ().contains(d))
					inPorts.remove(f.getInport());
				
				if(PubSubFlowUtil.isFlowCovered(newFlow, existingFlow)) continue;
				
				//Modify existing flows with updated output ports
				staticFlowEntryPusher.addFlowFromJSON(newFlow.getName(), newFlow.toJSON(sw.getStringId()), sw.getStringId());
    			tree.addFlow(swid, newFlow);
    			flowCount++;	 			
   			}
		}	
		
		//Add new flows if newDz is not null
				
		if(newDz != null){
			if(!existingFlow.isEmpty())
					addFlowForDZ(swid, tree, newDz, sub.getPortId(), null);
		}
		
		//For each input port
		//Get maxDZ required before and after unsubscription
		
		for(Short s : inPorts){
			
			dz MaxDz = null;
			dz MinDz = null;
			
			dz oldDz = PubSubFlowUtil.getMaxIncomingDz(s, fl);
			dz nDz = PubSubFlowUtil.getMaxIncomingDz(s, existingFlow);
			
			if(nDz != null){
				if(nDz.contains(oldDz)) continue;
				else if(nDz.contains(newDz) || (newDz == null)){
					MaxDz = oldDz;
					MinDz = nDz;
				}
			}
			else{
				MaxDz = oldDz;
				MinDz = newDz;
			}
			
			NodePortTuple nextNode = tree.getNextNodes(sub.getNodeId(), s);
			if(nextNode != null){
				recursiveUnsub(MaxDz, nextNode, tree, MinDz);
			}
		}
		
	}
	
	public void addFlowForDZ(Long swid, PubSubTree tree, dz d, Short outPort, Short inPort){
		Set<PubSubFlow> existingFlow = tree.getFlowForSwitch(swid);
		Set<Short> inPorts = new HashSet<Short>();
		Set<Short> outPorts = new HashSet<Short>();
		
		if(outPort != null) outPorts.add(outPort);
		if(inPort != null) inPorts.add(inPort);
		
		Map<Short, String> portIPmap = new HashMap<Short, String>();
		
    	int prio = 0;
    	int length = 0;
    	IOFSwitch sw = floodlightProvider.getSwitches().get(swid);
    	
    	for(PubSubFlow f : existingFlow){
    		if(f.getDZ().contains(d)){
    			portIPmap.putAll(f.getPortIPMapping());
    			inPorts.add(f.getInport());
    			outPorts.addAll(f.getOutport());
    			if(f.getPriority() > prio) prio = f.getPriority();
    			if(f.getDZ().getLength() > length) length = f.getDZ().getLength();
    		}
    	}
    	
    	prio = prio + d.getLength() - length;
    	
    	for(Short port : inPorts){
    	
    		String flowName = "PS_" + swid + "_" + flowCount;
    		PubSubFlow fl = new PubSubFlow(flowName, d, swid, port, outPorts );
			fl.setPriority(prio);
			fl.addPortIPMapping(portIPmap);
			
			if(PubSubFlowUtil.isFlowCovered(fl, existingFlow)) continue;
			
			PubSubFlow eqFlow = PubSubFlowUtil.getEquivalentFlow(fl, existingFlow);
			if(eqFlow != null){
				eqFlow.addOutPorts(fl.getOutport());
				eqFlow.addPortIPMapping(fl.getPortIPMapping());
				
				staticFlowEntryPusher.deleteFlow(eqFlow.getName());
				tree.deleteFlow(swid, eqFlow);
				
				staticFlowEntryPusher.addFlowFromJSON(eqFlow.getName(), 
									eqFlow.toJSON(sw.getStringId()), sw.getStringId());
				
				tree.addFlow(swid, eqFlow);
			}
			
			else{
				staticFlowEntryPusher.addFlowFromJSON(fl.getName(), fl.toJSON(sw.getStringId()), sw.getStringId());
				tree.addFlow(swid, fl);
				flowCount++;
			}	
    	}
		
	}
	
		
	/**
	 * Method : remove Publisher
	 * Invoked during un-Advertisement
	 * 
	 */
	
	public void removePublisher(NodePortTuple pub, dz d){
		//Get relevant trees
		ArrayList<PubSubTree> t = PubSubTreeManager.getRelevantTrees(d);
		Host publisher = new Host(pub, d, true);
		
		//For each tree
		for(PubSubTree tree : t){
			recursiveUnadvt(d, pub, tree, null);
			tree.removePublisher(publisher);
			if(tree.getPublishers().isEmpty()) PubSubTreeManager.deleteTree(tree.getDz());
		}
	}
	
	/**
	 * Recursively remove/update flows in a given tree, for un-advertisement situation
	 * @param d
	 * @param pub
	 * @param tree
	 */
	
	public void recursiveUnadvt(dz d, NodePortTuple pub, PubSubTree tree, dz newDz){
		
		//Get existing flows for this switch
		Set<PubSubFlow> existingFlow = tree.getFlowForSwitch(pub.getNodeId());
		
		if(existingFlow == null) return;
		if(existingFlow.isEmpty()) return;
		
		Long swid = pub.getNodeId();
		IOFSwitch sw = floodlightProvider.getSwitches().get(swid);
		
		//Get the flows to be deleted -- covered flows with the same in-port having dz s.t. d >= dz > newDz
		Set<PubSubFlow> fl = PubSubFlowUtil.getFlowTobeDeleted(d, pub.getPortId(), existingFlow, newDz);		
		
		//ignore the output ports to which more dzs are going
		
		Set<Short> outPorts = new HashSet<Short>();
		//Set<Short> newFlowPorts = new HashSet<Short>();
		Map<Short, String> ipPortMap = new HashMap<Short, String>();
		
		existingFlow.removeAll(fl);
		
		//The Set fl contains all the flows with DZ more than newDZ and hence need to be deleted
		//boolean flowSet = false;
		PubSubFlow newFlow = null;
		int priority = 0;
		int length = 0;
	
		for(PubSubFlow f : fl){
			staticFlowEntryPusher.deleteFlow(f.getName());
			tree.deleteFlow(pub.getNodeId(), f);
			
			outPorts.addAll(f.getOutport());
			ipPortMap.putAll(f.getPortIPMapping());
			
			//get maximum prio and length of the flows
			if(f.getPriority() > priority)	priority = f.getPriority();
			if(f.getDZ().getLength() > length)   length = f.getDZ().getLength();
		}
		
		if(newDz != null){
			String flowName = "PS_" + swid + flowCount;
			newFlow = new PubSubFlow(flowName, newDz, pub.getNodeId(), pub.getPortId(), outPorts);
			newFlow.addPortIPMapping(ipPortMap);
			newFlow.setPriority(priority + newDz.getLength() - length);
			
			if(!PubSubFlowUtil.isFlowCovered(newFlow, existingFlow)){
				PubSubFlow equivalentFlow = PubSubFlowUtil.getEquivalentFlow(newFlow, existingFlow);
				
				if(equivalentFlow == null){
					staticFlowEntryPusher.addFlowFromJSON(newFlow.getName(), newFlow.toJSON(sw.getStringId()), sw.getStringId());
					tree.addFlow(swid, newFlow);
					flowCount++;
				}
			}
		}
		
		
		
		/*
		for(PubSubFlow f : fl){
			
			staticFlowEntryPusher.deleteFlow(f.getName());
			tree.deleteFlow(pub.getNodeId(), f);
			outPorts.addAll(PubSubFlowUtil.getFilteredPorts(f, existingFlow));
			newFlowPorts.addAll(f.getOutport());
			ipPortMap.putAll(f.getPortIPMapping());
			
			//get maximum prio and length of the flows
			if(f.getPriority() > priority)	priority = f.getPriority();
			if(f.getDZ().getLength() > length)   length = f.getDZ().getLength();
			
			if(!flowSet && newDz != null){
				newFlow = new PubSubFlow(f.getName(), newDz, f.getSWID(), f.getInport(), f.getOutport());
				flowSet = true;
			}
			
		}
		
		//Add new flow with newDZ
		if(newFlow != null){
			newFlow.addOutPorts(newFlowPorts);
			newFlow.setPriority(priority + newDz.getLength() - length);
			newFlow.addPortIPMapping(ipPortMap);
			
			if(!PubSubFlowUtil.isFlowCovered(newFlow, existingFlow)){
				PubSubFlow equivalentFlow = PubSubFlowUtil.getEquivalentFlow(newFlow, existingFlow);
			
				if(equivalentFlow == null){
					staticFlowEntryPusher.addFlowFromJSON(newFlow.getName(), newFlow.toJSON(sw.getStringId()), sw.getStringId());
					tree.addFlow(swid, newFlow);
					flowCount++;
				}
				else{
					equivalentFlow.addOutPorts(newFlow.getOutport());
					equivalentFlow.addPortIPMapping(newFlow.getPortIPMapping());
			
					staticFlowEntryPusher.deleteFlow(equivalentFlow.getName());
					tree.deleteFlow(swid, equivalentFlow);
				
					staticFlowEntryPusher.addFlowFromJSON(equivalentFlow.getName(), 
									equivalentFlow.toJSON(sw.getStringId()), sw.getStringId());
				
					tree.addFlow(swid, equivalentFlow);
				}
			}
		}
		
		*/
		//if(outPorts.isEmpty()) return;
		
		//For the remaining output ports, get the maximum DZ needed, and recursively
		//downgrade the flows
		
		for(Short s : outPorts){
			//dz from this input port 
			dz oldDz = PubSubFlowUtil.getMaxDzRequired(s, fl);
			
			//dz from other input ports
			dz nDz = PubSubFlowUtil.getMaxDzRequired(s, existingFlow);
			
			//Get the max DZ that was going to this port
			//And the Min DZ which needs to be set for this port
			dz MaxDz = null;
			dz MinDz = null;
			
			if(nDz == null){
				MaxDz = oldDz;
				MinDz = newDz;
			}
			else{
				if(nDz.contains(oldDz))
					continue;
				else if((newDz == null) || (nDz.contains(newDz))){
					MaxDz = oldDz;
					MinDz = nDz;
				}
				else if((newDz != null) && (newDz.contains(nDz))){
					MaxDz = oldDz;
					MinDz = newDz;
				}
			}
			
			NodePortTuple nextNode = tree.getNextNodes(pub.getNodeId(), s);
			if(nextNode != null){
				recursiveUnadvt(MaxDz, nextNode, tree, MinDz);
			}
			
		}
		
		return;	
	}
		
	/**
	 * Method : addSubscriber
	 * Invoked during new subscription request
	 * @param dz
	 * @param sub
	 */
	
	public void addSubscriber(dz d, NodePortTuple sub){
		Host s = new Host(sub, d, false);
		if(!PubSubTreeManager.addSubscriber(d, s)) return;
		
		//get relevant trees
		ArrayList<PubSubTree> t = PubSubTreeManager.getRelevantTrees(d);
		
		if(t == null) return;
		if(t.size() == 0) return;
		
		//get publishers for each tree
		for(PubSubTree tree : t){
			ArrayList<Host> pubs = tree.getPublishers();
			//for each publisher in the tree, get paths
			for(Host pub : pubs){
				ArrayList<NodePortTuple> rt = tree.buildRoute(pub.getAttachmentPoint().getNodeId()
															, sub.getNodeId());
				rt.add(0, pub.getAttachmentPoint());
				rt.add(sub);
				
				//in each path add flows
				if(tree.getDz().contains(d)) addFlow(d, rt, tree, d);
				else addFlow(tree.getDz(), rt, tree, d);
			}
		}
	}
	
	
	/**
	 * Method : addPublisher
	 * Invoked during new advertisement
	 * @param dz
	 * @param pub
	 */
	public void addPublisher(dz d, NodePortTuple pub){
		//get relevant trees
		ArrayList<PubSubTree> t = PubSubTreeManager.getRelevantTrees(d);
		boolean createNewTrees = false;
		
		if(t == null){
			Host h = new Host(pub, d, true);
			PubSubTree tree = PubSubTreeManager.buildTree(pub.getNodeId(), d, h);
//			PubSubTreeManager.PrintTree(d);
			
			//Get paths and add flows for relevant subscribers
			Set<Host> subscribers = PubSubTreeManager.getInterestedSubscribers(d);
			if(subscribers == null) return;
			
			for(Host sub : subscribers){
				ArrayList<NodePortTuple> rt = PubSubTreeManager.getRoute(d, pub.getNodeId(), sub.getAttachmentPoint().getNodeId());
				rt.add(sub.getAttachmentPoint());
				rt.add(0, pub);
				
				//add flows
				if(d.contains(sub.getDz())) addFlow(sub.getDz(), rt, tree, sub.getDz());
				else addFlow(d, rt, tree, sub.getDz());
			}
			
			return;
		}
		
		for(PubSubTree tree: t){
			int c = compareDZ(tree.getDz(), d);
			if((c == 0) || (c == 1)){
				if(tree.hasPublisher(new Host(pub, d, true))){
					//Nothing to do
					break;
				}
				//New publisher's dz is already covered by some tree
				//Join the existing trees
				tree.addPublisher(new Host(pub, d, true));
				//get paths for all the subscribers for this tree and add flows
				
				Set<Host> subscribers = PubSubTreeManager.getInterestedSubscribers(d);
				if(subscribers == null) break;
				
				for(Host sub : subscribers){
					ArrayList<NodePortTuple> rt = tree.buildRoute(pub.getNodeId(), sub.getAttachmentPoint().getNodeId());
					rt.add(sub.getAttachmentPoint());
					rt.add(0, pub);
					
					if(d.contains(sub.getDz()))	addFlow(sub.getDz(), rt, tree, sub.getDz());
					else addFlow(d, rt, tree, sub.getDz());
				}
				
				break;
			}
			else if(c == 2){
				//New publisher's dz covers existing trees
				//Divide the dz and create child trees if they are not existing
				//join existing child trees
				
				if(!tree.hasPublisher(new Host(pub, d, true))){
					tree.addPublisher(new Host(pub, d, true));
							
					Set<Host> subscribers = PubSubTreeManager.getInterestedSubscribers(tree.getDz());
					if(subscribers == null) continue;
					
					for(Host sub : subscribers){
						ArrayList<NodePortTuple> rt = PubSubTreeManager.getRoute(tree.getDz(), pub.getNodeId(), sub.getAttachmentPoint().getNodeId());
						rt.add(sub.getAttachmentPoint());
						rt.add(0, pub);
						
						//add flows
						if(d.contains(sub.getDz())) addFlow(sub.getDz(), rt, tree, sub.getDz());
						else addFlow(d, rt, tree, sub.getDz());
					}
				
				}
				createNewTrees = true;
				continue;
			}	
		}
		if(createNewTrees){
			Set<dz> newKeys = getNewKeys(d);
			if(newKeys == null) return;
			
			for(dz key : newKeys){
				PubSubTree tree = PubSubTreeManager.buildTree(pub.getNodeId(), key, new Host(pub, d, true));
				//get subscribers interested in this dz
				
				Set<Host> subscribers = PubSubTreeManager.getInterestedSubscribers(key);
				if(subscribers == null) continue;
				
				//get routes for each of them
				for(Host sub : subscribers){
					ArrayList<NodePortTuple> rt = PubSubTreeManager.getRoute(key, pub.getNodeId(), sub.getAttachmentPoint().getNodeId());
					rt.add(sub.getAttachmentPoint());
					rt.add(0, pub);
					
					//add flows
					if(key.contains(sub.getDz())) addFlow(sub.getDz(), rt, tree, sub.getDz());
					else addFlow(key, rt, tree, sub.getDz());
				}
			}
		}
			
	}
	
	/**
	 * Get new dzs of the trees that are to be created
	 * @param d
	 * @return
	 */
	
	public Set<dz> getNewKeys(dz d){
		Set<dz> newKeys = new HashSet<dz>();
		ArrayList<dz> s = PubSubTreeManager.getKeys(d);
		
		if(s == null){
			newKeys.add(d);
			return newKeys;
		}
			
		int length = s.get(0).getLength() - d.getLength();
		for(int j = 0; j < (int)Math.pow(2, length); j++){
			boolean[] temp = intToBoolean(j,length);
			temp = concat(d.getArray(),temp);
			if(PubSubTreeManager.treeList.containsKey(new dz(temp))) continue;
			
			ArrayList<dz> arr = PubSubTreeManager.getKeys(new dz(temp));
			if(arr != null){ 
				if (!arr.isEmpty()) newKeys.addAll(getNewKeys(new dz(temp)));
			}
			else{
				newKeys.add(new dz(temp));
			}
				
		}
		if(newKeys.isEmpty()) return null;
		return newKeys;

	}
	
	/**
	 * Concatenate two boolean arrays
	 * @param A
	 * @param B
	 * @return
	 */
	
	public static boolean[] concat(boolean[] A, boolean[] B) {
		   int aLen = A.length;
		   int bLen = B.length;
		   boolean[] C= new boolean[aLen+bLen];
		   System.arraycopy(A, 0, C, 0, aLen);
		   System.arraycopy(B, 0, C, aLen, bLen);
		   return C;
	}
	
	/**
	 * Convert Integer to a boolean array of specified width
	 * @param n
	 * @param base
	 * @return
	 */

	public static boolean[] intToBoolean(int n, int base){
		
		    final boolean[] ret = new boolean[base];
		    for (int i = 0; i < base; i++) {
		        ret[base - 1 - i] = (1 << i & n) != 0;
		    }
		    return ret;
	}
	
	/**
	 * Convert boolean to integer.
	 * Used to make IP addresses out of DZ expressions
	 */
	
	public static int booleansToInt(boolean[] arr){
	    int n = 0;
	    for (boolean b : arr)
	        n = (n << 1) | (b ? 1 : 0);
	    return n;
	}
	
	/**
	 * Compare boolean arrays (dzs)
	 * -1 if unrelated, 0 if equal, 1 if t is a subset of s
	 *  2 if s is a subset of t
	 * @param s
	 * @param t
	 * @return
	 */
	
	public int compareDZ(dz s, dz t){
		int result = -1; // unrelated dz 
		
		if (s.equals(t)) { result = 0; return result; }
		
		// t is a subset of s in dz space
		if (s.contains(t)) { result = 1; return result; }
		
		// s is a subset of t in dz space
		if (t.contains(s)) { result = 2; return result; }
		
		return result;
	}

}