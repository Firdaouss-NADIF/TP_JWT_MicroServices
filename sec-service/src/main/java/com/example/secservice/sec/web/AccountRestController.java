package com.example.secservice.sec.web;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.example.secservice.sec.entities.AppRole;
import com.example.secservice.sec.entities.AppUser;
import com.example.secservice.sec.service.AccountService;
import lombok.Data;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class AccountRestController {
    private AccountService accountService;

    public AccountRestController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping(path = "/users")
    @PreAuthorize("hasAuthority('USER')")
    public List<AppUser> appUsers(){
       return accountService.listUsers();
    }

    @PostMapping(path = "/users")
    @PreAuthorize("hasAuthority('ADMIN')")
    public AppUser saveUser(@RequestBody AppUser appUser){
        return accountService.addNewUser(appUser);
    }

    @PostMapping(path = "/roles")
    @PreAuthorize("hasAuthority('ADMIN')")
    public AppRole saveRole(@RequestBody AppRole appRole){
        return accountService.addNewRole(appRole);
    }

    @PostMapping(path = "/addRoleToUser")
    @PreAuthorize("hasAuthority('ADMIN')")
    public void addRoleToUser(@RequestBody RoleUserForm roleUserForm){
        accountService.addRoleToUser(roleUserForm.getUserName(),roleUserForm.getRoleName());
    }

    @PostMapping("/refreshToken")
    public Map<String, String> refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            try {
                String jwtRefreshToken = token.substring(7);
                Algorithm algorithm = Algorithm.HMAC256("mySecret1234");
                JWTVerifier jwtVerifier = JWT.require(algorithm).build();
                DecodedJWT decodedJWT = jwtVerifier.verify(jwtRefreshToken);
                String username = decodedJWT.getSubject();
                AppUser appUser = accountService.loadUserByUsername(username);
                String jwtAccessToken = JWT
                        .create()
                        .withSubject(appUser.getUsername())
                        .withExpiresAt(new Date(System.currentTimeMillis() + 2 * 60 * 1000))
                        .withIssuer(request.getRequestURL().toString())
                        .withClaim("roles", appUser.getAppRoles().stream().map(e -> e.getRoleName()).collect(Collectors.toList()))
                        .sign(algorithm);
                Map<String, String> accessToken = new HashMap<>();
                accessToken.put("Access_Token", jwtAccessToken);
                accessToken.put("Refresh_Token", jwtRefreshToken);
                return accessToken;
            } catch (TokenExpiredException e) {
                response.setHeader("Error-Message", e.getMessage());
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
        }
        throw new RuntimeException("Bad Refresh Token");
    }
    @GetMapping(path="/profile")
    @PreAuthorize("hasAuthority('USER')")
    public AppUser profile(Principal principal){
        return accountService.loadUserByUsername(principal.getName());
    }
}

@Data
class RoleUserForm{
    private String userName;
    private String roleName;
}