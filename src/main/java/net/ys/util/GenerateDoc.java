package net.ys.util;

import net.sf.jxls.transformer.XLSTransformer;
import net.ys.bean.DataSource;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.thymeleaf.util.StringUtils;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: NMY
 * Date: 18-12-25
 */
public class GenerateDoc {

    public static String genDoc(DataSource dataSource, String beanPath) {
        try {
            Map<String, Object> map = new HashMap<>();
            List<Map<String, String>> fields;
            if (dataSource.getDbType() == 1) {
                fields = getAllFieldsMySql(dataSource);
            } else {
                fields = getAllFieldsOracle(dataSource);
            }

            if (fields.size() == 0) {
                return null;
            }
            map.put("records", fields);

            //获取每个表的字段数目，按照表名排序
            List<Integer> tableFieldCount;
            if (dataSource.getDbType() == 1) {
                tableFieldCount = getTableFieldCountMysql(dataSource);
            } else {
                tableFieldCount = getTableFieldCountOracle(dataSource);
            }

            InputStream resourceAsStream = GenerateDoc.class.getClassLoader().getResourceAsStream("doc.xls");
            String resultFileName = "doc-" + System.currentTimeMillis() + ".xls";
            FileOutputStream fos = new FileOutputStream(beanPath + "/" + resultFileName);
            BufferedInputStream is = new BufferedInputStream(resourceAsStream);
            XLSTransformer transformer = new XLSTransformer();
            Workbook wb = transformer.transformXLS(is, map);

            Sheet sheet = wb.getSheetAt(0);
            int firstRow = 1;
            int lastRow;
            for (Integer fc : tableFieldCount) {
                lastRow = firstRow + fc - 1;
                sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, 0, 0));
                firstRow = firstRow + fc;
            }

            wb.write(fos);
            is.close();
            fos.close();
            return resultFileName;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    private static List<Integer> getTableFieldCountMysql(DataSource dataSource) {
        List<Integer> fieldCount = new ArrayList<>();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection("jdbc:mysql://" + dataSource.getIp() + ":" + dataSource.getPort() + "/" + dataSource.getDbName(), dataSource.getUsername(), dataSource.getPassword());
            String sql = "SELECT COUNT(c.COLUMN_NAME) AS c FROM information_schema.`COLUMNS` c, information_schema.`TABLES` t WHERE t.TABLE_SCHEMA = c.TABLE_SCHEMA AND t.TABLE_NAME = c.TABLE_NAME AND t.TABLE_TYPE = 'BASE TABLE' AND c.TABLE_SCHEMA = ? GROUP BY c.TABLE_NAME ORDER BY c.TABLE_NAME";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, dataSource.getDbName());
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                fieldCount.add(rs.getInt("c"));
            }
            rs.close();
            statement.close();
            connection.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return fieldCount;
    }

    private static List<Integer> getTableFieldCountOracle(DataSource dataSource) {
        List<Integer> fieldCount = new ArrayList<>();
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            Connection connection = DriverManager.getConnection("jdbc:oracle:thin:@" + dataSource.getIp() + ":" + dataSource.getPort() + "/" + dataSource.getDbName(), dataSource.getUsername(), dataSource.getPassword());
            Statement statement = connection.createStatement();
            String sql = "SELECT COUNT(UTC.COLUMN_NAME) AS C FROM user_tab_columns UTC, user_tab_comments UTCM WHERE UTC.TABLE_NAME = UTCM.TABLE_NAME AND UTCM.TABLE_TYPE = 'TABLE' GROUP BY UTC.TABLE_NAME ORDER BY UTC.TABLE_NAME";
            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {
                fieldCount.add(rs.getInt("C"));
            }
            rs.close();
            statement.close();
            connection.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return fieldCount;
    }

    public static List<Map<String, String>> getAllFieldsMySql(DataSource dataSource) {
        List<Map<String, String>> fields = new ArrayList<>();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection("jdbc:mysql://" + dataSource.getIp() + ":" + dataSource.getPort() + "/" + dataSource.getDbName(), dataSource.getUsername(), dataSource.getPassword());
            PreparedStatement statement = connection.prepareStatement("SELECT c.TABLE_NAME, t.TABLE_COMMENT, c.COLUMN_NAME, c.COLUMN_TYPE AS DATA_TYPE, c.COLUMN_COMMENT FROM information_schema.`COLUMNS` c, information_schema.`TABLES` t WHERE t.TABLE_SCHEMA = c.TABLE_SCHEMA AND t.TABLE_NAME = c.TABLE_NAME AND t.TABLE_TYPE = 'BASE TABLE' AND c.TABLE_SCHEMA = ? ORDER BY c.TABLE_NAME, c.ORDINAL_POSITION");
            statement.setString(1, dataSource.getDbName());
            ResultSet rs = statement.executeQuery();
            String tableName;
            String tableComment;
            String columnName;
            String columnType;
            String columnComment;
            Map<String, String> map;
            while (rs.next()) {
                tableName = rs.getString("TABLE_NAME");
                tableComment = rs.getString("TABLE_COMMENT") == null ? "" : rs.getString("TABLE_COMMENT");
                columnName = rs.getString("COLUMN_NAME");
                columnType = rs.getString("DATA_TYPE");
                columnComment = rs.getString("COLUMN_COMMENT");
                if (StringUtils.isEmpty(columnComment)) {
                    columnComment = columnName;
                }

                map = new HashMap<>();
                map.put("tableId", tableName + "\n" + tableComment);
                map.put("fieldName", columnName);
                map.put("fieldType", columnType);
                map.put("comment", columnComment);
                fields.add(map);
            }
            rs.close();
            statement.close();
            connection.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return fields;
    }

    public static List<Map<String, String>> getAllFieldsOracle(DataSource dataSource) {
        List<Map<String, String>> fields = new ArrayList<>();
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            Connection connection = DriverManager.getConnection("jdbc:oracle:thin:@" + dataSource.getIp() + ":" + dataSource.getPort() + "/" + dataSource.getDbName(), dataSource.getUsername(), dataSource.getPassword());
            Statement statement = connection.createStatement();
            String sql = "SELECT UTC.TABLE_NAME, UTCC.COMMENTS AS TABLE_COMMENT, UTC.COLUMN_NAME, UCC.COMMENTS AS COLUMN_COMMENT, CASE WHEN UTC.DATA_TYPE = 'NUMBER' THEN 'NUMBER(' || UTC.DATA_PRECISION || ',' || UTC.DATA_SCALE || ')' WHEN UTC.DATA_TYPE = 'TIMESTAMP(0)' THEN 'TIMESTAMP(0)' ELSE UTC.DATA_TYPE || '(' || UTC.CHAR_LENGTH || ')' END DATA_TYPE FROM user_tab_columns UTC LEFT JOIN user_col_comments UCC ON UCC.TABLE_NAME = UTC.TABLE_NAME AND UCC.COLUMN_NAME = UTC.COLUMN_NAME, user_tab_comments UTCC WHERE UTCC.TABLE_NAME = UTC.TABLE_NAME AND UTCC.TABLE_TYPE = 'TABLE' ORDER BY UTC.TABLE_NAME, UTC.COLUMN_ID";
            ResultSet rs = statement.executeQuery(sql);
            String tableName;
            String tableComment;
            String columnName;
            String columnType;
            String columnComment;
            Map<String, String> map;
            while (rs.next()) {
                tableName = rs.getString("TABLE_NAME");
                tableComment = rs.getString("TABLE_COMMENT") == null ? "" : rs.getString("TABLE_COMMENT");
                columnName = rs.getString("COLUMN_NAME");
                columnType = rs.getString("DATA_TYPE");
                columnComment = rs.getString("COLUMN_COMMENT");
                if (StringUtils.isEmpty(columnComment)) {
                    columnComment = columnName;
                }

                map = new HashMap<>();
                map.put("tableId", tableName + "\n" + tableComment);
                map.put("fieldName", columnName);
                map.put("fieldType", columnType);
                map.put("comment", columnComment);
                fields.add(map);
            }
            rs.close();
            statement.close();
            connection.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return fields;
    }
}
