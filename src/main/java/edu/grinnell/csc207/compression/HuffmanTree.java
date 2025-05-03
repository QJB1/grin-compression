package edu.grinnell.csc207.compression;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * A HuffmanTree derives a space-efficient coding of a collection of byte
 * values.
 *
 * The huffman tree encodes values in the range 0--255 which would normally
 * take 8 bits. However, we also need to encode a special EOF character to
 * denote the end of a .grin file. Thus, we need 9 bits to store each
 * byte value. This is fine for file writing (modulo the need to write in
 * byte chunks to the file), but Java does not have a 9-bit data type.
 * Instead, we use the next larger primitive integral type, short, to store
 * our byte values.
 */
public class HuffmanTree {

    private static final short EOF = (short) 256;
    private final Node root;
    private final Map<Short, String> codes;

    /**
     * A node in the Huffman tree. Either a leaf or node.
     */
    private static class Node implements Comparable<Node> {
        final short value;
        final int freq;
        final Node left, right;

        /**
         * Constructs a leaf node.
         * 
         * @param value the byte value this leaf represents
         * @param freq  the frequency of that char
         */
        Node(short value, int freq) {
            this.value = value;
            this.freq = freq;
            this.left = null;
            this.right = null;
        }

        /**
         * Constructs an internal node by combining two child subtrees.
         * 
         * @param freq  the sum of the left and right child frequencies
         * @param left  the left subtree
         * @param right the right subtree
         */
        Node(int freq, Node left, Node right) {
            this.value = -1;
            this.freq = freq;
            this.left = left;
            this.right = right;
        }

        /**
         * Returns true if the node is a leaf
         * 
         * @return true if this node is a leaf (has no children)
         */
        boolean isLeaf() {
            return left == null && right == null;
        }

        /**
         * Compares two nodes by their frequency.
         * 
         * @param other the node to compare
         * @return int indicates how they compare, neg if less, zero if equal, positive
         *         if greater
         */
        @Override
        public int compareTo(Node other) {
            return Integer.compare(this.freq, other.freq);
        }
    }

    /**
     * Constructs a new HuffmanTree from a frequency map.
     * 
     * @param freqs a map from 9-bit values to frequencies.
     */
    public HuffmanTree(Map<Short, Integer> freqs) {
        // TODO: fill me in!
        // create priority queue
        PriorityQueue<Node> pq = new PriorityQueue<>();

        // for each char, create lead node and add to queue
        for (Short symbol : freqs.keySet()) {
            int count = freqs.get(symbol);
            pq.add(new Node(symbol, count));
        }

        pq.add(new Node(EOF, 1)); // add eof marker

        // new internal node, with the top two nodes of the priority queue as children
        while (pq.size() > 1) {
            Node left = pq.poll();
            Node right = pq.poll();
            pq.add(new Node(left.freq + right.freq, left, right));
        }

        // get the root,
        root = pq.poll();
        codes = new HashMap<>();
        deriveCode(root, "");
    }

    /**
     * Recursively traverses the Huffman tree to build the code map.
     * 
     * @param node the current node in the Huffman tree
     * @param path the huffman code in bits accumulated so far
     */
    private void deriveCode(Node node, String path) {
        // walks down from root, builds the huffman codes fr left/right directns
        if (node.isLeaf()) {
            codes.put(node.value, path);
        } else {
            deriveCode(node.left, path + '0');
            deriveCode(node.right, path + '1');
        }
    }

    /**
     * Constructs a new HuffmanTree from the given file.
     * 
     * @param in the input file (as a BitInputStream)
     */
    public HuffmanTree(BitInputStream in) {
        // TODO: fill me in!
        root = readTree(in);
        codes = new HashMap<>();
    }

    /**
     * Rebuilds the Huffman tree.
     * 
     * @param in the BitInputStream to read bits from
     * @return Node the rebuilt Node
     * @throws IllegalArgumentException if the stream ends unexpectedly
     */
    private Node readTree(BitInputStream in) throws IllegalArgumentException {
        // read bit to deal with tree errors
        int bit = in.readBit();
        if (bit < 0) {
            throw new IllegalArgumentException();
            // read bit to deal with leaf errors
        } else if (bit == 0) {
            int val = in.readBits(9);
            if (val < 0) {
                throw new IllegalArgumentException("Unexpected EOF in leaf");
            }
            return new Node((short) val, 0);
            // combines the left and right node to internal node
        } else {
            Node left = readTree(in);
            Node right = readTree(in);
            return new Node(left.freq + right.freq, left, right);
        }
    }

    /**
     * Writes this HuffmanTree to the given file as a stream of bits in a
     * serialized format.
     * 
     * @param out the output file as a BitOutputStream
     */
    public void serialize(BitOutputStream out) {
        // TODO: fill me in!
        // start serializing from root node
        writeTree(root, out);
    }

    /**
     * Recursively serializes the Huffman tree.
     * 
     * @param node the current node to serialize
     * @param out  the file to write the output to.
     */
    private void writeTree(Node node, BitOutputStream out) {
        // if node is leaf, write 0 plus its 9‑bit symbol val
        if (node.isLeaf()) {
            out.writeBit(0);
            out.writeBits(node.value, 9);
        } else { // if internal node, write 1 bit, then serialize left & right child
            out.writeBit(1);
            writeTree(node.left, out);
            writeTree(node.right, out);
        }
    }

    /**
     * Encodes the file given as a stream of bits into a compressed format
     * using this Huffman tree. The encoded values are written, bit-by-bit
     * to the given BitOuputStream.
     * 
     * @param in  the file to compress.
     * @param out the file to write the compressed output to.
     */
    public void encode(BitInputStream in, BitOutputStream out) {
        // TODO: fill me in!
        // read each 8-bit huffman val, get huffman code, then convert to binary bits
        int b;
        while ((b = in.readBits(8)) != -1) {
            String code = codes.get((short) b);
            for (char c : code.toCharArray()) {
                out.writeBit(c == '1' ? 1 : 0);
            }
        }
        // write out the eof marker at the end
        String eof = codes.get(EOF);
        for (char c : eof.toCharArray()) {
            out.writeBit(c == '1' ? 1 : 0);
        }
    }

    /**
     * Decodes a stream of huffman codes from a file given as a stream of
     * bits into their uncompressed form, saving the results to the given
     * output stream. Note that the EOF character is not written to out
     * because it is not a valid 8-bit chunk (it is 9 bits).
     * 
     * @param in  the file to decompress.
     * @param out the file to write the decompressed output to.
     */
    public void decode(BitInputStream in, BitOutputStream out) {
        // start at root, decode until encounter eof or streams ends
        Node node = root;
        while (true) {
            int bit = in.readBit();
            if (bit < 0) {
                break; // stream end
            }
            // traverses left or right
            if (bit == 0) {
                node = node.left;
            } else {
                node = node.right;
            }

            if (node.isLeaf()) {
                if (node.value == EOF) {
                    break; // break if eof
                }
                // write down the decoded byte, this becomes new root
                out.writeBits(node.value, 8);
                node = root;
            }
        }
    }
}
