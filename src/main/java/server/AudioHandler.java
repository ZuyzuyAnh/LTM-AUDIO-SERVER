package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;


public class AudioHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            // Đọc đường dẫn file từ request body
            BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
            String filePath = reader.readLine();

            File audioFile = new File(filePath);
            if (!audioFile.exists()) {
                // Nếu file không tồn tại, trả về lỗi 404
                exchange.sendResponseHeaders(404, 0);
                exchange.close();
                return;
            }

            // Đặt Content-Type là audio/wav
            exchange.getResponseHeaders().set("Content-Type", "audio/wav");
            exchange.sendResponseHeaders(200, audioFile.length());

            // Trả dữ liệu file .wav trực tiếp trong response
            try (OutputStream os = exchange.getResponseBody();
                 FileInputStream fis = new FileInputStream(audioFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
        } else {
            exchange.sendResponseHeaders(405, -1); // Method Not Allowed nếu không phải POST
        }
    }
}
