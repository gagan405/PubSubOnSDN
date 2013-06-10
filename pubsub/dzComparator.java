package net.floodlightcontroller.pubsub;

import java.util.Comparator;

public class dzComparator implements Comparator<dz> {
	
	@Override
	public int compare(dz o1, dz o2) {
		return ((Integer)o1.getLength()).compareTo((Integer)o2.getLength());
	}
}
