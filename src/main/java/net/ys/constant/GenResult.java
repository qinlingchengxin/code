package net.ys.constant;

import java.util.LinkedHashMap;
import java.util.Map;

public enum GenResult {
    SUCCESS(1000, "success"),

    FAILED(1001, "failed"),

    CONNECT_ERROR(1002, "connect error"),

    PARAM_ERROR(1003, "param error"),

    UNKNOWN_ERROR(9999, "unknown error");

    public int msgCode;
    public String message;

    GenResult(int msgCode, String message) {
        this.msgCode = msgCode;
        this.message = message;
    }

    public Map<String, Object> genResult() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("code", msgCode);
        map.put("msg", message);
        return map;
    }

    public Map<String, Object> genResult(Object data) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("code", msgCode);
        map.put("msg", message);
        map.put("data", data);
        return map;
    }
}
