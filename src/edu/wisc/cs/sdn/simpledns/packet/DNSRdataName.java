package edu.wisc.cs.sdn.simpledns.packet;

import java.nio.ByteBuffer;

public class DNSRdataName implements DNSRdata 
{
	private String name;
	
	public DNSRdataName()
	{ this.name = new String(); 
	}


	
	public DNSRdataName(String name)
	{ this.name = name; 
	
	System.err.println( "New DNSRdataName created = " + this.name );	
	}
	
	public String getName()
	{ return this.name; }
	
	public void setName(String name)
	{ this.name = name; 
	
	System.err.println( "New DNSRdataName set = " + this.name );	
	}
	
	public static DNSRdata deserialize(ByteBuffer bb)
	{
		DNSRdataName rdata = new DNSRdataName();		
		rdata.name = DNS.deserializeName(bb);
	System.err.println( "DNSRdataName deserialized = " + rdata.name );	
		return rdata;
	}
	
	public byte[] serialize()
	{ 
	System.err.println( "DNSRdataName serialized= " + this.name );	
			  return DNS.serializeName(this.name); }
	
	public int getLength()
	{ return this.name.length() + (this.name.length() > 0 ? 2 : 0); }
	
	public String toString()
	{ return this.name; }
}
