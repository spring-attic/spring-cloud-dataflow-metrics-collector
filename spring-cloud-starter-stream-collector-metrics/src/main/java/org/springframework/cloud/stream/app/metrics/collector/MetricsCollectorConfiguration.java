/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.app.metrics.collector;

import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.app.metrics.collector.endpoint.MetricsCollectorEndpoint;
import org.springframework.cloud.stream.app.metrics.collector.model.ApplicationMetrics;
import org.springframework.cloud.stream.app.metrics.collector.support.MetricJsonSerializer;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @author Mark Pollack
 * @author Vinicius Carvalho
 */
@Configuration
@EnableBinding(Sink.class)
@EnableConfigurationProperties(MetricCollectorProperties.class)
public class MetricsCollectorConfiguration {

	@Autowired
	private MetricCollectorProperties properties;

	@Bean
	public MetricJsonSerializer jsonSerializer(){
		return new MetricJsonSerializer();
	}

	/**
	 * This cache holds "raw" instances from messages arriving over the sink. It's lifecycle controls the normalized cache by
	 * evicting entries on the normalized cache as they get evicted here.
	 *
	 * @return
	 * @throws Exception
	 */
	@Bean
	public Cache<String, ApplicationMetrics> rawCache() throws Exception{
		return Caffeine.<String,ApplicationMetrics>newBuilder()
				.expireAfterWrite(properties.getEvictionTimeout(), TimeUnit.SECONDS)
				.build();
	}



	@Bean
	public MetricsAggregator metricsAggregator(Cache<String,ApplicationMetrics> rawCache){
		return new MetricsAggregator(rawCache);
	}

	@Bean
	public MetricsCollectorEndpoint metricsCollectorEndpoint(Cache<String,ApplicationMetrics> rawCache){
		return new MetricsCollectorEndpoint(rawCache);
	}
}
