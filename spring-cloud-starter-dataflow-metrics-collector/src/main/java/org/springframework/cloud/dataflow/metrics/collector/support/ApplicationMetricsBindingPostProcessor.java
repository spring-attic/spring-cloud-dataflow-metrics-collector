package org.springframework.cloud.dataflow.metrics.collector.support;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * @author Vinicius Carvalho
 * Sets the spring.cloud.stream.bindings.input.destination to default value of 'metrics', it can be overriden via environment properties abstraction.
 */
public class ApplicationMetricsBindingPostProcessor implements EnvironmentPostProcessor{
	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		Map<String, Object> propertiesToAdd = new HashMap<>();
		propertiesToAdd.put("spring.cloud.stream.bindings.input.destination",
				"metrics");
		propertiesToAdd.put("spring.jackson.default-property-inclusion","non_null");
		environment.getPropertySources()
				.addLast(new MapPropertySource("collectorDefaultProperties", propertiesToAdd));
	}
}
