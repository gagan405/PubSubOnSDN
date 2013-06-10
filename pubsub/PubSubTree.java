package net.floodlightcontroller.pubsub;

import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.topology.NodePortTuple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class PubSubTree {
	
	private dz d;
	private ArrayList<Host> publishers;
	
	private Long rootID;
	private Map<Long, PubSubTreeNode> tree;
	private Map<Long, Set<PubSubFlow>> flowList;
	
	public PubSubTree(Map<Long, PubSubTreeNode> t, Long root, dz d){
		this.tree = t;
		this.rootID = root.longValue();
		this.d = d;
		this.flowList = null;
	}
	
	public void updateTree(Map<Long, PubSubTreeNode> t){
		this.tree = t;
	}
	
	public Long getRootID(){
		return this.rootID;
	}
	
	public dz getDz(){
		return d;
	}
	
	public Map<Long, Set<PubSubFlow>> getAllFlows(){
		return flowList;
	}
	
	public void deleteAllFlows(){
		flowList = null;
	}
	
	public Set<PubSubFlow> getFlowForSwitch(Long swID){
		if(flowList == null) return null;
		if(flowList.isEmpty()) return null;
		return flowList.get(swID);
	}
	
	public void addFlow(Long swID, PubSubFlow flow){
		if(flowList == null) flowList = new HashMap<Long, Set<PubSubFlow>>();
		
		Set<PubSubFlow> f = null;
		
		if(flowList.containsKey(swID)){
			f = flowList.get(swID);
		}
		else
			f = new HashSet<PubSubFlow>();
		
		f.add(flow);
		flowList.put(swID, f);
	}
	
	public void deleteFlow(Long swID, PubSubFlow fl){
		if(flowList == null) return;
		if(!flowList.containsKey(swID)) return;
		
		Set<PubSubFlow> f = flowList.get(swID);
		f.remove(fl);
		flowList.remove(swID);
		flowList.put(swID, f);
		
	}
	
	public void deleteFlow(Long swID, String FlowName){
		if(flowList == null) return;
		if(!flowList.containsKey(swID)) return;
		
		Set<PubSubFlow> fl = flowList.get(swID);
		PubSubFlow d = null;
		for(PubSubFlow f : fl){
			if(f.getName().equals(FlowName))
				d = f;
		}
		fl.remove(d);
		
		flowList.put(swID, fl);
	}
		
	public void addPublisher(Host h){
		if(null == publishers) publishers = new ArrayList<Host>();
		publishers.add(h);
	}
	
	public void removePublisher(Host h){
		publishers.remove(h);
	}
/*	public void addSubscriber(Host h){
		if(null == subscribers) subscribers = new ArrayList<Host>();
		subscribers.add(h);
	}
*/	
	public ArrayList<Host> getPublishers(){
		return this.publishers;
	}
	
/*	public ArrayList<Host> getSubscribers(){
		return this.subscribers;
	}
*/	
	public boolean hasPublisher(Host h){
		if(null == publishers) return false;
		//for(int i = 0; i < publishers.size(); i++){
		//	if(h.equals(publishers.get(i))) return true;
		//}
		if(publishers.contains(h)) return true;
		return false;
	}
	
	void printTree(){
		System.out.println("++++++++++++++++++++++++TREE++++++++++++++++++++++++");
		System.out.println("DZ : " + d.toString() + " Root : " + rootID);
		System.out.println("Links : ");
		
		for (Long key : tree.keySet()) {
		    System.out.println("Key = " + key);
		    PubSubTreeNode nc = tree.get(key);
		    System.out.println("Link = " + nc.toString());
		}
		
		if(publishers != null){
			System.out.println("+++++++Publishers+++++++++");
			for(int i = 0; i < publishers.size(); i++){
				System.out.println(publishers.get(i).toString());
			}
		}
	}
	

	public ArrayList<NodePortTuple> buildRoute(long src, long dst){
		//Get labels of src and dst
		Long src_label = tree.get(src).getLabel();
		Long dst_label = tree.get(dst).getLabel();
		Long swID = (long)0;
		
		ArrayList<NodePortTuple> srcToDst = new ArrayList<NodePortTuple>();
		ArrayList<NodePortTuple> dstToSrc = new ArrayList<NodePortTuple>();
		
		if(src == dst) return srcToDst;
		
		//whichever is having higher label, move upwards till the label is same to the other
		if(src_label < dst_label){
			long label = dst_label;
			long srcID = tree.get(dst).getParent().getSrc();		
			
			while(label != src_label){
				long temp_srcID = tree.get(srcID).getParent().getSrc();
				short srcPort = tree.get(srcID).getParent().getSrcPort();	
				
				long temp_dstID = tree.get(srcID).getParent().getDst();
				short dstPort = tree.get(srcID).getParent().getDstPort();
				
				NodePortTuple srcpair = new NodePortTuple(temp_srcID, srcPort);
				NodePortTuple dstpair = new NodePortTuple(temp_dstID, dstPort);
				
				dstToSrc.add(srcpair);
				dstToSrc.add(dstpair);
				
				srcID = temp_dstID;
				label = tree.get(srcID).getLabel();
				
				if(label == src_label) swID = srcID;
				
			}
			
		}
		else if(src_label > dst_label){
			long label = src_label;
			long srcID = tree.get(src).getParent().getSrc();
			
			
			while(label != dst_label){
				long temp_srcID = tree.get(srcID).getParent().getSrc();
				short srcPort = tree.get(srcID).getParent().getSrcPort();	
				
				long temp_dstID = tree.get(srcID).getParent().getDst();
				short dstPort = tree.get(srcID).getParent().getDstPort();
				
				NodePortTuple srcpair = new NodePortTuple(temp_srcID, srcPort);
				NodePortTuple dstpair = new NodePortTuple(temp_dstID, dstPort);
				
				srcToDst.add(srcpair);
				srcToDst.add(dstpair);
				
				srcID = temp_dstID;
				label = tree.get(srcID).getLabel();
				
				if(label == dst_label)
					swID = srcID;
				
			}
		}
		
		//If the reached node is the required switch, we have the path
		//else move upwards both the nodes till we get the lowest common ancestor
		Long newSrc = src;
		Long newDst = dst;
		
		if(src_label > dst_label) 
			if ( swID == dst) return srcToDst;
			else {
				newSrc = swID;
			}
		
		//reverse and return the path
		else if (src_label < dst_label) 
			if (swID == src) {
				Collections.reverse(dstToSrc);
				return dstToSrc;
			}
			else {
				newDst = swID;
			}
		
		while(true){
			long src_temp_srcID = tree.get(newSrc).getParent().getSrc();
			long src_temp_dstID = tree.get(newSrc).getParent().getDst();
			short src_temp_srcPort = tree.get(newSrc).getParent().getSrcPort();
			short src_temp_dstPort = tree.get(newSrc).getParent().getDstPort();
			
			long dst_temp_srcID = tree.get(newDst).getParent().getSrc();
			long dst_temp_dstID = tree.get(newDst).getParent().getDst();
			short dst_temp_srcPort = tree.get(newDst).getParent().getSrcPort();
			short dst_temp_dstPort = tree.get(newDst).getParent().getDstPort();
			
			NodePortTuple src_srcpair = new NodePortTuple(src_temp_srcID, src_temp_srcPort);
			NodePortTuple src_dstpair = new NodePortTuple(src_temp_dstID, src_temp_dstPort);
			
			srcToDst.add(src_srcpair);
			srcToDst.add(src_dstpair);
			
			NodePortTuple dst_srcpair = new NodePortTuple(dst_temp_srcID, dst_temp_srcPort);
			NodePortTuple dst_dstpair = new NodePortTuple(dst_temp_dstID, dst_temp_dstPort);
			
			dstToSrc.add(dst_srcpair);
			dstToSrc.add(dst_dstpair);
			
			if(src_temp_dstID == dst_temp_dstID) break;
			
			newSrc =  src_temp_dstID;
			newDst =  dst_temp_srcID;
		}
		
		Collections.reverse(dstToSrc);
		srcToDst.addAll(dstToSrc);
		return srcToDst;
		
	}
	
	/**
	 * Given a switch ID and a set of its ports
	 * return a set of switch IDs connected to this switch at the given port
	 * @param swid
	 * @param outPorts
	 * @return
	 */
	
	public Set<NodePortTuple> getNextNodes(Long swid, Set<Short> outPorts){
		Set<NodePortTuple> nextNodes = new HashSet<NodePortTuple>();
		
		PubSubTreeNode n = tree.get(swid);
		Link p = n.getParent();
		
		if(outPorts.contains(p.getSrcPort())) nextNodes.add(new NodePortTuple(p.getDst(), p.getDstPort()));
		
		if(n.getChildren() != null)
			for(Link c : n.getChildren()){
				if(outPorts.contains(c.getSrcPort())) nextNodes.add(new NodePortTuple(c.getDst(), c.getDstPort()));
			}
		
		return nextNodes;
	}
	
	/**
	 * Given a switch ID and a single port
	 * return the switch ID connected to this switch at the given port
	 * @param swid
	 * @param outPorts
	 * @return
	 */
	
	public NodePortTuple getNextNodes(Long swid, Short port){
		
		PubSubTreeNode n = tree.get(swid);
		Link p = n.getParent();
		
		if(p != null)
			if(port.equals(p.getSrcPort())) return (new NodePortTuple(p.getDst(), p.getDstPort()));
		
		if(n.getChildren() == null) return null;
		
		for(Link c : n.getChildren()){
			if(port.equals(c.getSrcPort())) return (new NodePortTuple(c.getDst(), c.getDstPort()));
		}
		
		return null;
	}

		
}
