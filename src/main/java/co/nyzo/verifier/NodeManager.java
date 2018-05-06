package co.nyzo.verifier;

import co.nyzo.verifier.messages.NodeJoinMessage;
import co.nyzo.verifier.messages.BootstrapRequest;
import co.nyzo.verifier.messages.BootstrapResponse;
import co.nyzo.verifier.util.IpUtil;
import co.nyzo.verifier.util.PrintUtil;
import co.nyzo.verifier.util.UpdateUtil;

import java.nio.ByteBuffer;
import java.util.*;

public class NodeManager {

    // TODO: work out the details of multiple verifiers at a single IP address

    private static final Map<ByteBuffer, Node> ipAddressToNodeMap = new HashMap<>();

    private static final int consecutiveFailuresBeforeRemoval = 8;
    private static final Map<ByteBuffer, Integer> ipAddressToFailureCountMap = new HashMap<>();

    public static void updateNode(byte[] identifier, byte[] ipAddress, int port, boolean fullNode) {

        updateNode(identifier, ipAddress, port, fullNode, 0);
    }

    public static synchronized void updateNode(byte[] identifier, byte[] ipAddress, int port, boolean fullNode,
                                               long queueTimestamp) {

        System.out.println("adding node " + PrintUtil.compactPrintByteArray(identifier) + ", " +
                IpUtil.addressAsString(ipAddress));

        if (identifier != null && identifier.length == FieldByteSize.identifier && ipAddress != null &&
                ipAddress.length == FieldByteSize.ipAddress) {

            ByteBuffer ipAddressBuffer = ByteBuffer.wrap(ipAddress);
            Node node = new Node(identifier, ipAddress, port, fullNode);
            ipAddressToNodeMap.put(ipAddressBuffer, node);
        }
    }

    public static synchronized List<Node> getMesh() {
        return new ArrayList<>(ipAddressToNodeMap.values());
    }

    public static boolean connectedToMesh() {

        // When we request the node list from another node, it will add this node to the list. So, the minimum number
        // of nodes in a proper mesh is two.
        return ipAddressToNodeMap.size() > 1;
    }

    public static byte[] identifierForIpAddress(String addressString) {

        byte[] identifier = null;
        byte[] address = IpUtil.addressFromString(addressString);
        if (address != null) {
            ByteBuffer addressBuffer = ByteBuffer.wrap(address);
            Node node = ipAddressToNodeMap.get(addressBuffer);
            if (node != null) {
                identifier = node.getIdentifier();
            }
        }

        return identifier;
    }

    public static void markFailedConnection(String addressString) {

        byte[] address = IpUtil.addressFromString(addressString);
        if (address != null) {
            ByteBuffer addressBuffer = ByteBuffer.wrap(address);
            Integer count = ipAddressToFailureCountMap.get(addressBuffer);
            if (count == null) {
                count = 1;
            } else {
                count++;
            }

            if (count < consecutiveFailuresBeforeRemoval) {
                ipAddressToFailureCountMap.put(addressBuffer, count);
            } else {
                ipAddressToFailureCountMap.remove(addressBuffer);
                removeNodeFromMesh(addressBuffer);
            }
        }
    }

    public static void markSuccessfulConnection(String addressString) {

        byte[] address = IpUtil.addressFromString(addressString);
        if (address != null) {
            ByteBuffer addressBuffer = ByteBuffer.wrap(address);
            ipAddressToFailureCountMap.remove(addressBuffer);
        }
    }

    private static synchronized void removeNodeFromMesh(ByteBuffer addressBuffer) {

        ipAddressToNodeMap.remove(addressBuffer);
    }
}
