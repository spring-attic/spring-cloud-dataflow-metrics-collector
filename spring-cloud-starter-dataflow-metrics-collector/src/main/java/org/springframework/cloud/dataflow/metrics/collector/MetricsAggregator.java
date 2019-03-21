/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.cloud.dataflow.metrics.collector;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.metrics.collector.model.ApplicationMetrics;
import org.springframework.cloud.dataflow.metrics.collector.model.Metric;
import org.springframework.cloud.dataflow.metrics.collector.model.MicrometerMetric;
import org.springframework.cloud.dataflow.metrics.collector.services.ApplicationMetricsService;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Adds the incoming {@link ApplicationMetrics} payload into the in memory cache.
 * Supports metrics sent from Spring Cloud Stream 1.x and 2.x applications
 *
 * @author Vinicius Carvalho
 * @author Christian Tzolov
 */
@Component
public class MetricsAggregator {
	private Logger logger = LoggerFactory.getLogger(MetricsAggregator.class);

	private ObjectMapper mapper;
	private ApplicationMetricsService service;


	public MetricsAggregator(ApplicationMetricsService service) {
		this.service = service;
		this.mapper = new ObjectMapper();
	}

	private final static class Metric1TypeReference extends TypeReference<ApplicationMetrics<Metric<Number>>> {}

	private final static class Metric2TypeReference extends TypeReference<ApplicationMetrics<MicrometerMetric<Number>>> {}

	@StreamListener(Sink.INPUT)
	public void receive(String metrics) {

		ApplicationMetrics<Metric<Double>> applicationMetrics;
		try {
			// Use the "spring.integration.send" metric name as a version discriminator for old and new metrics
			if (StringUtils.hasText(metrics) && metrics.contains("spring.integration.send")) {
				ApplicationMetrics<MicrometerMetric<Number>> applicationMetrics2 = mapper.readValue(metrics, new Metric2TypeReference());
				applicationMetrics = convertMetric2ToMetric(applicationMetrics2);
				applicationMetrics.getProperties().put(ApplicationMetrics.STREAM_METRICS_VERSION, ApplicationMetrics.METRICS_VERSION_2);
			}
			else {
				applicationMetrics = mapper.readValue(metrics, new Metric1TypeReference());
				applicationMetrics.getProperties().put(ApplicationMetrics.STREAM_METRICS_VERSION, ApplicationMetrics.METRICS_VERSION_1);
			}

			this.processApplicationMetrics(applicationMetrics);
		}
		catch (IOException e) {
			logger.warn("Invalid metrics Json", e);
		}

	}

	/**
	 * Converts the new Micrometer metrics into the previous {@link Metric}
	 * format (e.g. to the Spring Boot 1.x actuator metrics)
	 * Only the successful Spring Integration channel metrics are filtered in. All other metrics are discarded!
	 * @param applicationMetrics2 {@link ApplicationMetrics} with Micrometer Metrics collection
	 * @return Returns {@link ApplicationMetrics} with SpringBoot1.5's Metric collection.
	 */
	private ApplicationMetrics<Metric<Double>> convertMetric2ToMetric(ApplicationMetrics<MicrometerMetric<Number>> applicationMetrics2) {
		List<Metric<Double>> metrics = applicationMetrics2.getMetrics().stream()
				.filter(metric -> metric.getId().getName().matches("spring\\.integration\\.send"))
				.filter(metric -> metric.getId().getTag("type").equals("channel"))
				.filter(metric -> metric.getId().getTag("result").equals("success"))
				.map(m2 -> new Metric<>(
						generateOldMetricName(m2),
						m2.getCount().doubleValue() / (applicationMetrics2.getInterval() / 1000), // normalize rate
						m2.getTimestamp()))
				.collect(Collectors.toList());
		ApplicationMetrics<Metric<Double>> applicationMetrics = new ApplicationMetrics(applicationMetrics2.getName(), metrics);
		applicationMetrics.setCreatedTime(applicationMetrics2.getCreatedTime());
		applicationMetrics.setProperties(applicationMetrics2.getProperties());
		return applicationMetrics;
	}

	/**
	 * Build an hierarchical Metric Ver.1 name from the the multi-dimension (e.g. multi-tag) micrometer Metric
	 * @param metric2 Micrometer based metrics
	 * @return Returns an hierarchical name compatible with the Metric ver.1 name convention.
	 */
	private String generateOldMetricName(MicrometerMetric<Number> metric2) {
		String oldMetricName = metric2.getId().getName();
		if (metric2.getId().getName().startsWith("spring.integration.")) {
			String channelName = metric2.getId().getTag("name");
			String metricResult = metric2.getId().getTag("result");
			String successSuffix = "success".equals(metricResult) ? "" : "." + metricResult;
			oldMetricName = "integration.channel." + channelName + ".send.mean" + successSuffix;
		}
		return oldMetricName;
	}

	private void processApplicationMetrics(ApplicationMetrics<Metric<Double>> metrics) {
		if (metrics.getProperties().get(ApplicationMetrics.APPLICATION_GUID) != null
				&& metrics.getProperties().get(ApplicationMetrics.APPLICATION_NAME) != null
				&& metrics.getProperties().get(ApplicationMetrics.STREAM_NAME) != null) {
			this.service.add(metrics);
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Metric : {} is missing key properties and will not be consumed by the collector", metrics.getName());
			}
		}
	}
}
