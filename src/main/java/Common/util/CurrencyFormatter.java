package Common.util;

import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyFormatter {

    /**
     * Chuyển đổi số tiền thành chuỗi định dạng VNĐ
     * Ví dụ: 3200000 -> "3.200.000 ₫"
     */
    public static String formatVND(long amount) {
        Locale vietnam = new Locale("vi", "VN");
        NumberFormat format = NumberFormat.getCurrencyInstance(vietnam);
        return format.format(amount);
    }
}