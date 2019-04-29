package net.uncrash.authorization.basic;

import lombok.extern.slf4j.Slf4j;
import net.uncrash.authorization.*;
import net.uncrash.authorization.api.web.Authentication;
import net.uncrash.authorization.api.web.GeneratedToken;
import net.uncrash.authorization.api.web.ParsedToken;
import net.uncrash.authorization.api.web.TokenParser;
import net.uncrash.authorization.basic.define.AuthorizationConst;
import net.uncrash.authorization.basic.domain.DefaultRole;
import net.uncrash.authorization.basic.domain.DefaultUser;
import net.uncrash.authorization.basic.domain.DefaultPermission;
import net.uncrash.authorization.basic.domain.PermissionRaw;
import net.uncrash.authorization.basic.jwt.JwtConfig;
import net.uncrash.authorization.basic.jwt.JwtTokenGenerator;
import net.uncrash.authorization.basic.jwt.JwtTokenParser;
import net.uncrash.authorization.exception.UnAuthorizedException;
import net.uncrash.core.utils.JSONUtil;
import net.uncrash.core.utils.WebUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author Sendya
 */
@Slf4j
@Service
public class DefaultAuthenticationSupplier implements AuthenticationSupplier, ApplicationContextAware {

    private static ApplicationContext applicationContext;
    private static StringRedisTemplate redisTemplate;
    private static JwtConfig jwtConfig;
    private static TokenParser parser;
    private static JwtTokenGenerator generator;

    @Override
    public Authentication get(String userId) {
        return null;
    }

    @Override
    public GeneratedToken set(String userId, Authentication authentication) {
        GeneratedToken generatedToken = generator.generate(authentication);
        final String userRedisKey = AuthorizationConst.joiner(generatedToken.getToken());
        log.info("Authentication RedisKey: {}", userRedisKey);

        redisTemplate.opsForValue().set(userRedisKey,
            JSONUtil.toJSON(authentication.getUser()),
        generatedToken.getTimeout(), TimeUnit.SECONDS);
        return generatedToken;
    }

    @Override
    public Authentication get() {
        if (WebUtil.getHttpServletRequest() == null) {
            return null;
        }
        ParsedToken token = parser.parseToken(WebUtil.getHttpServletRequest());

        log.info("Authentication Token: {}", JSONUtil.toJSON(token));
        if (Objects.isNull(token)) {
            // throw new UnAuthorizedException("Required Login", 401);
            return null;
        }

        final String userRedisKey = AuthorizationConst.joiner(token.getToken());
        log.info("Authentication RedisKey: {}", userRedisKey);

        String redisVal = redisTemplate.opsForValue().get(userRedisKey);
        DefaultUser loginUser = Optional.ofNullable(JSONUtil.toBean(redisVal, DefaultUser.class)).orElse(DefaultUser.builder().build());
        log.info("Login User: {}", loginUser);

        if (Objects.isNull(loginUser)) {
            return null;
        }

        AuthenticationUser authentication = new AuthenticationUser();
        authentication.setUser(loginUser);

        if (loginUser.getRoles() != null && loginUser.getRoles().size() > 0) {
            DefaultRole role = (DefaultRole) loginUser.getRoles().get(0);
            List<Permission> permissionList = role.getPermission();
            authentication.setRole(role);
            authentication.setPermissions(permissionList);
        } else {
            authentication.setRole(DefaultRole.builder().build());
            authentication.setPermissions(Collections.EMPTY_LIST);
        }
        return authentication;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        DefaultAuthenticationSupplier.applicationContext = applicationContext;
        redisTemplate = getBean("stringRedisTemplate");
        jwtConfig = getBean("jwtConfig");
        parser = new JwtTokenParser(jwtConfig);
        generator = new JwtTokenGenerator(jwtConfig);
        // add to suppliers
        AuthenticationHolder.addSupplier(this);
    }

    public static <T> T getBean(String name) {
        try {
            return (T) applicationContext.getBean(name);
        } catch (NoSuchBeanDefinitionException e) {}
        return null;
    }
}