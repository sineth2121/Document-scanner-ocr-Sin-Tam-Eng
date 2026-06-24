package util;

import javafx.application.Platform;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

public class PhoneCameraClient {
    private static final int UDP_DISCOVERY_PORT = 5555;
    private static final int TCP_STREAM_PORT = 6000;
    private static final int DISCOVERY_TIMEOUT_MS = 5000;
    private static final int MAX_IMAGE_SIZE = 50_000_000;

    private volatile String phoneIp = null;
    private volatile boolean connected = false;
    private Thread discoveryThread;
    private Consumer<Boolean> onConnectionChange;

    public void startDiscovery(Consumer<Boolean> onConnectionChange) {
        this.onConnectionChange = onConnectionChange;
        discoveryThread = new Thread(this::discoveryLoop);
        discoveryThread.setDaemon(true);
        discoveryThread.start();
    }

    private void discoveryLoop() {
        try (DatagramSocket udpSocket = new DatagramSocket(UDP_DISCOVERY_PORT)) {
            udpSocket.setSoTimeout(DISCOVERY_TIMEOUT_MS);
            byte[] buf = new byte[1024];

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    udpSocket.receive(packet);
                    String data = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                    if (data.contains("ANDROID_CAM")) {
                        String newIp = packet.getAddress().getHostAddress();
                        if (!newIp.equals(phoneIp)) {
                            phoneIp = newIp;
                            connected = true;
                            System.out.println("Phone discovered at: " + phoneIp);
                            if (onConnectionChange != null) {
                                Platform.runLater(() -> onConnectionChange.accept(true));
                            }
                        }
                    }
                } catch (SocketTimeoutException e) {
                    if (connected) {
                        System.out.println("Phone connection lost");
                        connected = false;
                        phoneIp = null;
                        if (onConnectionChange != null) {
                            Platform.runLater(() -> onConnectionChange.accept(false));
                        }
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("Phone discovery socket error: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Phone discovery error: " + e.getMessage());
        }
    }

    /**
     * Captures a single photo from the phone.
     * Sends a trigger byte, then reads frames until a non-STREAMING type is found.
     */
    public byte[] captureSingleImage() throws IOException {
        if (phoneIp == null) throw new IOException("Phone not discovered");

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(phoneIp, TCP_STREAM_PORT), 5000);
            socket.setSoTimeout(15000);
            InputStream in = socket.getInputStream();

            // Send trigger byte to request a capture
            try {
                OutputStream out = socket.getOutputStream();
                out.write(0x01);
                out.flush();
                System.out.println("Sent capture trigger to phone");
            } catch (IOException e) {
                System.out.println("Phone does not accept commands, reading raw stream");
            }

            // Try to read a non-STREAMING frame, fall back to first frame
            return readFirstValidFrame(in);
        }
    }

    /**
     * Receives whatever the phone is currently sending (snapshot of preview).
     * Connects and reads the first available frame without triggering a capture.
     */
    public byte[] receiveImage() throws IOException {
        if (phoneIp == null) throw new IOException("Phone not discovered");

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(phoneIp, TCP_STREAM_PORT), 5000);
            socket.setSoTimeout(15000);
            InputStream in = socket.getInputStream();

            System.out.println("Receiving image from phone...");
            return readFirstValidFrame(in);
        }
    }

    private byte[] readFirstValidFrame(InputStream in) throws IOException {
        // Read 8-byte type header
        byte[] typeBuf = readExact(in, 8);
        String mode = new String(typeBuf, "UTF-8").trim();
        System.out.println("Phone frame mode: " + mode);

        // Read 4-byte big-endian size
        byte[] sizeBuf = readExact(in, 4);
        int size = ((sizeBuf[0] & 0xFF) << 24) | ((sizeBuf[1] & 0xFF) << 16) |
                   ((sizeBuf[2] & 0xFF) << 8) | (sizeBuf[3] & 0xFF);

        if (size <= 0 || size > MAX_IMAGE_SIZE) {
            throw new IOException("Invalid image size: " + size);
        }

        System.out.println("Receiving " + size + " bytes (" + mode + ")");
        return readExact(in, size);
    }

    private byte[] readExact(InputStream in, int n) throws IOException {
        byte[] buf = new byte[n];
        int offset = 0;
        while (offset < n) {
            int read = in.read(buf, offset, n - offset);
            if (read == -1) throw new EOFException("Unexpected end of stream from phone");
            offset += read;
        }
        return buf;
    }

    public void stopDiscovery() {
        if (discoveryThread != null) {
            discoveryThread.interrupt();
        }
    }

    public boolean isConnected() { return connected; }
    public String getPhoneIp() { return phoneIp; }
}
