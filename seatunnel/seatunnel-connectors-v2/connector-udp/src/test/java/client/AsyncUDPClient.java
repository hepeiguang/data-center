package client;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncUDPClient {
    private static final ExecutorService executor = Executors.newFixedThreadPool(5);
    private static final boolean enableEncodeHex = true;
    public static void main(String[] args) throws UnknownHostException {
        String serverHost = "192.168.0.201";
        int serverPort = 9999;
        InetAddress serverAddress = InetAddress.getByName(serverHost);

        try (DatagramSocket clientSocket = new DatagramSocket();
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("异步UDP客户端已启动(输入exit退出):");

            while (true) {
                System.out.print("> ");
                String message = scanner.nextLine();

                if ("exit".equalsIgnoreCase(message.trim())) {
                    executor.shutdown();
                    break;
                }

                // 使用线程池异步发送
                executor.execute(() -> {
                    try {
                        String msg = message;
                        if (enableEncodeHex) {
                            msg = stringToHex(msg);
                        }
                        byte[] sendData = msg.getBytes(StandardCharsets.UTF_8);
                        DatagramPacket packet = new DatagramPacket(
                                sendData, sendData.length, serverAddress, serverPort);
                        clientSocket.send(packet);
                        System.out.println("[" + Thread.currentThread().getName() + "] 已发送: " + msg);
                    } catch (IOException e) {
                        System.err.println("发送失败: " + e.getMessage());
                    }
                });
            }
        } catch (IOException e) {
            System.err.println("客户端错误: " + e.getMessage());
        } finally {
            executor.shutdown();
        }
    }

    public static String stringToHex(String input) {
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02X", b));
        }
        return hexString.toString();
    }
}
