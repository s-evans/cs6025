// H15.java CS6025 Yizong Cheng March 2015
// ElGamal and DSA
// Usage: java H15

import java.io.*;
import java.util.*;
import java.math.*;

public class H15
{
    String hexQ = null;
    BigInteger q = null;
    BigInteger qminusone = null;
    static BigInteger alpha = new BigInteger( "2" );
    static final BigInteger one = new BigInteger( "1" );
    static final BigInteger two = new BigInteger( "2" );
    BigInteger privateKey;
    BigInteger publicKey;
    BigInteger message;

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
        qminusone = q.subtract( one );
    }

    void generateKeyPair()
    {
        Random random = new Random();
        privateKey = new BigInteger( 1235, random ).mod( q.subtract( two ) ).add( two );
        publicKey = alpha.modPow( privateKey, q );
        message = new BigInteger( 1235, random ).mod( q );
        System.out.println( "Message " + message.toString( 16 ) );
    }

    void ElGamalEncryption()
    {
        Random random = new Random();
        BigInteger k = new BigInteger( 1235, random ).mod( qminusone ).add( one ); // randomly choose k < q
        BigInteger K = publicKey.modPow( k, q ); // K = publicKey kth power mod q
        BigInteger C1 = alpha.modPow( k, q ); // C1 = alpha kth power mod q
        BigInteger C2 = K.multiply( message ).mod( q ); // C2 = K times message mod q
        // (C1, C2) is the encryption of message

        BigInteger K2 = C1.modPow( privateKey, q ); // K2 = C1 privateKey power mod q
        BigInteger m = C2.multiply( K2.modInverse( q ) ).mod( q ); // m = C2 times modInverse of K2 mod q
        // Is m the same as message?

        System.out.println( "message = " + message.toString() + ";" );
        System.out.println( "m = " + m.toString() + ";" );

        if ( m.equals( message ) ) {
            System.out.println( "m == message" );
        } else {
            System.out.println( "m != message" );
        }
    }

    void ElGamalSignature()
    {
        Random random = new Random();
        BigInteger k = null; // randomly choose k < q
        boolean tryAgain = false;

        do {
            try {
                tryAgain = false;
                k = new BigInteger( 1235, random ).mod( qminusone ).add( one ); // randomly choose k < q
                k.modInverse( qminusone );
            } catch ( ArithmeticException e ) {
                tryAgain = true;
            }
        } while ( tryAgain );

        BigInteger S1 = alpha.modPow( k, q ); // S1 = alpha Kth power mod q
        BigInteger S2 = k.modInverse( qminusone ).multiply( message.subtract( privateKey.multiply( S1 ) ) ).mod( qminusone ); // S2 = modInverse of K times (message minus privateKey times S1) mod q
        // (S1, S2) is the signature

        BigInteger V1 = alpha.modPow( message, q ); // V1 = alpha message power mod q
        BigInteger V2 = publicKey.modPow( S1, q ).multiply( S1.modPow( S2, q ) ).mod( q ); // V2 = publicKey power S1 times S1 power S2 mod q

        System.out.println( "V1 = " + V1.toString() + ";" );
        System.out.println( "V2 = " + V2.toString() + ";" );

        if ( V1.equals( V2 ) ) {
            System.out.println( "V1 == V2" );
        } else {
            System.out.println( "V1 != V2" );
        }
    }

    void DSA()
    {
        BigInteger p = qminusone.divide( two ); // Let us use DHGroup5 again.  p = (q-1)/2 is prime.
        BigInteger h = two; // h = 2 then g = 4
        BigInteger g = h.modPow( p.subtract( one ).divide( q ), p ); // h = 2 then g = 4

        Random random = new Random();
        BigInteger m = new BigInteger( 1235, random ).mod( q ); // message < p
        BigInteger x = new BigInteger( 1235, random ).mod( qminusone ).add( one ); // private key < p
        BigInteger y = g.modPow( x, p ); // public key y = g power x mod q
        BigInteger k = new BigInteger( 1235, random ).mod( qminusone ).add( one ); // per-message secret number k < p

        BigInteger r = g.modPow( k, p ).mod( q ); // r = g power k mod q mod p
        BigInteger s = k.modInverse( q ).multiply( m.add( x.multiply( r ) ) ).mod( q ); // s = modinverse of k times (m + xr) mod p
        // (r,s) is the signature for m

        BigInteger w = s.modInverse( q ); // w = inverse of s mod p
        BigInteger u1 = m.multiply( w ).mod( q ); // u1 = mw mod p
        BigInteger u2 = r.multiply( w ).mod( q ); // u2 = rw mod p
        BigInteger v = g.modPow( u1, p ).multiply( y.modPow( u2, p ) ).mod( p ).mod( q ); // v = g power u1 times y power u2 mod q mod p
        // Is v the same as r?

        System.out.println( "r = " + r.toString() + ";" );
        System.out.println( "v = " + v.toString() + ";" );

        if ( v.equals( r ) ) {
            System.out.println( "v == r" );
        } else {
            System.out.println( "v != r" );
        }
    }

    public static void main( String[] args )
    {
        H15 h15 = new H15();
        h15.readQ( "DHgroup5.txt" );
        h15.generateKeyPair();
        h15.ElGamalEncryption();
        h15.ElGamalSignature();
        h15.DSA();
    }

}

