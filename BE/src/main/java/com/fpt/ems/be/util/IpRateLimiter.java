package com.fpt.ems.be.util;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bộ giới hạn tần suất theo địa chỉ IP (Sliding Window In-Memory Rate Limiter).
 * Tự động làm sạch các bản ghi cũ quá cửa sổ trượt để tránh rò rỉ bộ nhớ.
 */
@Component
public class IpRateLimiter {

    private final Map<String, Deque<Instant>> ipRequestsMap = new ConcurrentHashMap<>();
    private static final long WINDOW_SECONDS = 3600; // 1 hour

    /**
     * Kiểm tra và ghi nhận 1 lượt request từ client IP.
     *
     * @param ip            địa chỉ IP của client
     * @param maxPerWindow  số lượt tối đa cho phép trong 1 cửa sổ thời gian
     * @return true nếu request được chấp thuận, false nếu đã vượt ngưỡng
     */
    public synchronized boolean tryAcquire(String ip, int maxPerWindow) {
        if (ip == null || ip.isBlank()) {
            ip = "unknown";
        }

        Instant now = Instant.now();
        Instant cutoff = now.minusSeconds(WINDOW_SECONDS);

        Deque<Instant> timestamps = ipRequestsMap.computeIfAbsent(ip, k -> new ArrayDeque<>());

        // Loại bỏ các request đã quá cửa sổ trượt 1 giờ
        while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= maxPerWindow) {
            return false;
        }

        timestamps.addLast(now);
        return true;
    }

    /**
     * Xóa lịch sử request của một IP (hữu ích cho testing).
     */
    public synchronized void reset(String ip) {
        if (ip != null) {
            ipRequestsMap.remove(ip);
        }
    }

    /**
     * Xóa toàn bộ lịch sử (hữu ích cho testing).
     */
    public synchronized void clearAll() {
        ipRequestsMap.clear();
    }
}
