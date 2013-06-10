package net.floodlightcontroller.pubsub;

import net.floodlightcontroller.topology.NodePortTuple;

public class Host {
	private dz key;
	private NodePortTuple attachmentPoint;
	private boolean type; // true = publisher
						  //false = subscriber
	
	public Host(NodePortTuple ap, dz d, boolean type){
		this.attachmentPoint = new NodePortTuple(ap.getNodeId(), ap.getPortId());
		this.key = new dz(d);
		this.type = type;
	}
	
	public NodePortTuple getAttachmentPoint(){
		return attachmentPoint;
	}
	
	public dz getDz(){
		return key;
	}
	
	@Override
	public String toString(){
		return "[ type : " + type + " dz : " + key.toString() + "; Attachment Point : " 
				+ attachmentPoint.toString() + " ]" ;
	}
	
	public boolean isPublisher(){
		return type;
	}
	
	@Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Host other = (Host) obj;
        if (!attachmentPoint.equals(other.attachmentPoint))
            return false;
        if (type != other.type)
            return false;
        if (!key.equals(other.key))
        	return false;
        return true;
    }
    
	@Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = result * prime +  attachmentPoint.hashCode();
        result = prime * result + key.hashCode();
        if(type) result = result * 17;
        
        return result;
    }
	
}
