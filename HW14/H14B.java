// H14B.java CS6025 Yizong Cheng March 2015
// A UDP client talking with H14A
// It uses the DH Group 5 q and alpha=2 to generate a Diffie-Hellman key pair.
// It sends the public key to the server.
// It then wait and receives the public key from the server
// It computes the shared secret and prints it out.
// Usage: java H14B serverIP/hostname serverPort

import java.io.*;
import java.util.*;
import java.math.*;
import java.net.*;

public class H14B
{
    static int MAXBF = 1024;
    String hexQ = null;
    BigInteger q = null;
    static BigInteger alpha = new BigInteger( "2" );
    BigInteger privateKey;
    BigInteger publicKey;
    BigInteger serverPublicKey;
    byte[] publicKeyBytes = null;
    BigInteger preMasterSecret;
    String hexkey = null;

    void readQ( String filename )
    {
        Scanner in = null;

        try {
            in = new Scanner( new File( filename ) );
        } catch ( FileNotFoundException e ) {
            System.err.println( filename + " not found" );
            System.exit( 1 );
        }

        hexQ = in.nextLine();
        in.close();
        q = new BigInteger( hexQ, 16 );
    }

    void generateKeyPair()
    {
        Random random = new Random();
        privateKey = new BigInteger( 1235, random );
        publicKey = alpha.modPow( privateKey, q );
        publicKeyBytes = publicKey.toByteArray();
    }

    void runUDPClient( String serverIP, int serverPort )
    {
        InetAddress iadd = null;

        try {
            iadd = InetAddress.getByName( serverIP );
        } catch ( UnknownHostException e ) {
            System.err.println( "Exception" );
            return;
        }

        DatagramSocket ds = null;
        DatagramPacket dp = null;
        byte[] buff = new byte[MAXBF];

        try {
            ds = new DatagramSocket();
            dp = new DatagramPacket( publicKeyBytes, publicKeyBytes.length, iadd, serverPort );
            ds.send( dp );
            dp = new DatagramPacket( buff, MAXBF );
            ds.receive( dp );
            int len = dp.getLength();
            byte[] serverPublicKeyBytes = new byte[len];

            for ( int i = 0; i < len; i++ ) {
                serverPublicKeyBytes[i] = buff[i];
            }

            serverPublicKey = new BigInteger( serverPublicKeyBytes );
        } catch ( IOException e ) {
            System.err.println( "IOException" );
            return;
        }
    }

    void computeSharedSecret()
    {
        preMasterSecret = serverPublicKey.modPow( privateKey, q );
        hexkey = preMasterSecret.toString( 16 );
        System.out.println( hexkey );
    }

    public static void main( String[] args )
    {
        if ( args.length < 2 ) {
            System.err.println( "Usage: java H14B serverIP serverPort" );
            System.exit( 1 );
        }

        H14B h14 = new H14B();
        h14.readQ( "DHgroup5.txt" );
        h14.generateKeyPair();
        h14.runUDPClient( args[0], Integer.parseInt( args[1] ) );
        h14.computeSharedSecret();
    }
}

