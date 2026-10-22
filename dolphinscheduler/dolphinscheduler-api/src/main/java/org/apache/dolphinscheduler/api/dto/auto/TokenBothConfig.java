package org.apache.dolphinscheduler.api.dto.auto;

import lombok.Data;

@Data
public class TokenBothConfig {
    private String query;        // 如 "__sid"
    private String queryToken;   // 如 "8683e4..."
    private String header;       // 如 "Cookie"
    private String headerToken;  // 完整 header 值
}
