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

package org.springframework.cloud.dataflow.metrics.collector;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.metrics.collector.endpoint.RootEndpoint;
import org.springframework.cloud.dataflow.metrics.collector.services.ApplicationMetricsService;
import org.springframework.cloud.dataflow.metrics.collector.support.CaffeineHealthIndicator;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.dataflow.metrics.collector.endpoint.MetricsCollectorEndpoint;
import org.springframework.cloud.dataflow.metrics.collector.model.ApplicationMetrics;
import org.springframework.cloud.dataflow.metrics.collector.support.MetricJsonSerializer;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.EntityLinks;


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


	@Bean
	public Cache<String,LinkedList<ApplicationMetrics>> metricsStorage(){
		return Caffeine.<String,ApplicationMetrics>newBuilder()
				.expireAfterWrite(properties.getEvictionTimeout(), TimeUnit.SECONDS)
				.recordStats()
				.build();
	}

	@Bean
	public ApplicationMetricsService applicationMetricsService(Cache<String,LinkedList<ApplicationMetrics>> metricsStorage) throws Exception{
		return new ApplicationMetricsService(metricsStorage);
	}

	@Bean
	public MetricsAggregator metricsAggregator(ApplicationMetricsService applicationMetricsService){
		return new MetricsAggregator(applicationMetricsService);
	}

	@Bean
	public MetricsCollectorEndpoint metricsCollectorEndpoint(ApplicationMetricsService applicationMetricsService){
		return new MetricsCollectorEndpoint(applicationMetricsService);
	}

	@Bean
	public RootEndpoint rootEndpoint(EntityLinks entityLinks){
		return new RootEndpoint(entityLinks);
	}

	@Bean
	public CaffeineHealthIndicator caffeineHealthIndicator(Cache<String,LinkedList<ApplicationMetrics>> metricsStorage){
		return new CaffeineHealthIndicator(metricsStorage);
	}
}
