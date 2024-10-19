import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class SCN {
    private static final int TIMEOUT = 1000;
    private static final int[] COMMON_PORTS = { 21, 22, 23, 25, 53, 80, 110, 143, 443, 445, 3306, 3389, 8080 };

    public static void main(String[] args) {
        String subnet = "192.168.1.";
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        List<Future<ScanResult>> futures = new ArrayList<>();
        for (int i = 1; i < 255; i++) {
            String host = subnet + i;
            futures.add(executorService.submit(() -> scanHost(host)));
        }

        for (Future<ScanResult> future : futures) {
            try {
                ScanResult result = future.get();
                if (result != null && result.isAlive) {
                    System.out.println("\nHost: " + result.ip + " is alive");
                    if (!result.openPorts.isEmpty()) {
                        System.out.println("Open ports: " + result.openPorts);
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        executorService.shutdown();
    }

    private static ScanResult scanHost(String host) {
        try {
            InetAddress inetAddress = InetAddress.getByName(host);
            if (inetAddress.isReachable(TIMEOUT)) {
                List<Integer> openPorts = scanPorts(host);
                return new ScanResult(host, true, openPorts);
            }
        } catch (Exception e) {
            System.err.println("Error scanning host: " + host);
        }
        return new ScanResult(host, false, new ArrayList<>());
    }

    private static List<Integer> scanPorts(String host) {
        List<Integer> openPorts = new ArrayList<>();

        for (int port : COMMON_PORTS) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), TIMEOUT);
                openPorts.add(port);
            } catch (Exception e) {
                //System.err.println("Port " + port + " on host " + host + " is closed");
            }
        }
        return openPorts;
    }

    private static class ScanResult {
        String ip;
        boolean isAlive;
        List<Integer> openPorts;

        ScanResult(String ip, boolean isAlive, List<Integer> openPorts) {
            this.ip = ip;
            this.isAlive = isAlive;
            this.openPorts = openPorts;
        }
    }
}