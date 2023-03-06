package net.kaholaz.setlist

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@SpringBootApplication
class SetListApplication

fun main(args: Array<String>) {
	runApplication<SetListApplication>(*args)
}

@Configuration
class Configurations : WebMvcConfigurer {
	override fun addCorsMappings(registry: CorsRegistry) {
		registry.addMapping("/**")
			.allowedHeaders("*")
			.allowedOrigins("*")
	}
}