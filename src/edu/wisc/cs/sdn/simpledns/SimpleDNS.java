package edu.wisc.cs.sdn.simpledns;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

import edu.wisc.cs.sdn.simpledns.packet.*;

public class SimpleDNS 
{
	DatagramSocket dsocket = null;
	DatagramPacket packet = null;
	
	DNS resultDNS = null;
	DatagramPacket resultPacket = null;
	
	String rootServerIP = null;
	String ec2CSV = null;
	
	
	final int port = 8053;
	final boolean debug = true;
	
	public static void main(String[] args)
	{
		
		System.out.println("Hello, DNS!"); 
				
		try {
			for( int i = 0; i < 4; ++i ) {
				if( args[ i ].equals( "-r" ) ) {
					rootServerIP = args[ i + 1 ];
				}
				else if( args[ i ].equals( "-e" ) {
					ec2CSV = args[ i + 1 ];
				}
			}
			
			if( rootServerIP == null || ec2CSV == null ) {
				throw new Exception( "One of them is null" );
			}
		}
		catch( Exception e ) {
			System.err.println( "Use it correctly : java edu.wisc.cs.sdn.simpledns.SimpleDNS -r <root server ip> -e <ec2 csv>" );
			e.printStackTrace();
		}
		
		new SimpleDNS().run();
	}
	
	private void run() {
		try {
			
      // Create a socket to listen on the port.
      dsocket = new DatagramSocket( port );

      // Create a buffer to read datagrams into. If a
      // packet is larger than this buffer, the
      // excess will simply be discarded!
      byte[] buffer = new byte[ 2048 ];

      // Create a packet to receive data into the buffer
      packet = new DatagramPacket( buffer, buffer.length );

      // Now loop forever, waiting to receive packets and printing them.
      while( true ) {
        // Wait to receive a datagram
        dsocket.receive( packet );

        // Convert the contents to a string, and display them
        String msg = new String( buffer, 0, packet.getLength() );
        log( packet.getAddress().getHostName() + ": "
            + msg );

				if( debug ) {
					String sendString = "Received UDP";
					byte[] sendData = sendString.getBytes();
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), port);
					dsocket.send(sendPacket);		
				}
				
						
        // Reset the length of the packet before reusing it.
        packet.setLength( buffer.length );
				
				handleUDP();
      }
    } 
		catch( Exception e ) {
      e.printStackTrace();
    }
	}
	
	private void handleUDP() {
		DNS dns = DNS.deserialize( packet.getData(), packet.length() );
		log( dns.toString() );
		if( dns.isQuery() ) {
			// query
			log( "isQuery" );		
			
			if( resultPacket == null ) {
				resultPacket = new DatagramPacket();
				resultPacket.setAddress( packet.getAddress() );
				resultPacket.setPort( packet.getPort() );
			}
			else {
				log( "previous query is not done" );
				return;
			}
			
			if( dns.getOpcode() == DNS.OPCODE_STANDARD_QUERY ) {
				// standard_query
				log( "isStandardQuery" );
				List<DNSQuestion> questions = dns.getQuestions();
				if( questions.size() > 1 ) {
					log( "WARNING : questions size > 1" );
				}
				DNSQuestion question = questions.get( 0 );
				if( question.getType() == DNS.TYPE_A ) {
					// type A : asks for IP address
					log( "isTypeA" );
					
					// redirect the packet to root NS
					packet.setAddress( InetAddress.getByName( rootServerIP ) );
					dsocket.send( packet );
				}
				else if( question.getType() == DNS.TYPE_NS ) {
					// type NS : asks for nameserver
					log( "isTypeNS" );
					
					// redirect the packet to root NS
					packet.setAddress( InetAddress.getByName( rootServerIP ) );
					dsocket.send( packet );
				}
			}
		}
		else {
			// response
			log( "isResponse" );
			
			if( resultDNS == null ) {
				resultDNS = new DNS();
				resultDNS.setId( dns.getId() );
				resultDNS.setOpcode( dns.getOpcode() );
				resultDNS.setRcode( DNS.RCODE_DEFAULT );
				resultDNS.setAuthoritative( dns.isAuthoritative() );
				resultDNS.setTruncated( dns.isTruncated() );
				resultDNS.setRecursionDesired( dns.isRecursionDesired() );
				resultDNS.setRecursionAvailable( dns.isRecursionAvailable() );
				resultDNS.setAuthenicated( dns.isAuthenticated() );
				resultDNS.setCheckingDisabled( dns.isCheckingDisabled() );
			}
			
			boolean isComplete = false;
			
			if( resultDNS.isRecursionAvailable() == false && resultDNS.isRecursionDesired() ) {
				
				List<DNSResourceRecord> answers = dns.getAnswers();
				log( "doingAnswers" );
				for( int i = 0; i < answers; ++i ) {
					if( answers.get( i ).getType() == DNS.TYPE_CNAME ) {
						log( "isTypeCNAME" );
						
					}
					else if( answers.get( i ).getType() == DNS.TYPE_EC2 ) {
						log( "isTypeEC2" );
					}
					else if( answers.get( i ).getType() == DNS.TYPE_A ) {
						log( "isTypeA" );
						
					}
				}
			}
			else {
				// recursion is done by other server.
				isComplete = true;
				resultDNS.
			}
			
			if( isCompleteResponse ) {
				// good to send back the response
				resultPacket.setData( resultDNS.serialize() );
				resultPAcket.setLength( resultDNS.getLength() );
				
				dsocket.send( resultPacket );
				resultPacket = null;
				resultDNS = null;
			}
		}
	}
	
	private void log( String ) {
		if( debug ) {
			System.err.println( String );
		}
	}
}
