package net.ys.util;

import net.ys.bean.DataSource;

import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class GenerateTools {

    static String oneEnter = "\r\n";
    static String twoEnter = "\r\n\r\n";
    static String oneTabStr = "\t";

    public static boolean generateBeanMysql(DataSource dataSource, String beanPath) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection("jdbc:mysql://" + dataSource.getIp() + ":" + dataSource.getPort() + "/" + dataSource.getDbName(), dataSource.getUsername(), dataSource.getPassword());
            Statement statement = connection.createStatement();

            String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA. TABLES WHERE TABLE_SCHEMA = '" + dataSource.getDbName() + "'";
            ResultSet rs = statement.executeQuery(sql);
            List<String> tables = new ArrayList<String>();
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME").toLowerCase());
            }

            if (tables.size() > 0) {
                sql = "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT FROM information_schema.`COLUMNS` WHERE TABLE_NAME = '%s' AND TABLE_SCHEMA='" + dataSource.getDbName() + "'";
                String columnName;
                String columnClassName;
                String columnComment;
                String fileName;
                FileWriter fileWriter;
                String attributeType;
                for (String table : tables) {
                    fileName = camelFormat(table, true);

                    fileWriter = new FileWriter(beanPath + fileName + ".java");

                    rs = statement.executeQuery("SELECT COUNT(COLUMN_TYPE) AS c FROM information_schema.`COLUMNS` WHERE TABLE_SCHEMA = '" + dataSource.getDbName() + "' AND TABLE_NAME = '" + table + "' AND DATA_TYPE = 'decimal'");
                    if (rs.first()) {
                        if (rs.getInt("c") > 0) {
                            fileWriter.write("import java.math.BigDecimal;" + twoEnter);
                        }
                    }
                    rs = statement.executeQuery("SELECT TABLE_COMMENT FROM information_schema.`TABLES` WHERE TABLE_SCHEMA = '" + dataSource.getDbName() + "' AND TABLE_NAME = '" + table + "';");
                    String tableComment = "";

                    while (rs.next()) {
                        tableComment = rs.getString("TABLE_COMMENT");
                        tableComment = tableComment == null ? "" : tableComment;
                        break;
                    }

                    fileWriter.write("import java.io.Serializable;" + oneEnter);
                    fileWriter.write("/**" + oneEnter);
                    fileWriter.write("* " + tableComment + oneEnter);
                    fileWriter.write("*/" + oneEnter);
                    fileWriter.write("public class " + fileName + " implements Serializable {" + twoEnter);

                    rs = statement.executeQuery(String.format(sql, table));
                    while (rs.next()) {
                        columnName = camelFormat(rs.getString("COLUMN_NAME").toLowerCase(), false);
                        columnClassName = rs.getString("DATA_TYPE");
                        columnComment = rs.getString("COLUMN_COMMENT");
                        columnComment = columnComment == null ? "" : columnComment;

                        if ("int".equals(columnClassName)) {
                            attributeType = "int";
                        } else if ("bigint".equals(columnClassName)) {
                            attributeType = "long";
                        } else if ("decimal".equals(columnClassName)) {
                            attributeType = "BigDecimal";
                        } else {
                            attributeType = "String";
                        }
                        fileWriter.write(oneTabStr + "private " + attributeType + " " + columnName + ";\t//" + columnComment + twoEnter);
                    }

                    fileWriter.write("}");
                    fileWriter.close();
                }
            }

            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return false;
    }

    public static boolean generateBeanOracle(DataSource dataSource, String beanPath) {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            Connection connection = DriverManager.getConnection("jdbc:oracle:thin:@" + dataSource.getIp() + ":" + dataSource.getPort() + "/" + dataSource.getDbName(), dataSource.getUsername(), dataSource.getPassword());
            Statement statement = connection.createStatement();

            String sql = "SELECT TABLE_NAME FROM user_tab_comments";
            ResultSet rs = statement.executeQuery(sql);
            List<String> tables = new ArrayList<>();
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME").toLowerCase());
            }

            if (tables.size() > 0) {
                sql = "SELECT ATC.COLUMN_NAME, ATC.DATA_TYPE, UCC.COMMENTS AS COLUMN_COMMENT FROM all_tab_columns ATC, user_col_comments UCC WHERE UCC.TABLE_NAME = ATC.TABLE_NAME AND UCC.COLUMN_NAME = ATC.COLUMN_NAME AND ATC.TABLE_NAME = '%s' AND ATC.OWNER = '%s'";
                String columnName;
                String columnClassName;
                String columnComment;
                String fileName;
                FileWriter fileWriter;
                String attributeType;
                for (String table : tables) {
                    fileName = camelFormat(table, true);

                    fileWriter = new FileWriter(beanPath + fileName + ".java");

                    rs = statement.executeQuery("SELECT COMMENTS AS TABLE_COMMENT FROM user_tab_comments WHERE TABLE_NAME = '" + table + "'");
                    String tableComment = "";
                    while (rs.next()) {
                        tableComment = rs.getString("TABLE_COMMENT");
                        tableComment = tableComment == null ? "" : tableComment;
                        break;
                    }

                    fileWriter.write("import java.io.Serializable;" + oneEnter);
                    fileWriter.write("/**" + oneEnter);
                    fileWriter.write("* " + tableComment + oneEnter);
                    fileWriter.write("*/" + oneEnter);
                    fileWriter.write("public class " + fileName + " implements Serializable {" + twoEnter);

                    rs = statement.executeQuery(String.format(sql, table, dataSource.getUsername().toUpperCase()));
                    while (rs.next()) {
                        columnName = camelFormat(rs.getString("COLUMN_NAME").toLowerCase(), false);
                        columnClassName = rs.getString("DATA_TYPE").toLowerCase();
                        columnComment = rs.getString("COLUMN_COMMENT");
                        columnComment = columnComment == null ? "" : columnComment;

                        if ("number".equals(columnClassName)) {
                            attributeType = "int";
                        } else {
                            attributeType = "String";
                        }
                        fileWriter.write(oneTabStr + "private " + attributeType + " " + columnName + ";\t//" + columnComment + twoEnter);
                    }

                    fileWriter.write("}");
                    fileWriter.close();
                }
            }

            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return false;
    }

    public static String firstToUpperCase(String str) {
        str = str.toLowerCase();
        String firstLetter = str.charAt(0) + "";
        str = firstLetter.toUpperCase() + str.substring(1);
        return str;
    }

    public static String camelFormat(String resource, boolean isClass) {
        if (resource != null && resource.trim().length() > 0) {
            String[] strings = resource.split("_+");
            if (strings.length > 1) {
                StringBuffer sb = new StringBuffer();
                if (isClass) {
                    sb.append(firstToUpperCase(strings[0]));
                } else {
                    sb.append(strings[0].toLowerCase());
                }
                for (int i = 1; i < strings.length; i++) {
                    sb.append(firstToUpperCase(strings[i]));
                }
                return sb.toString();
            } else {
                if (isClass) {
                    return firstToUpperCase(strings[0]);
                } else {
                    return strings[0].toLowerCase();
                }
            }
        }
        return "";
    }

    public static List<String> genSqlMysql(DataSource dataSource) {
        List<String> list = new ArrayList<>();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection("jdbc:mysql://" + dataSource.getIp() + ":" + dataSource.getPort() + "/" + dataSource.getDbName(), dataSource.getUsername(), dataSource.getPassword());
            Statement statement = connection.createStatement();

            String sql = "SELECT COLUMN_NAME FROM information_schema.`COLUMNS` WHERE TABLE_NAME = '%s' AND TABLE_SCHEMA='%s'";
            String columnName;
            List<String> columns = new ArrayList<>();
            ResultSet rs = statement.executeQuery(String.format(sql, dataSource.getTableName(), dataSource.getDbName()));
            while (rs.next()) {
                columnName = rs.getString("COLUMN_NAME").toLowerCase();
                columns.add(columnName);
            }

            StringBuffer select = new StringBuffer("SELECT ");
            StringBuffer update = new StringBuffer("UPDATE `").append(dataSource.getTableName()).append("` SET ");
            StringBuffer insert = new StringBuffer("INSERT INTO `").append(dataSource.getTableName()).append("` (");
            int size = 0;
            for (String column : columns) {
                select.append("`" + column + "`, ");

                if (!"id".equals(column)) {
                    update.append("`" + column + "` = ?, ");
                    insert.append("`" + column + "`, ");
                    size++;
                }
            }

            select.deleteCharAt(select.length() - 1);
            select.deleteCharAt(select.length() - 1);
            select.append(" FROM `" + dataSource.getTableName() + "` WHERE 1 = 1");
            list.add(select.toString());

            update.deleteCharAt(update.length() - 1);
            update.deleteCharAt(update.length() - 1);
            update.append(" WHERE 1 = 1");
            list.add(update.toString());

            insert.deleteCharAt(insert.length() - 1);
            insert.deleteCharAt(insert.length() - 1);
            insert.append(") VALUES (").append(genMark(size)).append(")");
            list.add(insert.toString());

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return list;
    }

    public static List<String> genSqlOracle(DataSource dataSource) {
        List<String> list = new ArrayList<>();
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            Connection connection = DriverManager.getConnection("jdbc:oracle:thin:@" + dataSource.getIp() + ":" + dataSource.getPort() + "/" + dataSource.getDbName(), dataSource.getUsername(), dataSource.getPassword());
            Statement statement = connection.createStatement();

            String sql = "SELECT COLUMN_NAME FROM all_tab_columns WHERE TABLE_NAME = '%s' AND OWNER = '%s'";
            String columnName;
            List<String> columns = new ArrayList<>();
            ResultSet rs = statement.executeQuery(String.format(sql, dataSource.getTableName().toUpperCase(), dataSource.getUsername().toUpperCase()));
            while (rs.next()) {
                columnName = rs.getString("COLUMN_NAME").toUpperCase();
                columns.add(columnName);
            }

            StringBuffer select = new StringBuffer("SELECT ");
            StringBuffer update = new StringBuffer("UPDATE `").append(dataSource.getTableName()).append("` SET ");
            StringBuffer insert = new StringBuffer("INSERT INTO `").append(dataSource.getTableName()).append("` (");
            int size = 0;
            for (String column : columns) {
                select.append("`" + column + "`, ");

                if (!"id".equals(column)) {
                    update.append("`" + column + "` = ?, ");
                    insert.append("`" + column + "`, ");
                    size++;
                }
            }

            select.deleteCharAt(select.length() - 1);
            select.deleteCharAt(select.length() - 1);
            select.append(" FROM `" + dataSource.getTableName() + "` WHERE 1 = 1");
            list.add(select.toString());

            update.deleteCharAt(update.length() - 1);
            update.deleteCharAt(update.length() - 1);
            update.append(" WHERE 1 = 1");
            list.add(update.toString());

            insert.deleteCharAt(insert.length() - 1);
            insert.deleteCharAt(insert.length() - 1);
            insert.append(") VALUES (").append(genMark(size)).append(")");
            list.add(insert.toString());

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return list;
    }

    public static String genMark(int size) {
        StringBuffer sb = new StringBuffer("?");
        for (int i = 1; i < size; i++) {
            sb.append(",?");
        }
        return sb.toString();
    }
}
