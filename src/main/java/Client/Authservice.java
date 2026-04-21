package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Authservice {
    public static boolean RqLogin(String name, String password) throws IOException {
        URL url = new URL();  // to do: lấy url gui cho phan back end
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String jsonInput = "{\"username\":\"" + name + "\", \"password\":\"" + password + "\"}";

        OutputStream os = conn.getOutputStream();
        os.write(jsonInput.getBytes());
        os.flush();
        os.close();

        boolean success = readLoginResult(conn);

        if (success) {
            System.out.println("Đăng nhập thành công, chuyển sang trang chính");
        } else {
            System.out.println("Sai tài khoản hoặc mật khẩu");
        }
        return success;
    }


    // ham đọc phản hồi
    public static boolean readLoginResult(HttpURLConnection conn) throws IOException {
        int responseCode = conn.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            // Đọc dữ liệu phản hồi từ server
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // Server trả về "true" hoặc "false"
            String result = response.toString().trim();
            return "true".equalsIgnoreCase(result);
        } else {
            return false;
        }
    }
}
