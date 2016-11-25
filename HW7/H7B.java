
// H7B.java CS6025 Yizong Cheng February 2015
// Adaptive Arithmetic Decoding with Bi-level Images
// Usage: java H7B < compressed > decoded.pbm

import java.io.*;

public class H7B
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
    int high = maxRange;
    int inBuf = 0;
    int inPosition = 0;
    int outBuf = 0;
    int outPosition = 0;
    int codeword = 0;

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

        // Write out the same header as given
        System.out.write( buffer, 0, pos2 );

        // Allocate and initialize bitmap as false
        bitmap = new boolean[3][width + 4];
        for ( int i = 0; i < 3; i++ ) { 
            for ( int j = 0; j < width + 4; j++ ) {
                bitmap[i][j] = false;
            }
        }

        bytesPerRow = width / 8;

        // Initialize counts to 1 for each context value
        for ( int i = 0; i < numberOfContexts; i++ ) {
            count[i][0] = count[i][1] = 1;
        }
    }

    void incrementCount( int context, boolean one )
    {
        // Convert type
        int v = one ? 1 : 0;

        // Increment count and compare
        if ( ++count[context][v] >= quarter ) {

            // Halve count values
            count[context][0] >>= 1;
            count[context][1] >>= 1;

            // Ensure nonincremented value is non-zero
            if ( count[context][1 - v] == 0 ) {
                count[context][1 - v] = 1;
            }
        }
    }

    void outputBit( boolean bit )
    {
        // save bits in buf when full output byte
        outBuf <<= 1;

        if ( bit ) {
            outBuf |= 1;
        }

        outPosition++;

        if ( outPosition == 8 ) {
            outPosition = 0;
            System.out.write( outBuf );
            outBuf = 0;
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

    int inputBit()
    {
        if ( inPosition == 0 ) {
            inBuf = getNextByte();

            if ( inBuf < 0 ) {
                return -1;
            }

            inPosition = 0x80;
        }

        int t = ( ( inBuf & inPosition ) == 0 ) ? 0 : 1;
        inPosition >>= 1;
        return t;
    }

    int follow = 0;

    boolean update( int context )
    {

        // Calculate a point in the range based on the context
        int t = low + count[context][0] * ( high - low ) / ( count[context][0] + count[context][1] );

        // Create a bit based on the codeword and the point
        boolean ret = codeword >= t;

        if ( ret ) {
            // Move range up if bit == 1
            low = t;
        } else {
            // Move range down if bit == 0
            high = t;
        }

        for ( ;; ) { 
            // double until larger than quarter
            if ( high < half ) {
                // Update range
                high *= 2;
                low *= 2;

                // Update codeword to follow the range
                codeword *= 2;

                // Shift in a new bit
                switch ( inputBit() )
                {
                case 1:
                    codeword |= 1;
                    break;
                case 0:
                    break;
                default:
                    System.exit( 0 );
                    break;
                }

            } else if ( low >= half ) {
                // Update range
                high = ( high * 2 ) - maxRange;
                low = ( low * 2 ) - maxRange;

                // Update codeword to follow the range
                codeword = ( codeword * 2 ) - maxRange;

                // Shift in a new bit
                switch ( inputBit() )
                {
                case 1:
                    codeword |= 1;
                    break;
                case 0:
                    break;
                default:
                    System.exit( 0 );
                    break;
                }

            } else if ( low > quarter && high <= threequarters ) {
                // Update range
                low = ( low * 4 - maxRange ) / 2;
                high = ( high * 4 - maxRange ) / 2;

                // Update codeword to follow the range
                codeword = ( codeword * 4 - maxRange ) / 2;

                // Shift in a new bit
                switch ( inputBit() )
                {
                case 1:
                    codeword |= 1;
                    break;
                case 0:
                    break;
                default:
                    System.exit( 0 );
                    break;
                }

            } else {
                break;
            }
        }

        return ret;
    }

    int getContext( int column )
    {
        // column >= 2
        int context = 0;

        // -1 - 2 = -3; |-3| = 3
        for ( int k = -1; k < 2; k++ ) {
            context <<= 1;

            if ( bitmap[0][column + k] ) {
                context |= 1;
            }
        }

        // -2 - 3 = -5; |-5| = 5
        for ( int k = -2; k < 3; k++ ) {
            context <<= 1;

            if ( bitmap[1][column + k] ) {
                context |= 1;
            }
        }

        // -2 - 0 = -2; |-2| = 2
        for ( int k = -2; k < 0; k++ ) {
            context <<= 1;

            if ( bitmap[2][column + k] ) {
                context |= 1;
            }
        }

        return context;
    }

    void uncompress()
    {
        // Read in a 16 bit codeword from file
        for ( int i = 0; i < 16; i++ ) {
            codeword <<= 1;
            if (inputBit() > 0) codeword|= 1;
        }

        for ( int i = 0; i < height; i++ ) {

            for ( int j = 0; j < width; j++ ) {

                // Get the context value about the current column
                int context = getContext( j + 2 );

                // Get the bit value 
                boolean one = update( context );

                // Update count of bit value for the context value
                incrementCount( context, one );

                // Read bit into row 2, which is always the freshest / current row, at the current column position;
                bitmap[2][j + 2] = one;

                // Write out the bit
                outputBit( one );
            }

            // Shift rows of bits up by one row
            for ( int j = 2; j < width + 2; j++ ) {
                bitmap[0][j] = bitmap[1][j];
                bitmap[1][j] = bitmap[2][j];
            }

        }

        System.out.flush();
    }

    public static void main( String[] args )
    {
        H7B h7 = new H7B();
        h7.readPBMHeader();
        h7.uncompress();
    }

}

