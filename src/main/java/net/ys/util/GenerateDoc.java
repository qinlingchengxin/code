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
            String sql = "SELECT COUNT(COLUMN_NAME) AS c FROM information_schema.`COLUMNS` WHERE TABLE_SCHEMA = '" + dataSource.getDbName() + "' GROUP BY TABLE_NAME ORDER BY TABLE_NAME";
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
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
            String sql = "SELECT COUNT(ATC.COLUMN_NAME) AS C FROM all_tab_columns ATC WHERE ATC. OWNER = '" + dataSource.getUsername().toUpperCase() + "' GROUP BY ATC.TABLE_NAME ORDER BY ATC.TABLE_NAME";
            ResultSet rs = statement.executeQuery(sql);
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

    public static List<Map<String, String>> getAllFieldsMySql(DataSource dataSource) {
        List<Map<String, String>> fields = new ArrayList<>();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection("jdbc:mysql://" + dataSource.getIp() + ":" + dataSource.getPort() + "/" + dataSource.getDbName(), dataSource.getUsername(), dataSource.getPassword());
            PreparedStatement statement = connection.prepareStatement("SELECT c.TABLE_NAME, t.TABLE_COMMENT, c.COLUMN_NAME, c.COLUMN_TYPE AS DATA_TYPE, c.COLUMN_COMMENT FROM information_schema.`COLUMNS` c LEFT JOIN information_schema.`TABLES` t ON t.TABLE_SCHEMA = c.TABLE_SCHEMA AND t.TABLE_NAME = c.TABLE_NAME WHERE c.TABLE_SCHEMA = ? AND t.TABLE_TYPE = 'BASE TABLE' ORDER BY c.TABLE_NAME, c.ORDINAL_POSITION");
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
            String sql = "SELECT ATC.TABLE_NAME, ATCC.COMMENTS AS TABLE_COMMENT, ATC.COLUMN_NAME, UCC.COMMENTS AS COLUMN_COMMENT, CASE WHEN ATC.DATA_TYPE = 'NUMBER' THEN 'NUMBER(' || ATC.DATA_PRECISION || ',' || ATC.DATA_SCALE || ')' WHEN ATC.DATA_TYPE = 'TIMESTAMP(0)' THEN 'TIMESTAMP(0)' ELSE ATC.DATA_TYPE || '(' || ATC.CHAR_LENGTH || ')' END DATA_TYPE FROM all_tab_columns ATC LEFT JOIN user_col_comments UCC ON UCC.TABLE_NAME = ATC.TABLE_NAME AND UCC.COLUMN_NAME = ATC.COLUMN_NAME LEFT JOIN all_tab_comments ATCC ON ATCC.TABLE_NAME = ATC.TABLE_NAME AND ATCC. OWNER = ATC. OWNER WHERE ATC. OWNER = '" + dataSource.getUsername().toUpperCase() + "' ORDER BY ATC.TABLE_NAME, ATC.COLUMN_ID";
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
