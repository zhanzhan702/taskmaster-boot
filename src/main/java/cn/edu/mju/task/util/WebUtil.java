package cn.edu.mju.task.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Web 层零碎工具：参数解析 + HTML 转义。
 *
 * <p><b>为什么会有这个类</b>：在没有框架的原生开发里，这两件事都得自己干：
 * <ul>
 *   <li>{@code request.getParameter()} 返回的是 {@code String}，
 *       要转成 int / Byte / Date 全靠手工，且必须处理非法值；</li>
 *   <li>JSP 里没有自动转义，{@code <%= ... %>} 原样输出，
 *       用户输入的标题里带 {@code <script>} 就会执行。</li>
 * </ul>
 * Spring MVC 用 {@code @RequestParam} + 类型转换器解决前者，
 * JSTL 的 {@code <c:out>} 或 Thymeleaf 默认转义解决后者。
 * 这里只能手写 —— 见痛点清单第 2、5 条。
 */
public final class WebUtil {

    private WebUtil() {
        throw new AssertionError("工具类不允许实例化");
    }

    // ==================== 参数解析（全都"宽松容错"）====================

    /** 去掉首尾空白；null 返回 null。 */
    public static String trim(String s) {
        return s == null ? null : s.trim();
    }

    /** 解析 int；缺失或非法时返回默认值。 */
    public static int toInt(String raw, int defaultValue) {
        if (raw == null || raw.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** 解析 Byte；缺失或非法时返回 null。 */
    public static Byte toByteOrNull(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return Byte.valueOf(Byte.parseByte(raw.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 取值范围校验的 Byte 解析；越界返回 null。 */
    public static Byte toByteInRangeOrNull(String raw, int min, int max) {
        Byte v = toByteOrNull(raw);
        if (v == null || v.byteValue() < min || v.byteValue() > max) {
            return null;
        }
        return v;
    }

    /** 解析 yyyy-MM-dd；缺失或非法时返回 null。 */
    public static Date toDateOrNull(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);
        try {
            return sdf.parse(raw.trim());
        } catch (ParseException e) {
            return null;
        }
    }

    // ==================== HTML 转义 ====================

    /**
     * 转义 HTML 特殊字符，防止 XSS。
     *
     * <p>折磨点在于：<b>每个输出用户数据的地方都要手工调用一次</b>，
     * 漏掉任何一处就是一个漏洞，而编译器完全不会提醒你。
     */
    public static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length() + 16);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#39;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
}
