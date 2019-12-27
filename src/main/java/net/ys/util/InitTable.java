package net.ys.util;

import net.ys.bean.DataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class InitTable {

    public static boolean mysql(DataSource dataSource) {
        try {
            String url = "jdbc:mysql://" + dataSource.getIp() + ":" + dataSource.getPort() + "/" + dataSource.getDbName();
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection(url, dataSource.getUsername(), dataSource.getPassword());
            Statement statement = connection.createStatement();

            String sqlTemp = "CREATE TABLE IF NOT EXISTS `%s` (" +
                    "`id` BIGINT (20) NOT NULL AUTO_INCREMENT COMMENT '主键'," +
                    "`create_time` BIGINT (20) NOT NULL DEFAULT 0 COMMENT '创建时间(13位Long类型时间戳)'," +
                    "`update_time` BIGINT (20) NOT NULL DEFAULT 0 COMMENT '修改时间(13位Long类型时间戳)'," +
                    "`is_deleted` INT (1) NOT NULL DEFAULT 1 COMMENT '是否删除：0-未删除/1-已删除'," +
                    "`sys_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP COMMENT '系统变更时间'," +
                    "PRIMARY KEY (`id`)" +
                    ") COMMENT = '%s';";

            String sql = String.format(sqlTemp, dataSource.getTableName().toLowerCase(), dataSource.getTableComment());
            statement.execute(sql);
            statement.close();
            connection.close();
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return false;
    }

    public static boolean oracle(DataSource dataSource) {
        try {
            String url = "jdbc:oracle:thin:@" + dataSource.getIp() + ":" + dataSource.getPort() + "/" + dataSource.getDbName();
            Class.forName("oracle.jdbc.driver.OracleDriver");

            Connection connection = DriverManager.getConnection(url, dataSource.getUsername(), dataSource.getPassword());
            Statement statement = connection.createStatement();
            String tableName = dataSource.getTableName().toUpperCase();
            statement.execute("CREATE TABLE \"" + tableName + "\" (\"ID\" VARCHAR2 (50) DEFAULT '' NOT NULL,\"CREATE_TIME\" NUMBER (20) DEFAULT 0 NOT NULL,\"UPDATE_TIME\" NUMBER (20) DEFAULT 0 NOT NULL,\"IS_DELETED\" NUMBER DEFAULT 1 NOT NULL,PRIMARY KEY (\"ID\"))");
            statement.execute("COMMENT ON TABLE \"" + tableName + "\" IS '" + dataSource.getTableComment() + "'");
            statement.execute("COMMENT ON COLUMN \"" + tableName + "\".\"ID\" IS '主键'");
            statement.execute("COMMENT ON COLUMN \"" + tableName + "\".\"CREATE_TIME\" IS '创建时间(13位Long类型时间戳)'");
            statement.execute("COMMENT ON COLUMN \"" + tableName + "\".\"UPDATE_TIME\" IS '修改时间(13位Long类型时间戳)'");
            statement.execute("COMMENT ON COLUMN \"" + tableName + "\".\"IS_DELETED\" IS '是否删除：0-未删除/1-已删除'");
            statement.close();
            connection.close();

            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return false;
    }
}
