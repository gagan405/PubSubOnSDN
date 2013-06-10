package net.floodlightcontroller.pubsub;

import java.util.Arrays;

public class dz{
	  private boolean array[];
	  private int length;
	  
	  public dz( boolean b[] ) {
	     array = b;
	     length = b.length;
	  }
	  
	  public dz( dz d ) {
		  this.array = new boolean[d.length];
		  this.length = d.length;
		  boolean[] arr = d.getArray();
		  
		  for(int i = 0; i < length; i++){
			  this.array[i] = arr[i]; 
		  }
		     
	  }
	  
	  public boolean contains(dz k){
		  if(k == null) return false;
		  if(this.length > k.length) return false;
		  else if(Arrays.equals(Arrays.copyOf(k.array, this.length), array)) return true;
		  return false;
	  }
	  
	  public boolean[] getArray(){
		  return this.array;
	  }
	  
	  public int getLength(){
		  return this.length;
	  }
	  
	  
	  public String toIP(){
		boolean[] IP = PubSub.intToBoolean(451, 9); //225.128
		IP = PubSub.concat(IP, array);
		if(IP.length > 32){
			IP = Arrays.copyOfRange(IP, 0, 32);
		}
		
		if(IP.length < 32){
			IP = PubSub.concat(IP, new boolean[32-IP.length]);
		}
		Integer i = PubSub.booleansToInt(Arrays.copyOfRange(IP, 0, 8));
		return Integer.toString(i) + "." + Integer.toString(PubSub.booleansToInt(Arrays.copyOfRange(IP, 8, 16)))
				+ "." + Integer.toString(PubSub.booleansToInt(Arrays.copyOfRange(IP, 16, 24)))
				+ "." + Integer.toString(PubSub.booleansToInt(Arrays.copyOfRange(IP, 24, 32)));
		
	  }
	  
	  public String toIP(boolean isIPaddr){
		  	
		  Integer i = PubSub.booleansToInt(Arrays.copyOfRange(array, 0, 8));
		  return Integer.toString(i) + "." + Integer.toString(PubSub.booleansToInt(Arrays.copyOfRange(array, 8, 16)))
					+ "." + Integer.toString(PubSub.booleansToInt(Arrays.copyOfRange(array, 16, 24)))
					+ "." + Integer.toString(PubSub.booleansToInt(Arrays.copyOfRange(array, 24, 32)));
			
		  }
	  
	  @Override
	  public int hashCode() {
	    int hash = 0;
	    for (int i = 0; i < array.length; i++)
	       if (array[i])
	          hash += Math.pow(2, i);
	    return hash;
	  }
	  
	  @Override
	  public boolean equals( Object b ) {
		if (b == null)	 			return false;
		if (!(b instanceof dz))	    return false;
	   
	    if ( array.length != ((dz)b).array.length )      return false;
	     
	    for (int i = 0; i < array.length; i++ )
	      if (array[i] != ((dz)b).array[i])   return false;
	     
	    return true;
	  }
	  
	  @Override
	  public String toString(){
		  return  Arrays.toString(array); 
	  }
}
