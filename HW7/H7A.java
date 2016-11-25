// H7A.java CS6025 Yizong Cheng February 2015
// Adaptive Arithmetic Coding with Bi-level Images
// Usage: java H7A < source.pbm > compressed

import java.io.*;

public class H7A
{

    static final int maxRange = 65536;  // 2 ** 16
    static final int half = 32768;
    static final int quarter = 16384;
    static final int threequarters = 49152;
    static final int numberOfContexts = 1024; // context is ten bits; 2^10 = 1024;
    int width = 0, height = 0, bytesPerRow = 0;  // dimension of the image
    static final int BFSZ = 8192;
    byte[] buffer = new byte[BFSZ];
    int readLen = 0;
    int index = 0;
    boolean[][] bitmap = null;

    int[][] count = new int[numberOfContexts][2];
    int low = 0;
    // The current interval is [low, high)
    int high = maxRange;
    int buf = 0;
    // Saving bits before outputing byte
    int position = 0;
    //  number of bits saved in buf
    int follow = 0;
    // number of time doubled but interval straddles

    void readPBMHeader()
    {
        try {
            readLen = System.in.read( buffer );
        } catch ( IOException e ) {
            System.err.println( e.getMessage() );
            System.exit( 1 );
        }

        if ( buffer[0] != 'P' || buffer[1] != '4' ) {
            System.err.println( " not P4" );
            System.exit( 1 );
        }

        int pos = 0;

        while ( buffer[pos++] != '\n' );

        int pos2 = pos + 1;

        while ( buffer[pos2++] != '\n' );

        String secondLine = new String( buffer, pos, pos2 - pos - 1 );
        String[] terms = secondLine.split( " " );
        width = Integer.parseInt( terms[0] );
        height = Integer.parseInt( terms[1] );
        index = pos2;

        // Write out the header directly
        System.out.write( buffer, 0, pos2 );

        // Allocate bitmap and initialize to false
        bitmap = new boolean[3][width + 4];  // only need three rows
        for ( int i = 0; i < 3; i++ ) {
            for ( int j = 0; j < width + 4; j++ ) {
                bitmap[i][j] = false;
            }
        }

        bytesPerRow = width / 8;

        // Initialize count values to one
        for ( int i = 0; i < numberOfContexts; i++ ) {
            count[i][0] = count[i][1] = 1;    // count is never zero
        }
    }


    int getNextByte()
    {
        if ( index >= readLen ) {
            try {
                readLen = System.in.read( buffer );
            } catch ( IOException e ) {
                System.err.println( e.getMessage() );
                System.exit( 1 );
            }

            if ( readLen < 0 ) {
                return -1;
            }

            index = 0;
        }

        int b = buffer[index++];
        return b < 0 ? b + 256 : b;
    }

    void incrementCount( int context, boolean one )
    {
        // determine which count to increment
        int v = one ? 1 : 0;

        // increment count for the context and bit and compare
        if ( ++count[context][v] >= quarter ) { 

            // halve counts if they are >= quarter
            count[context][0] >>= 1;
            count[context][1] >>= 1;

            // ensure non-incremented count is non-zero following halving
            if ( count[context][1 - v] == 0 ) {
                count[context][1 - v] = 1;
            }
        }
    }

    void outputBit( int bit )
    {
        // save bits in buf when full output byte
        buf <<= 1;

        if ( bit == 1 ) {
            buf |= 1;
        }

        position++;

        if ( position == 8 ) {
            position = 0;
            System.out.write( buf );
            buf = 0;
        }

    }

    void update( int context, boolean one )
    {

        // Calculate a point on the range
        int t = low + count[context][0] * ( high - low ) / ( count[context][0] + count[context][1] );

        // Assign the calculated point on the rage as the new high or low depending on the bit value
        if ( one ) {
            low = t;
        } else {
            high = t;
        }

        // Encode and output
        for ( ;; ) {
            // double until larger than quarter
            if ( high < half ) {
                // most significant bit is 0
                outputBit( 0 );

                for ( int i = 0; i < follow; i++ ) {
                    outputBit( 1 );
                }

                follow = 0;
                high *= 2;
                low *= 2;
            } else if ( low >= half ) {
                // most significant bit is 1
                outputBit( 1 );

                for ( int i = 0; i < follow; i++ ) {
                    outputBit( 0 );
                }

                follow = 0;
                high = ( high * 2 ) - maxRange;
                low = ( low * 2 ) - maxRange;
            } else if ( low > quarter && high <= threequarters ) {
                follow++;
                // most significant bit unknown yet
                low = ( low * 4 - maxRange ) / 2;
                high = ( high * 4 - maxRange ) / 2;
            } else {
                break;
            }
        }
    }

    int getContext( int column )
    {
        // column >= 2
        int context = 0;

        // [row,column]
        // bits of context = [0,k-1][0,k][0,k+1][1,k-2][1,k-1][1,k][1,k+1][1,k+2][2,k-2][2,k-1]

        // row 0; column + -1..1;
        for ( int k = -1; k < 2; k++ ) {
            context <<= 1;

            if ( bitmap[0][column + k] ) {
                context |= 1;
            }
        }

        // row 1; column + -2..2;
        for ( int k = -2; k < 3; k++ ) {
            context <<= 1;

            if ( bitmap[1][column + k] ) {
                context |= 1;
            }
        }

        // row 2; column + -2..-1;
        for ( int k = -2; k < 0; k++ ) {
            context <<= 1;

            if ( bitmap[2][column + k] ) {
                context |= 1;
            }
        }

        return context;
    }

    void compress()
    {

        for ( int i = 0; i < height; i++ ) {
            int column = 2;

            for ( int j = 0; j < bytesPerRow; j++ ) {
                int b = getNextByte();

                // Test every bit in the byte
                for ( int test = 0x80; test > 0; test >>= 1 ) {
                    // Context is used as an index into the count array.
                    int context = getContext( column );

                    // Check if current bit is set in the byte
                    boolean one = ( ( test & b ) != 0 );

                    // Update ranges and output bits
                    update( context, one );

                    // Update count array
                    incrementCount( context, one );

                    // Read in the bit into the last row at the current column position
                    bitmap[2][column++] = one;
                }
            }

            // Shift rows up by one
            for ( int j = 2; j < width + 2; j++ ) {
                bitmap[0][j] = bitmap[1][j];
                bitmap[1][j] = bitmap[2][j];
            }
        }

        // flush leftover bits
        if ( position > 0 ) { 
            // Make leftover bits into leading bits of a byte, with trailing zeroes
            buf <<= ( 8 - position );
            System.out.write( buf );
        }

        System.out.flush();
    }

    public static void main( String[] args )
    {
        H7A h7 = new H7A();
        h7.readPBMHeader();
        h7.compress();
    }

}

