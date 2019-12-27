package net.ys.bean;

import lombok.Data;

/**
 * User: NMY
 * Date: 2019-12-27
 * Time: 14:23
 */
@Data
public class DataSource {

    private int dbType;

    private String ip;

    private int port;

    private String dbName;

    private String username;

    private String password;

    private String tableName;

    private String tableComment;


}
