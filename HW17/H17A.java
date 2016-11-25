// H17A.java CS6025 Yizong Cheng March 2015
// RSA-PSS encoding
// Usage:  java H17A privateKey < message > signature

import java.io.*;
import java.util.*;
import java.math.*;
import java.security.*;

public class H17A
{

    static final int hLen = 20;
    static final int sLen = 20;
    static final int inBufferSize = 4096;
    Random rand = new Random();
    BigInteger n = null;
    int emBits;
    int emLen;
    BigInteger d = null;
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
    BigInteger m = null;
    BigInteger s = null;

    void readPrivateKey( String filename )
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
        d = new BigInteger( in.nextLine(), 16 );
        in.close();
        emBits = n.bitLength() - 1;
        emLen = emBits % 8 > 0 ? emBits / 8 + 1 : emBits / 8;
    }

    void encodingStep1()
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

    void encodingStep2()
    {
        BigInteger randomSalt = new BigInteger( sLen * 8, rand );
        salt = randomSalt.toByteArray();

        for ( int i = 0; i < 8; i++ ) {
            padding1[i] = 0;
        }
    }

    void encodingStep3()
    {
        MD.reset();
        MD.update( padding1 );
        MD.update( mHash );
        H = MD.digest( salt );
    }

    void encodingStep4()
    {
        int border = emLen - hLen - sLen - 2;
        DB = new byte[emLen - hLen - 1];

        for ( int i = 0; i < border; i++ ) {
            DB[i] = 0;
        }

        DB[border] = 1;

        for ( int i = 0; i < sLen; i++ ) {
            DB[border + i + 1] = salt[i];
        }
    }

    void encodingStep5()
    {
        dbMask = MGF1( H, emLen - hLen - 1 );
    }

    void encodingStep6()
    {
        for ( int i = 0; i < emLen - hLen - 1; i++ ) {
            DB[i] ^= dbMask[i];
        }
    }

    void encodingStep7()
    {
        int diff = 8 * emLen - emBits;
        int singleBit = 0x80;
        int mask = 0xff;

        for ( int i = 0; i < diff; i++ ) {
            mask ^= singleBit;
            singleBit >>= 1;
        }

        DB[0] &= mask;
    }

    void encodingStep8()
    {
        EM = new byte[emLen];

        for ( int i = 0; i < emLen - hLen - 1; i++ ) {
            EM[i] = DB[i];
        }

        for ( int i = 0; i < hLen; i++ ) {
            EM[emLen - hLen - 1 + i] = H[i];
        }

        EM[emLen - 1] = ( byte )0xbc;
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

    void formSignature()
    {
        m = new BigInteger( EM );
        s = m.modPow( d, n );
    }

    void printSignature()
    {
        System.out.println( s.toString( 16 ) );
    }

    void RSAEncryption()   // just for checking the keys
    {
        BigInteger M = new BigInteger( 2047, rand );
        System.out.println( M.toString( 16 ) );
        BigInteger c = M.modPow( e, n );
        System.out.println( c.toString( 16 ) );
        BigInteger decoded = c.modPow( d, n );
        System.out.println( decoded.toString( 16 ) );
    }

    void sign()
    {
        encodingStep1();
        encodingStep2();
        encodingStep3();
        encodingStep4();
        encodingStep5();
        encodingStep6();
        encodingStep7();
        encodingStep8();
        formSignature();
    }

    public static void main( String[] args )
    {
        if ( args.length < 1 ) {
            System.out.println( "Usage: java H17A privateKey < message > signature" );
            return;
        }

        H17A h17 = new H17A();
        h17.readPrivateKey( args[0] );
        h17.sign();
        h17.printSignature();
    }

}

