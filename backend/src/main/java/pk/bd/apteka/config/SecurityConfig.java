package pk.bd.apteka.config;

import jakarta.servlet.http.Cookie;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.CompositeLogoutHandler;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfLogoutHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecurityContextRepository securityContextRepository() {
    var sessions = new HttpSessionSecurityContextRepository();
    sessions.setDisableUrlRewriting(true);
    return new DelegatingSecurityContextRepository(
        new RequestAttributeSecurityContextRepository(), sessions);
  }

  @Bean
  CsrfTokenRepository csrfTokenRepository() {
    return new HttpSessionCsrfTokenRepository();
  }

  @Bean
  SessionAuthenticationStrategy sessionAuthenticationStrategy(CsrfTokenRepository csrf) {
    return new CompositeSessionAuthenticationStrategy(List.of(
        new ChangeSessionIdAuthenticationStrategy(), new CsrfAuthenticationStrategy(csrf)));
  }

  @Bean
  LogoutHandler sessionLogoutHandler(SecurityContextRepository contexts, CsrfTokenRepository csrf,
      @Value("${server.servlet.session.cookie.name}") String name,
      @Value("${server.servlet.session.cookie.path}") String path,
      @Value("${server.servlet.session.cookie.secure}") boolean secure) {
    var contextLogout = new SecurityContextLogoutHandler();
    contextLogout.setSecurityContextRepository(contexts);
    var cookie = new Cookie(name, "");
    cookie.setPath(path);
    cookie.setMaxAge(0);
    cookie.setHttpOnly(true);
    cookie.setSecure(secure);
    cookie.setAttribute("SameSite", "Lax");
    return new CompositeLogoutHandler(new CsrfLogoutHandler(csrf), contextLogout,
        new CookieClearingLogoutHandler(cookie));
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository contexts,
      CsrfTokenRepository csrf) throws Exception {
    return http
        .cors(cors -> {})
        .csrf(config -> config.csrfTokenRepository(csrf).requireCsrfProtectionMatcher(request ->
            HttpMethod.POST.matches(request.getMethod())
                && ("/auth/login".equals(request.getServletPath())
                    || "/auth/logout".equals(request.getServletPath()))))
        .securityContext(config -> config.securityContextRepository(contexts))
        .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .requestCache(config -> config.requestCache(new NullRequestCache()))
        .authorizeHttpRequests(config -> config
            .requestMatchers(HttpMethod.GET, "/auth/me").authenticated()
            .anyRequest().permitAll())
        .exceptionHandling(config -> config
            .authenticationEntryPoint((request, response, error) -> response.setStatus(401))
            .accessDeniedHandler((request, response, error) -> response.setStatus(403)))
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .build();
  }
}
