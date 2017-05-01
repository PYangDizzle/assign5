package edu.wisc.cs.sdn.simpledns;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.ArrayList;

import edu.wisc.cs.sdn.simpledns.packet.*;

public class SimpleDNS 
{
	DatagramSocket dsocket = null;
	DatagramPacket packet = null;
	
	DNS resultDNS = null;
	DatagramPacket resultPacket = null;
	byte[] resultBuffer = new byte[ 2048 ];
	DatagramSocket remoteSocket = null;

	
	String rootServerIP = null;
	String ec2CSV = null;
	
	boolean doWait = false;
	
	final int port = 8053;
	final boolean debug = true;


	
	public static void main(String[] args)
	{
		
		System.out.println("Hello, DNS!"); 
		SimpleDNS simpleDNS = new SimpleDNS();
				
		try {
			for( int i = 0; i < 4; ++i ) {
				if( args[ i ].equals( "-r" ) ) {
					simpleDNS.rootServerIP = args[ i + 1 ];
				}
				else if( args[ i ].equals( "-e" ) ) {
					simpleDNS.ec2CSV = args[ i + 1 ];
				}
			}
			
			if( simpleDNS.rootServerIP == null || simpleDNS.ec2CSV == null ) {
				throw new Exception( "One of them is null" );
			}
		}
		catch( Exception e ) {
			System.err.println( "Use it correctly : java edu.wisc.cs.sdn.simpledns.SimpleDNS -r <root server ip> -e <ec2 csv>" );
			e.printStackTrace();
		}
		
		simpleDNS.run();
	}
	
	private void run() {
		try {
			
      // Create a socket to listen on the ports.
      dsocket = new DatagramSocket( port );
	remoteSocket = new DatagramSocket( 8888 );

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
	/*
        String msg = new String( buffer, 0, packet.getLength() );
        log( packet.getAddress().getHostName() + ": "
            + msg );

				if( debug ) {
					String sendString = "Received UDP";
					byte[] sendData = sendString.getBytes();
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), port);
					dsocket.send(sendPacket);		
				}
				
	*/					
        // Reset the length of the packet before reusing it.
	resultPacket = new DatagramPacket( resultBuffer, resultBuffer.length );
	resultPacket.setAddress( packet.getAddress() );
	resultPacket.setPort( packet.getPort() );
	handleUDP();
        packet.setLength( buffer.length );
      }
    } 
		catch( Exception e ) {
      e.printStackTrace();
    }
	}
	
	private void handleUDP() throws Exception {
		while( true ) {
			doWait = true;
			log( "packetLength = " + packet.getLength() );
			DNS dns = DNS.deserialize( packet.getData(), packet.getLength() );
			log( dns.toString() );
			if( dns.isQuery() ) {
				// query
				log( "isQuery" );		
				
				
				if( dns.getOpcode() == DNS.OPCODE_STANDARD_QUERY ) {
					// standard_query
					log( "isStandardQuery" );
					List<DNSQuestion> questions = dns.getQuestions();
					if( questions.size() > 1 ) {
						log( "WARNING : questions size > 1" );
					}
					DNSQuestion question = questions.get( 0 );
					try{ 
						if( question.getType() == DNS.TYPE_A ) {
							// type A : asks for IP address
							log( "isTypeA" );
							
							// redirect the packet to root NS
							packet.setAddress( InetAddress.getByName( rootServerIP ) );
//							packet.setPort( 8888 );
							packet.setPort( 53 );

							log( "redirecting to " + InetAddress.getByName( rootServerIP ) );
							log( "packet : " );
							log( packet.getPort() );

							remoteSocket.send( packet );
							waitResponse();
						}
						else if( question.getType() == DNS.TYPE_NS ) {
							// type NS : asks for nameserver
							log( "isTypeNS" );
							
							// redirect the packet to root NS
							packet.setAddress( InetAddress.getByName( rootServerIP ) );
							dsocket.send( packet );
						}
						else if( question.getType() == DNS.TYPE_AAAA ) {
							log( "isTypeAAAA" );
							// redirect the packet to root NS
							packet.setAddress( InetAddress.getByName( rootServerIP ) );
							dsocket.send( packet );
						}
						else if( question.getType() == DNS.TYPE_CNAME ) {
							log( "isTypeCNAME" );
							// redirect the packet to root NS
							packet.setAddress( InetAddress.getByName( rootServerIP ) );
							dsocket.send( packet );
						}
					}
					catch( Exception e ) {
						e.printStackTrace();
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
				
				boolean isCompleteResponse = false;
				
				if( dns.isRecursionAvailable() == false && dns.isRecursionDesired() ) {
					log( "doing recursion" );
					
					List<DNSResourceRecord> answers = dns.getAnswers();
					List<DNSResourceRecord> authorities = dns.getAuthorities();
					List<DNSResourceRecord> additionals = dns.getAdditional();

					boolean hasCNAME = false;
					String CNAME = null;


					if( answers.isEmpty() ) {
						log( "doesn't have answers" );
						String additionalName = null;
						if( authorities.isEmpty() == false ) {
							log( "authority is not empty " );
							additionalName = ((DNSRdataName)( authorities.get( 0 ).getData() )).getName();
						}
						log( "additionalName = " + additionalName );
						
						if( additionals.isEmpty() == false ) {
							if( additionalName != null ) {
								for( int i = 0; i < additionals.size(); ++i ) {
									if( additionals.get( i ).getName().equals( additionalName ) ) {
										packet.setAddress( ((DNSRdataAddress)additionals.get( i ).getData()).getAddress() );
										log( "redirecting to " + ((DNSRdataAddress)additionals.get( i ).getData()).toString() );
										break;
									}
								}
							}
							else {
								packet.setAddress( ((DNSRdataAddress)additionals.get( 0 ).getData()).getAddress() );
								log( "redirecting to " + ((DNSRdataAddress)additionals.get( 0 ).getData()).toString() );
							}
							packet.setPort( 53 );
							dns.setQuery( true );
							dns.setAnswers( new ArrayList<DNSResourceRecord>() );
							dns.setAuthorities( new ArrayList<DNSResourceRecord>() );
							dns.setAdditional( new ArrayList<DNSResourceRecord>() );
							packet.setData( dns.serialize() );
							packet.setLength( dns.getLength() );

							log( "packetLength when send " + packet.getLength() );
							
							remoteSocket.send( packet );
							waitResponse();
						}
					}

					else {
						log( "hasAnswers" );

						for( int i = 0; i < answers.size(); ++i ) {
							if( answers.get( i ).getType() == DNS.TYPE_CNAME ) {
								log( "hasTypeCNAME" );
								hasCNAME = true;
								CNAME = answers.get( i ).getName();
							}
							else if( answers.get( i ).getType() == DNS.TYPE_EC2 ) {
								log( "hasTypeEC2" );
							}
							else if( answers.get( i ).getType() == DNS.TYPE_A ) {
								log( "hasTypeA" );
								if( answers.get( i ).getName().equals( dns.getQuestions().get( 0 ).getName() ) ) {
									isCompleteResponse = true;
								}
								else {
									packet.setAddress( ((DNSRdataAddress)answers.get( i ).getData()).getAddress() );
									remoteSocket.send( packet );
									waitResponse();
								}
							}
						}
					

						if( hasCNAME == false ) {
							isCompleteResponse = true;
						}
						else {
							log( "CNAME Found : " + CNAME );
							/*
							dns.getQuestions().get( 0 ).getName
							packet
							// redirect the packet to root NS
							packet.setAddress( InetAddress.getByName( rootServerIP ) );
							dsocket.send( packet );
							*/
						}
					}
				}
				else {
					log( "recursion was available" );
					// recursion is done by other server.
					isCompleteResponse = true;
				}
				
				if( isCompleteResponse ) {
					log( "Complete" );
					resultDNS.setAnswers( dns.getAnswers() );
					resultDNS.setAuthorities( dns.getAuthorities() );
					resultDNS.setAdditional( dns.getAdditional() );
					// good to send back the response

					try{ 
						resultPacket.setData( resultDNS.serialize() );
						resultPacket.setLength( resultDNS.getLength() );
						
						dsocket.send( resultPacket );
	
					}
					catch( Exception e ) {
						e.printStackTrace();
					}
					resultPacket = null;
					resultDNS = null;
					return;
				}
				else {
					waitResponse();
				}
			}
		}
	}

	private void waitResponse() {
		if( doWait ) {
			log( "waiting" );
			doWait = false;
			try {
       				remoteSocket.receive( packet );
				log( "packetLength when receive " + packet.getLength() );
			}
			catch( Exception e ) {
				e.printStackTrace();
			}
		}
			  
	}

	private void log( int msg ) {
		log( String.format( "%d", msg ) );
	}
	
	private void log( String msg ) {
		if( debug ) {
			System.err.println( msg );
		}
	}
}
