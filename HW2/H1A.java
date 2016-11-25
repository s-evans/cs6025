// H1A.java CS6025 Yizong Cheng January 2015
// Huffman encoder
// Usage: java H1A original > encoded

import java.io.*;
import java.util.*;

class Node implements Comparable {

    Node left, right;
    int symbol;
    int frequency;

    public Node( Node l, Node r, int s, int f ) {
        left = l;
        right = r;
        symbol = s;
        frequency = f;
    }

    public int compareTo( Object obj ) {
        Node n = (Node) obj;
        return frequency - n.frequency;
    }

}

public class H1A {

    static final int numberOfSymbols = 256;
    static final int blockSize = 1024;
    int[] freq = new int[numberOfSymbols];
    Node tree = null;
    String[] codewords = new String[numberOfSymbols];
    int[][] codetree = null;
    int buf = 0;
    int position = 0;
    int actualNumberOfSymbols = 0;
    int compressed_size = 0;
    int uncompressed_size = 0;

    double getEntropy( )
    {
        double entropy = 0;
        double log2 = Math.log( 2.0 );

        for ( int i = 0; i < numberOfSymbols; i++ ) {
            if ( freq[i] > 0 ) {
                double prob = freq[i] * 1.0 / uncompressed_size;
                entropy += prob * Math.log( prob ) / log2;
            }
        }

        return -entropy;
    }


    void count( String filename ) { // count symbol frequencies
        byte[] buffer = new byte[blockSize];
        FileInputStream fis = null;

        // Open the file
        try {
            fis = new FileInputStream(filename);
        } catch ( FileNotFoundException e ) {
            System.err.println(filename + " not found");
            System.exit(1);
        }

        int len = 0;

        // Zero out the frequency count array
        for ( int i = 0; i < numberOfSymbols; i++ ) {
            freq[i] = 0;
        }

        // Count symbols
        try {
            while ( ( len = fis.read( buffer ) ) >= 0 ) {
                for (int i = 0; i < len; i++) {
                    int symbol = buffer[i];

                    if ( symbol < 0 ) {
                        symbol += 256;
                    }

                    freq[symbol]++;
                }
            }

            fis.close();
        } catch ( IOException e ) {
            System.err.println("IOException");
            System.exit(1);
        }
    }

    void makeTree() { // make Huffman prefix codword tree
        PriorityQueue<Node> pq = new PriorityQueue<Node>();

        // Count number of symbols used and populate priority tree with nodes
        for ( int i = 0; i < numberOfSymbols; i++ ) {
            if ( freq[i] > 0 ) {
                actualNumberOfSymbols++;
                pq.add( new Node( null, null, i, freq[i] ) );
            }
        }

        while ( pq.size() > 1 ) {
            // Remove lowest frequency symbols
            Node a = pq.poll();
            Node b = pq.poll();

            // Create new node that summarizes the two nodes and add it to the priority queue
            pq.add( new Node( a, b, -1, a.frequency + b.frequency ) );
        }

        tree = pq.poll();
    }

    void dfs( Node n, String code ) { // generate all codewords
        if ( n.symbol < 0 ) {
            // For intermediate nodes, continue dfs appending zero for the left node's code word, and one for the right's
            dfs( n.left, code + "0" );
            dfs( n.right, code + "1" );
        } else {
            // When a leaf node is reached, save the code word
            codewords[n.symbol] = code;
        }
    }

    // TODO: Figure out what this does
    void buildTree() { // make the prefix code tree

        codetree = new int[actualNumberOfSymbols * 2 - 1][2];
        int treeSize = 1;

        // Zero out the code tree
        for ( int i = 0; i < actualNumberOfSymbols * 2 - 1; i++ ) {
            codetree[i][0] = codetree[i][1] = 0;
        }

        // For each possible symbol
        for ( int cw_idx = 0; cw_idx < numberOfSymbols; cw_idx++ ) {

            // For each symbol used
            if ( codewords[cw_idx] != null ) {
                int len = codewords[cw_idx].length();
                int row = 0;

                // For each character in the codeword
                for ( int char_idx = 0; char_idx < len; char_idx++ ) {

                    // Determine the value of the character (0 or 1)
                    int side = codewords[cw_idx].charAt(char_idx) - '0';

                    // Get the value in the table [row][]
                    if ( codetree[row][side] <= 0 ) {
                        // If the value is zero, set the table value to the tree size and increment the tree size
                        codetree[row][side] = treeSize++;
                    }

                    // Set row to the value in the table
                    row = codetree[row][side];
                }

                // Set the right side to the iterator
                codetree[row][1] = cw_idx;
            }
        }
    }

    void outputTree() {
        // Output format consists of the size of the dictionary ...
        System.out.write( actualNumberOfSymbols );

        // ... then the tree
        for ( int i = 0; i < actualNumberOfSymbols * 2 - 1; i++ ) {
            System.out.write( codetree[i][0] );
            System.out.write( codetree[i][1] );
        }
    }

    void encoding( String filename ) { // compress filename to System.out
        byte[] buffer = new byte[blockSize];
        FileInputStream fis = null;

        try {
            fis = new FileInputStream( filename );
        } catch ( FileNotFoundException e ) {
            System.err.println( filename + " not found" );
            System.exit( 1 );
        }

        int len = 0;

        try {
            while ( ( len = fis.read( buffer ) ) >= 0 ) {
                uncompressed_size += len;

                for ( int i = 0; i < len; i++ ) {
                    int symbol = buffer[i];

                    if ( symbol < 0 ) {
                        symbol += 256;
                    }

                    outputbits( codewords[symbol] );
                }
            }

            fis.close();
        } catch ( IOException e ) {
            System.err.println("IOException");
            System.exit(1);
        }

        if ( position > 0 ) {
            System.out.write( buf );
            compressed_size++;
        }

        System.out.flush();
    }

    void outputbits( String bitstring ) { // output codeword
        for ( int i = 0; i < bitstring.length(); i++ ) {
            buf <<= 1;

            if ( bitstring.charAt( i ) == '1' ) {
                buf |= 1;
            }

            position++;

            if ( position == 8 ) {
                System.out.write( buf );
                position = 0;
                buf = 0;
                compressed_size++;  // size of the compressed file
            }
        }
    }

    public static void main( String[] args ) {
        if ( args.length < 1 ) {
            System.err.println( "Usage: java Huffman file > compressed" );
            return;
        }

        H1A h1 = new H1A();
        h1.count( args[0] );
        h1.makeTree();
        h1.dfs( h1.tree, "" );
        h1.buildTree();
        h1.outputTree();
        h1.encoding( args[0] );

        System.err.format( "Entropy = %f\n", h1.getEntropy() );
        System.err.format( "compressed size = %d bytes\n", h1.compressed_size );
        System.err.format( "uncompressed size = %d bytes\n", h1.uncompressed_size );
        System.err.format( "compression ratio = %f bytes\n", (h1.uncompressed_size * 1.0) / (h1.compressed_size * 1.0) );
    }
}

