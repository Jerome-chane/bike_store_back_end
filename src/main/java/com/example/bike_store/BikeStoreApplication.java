package com.example.bike_store;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Arrays;

@SpringBootApplication
public class BikeStoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(BikeStoreApplication.class, args);
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	public CommandLineRunner initData(PersonRepository personRepository, BikeRepository bikeRepository) {
		return (args) -> {
		   Person u1 = new Person("Jerome","c","j.com", "admin",passwordEncoder().encode("123"));
			Person u2 = new Person("Bob","s","b.com", "customer",passwordEncoder().encode("123"));
			personRepository.save(u1);personRepository.save(u2);
			Bike b1 = new Bike("Cruiser", "Nice Brand", "Normal", "https://ebrobizi.es/wp-content/uploads/2016/12/Ebrobizi-Ebike-Flebi-SUPRA-2016-02.jpg",10, 6);
			Bike b2 = new Bike("Ultra Deluxe", "Shimano", "Normal", "https://fabricbike.com/123-large_default/fixed-gear-original.jpg",10, 4);
			Bike b3 = new Bike("City Driver", "NCB", "Normal","https://a-bike.eu/wp-content/uploads/2017/07/touring-bike-rental.jpg", 10, 3);
			Bike b4 = new Bike("Mountain killer", "BMC", "Mountain", "https://media.alltricks.com/hd/5c45df5df01e8.jpg",12, 2);
			Bike b5 = new Bike("Trek Deluxe", "Subaru", "Mountain","https://www.bikelec.es/media/catalog/product/cache/1/image/1200x1020/9df78eab33525d08d6e5fb8d27136e95/s/t/stamina-montana-electrica-1000w_1.jpg", 12, 1);
			bikeRepository.save(b1);bikeRepository.save(b2);bikeRepository.save(b3);bikeRepository.save(b4);bikeRepository.save(b5);
		};
	}
}



@Configuration
class WebSecurityConfiguration extends GlobalAuthenticationConfigurerAdapter {
	@Autowired
	PersonRepository personRepository;

	@Override
	public void init(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(inputName-> {
			Person person = personRepository.findByEmail(inputName);
			if (person != null) {
				return new User(person.getEmail(), person.getPassword(),
						AuthorityUtils.createAuthorityList(person.getRole()));
			}
			else {
				throw new UsernameNotFoundException("Unknown person: " + inputName);
			}
		});
	}

}

@Configuration
@EnableWebSecurity
class WebSecurityConfig extends WebSecurityConfigurerAdapter {
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.cors();
		http.authorizeRequests()
				.antMatchers("/api/**").permitAll()
				.antMatchers("/api/signup/**").permitAll()
				.antMatchers("/api/login").permitAll()
				.antMatchers("/h2-console/**").permitAll()
				.antMatchers("/login.html").permitAll()
				.antMatchers("/login.js").permitAll()
//				.antMatchers("/my-rentals").hasAnyAuthority("customer")
//				.antMatchers("/return").hasAnyAuthority("customer")
				.antMatchers("/my-rentals").permitAll()
				.antMatchers("/return/**").permitAll()
				.antMatchers("/rent/**").permitAll()
				.anyRequest()
				.fullyAuthenticated();
		http.formLogin()
				.usernameParameter("email")
				.passwordParameter("pwd")
				.loginPage("/api/login");
		http.logout().logoutUrl("/api/logout");
		// turn off checking for CSRF tokens
		http.csrf().disable();
//		System.out.println("login request received");
		// if user is not authenticated, just send an authentication failure response
		http.exceptionHandling().authenticationEntryPoint((req, res, exc) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED));
		// if login is successful, just clear the flags asking for authentication
		http.formLogin().successHandler((req, res, auth) -> clearAuthenticationAttributes(req));
		// if login fails, just send an authentication failure response
		http.formLogin().failureHandler((req, res, exc) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED));
		// if logout is successful, just send a success response
		http.logout().logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler());
		http.headers().frameOptions().disable();
	}

	private void clearAuthenticationAttributes(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
		}

	}
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		final CorsConfiguration configuration = new CorsConfiguration();
		// The value of the 'Access-Control-Allow-Origin' header in the response must not be the wildcard '*' when the request's credentials mode is 'include'.
		configuration.setAllowedOrigins(Arrays.asList("*"));
		configuration.setAllowedMethods(Arrays.asList("HEAD",
				"GET", "POST", "PUT", "DELETE", "PATCH"));
		// setAllowCredentials(true) is important, otherwise:
		// will fail with 403 Invalid CORS request
		configuration.setAllowCredentials(true);
		// setAllowedHeaders is important! Without it, OPTIONS preflight request
		configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
		final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

}