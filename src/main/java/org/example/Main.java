package org.example;

import com.sun.net.httpserver.HttpServer;
import server.AudioHandler;
import server.FileUploadHandler;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(8081), 0);
        httpServer.createContext("/upload", new FileUploadHandler());
        httpServer.createContext("/audio", new AudioHandler());
        httpServer.setExecutor(null);
        httpServer.start();

        try (ServerSocket serverSocket = new ServerSocket(8082)) {
            System.out.println("Audio server started, waiting for connection...");
            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    System.out.println("Client connected");
                    handleClient(clientSocket);
                }
            }
        }
    }
    private static void handleClient(Socket clientSocket) throws IOException {
        // Đọc đường dẫn tệp âm thanh từ client
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String filePath = reader.readLine();  // Đọc đường dẫn file từ client

        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.out.println("Invalid file path: " + filePath);
            return;
        }

        // Đọc dữ liệu âm thanh từ tệp
        try (InputStream fileIn = new FileInputStream(file)) {
            // Gửi dữ liệu âm thanh cho client
            OutputStream out = clientSocket.getOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);  // Gửi dữ liệu âm thanh ra client
            }
            out.flush();  // Đảm bảo gửi hết dữ liệu
        } catch (IOException e) {
            System.out.println("Error reading or sending audio file: " + e.getMessage());
        }
    }
}