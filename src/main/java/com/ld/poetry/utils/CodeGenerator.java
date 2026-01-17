package com.ld.poetry.utils;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;

import java.util.Scanner;

/**
 * MyBatis-Plus 代码生成器
 * 适配 MyBatis-Plus Generator 3.5.5
 */
public class CodeGenerator {

    public static String scanner(String tip) {
        try (Scanner scanner = new Scanner(System.in)) {
            StringBuilder help = new StringBuilder();
            help.append("请输入" + tip + "：");
            System.out.println(help.toString());
            if (scanner.hasNext()) {
                String ipt = scanner.next();
                if (StringUtils.isNotBlank(ipt)) {
                    return ipt;
                }
            }
        }
        throw new MybatisPlusException("请输入正确的" + tip + "！");
    }

    public static void main(String[] args) {
        String projectPath = System.getProperty("user.dir");
        String[] tables = scanner("表名，多个英文逗号分割").split(",");
        
        FastAutoGenerator.create("jdbc:mysql://onecloud:3306/Ablaze?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai",
                "test", "janx123666land")
            .globalConfig(builder -> {
                builder.author("sara")
                    .outputDir(projectPath + "/src/main/java")
                    .disableOpenDir();
            })
            .packageConfig(builder -> {
                builder.parent("com.ld.poetry")
                    .mapper("dao");
            })
            .strategyConfig(builder -> {
                builder.addInclude(tables)
                    .entityBuilder()
                        .enableLombok()
                        .enableTableFieldAnnotation()
                        .naming(NamingStrategy.underline_to_camel)
                        .columnNaming(NamingStrategy.underline_to_camel)
                        .idType(IdType.AUTO)
                    .controllerBuilder()
                        .enableRestStyle()
                    .serviceBuilder()
                        .formatServiceFileName("%sService")
                    .mapperBuilder()
                        .enableBaseResultMap()
                        .enableBaseColumnList();
            })
            .execute();
    }
}
