package com.same.payroll.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.security.jackson.SecurityJacksonModules;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

/**
 * Spring Session stores HttpSession attributes in Redis using whatever
 * RedisSerializer is wired as "springSessionDefaultRedisSerializer". The
 * default falls back to plain Java serialization, which breaks for
 * OAuth2-related session attributes:
 *   - OAuth2AuthorizationRequest (held briefly during the Google redirect)
 *   - OAuth2AuthorizedClient (the access/refresh token holder)
 * Both are only safely handled through Spring Security's own Jackson
 * mixins, so we build a JsonMapper with those registered via
 * SecurityJacksonModules and use it for the session's JSON serializer.
 *
 * As of Spring Data Redis 4.0 / Spring Security 7.0, the Jackson-2-based
 * GenericJackson2JsonRedisSerializer / SecurityJackson2Modules are
 * deprecated in favor of the Jackson-3-based GenericJacksonJsonRedisSerializer
 * / SecurityJacksonModules used here (note: different package, tools.jackson
 * instead of com.fasterxml.jackson, wired through JsonMapper.Builder rather
 * than a plain ObjectMapper). SecurityJacksonModules automatically includes
 * the OAuth2 client module as long as spring-boot-starter-oauth2-client is
 * on the classpath - no need to register it separately.
 *
 * IMPORTANT: SecurityJacksonModules configures a strict allowlist-based
 * PolymorphicTypeValidator that only recognizes Spring Security's own
 * classes by default. CustomUserPrincipal is OUR class and gets stored
 * directly as the OAuth2User/principal on the session, so it must be
 * explicitly allowed here via allowIfSubType(...) - otherwise reading the
 * session back fails with "Configured PolymorphicTypeValidator denied
 * resolution" even though writing it succeeded. If you add other custom
 * classes that end up on the SecurityContext (custom Authentication
 * implementations, custom principals, etc.), allow them the same way.
 *
 * Timeout and Redis key namespace are both controlled here directly via
 * the @EnableRedisHttpSession annotation parameters, rather than through
 * application.properties, so there is a single source of truth for them.
 */
@Configuration
@EnableRedisHttpSession(
        maxInactiveIntervalInSeconds = 1800,
        redisNamespace = "payroll:sessions"
)
public class RedisSessionConfig {

    @Bean
    public GenericJacksonJsonRedisSerializer springSessionDefaultRedisSerializer() {
        ClassLoader loader = getClass().getClassLoader();

        BasicPolymorphicTypeValidator.Builder typeValidatorBuilder =
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("com.same.payroll.security.");

        return GenericJacksonJsonRedisSerializer.builder()
                .customize(builder ->
                        builder.addModules(SecurityJacksonModules.getModules(loader, typeValidatorBuilder)))
                .build();
    }
}