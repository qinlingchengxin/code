package net.ys.util;

import net.sf.jxls.transformer.XLSTransformer;
import net.ys.bean.DataSource;
import org.apache.poi.ss.usermodel.Workbook;
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
            InputStream resourceAsStream = GenerateDoc.class.getClassLoader().getResourceAsStream("doc.xls");
            String resultFileName = "doc-" + System.currentTimeMillis() + ".xls";
            FileOutputStream fos = new FileOutputStream(beanPath + "/" + resultFileName);
            BufferedInputStream is = new BufferedInputStream(resourceAsStream);
            XLSTransformer transformer = new XLSTransformer();
            Workbook wb = transformer.transformXLS(is, map);
            wb.write(fos);
            is.close();
            fos.close();
            return resultFileName;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public static List<Map<String, String>> getAllFieldsMySql(DataSource dataSource) {
        List<Map<String, String>> fields = new ArrayList<>();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection("jdbc:mysql://" + dataSource.getIp() + ":" + dataSource.getPort() + "/" + dataSource.getDbName(), dataSource.getUsername(), dataSource.getPassword());
            PreparedStatement statement = connection.prepareStatement("SELECT c.TABLE_NAME, t.TABLE_COMMENT, c.COLUMN_NAME, c.DATA_TYPE, c.COLUMN_COMMENT FROM information_schema.`COLUMNS` c LEFT JOIN information_schema.`TABLES` t ON t.TABLE_SCHEMA = c.TABLE_SCHEMA AND t.TABLE_NAME = c.TABLE_NAME WHERE c.TABLE_SCHEMA = ? AND t.TABLE_TYPE = 'BASE TABLE' ORDER BY c.TABLE_NAME, c.COLUMN_NAME");
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
            String sql = "SELECT ATC.TABLE_NAME, ATCC.COMMENTS AS TABLE_COMMENT, ATC.COLUMN_NAME, UCC.COMMENTS AS COLUMN_COMMENT, ATC.DATA_TYPE FROM all_tab_columns ATC LEFT JOIN user_col_comments UCC ON UCC.TABLE_NAME = ATC.TABLE_NAME AND UCC.COLUMN_NAME = ATC.COLUMN_NAME LEFT JOIN all_tab_comments ATCC ON ATCC.TABLE_NAME = ATC.TABLE_NAME AND ATCC.OWNER = ATC.OWNER WHERE ATC.OWNER = '" + dataSource.getUsername().toUpperCase() + "'";
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
