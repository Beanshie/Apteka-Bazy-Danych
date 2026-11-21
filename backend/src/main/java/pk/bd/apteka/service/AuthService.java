package pk.bd.apteka.service;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pk.bd.apteka.dto.AuthLogin;
import pk.bd.apteka.dto.AuthUser;
import pk.bd.apteka.dto.TypKonta;
import pk.bd.apteka.security.SessionIdentity;

@Service
public class AuthService {
  private final NamedParameterJdbcTemplate jdbc;
  private final PasswordEncoder passwords;
  private final String dummyHash;

  public AuthService(NamedParameterJdbcTemplate jdbc, PasswordEncoder passwords) {
    this.jdbc = jdbc;
    this.passwords = passwords;
    this.dummyHash = passwords.encode("unused-account-password");
  }

  public AuthUser authenticate(AuthLogin value) {
    if (value == null || value.getTypKonta() == null
        || value.getIdentyfikator() == null || value.getIdentyfikator().isBlank()
        || value.getHaslo() == null || value.getHaslo().isEmpty()
        || value.getHaslo().getBytes(StandardCharsets.UTF_8).length > 72
        || value.getIdentyfikator().length() > (value.getTypKonta() == TypKonta.KLIENT ? 100 : 50)) {
      throw new IllegalArgumentException("Niepoprawne dane logowania");
    }
    boolean customer = value.getTypKonta() == TypKonta.KLIENT;
    String sql = customer
        ? "SELECT id_klienta AS id,email AS identyfikator,imie,nazwisko,haslo,"
            + "NULL AS rola,TRUE AS aktywny FROM Klient WHERE email=:identyfikator"
        : "SELECT id_pracownika AS id,login AS identyfikator,imie,nazwisko,haslo,"
            + "rola,aktywny FROM Pracownik WHERE login=:identyfikator";
    List<Credentials> accounts = jdbc.query(sql,
        new MapSqlParameterSource("identyfikator", value.getIdentyfikator()),
        (row, index) -> new Credentials(user(row, value.getTypKonta()),
            row.getString("haslo"), row.getBoolean("aktywny")));
    Credentials account = accounts.isEmpty() ? null : accounts.getFirst();
    boolean matches = passwords.matches(value.getHaslo(), account == null ? dummyHash : account.hash());
    if (account == null || !matches || !account.active()) {
      throw new BadCredentialsException("Niepoprawne dane logowania");
    }
    return account.user();
  }

  public AuthUser currentUser(SessionIdentity identity) {
    boolean customer = identity.typKonta() == TypKonta.KLIENT;
    String sql = customer
        ? "SELECT id_klienta AS id,email AS identyfikator,imie,nazwisko,NULL AS rola"
            + " FROM Klient WHERE id_klienta=:id"
        : "SELECT id_pracownika AS id,login AS identyfikator,imie,nazwisko,rola"
            + " FROM Pracownik WHERE id_pracownika=:id AND aktywny=TRUE";
    List<AuthUser> users = jdbc.query(sql, new MapSqlParameterSource("id", identity.id()),
        (row, index) -> user(row, identity.typKonta()));
    if (users.isEmpty()) throw new BadCredentialsException("Niepoprawne dane logowania");
    return users.getFirst();
  }

  private static AuthUser user(ResultSet row, TypKonta type) throws SQLException {
    return new AuthUser().id(row.getInt("id")).typKonta(type)
        .identyfikator(row.getString("identyfikator")).imie(row.getString("imie"))
        .nazwisko(row.getString("nazwisko")).rola(row.getString("rola"));
  }

  private record Credentials(AuthUser user, String hash, boolean active) {}
}
