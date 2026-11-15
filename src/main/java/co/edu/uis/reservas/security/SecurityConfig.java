package co.edu.uis.reservas.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/img/**", "/login", "/registro", "/olvide-contrasena", "/restablecer", "/h2-console/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")           // REQ-7, REQ-45, REQ-56
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/inicio", true)
                .permitAll())
            .logout(logout -> logout
                .logoutSuccessUrl("/login?salio")
                .permitAll())
            .exceptionHandling(ex -> ex.accessDeniedPage("/acceso-denegado")) // REQ-7
            .headers(h -> h.frameOptions(f -> f.sameOrigin()))   // solo para la consola H2
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
