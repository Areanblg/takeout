package com.aearn.takeout.config;

import com.aearn.takeout.common.JacksonObjectMapper;
import com.aearn.takeout.interceptor.AdminLoginInterception;
import com.aearn.takeout.interceptor.LoginInterception;
import com.aearn.takeout.interceptor.UserLoginInterception;
import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;


import java.util.List;


@Slf4j
@Configuration
@EnableSwagger2
@EnableKnife4j
public class WebMvcConfig extends WebMvcConfigurationSupport{

    private String[] ignoreLogin = new String[]{
            "/employee/login",
            "/front/**",
            "/backend/**",
            "/common/**",
            "/user/sendMsg",
            "/user/login",
            "/doc.html",
            "/webjars/**",
            "/swagger-resources",
            "/v2/api-docs"
    };

    /**
     * 管理员登录的URL
     */
    private String[] adminLoginUrl = new String[]{
            "/backend/**",
            "/employee/**"
    };

    /**
     * 用户登录拦截的URL
     */
    private String[] userLoginUrl = new String[]{
            "/front/**"
    };


    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 设置静态资源映射
     * @param registry
     */
    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("开始进行静态资源映射...");
        registry.addResourceHandler("doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
        registry.addResourceHandler("/backend/**").addResourceLocations("classpath:/backend/");
        registry.addResourceHandler("/front/**").addResourceLocations("classpath:/front/");
    }

    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        /**
         * 针对不同的情景，需要有不同的拦截器进行拦截
         * 优先级1： 登录拦截器
         * 优先级2： 用户登录拦截器、管理员登录拦截器
         */
        //登录拦截，优先级最高
        registry.addInterceptor(new LoginInterception(redisTemplate))
                .addPathPatterns("/**")
                .excludePathPatterns(ignoreLogin).order(1);
//        管理员拦截，只拦截后台
        registry.addInterceptor(new AdminLoginInterception())
                .addPathPatterns(adminLoginUrl)
                .excludePathPatterns(ignoreLogin).order(2);
        //用户拦截
        registry.addInterceptor(new UserLoginInterception(redisTemplate))
                .addPathPatterns(userLoginUrl)
                .excludePathPatterns(ignoreLogin).order(2);
    }
    /**
     * 扩展mvc框架的消息转换器
     * @param converters
     */
    @Override
    protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("扩展消息转换器...");
        //创建消息转换器对象
        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
        //设置对象转换器，底层使用Jackson将Java对象转为json
        messageConverter.setObjectMapper(new JacksonObjectMapper());
        //将上面的消息转换器对象追加到mvc框架的转换器集合中
        converters.add(0,messageConverter);
    }

    @Bean
    public Docket createRestApi() {
        // 文档类型
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.aearn.takeout.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("外卖系统")
                .version("1.0")
                .description("接口文档")
                .build();
    }


}
