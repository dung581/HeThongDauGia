package Common.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class DateTimeUtil {

    /**
     * Hiển thị thời gian tương đối (Facebook style)
     * Ví dụ: "Vừa xong", "5 phút trước", "2 giờ trước"
     */
    public static String formatRelative(LocalDateTime pastTime) {
        if (pastTime == null) return "";

        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(pastTime, now);

        if (minutes < 1) return "Vừa xong";
        if (minutes < 60) return minutes + " phút trước";

        long hours = ChronoUnit.HOURS.between(pastTime, now);
        if (hours < 24) return hours + " giờ trước";

        long days = ChronoUnit.DAYS.between(pastTime, now);
        return days + " ngày trước";
    }

    /**
     * Định dạng số giây còn lại thành đồng hồ đếm ngược
     * Ví dụ: 125 -> "02:05"
     */
    public static String formatClock(long secondsRemaining) {
        if (secondsRemaining <= 0) return "00:00";

        long minutes = secondsRemaining / 60;
        long seconds = secondsRemaining % 60;

        // %02d đảm bảo luôn có 2 chữ số (ví dụ: 5 -> 05)
        return String.format("%02d:%02d", minutes, seconds);
    }
}
