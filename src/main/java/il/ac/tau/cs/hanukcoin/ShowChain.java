package il.ac.tau.cs.hanukcoin;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A program to choe current status of HanukCoin netwrk and block-chain
 */
public class ShowChain {
    public static final int BEEF_BEEF = 0xbeefBeef;
    public static final int DEAD_DEAD = 0xdeadDead;
    public static List<NodeInfo> nodeList = new ArrayList<>(); // Global node list for testing
    public static List<Block> blockChain = new ArrayList<>(); // Global blockchain list

    public static void log(String fmt, Object... args) {
        println(fmt, args);
    }
    public static void println(String fmt, Object... args) {
        System.out.format(fmt + "\n", args);
    }


    static class NodeInfo {
        // FRANJI: Discussion - public members - pro/cons. What is POJO
        public String name;
        public String host;
        public int port;
        public int lastSeenTS;
        // TODO(students): add more fields you may need such as number of connection attempts failed
        //  last time connection was attempted, if this node is new ot alive etc.

        public static String readLenStr(DataInputStream dis) throws IOException {
            byte strLen = dis.readByte();
            byte[] strBytes = new byte[strLen];
            dis.readFully(strBytes);
            return new String(strBytes, "utf-8");
        }

        public void writeNode(DataOutputStream dos) throws IOException {
            byte[] nameBytes = name.getBytes("utf-8");
            dos.writeByte(nameBytes.length);
            dos.write(nameBytes);
            byte[] hostBytes = host.getBytes("utf-8");
            dos.writeByte(hostBytes.length);
            dos.write(hostBytes);
            dos.writeShort(port);
            dos.writeInt(lastSeenTS);
        }

        public static NodeInfo readFrom(DataInputStream dis) throws IOException {
            NodeInfo n = new NodeInfo();
            n.name = readLenStr(dis);
            n.host = readLenStr(dis);
            n.port = dis.readShort();
            n.lastSeenTS =dis.readInt();
            // TODO(students): update extra fields
            return n;
        }
    }


    static class ClientConnection {
        private DataInputStream dataInput;
        private DataOutputStream dataOutput;

        public ClientConnection(Socket connectionSocket) {
            try {
                dataInput = new DataInputStream(connectionSocket.getInputStream());
                dataOutput = new DataOutputStream(connectionSocket.getOutputStream());

            } catch (IOException e) {
                throw new RuntimeException("FATAL = cannot create data streams", e);
            }
        }

        public void sendReceive() {
            try {
                sendRequest(1, dataOutput);
                parseMessage(dataInput);

            } catch (IOException e) {
                throw new RuntimeException("send/recieve error", e);
            }
        }

        public void parseMessage(DataInputStream dataInput) throws IOException  {
            int cmd = dataInput.readInt(); // skip command field

            int beefBeef = dataInput.readInt();
            if (beefBeef != BEEF_BEEF) {
                throw new IOException("Bad message no BeefBeef");
            }
            int nodesCount = dataInput.readInt();
            // FRANJI: discussion - create a new list in memory or update global list?
            ArrayList<NodeInfo> receivedNodes =  new ArrayList<>();
            for (int ni = 0; ni < nodesCount; ni++) {
                NodeInfo newInfo = NodeInfo.readFrom(dataInput);
                receivedNodes.add(newInfo);
            }
            int deadDead = dataInput.readInt();
            if (deadDead != DEAD_DEAD) {
                throw new IOException("Bad message no DeadDead");
            }
            int blockCount = dataInput.readInt();
            // FRANJI: discussion - create a new list in memory or update global list?
            ArrayList<Block> receivedBlocks =  new ArrayList<>();
            for (int bi = 0; bi < blockCount; bi++) {
                Block newBlock = Block.readFrom(dataInput);
                receivedBlocks.add(newBlock);
            }
            blockChain = receivedBlocks; // Update the global blockchain
            printMessage(receivedNodes, receivedBlocks);
        }

        private void printMessage(List<NodeInfo> receivedNodes, List<Block> receivedBlocks) {
            println("==== Nodes ====");
            for (NodeInfo ni : receivedNodes) {
                println("%20s\t%s:%s\t%d",ni.name,  ni.host, ni.port, ni.lastSeenTS);
            }
            println("==== Blocks ====");
            for (Block b : receivedBlocks) {
                println("%5d\t0x%08x\t%s", b.getSerialNumber(), b.getWalletNumber(), b.binDump().replace("\n", "  "));
            }
        }

        private void sendRequest(int cmd, DataOutputStream dos) throws IOException {
            dos.writeInt(cmd);
            dos.writeInt(BEEF_BEEF);

            nodeList.clear(); // Clear previous data in the global node list for testing
            // Example node creation to showcase writing functionality
            NodeInfo exampleNode = new NodeInfo();
            exampleNode.name = "DogPool";
            exampleNode.host = "172.30.99.137";
            exampleNode.port = 8080;
            exampleNode.lastSeenTS = (int) (System.currentTimeMillis() / 1000);
            nodeList.add(exampleNode); // Update the global list
            log("INFO - Example node '%s' added to the global node list", exampleNode.name);

            int activeNodes = nodeList.size();
            dos.writeInt(activeNodes); // Write active node count

            for (NodeInfo node : nodeList) {
                node.writeNode(dos); // Write node details to stream
            }

            dos.writeInt(DEAD_DEAD);
            int blockChain_size = blockChain.size();
            dos.writeInt(blockChain_size);

            for (Block block : blockChain) {
                block.writeTo(dos); // Write block details to stream
            }
        }
    }


    public static void sendReceive(String host, int port){
        try {
            log("INFO - Sending request message to %s:%d", host, port);
            Socket soc = new Socket(host, port);
            ClientConnection connection = new ClientConnection(soc);
            connection.sendReceive();
        } catch (IOException e) {
            log("WARN - open socket exception connecting to %s:%d: %s", host, port, e.toString());
        }
    }

    public static void main(String argv[]) {
        if (argv.length != 1 || !argv[0].contains(":")){
            println("ERROR - please provide HOST:PORT");
            return;
        }
        String[] parts = argv[0].split(":");
        String addr = parts[0];
        int port = Integer.parseInt(parts[1]);
        sendReceive(addr, port);
    }


    /**
     * Returns the current blockchain.
     *
     * @return ArrayList of Block objects that represent the current blockchain.
     */
    public static List<Block> getBlockChain() {
        return new ArrayList<>(blockChain); // Return a copy of the blockchain
    }
}

