package net.ys.controller;

import net.ys.bean.DataSource;
import net.ys.constant.GenResult;
import net.ys.constant.X;
import net.ys.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.util.StringUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * RSA加解密
 * Date: 2019-8-30
 * Time: 16:05
 */
@Controller
public class MainController {

    @Value("${code.file.path}")
    private String codeFilePath;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @ResponseBody
    @PostMapping("/testConn")
    public Map<String, Object> testConn(DataSource dataSource) {
        boolean flag;
        if (dataSource.getDbType() == 1) {
            flag = DbUtil.testConnMySql(dataSource);
        } else {
            flag = DbUtil.testConnOracle(dataSource);
        }

        if (!flag) {
            return GenResult.FAILED.genResult();
        }
        return GenResult.SUCCESS.genResult();
    }

    @ResponseBody
    @PostMapping(value = "code")
    public void code(HttpServletResponse response, DataSource dataSource) {
        try {

            boolean flag;
            if (dataSource.getDbType() == 1) {
                flag = DbUtil.testConnMySql(dataSource);
            } else {
                flag = DbUtil.testConnOracle(dataSource);
            }

            if (!flag) {
                return;
            }

            response.setCharacterEncoding(X.ENCODING.U);
            String fileName = genBeanFile(codeFilePath, dataSource);
            if (fileName == null) {
                return;
            }
            String filePath = codeFilePath + "/" + fileName;
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

            InputStream is = new FileInputStream(filePath);
            ServletOutputStream out = response.getOutputStream();
            byte[] bytes = new byte[1024];
            int len;
            while ((len = is.read(bytes)) > 0) {
                out.write(bytes, 0, len);
                out.flush();
            }
            out.close();
            is.close();
        } catch (Exception e) {
            try {
                response.getWriter().write("下载失败");
            } catch (IOException ex) {
            }
        }
    }

    private String genBeanFile(String savePath, DataSource dataSource) {
        String desFileName = String.valueOf(System.currentTimeMillis());
        String desFilePath = savePath + "/" + desFileName;
        new File(desFilePath).mkdirs();
        boolean flag;
        if (dataSource.getDbType() == 1) {
            flag = GenerateTools.generateBeanMysql(dataSource, desFilePath + "/");
        } else {
            flag = GenerateTools.generateBeanOracle(dataSource, desFilePath + "/");
        }

        if (!flag) {
            return null;
        }

        ZIPFileUtil.compressFile(desFilePath + "/", desFilePath + ".zip");
        return desFileName + ".zip";
    }

    @ResponseBody
    @PostMapping("/initTable")
    public Map<String, Object> initTable(DataSource dataSource) {

        boolean flag;
        if (dataSource.getDbType() == 1) {
            flag = DbUtil.testConnMySql(dataSource);
        } else {
            flag = DbUtil.testConnOracle(dataSource);
        }

        if (StringUtils.isEmpty(dataSource.getTableName()) || StringUtils.isEmpty(dataSource.getTableComment())) {
            return GenResult.PARAM_ERROR.genResult();
        }

        if (!flag) {
            return GenResult.CONNECT_ERROR.genResult();
        }

        if (dataSource.getDbType() == 1) {
            flag = InitTable.mysql(dataSource);
        } else {
            flag = InitTable.oracle(dataSource);
        }

        if (!flag) {
            return GenResult.FAILED.genResult();
        }
        return GenResult.SUCCESS.genResult();
    }

    @ResponseBody
    @PostMapping(value = "doc")
    public void doc(HttpServletResponse response, DataSource dataSource) {
        try {

            boolean flag;
            if (dataSource.getDbType() == 1) {
                flag = DbUtil.testConnMySql(dataSource);
            } else {
                flag = DbUtil.testConnOracle(dataSource);
            }

            if (!flag) {
                return;
            }

            response.setCharacterEncoding(X.ENCODING.U);
            String fileName = GenerateDoc.genDoc(dataSource, codeFilePath);
            if (fileName == null) {
                return;
            }
            String filePath = codeFilePath + "/" + fileName;
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

            InputStream is = new FileInputStream(filePath);
            ServletOutputStream out = response.getOutputStream();
            byte[] bytes = new byte[1024];
            int len;
            while ((len = is.read(bytes)) > 0) {
                out.write(bytes, 0, len);
                out.flush();
            }
            out.close();
            is.close();
        } catch (Exception e) {
            try {
                response.getWriter().write("下载失败");
            } catch (IOException ex) {
            }
        }
    }
}
