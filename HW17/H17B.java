// H17B.java CS6025 Yizong Cheng March 2015
// RSA-PSS verification
// Usage:  java H17B publicKey signature < message

import java.io.*;
import java.util.*;
import java.math.*;
import java.security.*;

public class H17B
{

    static final int hLen = 20;
    static final int sLen = 20;
    static final int inBufferSize = 4096;
    Random rand = new Random();
    BigInteger n = null;
    int emBits;
    int emLen;
    BigInteger e = null;
    byte[] inBuffer = new byte[inBufferSize];
    int messageLen = 0;
    MessageDigest MD = null;
    byte[] mHash = null;
    byte[] padding1 = new byte[8];
    byte[] salt = null;
    byte[] H = null;
    byte[] DB = null;
    byte[] dbMask = null;
    byte[] EM = null;
    byte[] maskedDB = null;
    byte[] Mprime = null;
    byte[] Hprime = null;
    BigInteger m = null;
    BigInteger s = null;

    void readPublicKey( String filename )
    {
        Scanner in = null;

        try {
            in = new Scanner( new File( filename ) );
        } catch ( FileNotFoundException e ) {
            System.err.println( filename + " not found" );
            System.exit( 1 );
        }

        e = new BigInteger( in.nextLine(), 16 );
        n = new BigInteger( in.nextLine(), 16 );
        in.close();
        emBits = n.bitLength() - 1;
        emLen = emBits % 8 > 0 ? emBits / 8 + 1 : emBits / 8;
    }

    void readSignature( String filename )
    {
        Scanner in = null;

        try {
            in = new Scanner( new File( filename ) );
        } catch ( FileNotFoundException e ) {
            System.err.println( filename + " not found" );
            System.exit( 1 );
        }

        s = new BigInteger( in.nextLine(), 16 );
        in.close();
    }

    void verifyStep1()
    {
        byte[] inBuffer = new byte[inBufferSize];
        int messageLen = 0;

        try {
            MD = MessageDigest.getInstance( "SHA-1" );
        } catch ( NoSuchAlgorithmException e ) {
            System.err.println( e.getMessage() );
            System.exit( 1 );
        }

        do {
            try {
                messageLen = System.in.read( inBuffer );
            } catch ( IOException e ) {
                System.err.println( e.getMessage() );
                System.exit( 1 );
            }

            if ( messageLen > 0 ) {
                MD.update( inBuffer, 0, messageLen );
            }
        } while ( messageLen > 0 );

        mHash = MD.digest();
    }

    void verifyStep2()
    {
        if ( emLen < hLen + sLen + 2 ) {
            System.err.println( "Inconsistent: emLen too big" );
            System.exit( 1 );
        }
    }

    void verifyStep3()
    {
        int lastByte = EM[emLen - 1];

        if ( lastByte < 0 ) {
            lastByte += 256;
        }

        if ( lastByte != 0xbc ) {
            System.err.println( "Inconsistent: BC" );
            System.exit( 1 );
        }
    }

    void verifyStep4()
    {
        maskedDB = new byte[emLen - hLen - 1];
        System.arraycopy( EM, 0, maskedDB, 0, emLen - hLen - 1 );

        H = new byte[hLen];
        System.arraycopy( EM, emLen - hLen - 1, H, 0, hLen );
    }

    void verifyStep5()
    {
        for ( int i = 0 ; i < emLen * 8 - emBits ; ++i ) {
            int octet = i / 8;
            int bit = 7 - i % 8;
            if ( ( maskedDB[octet] & ( 1 << bit ) ) != 0 ) {
                System.err.println( "Inconsistent: maskedDB" );
                System.exit( 1 );
            }
        }
    }

    void verifyStep6()
    {
        dbMask = MGF1( H, emLen - hLen - 1 );
    }

    void verifyStep7()
    {
        DB = new byte[emLen - hLen - 1];

        for ( int i = 0 ; i < emLen - hLen - 1 ; ++i ) {
            DB[i] = (byte) ( maskedDB[i] ^ dbMask[i] );
        }
    }

    void verifyStep8()
    {
        for ( int i = 0 ; i < 8 * emLen - emBits ; ++i ) { 
            int octet = i / 8;
            int bit = 7 - i % 8;
            DB[octet] &= ~( 1 << bit );
        }
    }

    void verifyStep9()
    {
        for ( int i = 0 ; i < emLen - hLen - sLen - 2 ; ++i ) {
            if ( DB[i] != 0 ) {
                System.err.println( "Inconsistent: DB[" + i + "] != 0" );
                System.exit( 1 );
            }
        }

        if ( DB[emLen - hLen - sLen - 2] != 1 ) {
            System.err.println( "Inconsistent: DB[final] != 1" );
            System.exit( 1 );
        }
    }

    void verifyStep10()
    {
        salt = new byte[sLen];
        System.arraycopy( DB, emLen - hLen - 1 - sLen, salt, 0, sLen );
    }

    void verifyStep11()
    {
        Mprime = new byte[sLen + mHash.length + 8];

        for ( int i = 0 ; i < 8 ; ++i ) { 
            Mprime[i] = 0; 
        }

        System.arraycopy( mHash, 0, Mprime, 8, mHash.length );
        System.arraycopy( salt, 0, Mprime, 8 + mHash.length, sLen );
    }

    void verifyStep12()
    {
        try {
            MD = MessageDigest.getInstance( "SHA-1" );
        } catch ( NoSuchAlgorithmException e ) {
            System.err.println( e.getMessage() );
            System.exit( 1 );
        }

        MD.reset();

        MD.update( Mprime, 0, Mprime.length );

        Hprime = MD.digest();
    }

    void verifyStep13()
    {
        if ( Arrays.equals( H, Hprime ) ) {
            System.out.println( "Consistent" );
        } else {
            System.out.println( "Inconsistent: Hprime" );
        }
    }

    byte[] MGF1( byte[] X, int maskLen )
    {
        byte[] mask = new byte[maskLen];
        byte[] counter = new byte[4];

        for ( int i = 0; i < 4; i++ ) {
            counter[i] = 0;
        }

        int k = maskLen % hLen > 0 ? maskLen / hLen : maskLen / hLen - 1;
        int offset = 0;

        for ( byte count = 0; count <= k; count++ ) {
            MD.reset();
            MD.update( X );
            counter[3] = count;
            byte[] h = MD.digest( counter );

            for ( int i = 0; i < hLen; i++ ) {
                if ( offset + i < maskLen ) {
                    mask[offset + i] = h[i];
                }
            }

            offset += hLen;
        }

        return mask;
    }

    void decrypt()
    {
        m = s.modPow( e, n );
        EM = m.toByteArray();
        emLen = EM.length;
    }

    void verify()
    {
        decrypt();
        verifyStep1();
        verifyStep2();
        verifyStep3();
        verifyStep4();
        verifyStep5();
        verifyStep6();
        verifyStep7();
        verifyStep8();
        verifyStep9();
        verifyStep10();
        verifyStep11();
        verifyStep12();
        verifyStep13();
    }

    public static void main( String[] args )
    {
        if ( args.length < 2 ) {
            System.out.println( "Usage: java H17B publicKey signature < message" );
            return;
        }

        H17B h17 = new H17B();
        h17.readPublicKey( args[0] );
        h17.readSignature( args[1] );
        h17.verify();
    }

}

