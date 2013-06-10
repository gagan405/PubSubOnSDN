package net.floodlightcontroller.pubsub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.topology.NodePortTuple;

import java.util.Queue;

public class PubSubTreeManager {
	
	protected static Map<Long, Set<Link>> swlinks;
	protected static Map<Long, Set<Link>> graph;
//	protected static Map<Long, PubSubTreeNode> tree;
	protected static Map<dz, PubSubTree> treeList;
	protected static Map<dz, Set<Host>> subscribers;
	protected static Map<NodePortTuple, String> ipForSub;
	
	/**
	 * Method : Add a subscriber with a given dz
	 * @param d
	 * @param h
	 */
	
	public static boolean addSubscriber(dz d, Host h){
		if (subscribers == null) subscribers = new HashMap<dz, Set<Host>>();
		Set<Host> temp = null;
		
		if (subscribers.containsKey(d)){ temp = subscribers.get(d);	}
		else	{	temp = new HashSet<Host>();	}
		
		if(temp.contains(h)) return false;
		
		temp.add(h);
		subscribers.put(d, temp);
		
		return true;
	}
	
	/**
	 * Set listening IP for subscriber
	 * @param ip
	 * @param h
	 */
	
	public static void setIPForSub(String ip, NodePortTuple n){
//		if (subscribers == null) return;
		if (ipForSub == null) ipForSub = new HashMap<NodePortTuple, String>();
		
	    ipForSub.put(n, ip);
			
		return;
	}
	
	/**
	 * Return the IP to which the subscriber is listening
	 * @param h
	 * @return
	 */
	
	public static String getIPForSub(NodePortTuple h){
		if (ipForSub == null) return null;
		if (!ipForSub.containsKey(h)) return null;
		
		return ipForSub.get(h);
	}
	
	/**
	 * Method : Remove a subscriber with a given dz
	 * @param d
	 * @param h
	 */
	
	public static void removeSubscriber(dz d, Host h){
		if (subscribers == null) return;
		if (subscribers.isEmpty()) return;
		
		if (subscribers.containsKey(d)){ 
			Set<Host> temp = subscribers.get(d);
			if(temp.contains(h))	temp.remove(h);
			
			if(temp.isEmpty()) 		subscribers.remove(d);
			else subscribers.put(d, temp);
			
			return;
		}
		else return;
	}
	
	public static void removeSubscriber(){
		if (subscribers == null) return;
		if (subscribers.isEmpty()) return;
		
		subscribers = null;
		ipForSub = null;
	}
	
	/**
	 * get subscribers who might be interested in the dz, e.g. subscriber 0000 will be
	 * interested in both 00 and 000000
	 * @param d
	 * @return
	 */
	
	public static Set<Host> getInterestedSubscribers(dz d){
		if(subscribers == null) return null;
		if(subscribers.isEmpty()) return null;
		
		Set<Host> sub = new HashSet<Host>();
		
		for(dz key : subscribers.keySet()){
			if(d.equals(key)) sub.addAll(subscribers.get(key));
			else if(d.getLength() < key.getLength()){
				if (d.contains(key)) sub.addAll(subscribers.get(key));
			}
			else if(key.getLength() < d.getLength()){
				if (key.contains(d)) sub.addAll(subscribers.get(key)); 
			}
		}			
		return sub;
	}
	
	/**
	 * get dzs of the available trees in sorted order w.r.t an input dz
	 * i.e. return key 000 if input is 00 or 0. Not the other way round.
	 * @param x
	 * @return
	 */
	
	static ArrayList<dz> getKeys(dz x){
		ArrayList<dz> s = new ArrayList<dz>();
		
		for (dz k : treeList.keySet()){
			//get keys with prefix matching	
			if(x.getLength() < k.getLength()){
					if (x.contains(k)) s.add(k);
			}
		}
		if(s.isEmpty()) return null;
		
		Collections.sort(s, new dzComparator());
		return s;
	}
	
	/**
	 * update the switch links
	 * @param swl
	 */
	
	public static void updateSwitchLinks(Map<Long, Set<Link>> swl){
		if(swl.equals(swlinks)){
			if(graph == null) buildGraph();
			if(graph.isEmpty()) buildGraph();
			if(graph.keySet().size() != swlinks.size()) buildGraph();
			return;
		}
		else{
			swlinks = swl;
			if(swlinks == null) {
				graph.clear();
				return;
			}
			if(swlinks.isEmpty()){
				if(graph != null)
					graph.clear();
				return;
			}
			
			//Store the graph
			buildGraph();
			
			//Update the trees 
			if(treeList != null){
				for(dz d : treeList.keySet()){
					buildTree(treeList.get(d).getRootID(), d, null);
				}
			}
		}
	}
	/**
	 * Get relevant trees for a dz
	 * e.g. 000 and 0, both are relevant for dz 00
	 * @param d
	 * @return
	 */
	
	public static ArrayList<PubSubTree> getRelevantTrees(dz d){
		
		if (treeList == null) return null;
		if (treeList.isEmpty()) return null;
		
		ArrayList<PubSubTree> trees = new ArrayList<PubSubTree>();
			
		for(dz key : treeList.keySet()){
			if(d.equals(key)) trees.add(treeList.get(key));
			else if(d.getLength() < key.getLength()){
				if (d.contains(key)) trees.add(treeList.get(key));
			}
			else if(key.getLength() < d.getLength()){
				if (key.contains(d)) trees.add(treeList.get(key)); 
			}
		}
		
		if(trees.isEmpty()) return null;
		
		return trees;
	}
	
	
	public static void deleteTree(dz d){
		if (treeList == null) return;
		if (treeList.isEmpty()) return;
		
		if(treeList.containsKey(d))
			treeList.remove(d);
		return;
	}
	
	public static void deleteTree(){
		treeList = null;
	}
	
	/**
	 * Print Subscribers for a dz
	 * if dz is null, print the available mappings
	 */
	
	public static void printSubscribers(dz d){
		if(subscribers == null){
			System.out.println("***********************************No subscribers found");
			return;
		}
		if(subscribers.isEmpty()){
			System.out.println("***********************************No subscribers found");
			return;
		}
		
		if (d == null){
			// print mappings
			for(dz key : subscribers.keySet()){
				System.out.println("dz : " + key.toString());
				for(Host h : subscribers.get(key)){
					System.out.println(h.toString());
				}
			}
			return;
		}
		
		Set<Host> sub = getInterestedSubscribers(d);
		if(sub != null)
			for(Host h : sub){
				System.out.println(h.toString());
			}
		
		return;
	}
	
	/**
	 * Print the links
	 * Useful for debugging
	 */
	
	public static void printLinks(){
		if(null == swlinks) return;
		
		System.out.println("+++++++++++++++++++++++LINKS+++++++++++++++++++++");
		for (Long key : swlinks.keySet()) {
		    System.out.println("Key = " + key);
		    System.out.println(swlinks.get(key).toString());
		}
	}
	
	/**
	 * print the graph
	 * useful for debugging
	 */
	
	public static void printGraph(){
		if(null == graph) return;
		
		System.out.println("++++++++++++++++++++++++GRAPH++++++++++++++++++++++++");
		if(graph.isEmpty() && !swlinks.isEmpty()) buildGraph();
		
		for (Long key : graph.keySet()) {
		    System.out.println("Key = " + key);
		    System.out.println(graph.get(key).toString());
		}
	}
	
	/**
	 * Print tree of a particular dz
	 * if input is null, print available dzs of the trees
	 * @param d
	 */
	
	public static void PrintTree(dz d){
		if(d == null){
			System.out.println("Available trees : ");
			for(dz k : treeList.keySet()){
				System.out.println(k.toString());
			}
			return;
		} 
		PubSubTree tree = treeList.get(d);
		if (tree == null){
			System.out.println("Tree for dz " + d.toString() + " doesn't exist!");
			return;
		}
		tree.printTree();
	}
	
	/**
	 * Print all available trees
	 * 
	 */
	
	public static void PrintAllTrees(){
		if(treeList == null){
			System.out.println("*****************************************No trees constructed!");
			return;
		}
		for(dz k : treeList.keySet()){
			treeList.get(k).printTree();
		}
	}
	
	public static Set<PubSubTree> getAllTrees(){
		Set<PubSubTree> t = new HashSet<PubSubTree>();
		if(treeList == null)		return t;
		if(treeList.isEmpty())		return t;
	
		t.addAll(treeList.values());
		return t;
	}
	
	/**
	 * Get route from source to destination in a tree
	 * @param d
	 * @param src
	 * @param dst
	 * @return
	 */
	public static ArrayList<NodePortTuple> getRoute(dz d, long src, long dst){
		PubSubTree tree = treeList.get(d);
		
		if(tree == null){
			System.out.println("No available tree for dz : " + d.toString());
			return null;
		}
		
		return tree.buildRoute(src, dst);
	}
	
	/**
	 * Construct tree from the graph, with a particular node as the root
	 * Uses breadth-first-search in the graph
	 * @param root
	 * @param d
	 * @param pub
	 */
	
	public static PubSubTree buildTree(long root, dz d, Host pub){
		//breadth first search from the root switch
		if(treeList == null) treeList = new HashMap<dz, PubSubTree>();
		
		Map<Long, PubSubTreeNode> temp = new HashMap<Long, PubSubTreeNode>();
		
		long swid = root;
		//Set<Link> l = graph.get(root);
		Link n = new Link(-1, -1, -1, -1);
		
		PubSubTreeNode no = new PubSubTreeNode(n, null,(long) 0);
		
		temp.put(swid, no);
		
		Queue<Long> q = new LinkedList<Long>();
		q.add(swid);
		
		while(!q.isEmpty()){
			long sw = q.poll();
			ArrayList<Link> linkArr = new ArrayList<Link>();
			
			
			for(Link li : graph.get(sw)){
				long childNode = li.getDst();
				
				if(!temp.containsKey(childNode)){
					
					Link parentLink = new Link(li.getDst(), li.getDstPort(), li.getSrc(), li.getSrcPort());
					PubSubTreeNode nc = new PubSubTreeNode(parentLink, null, temp.get(sw).getLabel()+1 );
					linkArr.add(li);
					
					temp.put(childNode, nc);
					q.add(childNode);
				}			
			}
			if(!linkArr.isEmpty()){
				PubSubTreeNode t = temp.get(sw);
				t.setChildren(linkArr);
				temp.put(sw, t);
			}
			
		}
		PubSubTree tree;
		if(treeList.containsKey(d)){
			tree = treeList.get(d);
			tree.updateTree(temp);
		}
		else{
			tree = new PubSubTree(temp, root, d);
		}
		
		if(pub != null) tree.addPublisher(pub);

		treeList.put(d, tree);
		
		return tree;
		
	}
	
	/**
	 * Build the graph as an adjacency list from the switch link information
	 * 
	 */
	
	public static void buildGraph(){
		//Graph as an adjacency list
		//Map between Switch IDs and Set of links attached to the switch
		//Links are put in the set only where the switch is the source
		//This removes repeating the links twice
		
		if(graph == null) graph = new HashMap<Long, Set<Link>>();
		
		for(Long key : swlinks.keySet()){
			Set<Link> l = swlinks.get(key);
			Set<Link> temp = new HashSet<Link>();
			
			for(Link li : l){
				if(li.getSrc() == key){
					Link new_link = new Link(li.getSrc(), li.getSrcPort(), li.getDst(), li.getDstPort());
					temp.add(new_link);
				}
			}
			graph.put(key, temp);
		}	
		
	}
}

