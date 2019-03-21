/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.metrics.collector.support;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * @author Vinicius Carvalho Sets the spring.cloud.stream.bindings.input.destination to
 * default value of 'metrics', it can be overriden via environment properties abstraction.
 */
public class ApplicationMetricsBindingPostProcessor implements EnvironmentPostProcessor {
	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		Map<String, Object> propertiesToAdd = new HashMap<>();
		propertiesToAdd.put("spring.cloud.stream.bindings.input.destination", "metrics");
		propertiesToAdd.put("spring.jackson.default-property-inclusion", "non_null");
		environment.getPropertySources().addLast(new MapPropertySource("collectorDefaultProperties", propertiesToAdd));
	}
}
