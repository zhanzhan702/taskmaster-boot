package cn.edu.mju.task.service;

/**
 * 业务层异常。
 *
 * <p><b>为什么要有它</b>：实验二的 {@code TaskDao} 每个方法都 {@code throws SQLException}，
 * 导致 Servlet 里全是 {@code try/catch(SQLException)} 或再包一层 {@code ServletException}。
 * 一旦引入业务层，就应该在这里把"受检的技术异常"翻译成"运行时的业务异常"，
 * 让上层不必关心底层是 JDBC 还是别的什么 —— 这也是痛点清单第 7 条的解法第一步。
 */
public class ServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceException(String message) {
        super(message);
    }
}
