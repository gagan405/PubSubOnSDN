package net.floodlightcontroller.pubsub;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PubSubFlow {
	private String flowName;
	private dz key;
	private Long swID;
	private Short inPort;
	private Set<Short> outPort;
	private Map<Short,String> portIPmap;
	private int priority;
	
	public String getName(){
		return flowName;
	}
	
	
	public dz getDZ(){
		return key;
	}
	
	public void setDZ(dz d){
		this.key = d;
	}
	
	public Long getSWID(){
		return swID;
	}
	
	public Short getInport(){
		return inPort;
	}
	
	public void setDstIP(String dst, Short port){
		portIPmap.put(port, dst);
	}
	
	public String getDstIP(Short port){
		if(portIPmap.containsKey(port)) return portIPmap.get(port);
		else return null;
	}
	
	public Map<Short, String> getPortIPMapping(){
		return this.portIPmap;
	}
	
	public void addPortIPMapping(Map<Short,String> map){
		this.portIPmap.putAll(map);
	}
	
	public boolean isDstIPSet(Short port){
		if(portIPmap.containsKey(port)) return true;
		else return false;
	}
	
	public Set<Short> getOutport(){
		return outPort;
	}
	
	public void addOutPorts(Set<Short> op){
		outPort.addAll(op);
	}
	
	public void addOutPorts(Short op){
		outPort.add(op);
	}
	
	public void removeOutPorts(Set<Short> op){
		if (op == null) return;
		if (op.isEmpty()) return;
		this.outPort.removeAll(op);
		
		for(Short p : op)
			this.portIPmap.remove(p);
		
		return;
	}
	
	public void removeOutPorts(Short op){
		this.outPort.remove(op);
		this.portIPmap.remove(op);
		return;
	}
	
	public void setPriority(int p){
		this.priority = p;
	}
	
	public int getPriority(){
		return this.priority;
	}
	
	/**
	 * Create JSON style flow-mod string to add flows
	 * @param swid
	 * @return
	 */
	
	public String toJSON(String swid){
		String ip = key.toIP();
		if(ip == null) return null;
		
		int maskLen = 9 + key.getLength();
		
		String fmJson = "{\"switch\":\""+ swid + "\"," +
				"\"name\":\"" + flowName + "\"," +
				"\"cookie\":\"0\"," +
				"\"priority\":\"" + priority + "\"," +
				"\"ether-type\":\"2048\"," +
				"\"ingress-port\":\"" + inPort + "\"," +
				"\"dst-ip\":\"" + ip;
		
		if(maskLen < 32){
			fmJson += "/" + maskLen + "\",";
		}
		else{
			fmJson += "\",";
		}
		 
		fmJson +=	"\"active\":\"true\"," +
					"\"actions\":\"" ;
			
		Set<Short> op = new HashSet<Short>();
		Set<Short> op1 = new HashSet<Short>();
		for(Short s: outPort){
			if(!portIPmap.containsKey(s))
				op.add(s);
			else op1.add(s);
		}
		
		for(Short p : op){
			fmJson += "output=";
			if(p == (short)0xfff8){
				fmJson += "ingress-port";
			}
			else{
				fmJson += p;
			}
			fmJson += ",";
		}
		for(Short p : op1){
			fmJson += "set-dst-ip=";
			fmJson += getDstIP(p);
			fmJson += ",output=";
			if(p == (short)0xfff8){
				fmJson += "ingress-port";
			}
			else{
				fmJson += p;
			}
			fmJson += ",";
		}
		fmJson = fmJson.substring(0, fmJson.length());
		
		fmJson += "\"}";
		return fmJson;
	}
	
	
	public boolean contains(PubSubFlow f){
		if(f.inPort != this.inPort) return false;
		
		if(f.key.getLength() < this.key.getLength()) return false;
		
		for(Short op: f.getOutport()){
			if(!outPort.contains(op)) return false;
			if((portIPmap.containsKey(op) && (f.getDstIP(op) == null)) ||
			   (!portIPmap.containsKey(op) && (f.getDstIP(op) != null)) ||
			   ((portIPmap.containsKey(op) && (f.getDstIP(op) != null)) && !f.getDstIP(op).equals(getDstIP(op))))		
				return false;
			
		}
		
		return true;
	}
	
	public boolean dzCovers(PubSubFlow f){
		
		if(f.inPort != this.inPort) return false;
		if(f.key.getLength() < this.key.getLength()) return false;
		if(!this.key.contains(f.key)) return false;
		
		boolean covers = false;
		
		for(Short op: f.getOutport()){
			if(!outPort.contains(op)) {
				covers = true;
				break;
			}
		}
		
		return covers;
		
	}
	
	public boolean dzEquals(PubSubFlow f){
		
		if(f.inPort != this.inPort) return false;
		if(f.key.getLength() != this.key.getLength()) return false;
		if(!f.key.equals(this.key)) return false;
		
		return true;
	}
	
	public PubSubFlow(String name, dz d, Long dpid, Short in, Set<Short> out){
		this.flowName = name;
		this.key = d;
		this.inPort = in;
		this.swID = dpid;
		this.outPort = new HashSet<Short>();
		for(Short s : out){
			this.outPort.add(s.shortValue());
		}
		this.portIPmap = new HashMap<Short, String>();
	}
	
	@Override
	public int hashCode() {
		int hash = 17;
		hash += 31*key.hashCode();
		hash += 31*flowName.hashCode();
		hash += 31*swID;
		hash += 31*priority;
		hash += 31*inPort;
		
		for(Short s : outPort){
			hash += 31*s;
		}
		
	    return hash;
	 }
	  
	@Override
	public boolean equals( Object b ) {
	     
		if(!(b instanceof PubSubFlow))  return false;
		if(!this.key.equals(((PubSubFlow)b).key))  return false;
		if(!flowName.equals(((PubSubFlow)b).flowName)) return false;
		if(this.priority != ((PubSubFlow)b).priority)  return false;
		if(this.inPort != ((PubSubFlow)b).inPort) return false;
		if(this.swID != ((PubSubFlow)b).swID) return false;
		if(outPort.size() != ((PubSubFlow)b).outPort.size()) return false;
		
		for(Short s : outPort){
			if(!(((PubSubFlow)b).outPort.contains(s))) return false;
		}
		
		return true;
	}
	
	@Override
	public String toString(){
		String s = "[ Flow: " + this.flowName +
					"|dz: " +	this.key.toString() +
					"|swID: " +	this.swID.toString() +
					"|inPort: " + this.inPort.toString() +
					"|outPorts: ";
		
		for(Short o : outPort){
			s += o.toString();
			
			if(this.portIPmap.containsKey(o))
				s += " IP: " + this.portIPmap.get(o);
			s += ",";
		}
		
		return s + " ]";		
	}
	
}
