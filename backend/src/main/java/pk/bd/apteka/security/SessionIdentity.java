package pk.bd.apteka.security;

import java.io.Serializable;
import pk.bd.apteka.dto.TypKonta;

/** The session keeps an account reference, never a password or password hash. */
public record SessionIdentity(TypKonta typKonta, int id) implements Serializable {}
