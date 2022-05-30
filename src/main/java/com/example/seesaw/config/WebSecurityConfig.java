package com.example.seesaw.config;


import com.example.seesaw.security.FilterSkipMatcher;
import com.example.seesaw.security.FormLoginSuccessHandler;
import com.example.seesaw.security.filter.FormLoginFilter;
import com.example.seesaw.security.filter.JwtAuthFilter;
import com.example.seesaw.security.jwt.HeaderTokenExtractor;
import com.example.seesaw.security.provider.FormLoginAuthProvider;
import com.example.seesaw.security.provider.JWTAuthProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity // 스프링 Security 지원을 가능하게 함
@EnableGlobalMethodSecurity(securedEnabled = true) // @Secured 어노테이션 활성화
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private final JWTAuthProvider jwtAuthProvider;
    private final HeaderTokenExtractor headerTokenExtractor;

    public WebSecurityConfig(
            JWTAuthProvider jwtAuthProvider,
            HeaderTokenExtractor headerTokenExtractor
    ) {
        this.jwtAuthProvider = jwtAuthProvider;
        this.headerTokenExtractor = headerTokenExtractor;
    }

    @Bean
    public BCryptPasswordEncoder encodePassword() {
        return new BCryptPasswordEncoder();
    }

    @Override
    public void configure(AuthenticationManagerBuilder auth) {
        auth
                .authenticationProvider(formLoginAuthProvider())
                .authenticationProvider(jwtAuthProvider);
    }

    @Override
    public void configure(WebSecurity web) {
        // h2-console 사용에 대한 허용 (CSRF, FrameOptions 무시)
        web
                .ignoring()
                .antMatchers("/h2-console/**")
                .antMatchers("/oauth/**")
                .antMatchers(
                        "/favicon.ico"
                        ,"/error"
                        ,"/swagger-ui/**"
                        ,"/swagger-resources/**"
                        ,"/v2/api-docs");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.httpBasic().disable()
                .csrf()
                .disable()
                .formLogin().disable() // 소셜로그인 수정
                .authorizeRequests()
                .antMatchers(HttpMethod.OPTIONS).permitAll() // preflight 대응
                .antMatchers("/oauth/**").permitAll(); // /auth/**에 대한 접근을 인증 절차 없이 허용(로그인 관련 url)
        // 특정 권한을 가진 사용자만 접근을 허용해야 할 경우, 하기 항목을 통해 가능
        //.antMatchers("/admin/**").hasAnyRole("ADMIN");


        http
                .cors()
                .and()
                .csrf()
                .disable()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        http.headers().frameOptions().disable();

        /* 1.
         * UsernamePasswordAuthenticationFilter 이전에 FormLoginFilter, JwtFilter 를 등록합니다.
         * FormLoginFilter : 로그인 인증을 실시합니다.
         * JwtFilter       : 서버에 접근시 JWT 확인 후 인증을 실시합니다.
         */
        http
                .addFilterBefore(formLoginFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter(), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling();

        http.authorizeRequests()
                // 회원 관리 처리 API 전부를 login 없이 허용
                // 그 외 어떤 요청이든 '인증'
                .anyRequest()
                .permitAll()
                .and()
                // [로그아웃 기능]
                .logout()
                // 로그아웃 요청 처리 URL
                .logoutUrl("/user/logout")
                .logoutSuccessHandler(new LogoutSuccessHandler() {

                    @Override
                    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                                                Authentication authentication) throws IOException, ServletException {
                        System.out.println("success");
                    }
                })
                .permitAll();
    }

    @Bean
    public FormLoginFilter formLoginFilter() throws Exception {
        FormLoginFilter formLoginFilter = new FormLoginFilter(authenticationManager());
        formLoginFilter.setFilterProcessesUrl("/user/login");
        formLoginFilter.setAuthenticationSuccessHandler(formLoginSuccessHandler());
//        formLoginFilter.setAuthenticationFailureHandler(formLoginFailureHandler());
        formLoginFilter.afterPropertiesSet();
        return formLoginFilter;
    }

    @Bean
    public FormLoginSuccessHandler formLoginSuccessHandler() {
        return new FormLoginSuccessHandler();
    }

//    @Bean
//    public FormLoginFailureHandler formLoginFailureHandler() { return new FormLoginFailureHandler(); }

    @Bean
    public FormLoginAuthProvider formLoginAuthProvider() {
        return new FormLoginAuthProvider(encodePassword());
    }

    private JwtAuthFilter jwtFilter() throws Exception {
        List<String> skipPathList = new ArrayList<>();

        // 회원 관리 API 허용
        skipPathList.add("GET,/user/**");
        skipPathList.add("POST,/user/**");
        skipPathList.add("GET,/oauth/**");

        skipPathList.add("GET,/image/**");
        skipPathList.add("GET,/api/main/**");
        skipPathList.add("GET,/");

        skipPathList.add("GET,/user/kakao/**");
        skipPathList.add("POST,/user/kakao/**");

        // 채팅 관리 허용 (소켓통신을 위해)
        skipPathList.add("GET,/mainchat/**");
        skipPathList.add("GET,/ws-seesaw/**");
        skipPathList.add("POST,/ws-seesaw/**");

        FilterSkipMatcher matcher = new FilterSkipMatcher(
                skipPathList,
                "/**"
        );

        JwtAuthFilter filter = new JwtAuthFilter(
                matcher,
                headerTokenExtractor
        );
        filter.setAuthenticationManager(super.authenticationManagerBean());

        return filter;
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("http://localhost:3000"); // local 테스트 시
        configuration.addAllowedOrigin("https://main.d3ezz3muzp1rz5.amplifyapp.com"); // 배포 시
        configuration.addAllowedOrigin("https://play-seeso.com"); // 배포 시
        configuration.addAllowedOrigin("https://www.play-seeso.com"); // 배포 시
        configuration.addAllowedOrigin("https://play-seeso.com:443"); // 배포 시
        configuration.addAllowedOrigin("https://www.play-seeso.com:443"); // 배포 시
        configuration.addAllowedOrigin("https://walbu.shop"); // 배포 시
        configuration.addAllowedOrigin("https://walbu.shop:443"); // 배포 시
        //configuration.addAllowedOrigin("http://saintrabby.shop.s3-website.ap-northeast-2.amazonaws.com"); // 배포 시
        configuration.addAllowedOriginPattern("*");
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.addExposedHeader("Authorization");
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

