package com.college.placement.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.*;
import java.util.Date;


@Service
public class JwtService {
	
	private final SecretKey key;
	private final long expiration;

	public JwtService(@Value("${placement.jwt.secret}") String secret,
			@Value("${placement.jwt.expiration-ms}") long expiration) {
		byte[] decoded;
		try {
			decoded = Base64.getDecoder().decode(secret);
		} catch (IllegalArgumentException ex) {
			throw new IllegalStateException("JWT_SECRET must be Base64 encoded", ex);
		}
		if (decoded.length < 32)
			throw new IllegalStateException("JWT_SECRET must decode to at least 32 bytes");
		this.key = Keys.hmacShaKeyFor(decoded);
		this.expiration = expiration;
	}

	public String generate(String email, String role) {
		Date now = new Date();
		return Jwts.builder().subject(email).claim("role", role).issuedAt(now)
				.expiration(new Date(now.getTime() + expiration)).signWith(key).compact();
	}

	public String extractEmail(String token) {
		return parse(token).getPayload().getSubject();
	}

	public boolean isValid(String token, String email) {
		try {
			return email.equals(extractEmail(token));
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}

	private Jws<Claims> parse(String token) {
		return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
	}
}