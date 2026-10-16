package com.naresh.shop;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
class Security {
 @Bean UserDetailsService users(@Value("${shop.username}") String username,@Value("${shop.password}") String password) {
  if (password.length()<16 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new IllegalArgumentException("SHOP_PASSWORD must have at least 16 characters and at most 72 UTF-8 bytes");
  return new InMemoryUserDetailsManager(User.withUsername(username).password(new BCryptPasswordEncoder().encode(password)).roles("OWNER").build());
 }
 @Bean BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
 @Bean SecurityFilterChain chain(HttpSecurity http) throws Exception {
  return http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .authorizeHttpRequests(a->a.requestMatchers("/api/health").permitAll().requestMatchers("/api/**").authenticated().anyRequest().permitAll())
   .httpBasic(b->b.authenticationEntryPoint((req,res,e)->{res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"message\":\"Please sign in with valid shop credentials\"}");}))
   .headers(h->h.contentSecurityPolicy(c->c.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'")))
   .build();
 }
}
