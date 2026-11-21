package pk.bd.apteka.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import pk.bd.apteka.api.AuthApi;
import pk.bd.apteka.dto.AuthCsrf;
import pk.bd.apteka.dto.AuthLogin;
import pk.bd.apteka.dto.AuthUser;
import pk.bd.apteka.dto.TypKonta;
import pk.bd.apteka.security.SessionIdentity;
import pk.bd.apteka.service.AuthService;

@RestController
public class AuthController implements AuthApi {
  private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);
  private final AuthService service;
  private final HttpServletRequest request;
  private final HttpServletResponse response;
  private final SecurityContextRepository contexts;
  private final SessionAuthenticationStrategy sessions;
  private final LogoutHandler logout;

  public AuthController(AuthService service, HttpServletRequest request, HttpServletResponse response,
      SecurityContextRepository contexts, SessionAuthenticationStrategy sessions, LogoutHandler logout) {
    this.service = service;
    this.request = request;
    this.response = response;
    this.contexts = contexts;
    this.sessions = sessions;
    this.logout = logout;
  }

  @Override
  public ResponseEntity<AuthCsrf> getAuthCsrf() {
    var token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .body(new AuthCsrf().token(token.getToken()).headerName(token.getHeaderName()));
  }

  @Override
  public ResponseEntity<AuthUser> loginAuth(String xCSRFToken, AuthLogin value) {
    try {
      var user = service.authenticate(value);
      String role = user.getTypKonta() == TypKonta.KLIENT ? "KLIENT" : user.getRola();
      var authentication = UsernamePasswordAuthenticationToken.authenticated(
          new SessionIdentity(user.getTypKonta(), user.getId()), null,
          List.of(new SimpleGrantedAuthority("ROLE_" + role)));
      sessions.onAuthentication(authentication, request, response);
      var context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(authentication);
      SecurityContextHolder.setContext(context);
      contexts.saveContext(context, request, response);
      return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(user);
    } catch (BadCredentialsException error) {
      return ResponseEntity.status(401).build();
    } catch (IllegalArgumentException error) {
      return ResponseEntity.badRequest().build();
    } catch (DataAccessException error) {
      LOGGER.error("Authentication database operation failed", error);
      return ResponseEntity.internalServerError().build();
    }
  }

  @Override
  public ResponseEntity<AuthUser> getAuthMe() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof SessionIdentity identity)) {
      return ResponseEntity.status(401).build();
    }
    try {
      return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.currentUser(identity));
    } catch (BadCredentialsException error) {
      logout.logout(request, response, authentication);
      return ResponseEntity.status(401).build();
    } catch (DataAccessException error) {
      LOGGER.error("Authentication database operation failed", error);
      return ResponseEntity.internalServerError().build();
    }
  }

  @Override
  public ResponseEntity<Void> logoutAuth(String xCSRFToken) {
    logout.logout(request, response, SecurityContextHolder.getContext().getAuthentication());
    return ResponseEntity.noContent().build();
  }

  // Validation exceptions can include rejected passwords: do not log their contents.
  @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
      HandlerMethodValidationException.class, ConstraintViolationException.class})
  public ResponseEntity<Void> invalidRequest() {
    return ResponseEntity.badRequest().build();
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<Void> missingCsrfHeader() {
    // A token in a query/form parameter does not replace the documented header.
    return ResponseEntity.status(403).build();
  }
}
