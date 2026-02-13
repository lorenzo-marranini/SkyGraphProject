package it.unipi.SkyGraph;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@SecurityScheme(
        name = "Bearer Authentication",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
// 2. Apply it globally to all your APIs
@OpenAPIDefinition(
        info = @Info(title = "SkyGraph API", version = "1.0"),
        security = @SecurityRequirement(name = "Bearer Authentication")
)
public class SkyGraphApplication {

	public static void main(String[] args) {
		SpringApplication.run(SkyGraphApplication.class, args);
	}

}
