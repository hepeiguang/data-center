package org.apache.dolphinscheduler.plugin.datasource.sqlserver;

import java.sql.*;

public class SQLServer2008Test {
    public static void main(String[] args) {
        String server = "172.41.133.84";
        String database = "master";
        String user = "sa";
        String password = "admin@123";

        String connectionUrl = String.format(
                "jdbc:sqlserver://%s:1433;" +
                        "databaseName=%s;" +
                        "user=%s;" +
                        "password=%s;" +
                        "encrypt=true;" +
                        "trustServerCertificate=true;" +
                        "sslProtocol=TLSv1;" +
                        "loginTimeout=30;" +
                        "sendTimeAsDatetime=false;" +
                        "sendStringParametersAsUnicode=true;",
                server, database, user, password
        );

        System.out.println("正在连接 SQL Server 2008 R2...");

        try (Connection conn = DriverManager.getConnection(connectionUrl)) {
            System.out.println("✅ 连接成功!");

            // 显示服务器信息
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT @@VERSION, @@SERVERNAME, DB_NAME()")) {
                if (rs.next()) {
                    System.out.println("SQL Server 版本: " + rs.getString(1));
                    System.out.println("服务器名称: " + rs.getString(2));
                    System.out.println("当前数据库: " + rs.getString(3));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ 连接失败: " + e.getMessage());

            // 尝试不使用加密
            try {
                String noEncryptUrl = connectionUrl.replace("encrypt=true;", "encrypt=false;");
                Connection conn2 = DriverManager.getConnection(noEncryptUrl);
                System.out.println("⚠️  不使用加密连接成功（仅限测试）");
                try (Statement stmt = conn2.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT @@VERSION, @@SERVERNAME, DB_NAME()")) {
                    if (rs.next()) {
                        System.out.println("SQL Server 版本: " + rs.getString(1));
                        System.out.println("服务器名称: " + rs.getString(2));
                        System.out.println("当前数据库: " + rs.getString(3));
                    }
                }
                conn2.close();
            } catch (SQLException e2) {
                System.err.println("无加密连接也失败: " + e2.getMessage());
            }
        }
    }
}
