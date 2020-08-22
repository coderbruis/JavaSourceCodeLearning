package com.bruis.springsecurity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.List;

/**
 * @author LuoHaiYang
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {

    public static final String AUTHORIZATION = "Authorization";
    public static final String MODELREF = "string";
    public static final String PARAMETERTYPE = "header";
    public static final String TITLE = "SpringSecurity学习";
    public static final String DESCRIPTION = "Learn more about SpringSecurity!";
    public static final String VERSION = "1.0";

    @Bean
    public Docket createRestApi() {
        // 添加请求参数, 把token作为请求头参数
        ParameterBuilder builder = new ParameterBuilder();
        List<Parameter> parameters = new ArrayList<>();

        builder.name(AUTHORIZATION)
                .description("请求令牌")
                .modelRef(new ModelRef(MODELREF))
                .parameterType(PARAMETERTYPE)
                .required(false)
                .build();

        parameters.add(builder.build());

        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build()
                .globalOperationParameters(parameters);
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title(TITLE)
                .description(DESCRIPTION)
                .version(VERSION)
                .build();
    }
}
