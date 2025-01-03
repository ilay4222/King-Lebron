package il.ac.tau.cs.hanukcoin;

    /*
     * Block is 36 byte/288bit long.
     * record/block format:
     * 32 bit serial number
     * 32 bit wallet number
     * 64 bit prev_sig[:8]highest bits  (first half ) of previous block's signature (including all the block)
     * 64 bit puzzle answer
     * 96 bit sig[:12] - md5 - the first 12 bytes of md5 of above fields. need to make last N bits zero
     */


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

import il.ac.tau.cs.hanukcoin.Block.BlockError;

/**
 * Class that represnts one block in the block chane.
 * The clok hods a 36-bytes array and all operations are peformed directly on this array.
 */
public class Block {
    public static final int BLOCK_SZ = 36;
    public enum BlockError {OK, BAD_SERIAL_NO, SAME_WALLET_PREV, NO_PREV_SIG, SIG_NO_ZEROS, SIG_BAD, START_NOT_EQUAL}
    public byte[] data;
    public int getSerialNumber() {
        return HanukCoinUtils.intFromBytes(data, 0);
    }
    public int getWalletNumber() {
        return HanukCoinUtils.intFromBytes(data, 4);
    }
    
    public void writeTo(DataOutputStream dos) throws IOException {
        dos.write(getBytes(), 0, BLOCK_SZ);
    }
    public boolean equals(Block other) {
        return Arrays.equals(other.getBytes(), this.getBytes());
    }
    

    /**
     * Creste a block without a signature or puzzle fields.
     * @param serialNumber
     * @param walletNumber
     * @param prevSig8
     * @return new block
     */
    public static Block createNoSig(int serialNumber, int walletNumber, byte[] prevSig8) {
        Block b = new Block();
        b.data = new byte[BLOCK_SZ];
        HanukCoinUtils.intIntoBytes(b.data, 0, serialNumber);
        HanukCoinUtils.intIntoBytes(b.data, 4, walletNumber);
        System.arraycopy(prevSig8, 0, b.data, 8, 8);
        return b;
    }
    public static Block create(int serialNumber, int walletNumber, byte[] prevSig8, byte[] puzzle8, byte[] sig12) {
        Block b = createNoSig(serialNumber, walletNumber, prevSig8);
        System.arraycopy(sig12, 0, b.data, 24, 12);
        System.arraycopy(puzzle8, 0, b.data, 16, 8);
        return b;
    }

    public static Block readFrom(DataInputStream dis) throws IOException {
        Block b = new Block();
        b.data = new byte[BLOCK_SZ];
        dis.readFully(b.data);
        return b;
    }


    /**
     * put 8 bytes dat into puzzle field
     * @param longPuzzle - 64 bit puzzle
     */
    public void setLongPuzzle(long longPuzzle) {
        // Treat it as 2 32bit integers
        HanukCoinUtils.intIntoBytes(data, 16, (int)(longPuzzle >> 32));
        HanukCoinUtils.intIntoBytes(data, 20, (int)(longPuzzle & 0xFFFFFFFF));
        
        
    }
    
    /**
     * compare this.puzzle - other.puzzle
     * @param other
     * @return 1 if this puzzle bigger, 0 if equal, -1 if this smaller
     */
    public int comparePuzzle(Block other) {
        return HanukCoinUtils.ArraysPartCompare(8, this.getBytes(), 16, other.getBytes(), 16);
    }
    
    /**
     * given a block signature - take first 12 bytes of it and put into the signature field of this block
     * @param sig
     */
    public void setSignaturePart(byte[] sig) {
        System.arraycopy(sig, 0, data, 24, 12);
    }

    public Block clone() {
        Block b = new Block();
        b.data = Arrays.copyOf(this.getBytes(), BLOCK_SZ);
        return b;
    }
    
    /**
     * calc block signature based on all fields besides signature itself.
     * @return  16 byte MD5 signature
     */
    public byte[] calcSignature() {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");  // may cause NoSuchAlgorithmException
            md.update(data, 0, 24);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Internal error - missing MD5");
        }
    }

    /**
     * getter for internal bytes of block
     * @return
     */
    public byte[] getBytes() {
        return data;
    }

    /**
     * calc signature for the bloxk and see if in has the required number of zeros and matches signature written to the block
     * @return BlockError: SIG_NO_ZEROS or SIG_BAD or OK
     */
    public BlockError checkSignature() {
        byte[] sig = calcSignature();
        int serialNum = getSerialNumber();
        int nZeros = HanukCoinUtils.numberOfZerosForPuzzle(serialNum);
        if (!HanukCoinUtils.checkSignatureZeros(sig, nZeros)) {
            return BlockError.SIG_NO_ZEROS;
        }
        if (!HanukCoinUtils.ArraysPartEquals(12, data, 24, sig, 0)) {
            return BlockError.SIG_BAD;
        }
        return BlockError.OK;
    }

    /**
     * given a block oprevios to this one - check if this one is valid.
     * @param prevBlock
     * @return BlockError
     */
    public BlockError checkValidNext(Block prevBlock) {
        if (getSerialNumber() !=  prevBlock.getSerialNumber() + 1) {
            return BlockError.BAD_SERIAL_NO;  // bad serial number - should be prev + 1
        }
        if (getWalletNumber() == prevBlock.getWalletNumber()) {
            return BlockError.SAME_WALLET_PREV;  // don't allow two consequent blocks with same wallet
        }
        if (!HanukCoinUtils.ArraysPartEquals(8, data, 8, prevBlock.data, 24)) {
            return BlockError.NO_PREV_SIG;  // check prevSig field is indeed signature of prev block
        }
        return checkSignature();
    }

    /**
     * String with HEX dunp of block for debugging.
     * @return string - hex dump
     */
    public String binDump() {
        String dump = "";
        for (int i = 0; i < BLOCK_SZ; i++) {
            if ((i % 4) == 0) {
                dump += " ";
            }
            if ((i % 8) == 0) {
                dump += "\n";
            }
            dump += String.format("%02X ", data[i]);
        }
        return dump;
    }
    
    static private byte[] parseByteStr(String s) {
    	   ArrayList<Byte> a = new ArrayList<Byte>();
    	   for (String hex : s.split("\\s+")) {
    	       byte b = (byte) Integer.parseInt(hex, 16);
    	       a.add(b);
    	   }
    	   byte[] result = new byte[a.size()];
    	   for(int i = 0; i < a.size(); i++) {
    	       result[i] = a.get(i);
    	   }
    	   return result;
    	}

    	public static Block createBlock0forTestStage() {
    	   Block g = new Block();
    	        g.data = parseByteStr("00 00 00 00  00 00 00 00  \n" +
    	                        "43 4F 4E 54  45 53 54 30  \n" +
    	                        "6C E4 BA AA  70 1C E0 FC  \n" +
    	                        "4B 72 9D 93  A2 28 FB 27  \n" +
    	                        "4D 11 E7 25 ");

    	   return g;
    	}

    	public static BlockError checkBlocksValidity(ArrayList<Block> a) {
    		Block blockI0 = a.get(0);
    		if (!blockI0.equals(createBlock0forTestStage())){
    			return BlockError.START_NOT_EQUAL;
    		}
    		BlockError err0 = blockI0.checkSignature();
    		if (!err0.equals(BlockError.OK)) {
    			return err0;
    		}
    		for (int i = 1 ; i < a.size() ; i ++) {
    			BlockError errI =  a.get(i).checkValidNext(a.get(i-1));
    			if (!errI.equals(BlockError.OK)) {
        			return errI;
        		}
    		}
    		return BlockError.OK;
    	}
}