// H14C.java CS6025 Yizong Cheng March 2015
// checking primality of Group 5 q and (q-1)/2
// checking that 2 is a primitive element in GF(q) (a primitive root of q)
// Usage: java H14C

import java.math.*;
import java.io.*;
import java.util.*;

public class H14C
{
    String hexQ = null;
    BigInteger q = null;
    BigInteger p = null;  // p = (q-1)/ 2
    static final BigInteger two = new BigInteger( "2" );
    static final BigInteger one = new BigInteger( "1" );

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

    void testPrimality()
    {
        if ( q.isProbablePrime( 200 ) ) {
            System.out.println( "q is probably prime" );
        }

        p = q.subtract( one ).shiftRight( 1 );

        if ( p.isProbablePrime( 200 ) ) {
            System.out.println( "p is probably prime" );
        }
    }

    void testPrimitiveness()
    {
        BigInteger twopq = two.modPow(p, q);
        System.out.println( twopq.toString( 16 ) );
    }

    public static void main( String[] args )
    {
        H14C h14 = new H14C();
        h14.readQ( "DHgroup5.txt" );
        h14.testPrimality();
        h14.testPrimitiveness();
    }
}

