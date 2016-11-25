// H9.java CS6025 Yizong Cheng February 2015
// Testing an implementation of CRC32 against java.util.zip.CRC32
// Usage: java H9

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class H9
{

    static final int numberOfBytes = 256;
    CRC32 crc32 = new CRC32();
    long[] crc_table = new long[numberOfBytes];
    byte[] buf = new byte[numberOfBytes];

    void test()
    {
        Random random = new Random();

        for ( int i = 0; i < numberOfBytes; i++ ) {
            buf[i] = ( byte )( random.nextInt( 256 ) );
        }

        crc32.update( buf );
        long crc32csum = crc32.getValue();

        System.out.print( "CRC32 = " );
        System.out.print( Long.toHexString( crc32csum ) );
        System.out.println( "" );

        long h9csum = checksum( buf );

        System.out.print( "H9 = " );
        System.out.print( Long.toHexString( h9csum ) );
        System.out.println( "" );

        System.out.flush();
    }

    void makeTable()
    {
        for ( int n = 0; n < 256; n++ ) {
            long c = ( long ) n;

            for ( int k = 0; k < 8; k++ ) {
                if ( ( c & 1 ) != 0 ) {
                    c = 0xedb88320L ^ ( c >> 1 );
                } else {
                    c = c >> 1;
                }
            }

            crc_table[n] = c;
        }
    }

    long checksum( byte[] buf )
    {
        long c = 0xffffffffL;
        int len = buf.length;

        for ( int n = 0; n < len; n++ ) {
            c = crc_table[( int )( c ^ buf[n] ) & 0xff] ^ ( c >> 8 );
        }

        return c ^ 0xffffffffL;
    }

    public static void main( String[] args )
    {
        H9 crc = new H9();
        crc.makeTable();
        crc.test();
    }

}

