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
@EnableWebSecurity // ????????? Security ????????? ???????????? ???
@EnableGlobalMethodSecurity(securedEnabled = true) // @Secured ??????????????? ?????????
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
        // h2-console ????????? ?????? ?????? (CSRF, FrameOptions ??????)
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
                .formLogin().disable() // ??????????????? ??????
                .authorizeRequests()
                .antMatchers(HttpMethod.OPTIONS).permitAll() // preflight ??????
                .antMatchers("/oauth/**").permitAll(); // /auth/**??? ?????? ????????? ?????? ?????? ?????? ??????(????????? ?????? url)
        // ?????? ????????? ?????? ???????????? ????????? ???????????? ??? ??????, ?????? ????????? ?????? ??????
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
         * UsernamePasswordAuthenticationFilter ????????? FormLoginFilter, JwtFilter ??? ???????????????.
         * FormLoginFilter : ????????? ????????? ???????????????.
         * JwtFilter       : ????????? ????????? JWT ?????? ??? ????????? ???????????????.
         */
        http
                .addFilterBefore(formLoginFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter(), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling();

        http.authorizeRequests()
                // ?????? ?????? ?????? API ????????? login ?????? ??????
                // ??? ??? ?????? ???????????? '??????'
                .anyRequest()
                .permitAll()
                .and()
                // [???????????? ??????]
                .logout()
                // ???????????? ?????? ?????? URL
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

        // ?????? ?????? API ??????
        skipPathList.add("GET,/user/**");
        skipPathList.add("POST,/user/**");
        skipPathList.add("GET,/oauth/**");

        skipPathList.add("GET,/image/**");
        skipPathList.add("GET,/api/main/**");
        skipPathList.add("GET,/");

        skipPathList.add("GET,/user/kakao/**");
        skipPathList.add("POST,/user/kakao/**");

        // ?????? ?????? ?????? (??????????????? ??????)
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
        configuration.addAllowedOrigin("http://localhost:3000"); // local ????????? ???
        configuration.addAllowedOrigin("https://main.d3ezz3muzp1rz5.amplifyapp.com"); // ?????? ???
        configuration.addAllowedOrigin("https://play-seeso.com"); // ?????? ???
        configuration.addAllowedOrigin("https://www.play-seeso.com"); // ?????? ???
        configuration.addAllowedOrigin("https://play-seeso.com:443"); // ?????? ???
        configuration.addAllowedOrigin("https://www.play-seeso.com:443"); // ?????? ???
        configuration.addAllowedOrigin("https://walbu.shop"); // ?????? ???
        configuration.addAllowedOrigin("https://walbu.shop:443"); // ?????? ???
        //configuration.addAllowedOrigin("http://saintrabby.shop.s3-website.ap-northeast-2.amazonaws.com"); // ?????? ???
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

