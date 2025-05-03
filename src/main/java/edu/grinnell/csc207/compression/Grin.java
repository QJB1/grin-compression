package edu.grinnell.csc207.compression;

import java.util.HashMap;
import java.util.Map;

/**
 * The driver for the Grin compression program.
 */
public class Grin {
    /**
     * Decodes the .grin file denoted by infile and writes the output to the
     * .grin file denoted by outfile.
     * 
     * @param infile  the file to decode
     * @param outfile the file to ouptut to
     */
    public static void decode(String infile, String outfile) throws java.io.IOException {
        // TODO: fill me in!
        // open bit‑level input and output streams
        BitInputStream in = new BitInputStream(infile);
        BitOutputStream out = new BitOutputStream(outfile);

        // verifies the file signature
        int magic = in.readBits(32);
        if (magic != 0x736) {
            in.close();
            out.close();
            throw new IllegalArgumentException("Invalid magic number");
        }

        // uses tree to decode, write to output
        HuffmanTree tree = new HuffmanTree(in);
        tree.decode(in, out);
        in.close();
        out.close();
    }

    /**
     * Creates a mapping from 8-bit sequences to number-of-occurrences of
     * those sequences in the given file. To do this, read the file using a
     * BitInputStream, consuming 8 bits at a time.
     * 
     * @param file the file to read
     * @return a freqency map for the given file
     */
    public static Map<Short, Integer> createFrequencyMap(String file) throws java.io.IOException {
        // initialize freq map
        Map<Short, Integer> freq = new HashMap<>();

        // open the input file as bit stream, read 8 bits until end of file
        BitInputStream in = new BitInputStream(file);
        int b;
        while ((b = in.readBits(8)) != -1) {
            short key = (short) b;
            freq.put(key, freq.getOrDefault(key, 0) + 1);
        }
        in.close();

        return freq;
    }

    /**
     * Encodes the given file denoted by infile and writes the output to the
     * .grin file denoted by outfile.
     * 
     * @param infile  the file to encode.
     * @param outfile the file to write the output to.
     */
    public static void encode(String infile, String outfile) throws java.io.IOException {
        // TODO: fill me in!
        // builds a frequency map from input/output streams
        Map<Short, Integer> freq = createFrequencyMap(infile);
        BitInputStream in = new BitInputStream(infile);
        BitOutputStream out = new BitOutputStream(outfile);

        // write the 32-bit file signature, serialize huffman tree into output
        HuffmanTree tree = new HuffmanTree(freq);
        out.writeBits(0x736, 32);
        tree.serialize(out);

        // encodes file bytes, then closes streams
        tree.encode(in, out);
        in.close();
        out.close();
    }

    /**
     * The entry point to the program.
     * 
     * @param args the command-line arguments.
     */
    public static void main(String[] args) throws java.io.IOException {
        // TODO: fill me in!
        // decides if encode or decode based on command line input
        if (args.length == 3 && "encode".equals(args[0])) {
            encode(args[1], args[2]);
        } else if (args.length == 3 && "decode".equals(args[0])) {
            decode(args[1], args[2]);
        } else {
            System.out.println("Usage: java Grin <encode|decode> <infile> <outfile>");
        }
    }
}
