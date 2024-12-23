package il.ac.tau.cs.hanukcoin;

import java.util.Random;

public class MineThread extends Thread{
	int id;
	boolean[] notFound;//len 1
	Block[] daBlock;//len 1
	
	public MineThread(int n) {
		id = n;
	}
	
	public static Block mineCoinAttempt(int myWalletNum, Block prevBlock, int attemptsCount) {
        int newSerialNum = prevBlock.getSerialNumber() + 1;
        if (prevBlock.getWalletNumber() == myWalletNum) {
            return null;  // no point in trying to mine
        }
        byte[] prevSig = new byte[8];
        System.arraycopy(prevBlock.getBytes(), 24, prevSig, 0, 8);
        Block newBlock = Block.createNoSig(newSerialNum, myWalletNum, prevSig);
        return mineCoinAttemptInternal(newBlock, attemptsCount);
    }

    public static Block mineCoinAttemptInternal(Block newBlock, int attemptsCount) {
        Random rand = new Random();
        for (int attempt= 0; attempt < attemptsCount; attempt++) {
            long puzzle = rand.nextLong();
            newBlock.setLongPuzzle(puzzle);
            Block.BlockError result = newBlock.checkSignature();
            if (result != Block.BlockError.SIG_NO_ZEROS) {
                // if enough zeros - we got error because of other reason - e.g. sig field not set yet
                byte[] sig = newBlock.calcSignature();
                newBlock.setSignaturePart(sig);
                // recheck block
                result = newBlock.checkSignature();
                if (result != Block.BlockError.OK) {
                    return null; //failed
                }
                return newBlock;
            }
        }
        return null;
    }
	
	public void run(Block prev) {
		System.out.println("Thread "+id+" running");
		
	}
	
	
}
