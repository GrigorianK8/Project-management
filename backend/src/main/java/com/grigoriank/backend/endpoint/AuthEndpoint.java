package com.grigoriank.backend.endpoint;

import com.grigoriank.backend.dto.AuthResponseDto;
import com.grigoriank.backend.dto.LoginRequestDto;
import com.grigoriank.backend.entity.User;
import com.grigoriank.backend.repository.UserRepository;
import com.grigoriank.backend.security.CurrentUserDetailsServiceImpl;
import com.grigoriank.backend.util.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthEndpoint {

    private final CurrentUserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponseDto> createUser(@RequestBody User user) throws Exception {
        User existUser = userRepository.findByEmail(user.getEmail());
        if (existUser != null) {
            throw new Exception("email already exist");
        }
        User createUser = new User();
        createUser.setFullname(user.getFullname());
        createUser.setEmail(user.getEmail());
        createUser.setPassword(passwordEncoder.encode(user.getPassword()));
        User saveUser = userRepository.save(createUser);

        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = JwtProvider.generateToken(authentication);

        AuthResponseDto response = new AuthResponseDto();
        response.setMessage("Signup success!");
        response.setJwt(jwt);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/sign-in")
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto loginReqDto) {
        String username = loginReqDto.getEmail();
        String password = loginReqDto.getPassword();

        Authentication authentication = authenticate(username, password);
        String jwt = JwtProvider.generateToken(authentication);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        AuthResponseDto response = new AuthResponseDto();
        response.setMessage("Sugn-in success!");
        response.setJwt(jwt);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private Authentication authenticate(String username, String password) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (userDetails == null) {
            throw new BadCredentialsException("Invalid username!");
        }
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new BadCredentialsException("Invalid password!");
        }

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
