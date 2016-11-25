// H10B.java CS6025 Yizong Cheng February 2015
// Implementing GF(2^n) with n = numberOfBits (3 in this version)
// Usage: java H10B

import java.io.*;
import java.util.*;

public class H10B
{

    static final int numberOfBits = 8; // 8 = log2(256)
    static final int fieldSize = 1 << numberOfBits; // = 2^numberOfBits
    static final int irreducible = 505; // log2(505) = 9 bits; degree = 8;
    static final int logBase = 3; 
    int[] alog = new int[fieldSize];
    int[] log = new int[fieldSize];

    int add( int a, int b )
    {
        return a ^ b;
    }

    int multiply( int a, int b )
    {
        int product = 0;

        for ( ; b > 0; b >>= 1 ) {
            if ( ( b & 1 ) > 0 ) {
                product ^= a;
            }

            a <<= 1;
        }

        return product;
    }

    int modMultiply( int a, int b, int m )
    {
        int product = 0;

        for ( ; b > 0; b >>= 1 ) {
            if ( ( b & 1 ) > 0 ) {
                product ^= a;
            }

            a <<= 1;

            if ( ( a & fieldSize ) > 0 ) {
                a ^= m;
            }
        }

        return product;
    }

    void makeLog()
    {
        alog[0] = 1;

        for ( int i = 1; i < fieldSize; i++ ) {
            alog[i] = modMultiply( logBase, alog[i - 1], irreducible );
        }

        for ( int i = 1; i < fieldSize; i++ ) {
            log[alog[i]] = i;
        }
    }

    int logMultiply( int a, int b )
    {
        return ( a == 0 || b == 0 ) ? 0 : alog[( log[a] + log[b] ) % ( fieldSize - 1 )];
    }

    int multiplicativeInverse( int a )
    {
        return alog[fieldSize - 1 - log[a]];
    }

    void irreducibles()
    {
        HashSet<Integer> hset = new HashSet<Integer>( 100 );

        for ( int i = 2; i < 256; i++ ) {
            for ( int j = i; j < 256; j++ ) {
                hset.add( multiply( i, j ) );
            }
        }

        for ( int i = 0; i < 1024; i++ ) {
            if ( !hset.contains( i ) ) {
                System.out.print( i + " " );
            }
        }

        System.out.println();
    }

    void printTables()
    {

        for ( int i = 0; i < fieldSize; i++ ) {
            for ( int j = 0; j < fieldSize; j++ ) {
                System.out.print( add( i, j ) + " " );
            }

            System.out.println();
        }  // addition table

        System.out.println();

        for ( int i = 1; i < fieldSize; i++ ) {
            for ( int j = 1; j < fieldSize; j++ ) {
                System.out.print( modMultiply( i, j, irreducible ) + " " );
            }

            System.out.println();
        }  // multiplication table

        System.out.println();

        for ( int i = 1; i < fieldSize; i++ ) {
            for ( int j = 1; j < fieldSize; j++ ) {
                System.out.print( logMultiply( i, j ) + " " );
            }

            System.out.println();
        }  // multiplication table

        System.out.println();

        for ( int i = 1; i < fieldSize; i++ ) {
            int power = 1;

            for ( int j = 1; j < fieldSize; j++ ) {
                power = modMultiply( i, power, irreducible );
                System.out.print( power + " " );
            } // power table

            System.out.println();
        }

        System.out.println();
    }

    public static void main( String[] args )
    {
        H10B h10 = new H10B();
        h10.makeLog();
        h10.printTables();
        h10.irreducibles();
    }

}

