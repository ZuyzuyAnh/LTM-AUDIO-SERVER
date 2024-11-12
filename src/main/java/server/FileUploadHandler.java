package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileUploadHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.startsWith("multipart/form-data")) {
                exchange.sendResponseHeaders(400, 0);  // Bad Request nếu không phải multipart
                return;
            }

            String boundary = "--" + contentType.split("boundary=")[1];
            InputStream inputStream = exchange.getRequestBody();

            String filePath = "";
            String fileName = "";
            boolean isFilePart = false;
            byte[] buffer = new byte[4096];
            int bytesRead;

            // Tạo thư mục để lưu trữ nếu chưa có
            File audioDir = new File("audio");
            if (!audioDir.exists()) {
                audioDir.mkdirs();
            }

            // Đọc dữ liệu nhị phân từ request
            try (BufferedInputStream bis = new BufferedInputStream(inputStream)) {
                // Đọc từng đoạn nhỏ để tìm boundary
                while ((bytesRead = bis.read(buffer)) != -1) {
                    String part = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                    if (part.contains(boundary)) {
                        // Tìm header của phần file
                        if (part.contains("Content-Disposition: form-data; name=\"file\"")) {
                            // Lấy tên file từ Content-Disposition
                            int filenameIndex = part.indexOf("filename=\"");
                            if (filenameIndex != -1) {
                                int filenameStart = filenameIndex + 10;
                                int filenameEnd = part.indexOf("\"", filenameStart);
                                fileName = part.substring(filenameStart, filenameEnd);

                                File outputFile = new File(audioDir, fileName);
                                filePath = outputFile.getAbsolutePath();

                                // Ghi nội dung file sau khi bỏ qua các dòng header
                                try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
                                    int headerEndIndex = part.indexOf("\r\n\r\n") + 4;
                                    fileOutputStream.write(buffer, headerEndIndex, bytesRead - headerEndIndex);

                                    // Ghi các phần tiếp theo vào file
                                    while ((bytesRead = bis.read(buffer)) != -1) {
                                        String currentPart = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                                        if (currentPart.contains(boundary)) {
                                            // Dừng khi gặp boundary mới (kết thúc file)
                                            int boundaryIndex = currentPart.indexOf(boundary);
                                            fileOutputStream.write(buffer, 0, boundaryIndex - 2); // loại bỏ \r\n trước boundary
                                            break;
                                        }
                                        fileOutputStream.write(buffer, 0, bytesRead);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Trả về đường dẫn của file sau khi upload thành công
            String response = filePath;
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
