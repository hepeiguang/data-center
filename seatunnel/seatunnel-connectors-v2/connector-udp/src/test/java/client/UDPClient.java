package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class UDPClient {
    private static final int BUFFER_SIZE = 1024;

    public static void main(String[] args) {
        // 服务端IP和端口
        String serverHost = "127.0.0.1"; // 替换为你的服务端IP
        int serverPort = 9999;          // 替换为你的服务端端口

        try (DatagramSocket clientSocket = new DatagramSocket();
             Scanner scanner = new Scanner(System.in)) {

            InetAddress serverAddress = InetAddress.getByName(serverHost);
            System.out.println("UDP客户端已启动，输入消息发送到服务端(输入exit退出):");

            while (true) {
                // 读取用户输入
                System.out.print("> ");
                String message = scanner.nextLine();

                // 退出条件
                if ("exit".equalsIgnoreCase(message.trim())) {
                    break;
                }

                // 发送数据
                byte[] sendData = message.getBytes(StandardCharsets.UTF_8);
                DatagramPacket sendPacket = new DatagramPacket(
                        sendData,
                        sendData.length,
                        serverAddress,
                        serverPort
                );
                clientSocket.send(sendPacket);
                System.out.println("已发送: " + message);

                // 接收响应（可选）
                byte[] receiveData = new byte[BUFFER_SIZE];
                DatagramPacket receivePacket = new DatagramPacket(
                        receiveData,
                        receiveData.length
                );
                clientSocket.receive(receivePacket);
                String response = new String(
                        receivePacket.getData(),
                        0,
                        receivePacket.getLength(),
                        StandardCharsets.UTF_8
                );
                System.out.println("收到服务端响应: " + response);
            }
        } catch (IOException e) {
            System.err.println("客户端错误: " + e.getMessage());
        }
        System.out.println("客户端已关闭");
    }
}
