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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.cloud.dataflow.metrics.collector.endpoint.MetricsCollectorEndpoint;
import org.springframework.cloud.dataflow.metrics.collector.model.Application;
import org.springframework.cloud.dataflow.metrics.collector.model.ApplicationMetrics;
import org.springframework.cloud.dataflow.metrics.collector.model.Instance;
import org.springframework.cloud.dataflow.metrics.collector.model.StreamMetrics;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @author Vinicius Carvalho
 */
public class MetricsAggregatorTests extends BaseCacheTests {

	@Before
	public void setup() {
		HttpServletRequest mockRequest = new MockHttpServletRequest();
		ServletRequestAttributes servletRequestAttributes = new ServletRequestAttributes(mockRequest);
		RequestContextHolder.setRequestAttributes(servletRequestAttributes);
	}

	@After
	public void teardown() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	public void includeOneMetric() throws Exception {
		ApplicationMetrics app = createMetrics("httpIngest", "http", "foo", 0, 1.0, 0.0);
		Cache<String, ApplicationMetrics> rawCache = Caffeine.newBuilder().build();
		MetricsAggregator aggregator = new MetricsAggregator(rawCache);
		MetricsCollectorEndpoint endpoint = new MetricsCollectorEndpoint(rawCache);
		aggregator.receive(app);

		Assert.assertEquals(1, rawCache.estimatedSize());
		StreamMetrics streamMetrics = endpoint.fetchMetrics("").getBody().iterator().next();
		Application application = streamMetrics.getApplications().get(0);
		Assert.assertNotNull(streamMetrics);
		Assert.assertEquals("http", application.getName());
		Instance instance = application.getInstances().get(0);
		Assert.assertEquals(app.getName(),instance.getKey());
		Assert.assertEquals("foo", instance.getGuid());
	}

	@Test
	public void incrementMetric() throws Exception {
		ApplicationMetrics app = createMetrics("httpIngest", "http", "foo", 0, 1.0, 0.0);
		Cache<String, ApplicationMetrics> rawCache = Caffeine.newBuilder().build();
		MetricsAggregator aggregator = new MetricsAggregator(rawCache);
		MetricsCollectorEndpoint endpoint = new MetricsCollectorEndpoint(rawCache);
		aggregator.receive(app);

		Assert.assertEquals(1, rawCache.estimatedSize());
		StreamMetrics streamMetrics = endpoint.fetchMetrics("").getBody().iterator().next();
		Application application = streamMetrics.getApplications().get(0);
		Assert.assertNotNull(streamMetrics);
		Assert.assertEquals("http", application.getName());
		Instance instance = application.getInstances().get(0);
		Assert.assertEquals("foo", instance.getGuid());
		Assert.assertEquals(new Double(1.0), instance.getIncomingRate());
		ApplicationMetrics app2 = createMetrics("httpIngest", "http", "foo", 0, 10.0, 0.0);
		aggregator.receive(app2);

		Assert.assertEquals(1, rawCache.estimatedSize());
		streamMetrics = endpoint.fetchMetrics("").getBody().iterator().next();
		application = streamMetrics.getApplications().get(0);
		Assert.assertNotNull(streamMetrics);
		Assert.assertEquals("http", application.getName());
		instance = application.getInstances().get(0);
		Assert.assertEquals("foo", instance.getGuid());
		Assert.assertEquals(new Double(10.0), instance.getIncomingRate());
	}

	@Test
	public void addInstance() throws Exception {
		ApplicationMetrics app = createMetrics("httpIngest", "http", "foo", 0, 1.0, 0.0);
		Cache<String, ApplicationMetrics> rawCache = Caffeine.newBuilder().build();
		MetricsAggregator aggregator = new MetricsAggregator(rawCache);
		MetricsCollectorEndpoint endpoint = new MetricsCollectorEndpoint(rawCache);
		aggregator.receive(app);

		Assert.assertEquals(1, rawCache.estimatedSize());
		StreamMetrics streamMetrics = endpoint.fetchMetrics("").getBody().iterator().next();
		Application application = streamMetrics.getApplications().get(0);
		Assert.assertNotNull(streamMetrics);
		Assert.assertEquals("http", application.getName());
		Instance instance = application.getInstances().get(0);
		Assert.assertEquals("foo", instance.getGuid());
		Assert.assertEquals(new Double(1.0), instance.getIncomingRate());
		ApplicationMetrics app2 = createMetrics("httpIngest", "http", "bar", 1, 10.0, 0.0);
		aggregator.receive(app2);

		Assert.assertEquals(2, rawCache.estimatedSize());
		streamMetrics = endpoint.fetchMetrics("").getBody().iterator().next();
		application = streamMetrics.getApplications().get(0);
		Assert.assertNotNull(streamMetrics);
		Assert.assertEquals("http", application.getName());
		Assert.assertEquals(2, application.getInstances().size());

	}

	@Test
	public void removeInstance() throws Exception {
		ApplicationMetrics app = createMetrics("httpIngest", "http", "foo", 0, 1.0, 0.0);
		ApplicationMetrics app2 = createMetrics("httpIngest", "http", "bar", 1, 10.0, 0.0);
		Cache<String, ApplicationMetrics> rawCache = Caffeine.newBuilder().build();
		MetricsCollectorEndpoint endpoint = new MetricsCollectorEndpoint(rawCache);
		MetricsAggregator aggregator = new MetricsAggregator(rawCache);

		aggregator.receive(app);
		aggregator.receive(app2);

		StreamMetrics streamMetrics = endpoint.fetchMetrics("").getBody().iterator().next();
		Application application = streamMetrics.getApplications().get(0);
		Instance instance = application.getInstances().get(0);

		Assert.assertEquals(2, rawCache.estimatedSize());
		streamMetrics = endpoint.fetchMetrics("").getBody().iterator().next();
		application = streamMetrics.getApplications().get(0);
		Assert.assertNotNull(streamMetrics);
		Assert.assertEquals("http", application.getName());
		Assert.assertEquals(2, application.getInstances().size());
		rawCache.invalidate("httpIngest.http.bar");
		Thread.sleep(1000);
		Assert.assertEquals(1, rawCache.estimatedSize());
		streamMetrics = endpoint.fetchMetrics("").getBody().iterator().next();
		application = streamMetrics.getApplications().get(0);

		Assert.assertEquals(1, application.getInstances().size());
	}

	@Test
	public void addApplication() throws Exception {
		Cache<String, ApplicationMetrics> rawCache = Caffeine.newBuilder().build();
		MetricsCollectorEndpoint endpoint = new MetricsCollectorEndpoint(rawCache);
		MetricsAggregator aggregator = new MetricsAggregator(rawCache);

		ApplicationMetrics app = createMetrics("httpIngest", "http", "foo", 0, 1.0, 0.0);
		ApplicationMetrics app2 = createMetrics("httpIngest", "log", "bar", 0, 1.0, 0.0);

		aggregator.receive(app);
		aggregator.receive(app2);

		Assert.assertEquals(2, rawCache.estimatedSize());
		StreamMetrics streamMetrics = endpoint.fetchMetrics("").getBody().iterator().next();
		Assert.assertEquals(2, streamMetrics.getApplications().size());
	}

	@Test
	public void addStream() throws Exception {
		Cache<String, ApplicationMetrics> rawCache = Caffeine.newBuilder().build();
		MetricsAggregator aggregator = new MetricsAggregator(rawCache);
		MetricsCollectorEndpoint endpoint = new MetricsCollectorEndpoint(rawCache);

		ApplicationMetrics app = createMetrics("httpIngest", "http", "foo", 0, 1.0, 0.0);
		ApplicationMetrics app2 = createMetrics("woodchuck", "time", "bar", 0, 1.0, 0.0);

		aggregator.receive(app);
		aggregator.receive(app2);

		Assert.assertEquals(2, endpoint.fetchMetrics("").getBody().getContent().size());
	}

	@Test
	public void filterByStream() throws Exception {
		Cache<String, ApplicationMetrics> rawCache = Caffeine.newBuilder().build();
		MetricsAggregator aggregator = new MetricsAggregator(rawCache);
		MetricsCollectorEndpoint endpoint = new MetricsCollectorEndpoint(rawCache);

		ApplicationMetrics app = createMetrics("httpIngest", "http", "foo", 0, 1.0, 0.0);
		ApplicationMetrics app2 = createMetrics("woodchuck", "time", "bar", 0, 1.0, 0.0);
		ApplicationMetrics app3 = createMetrics("twitter", "twitterstream", "bar", 0, 1.0, 0.0);

		aggregator.receive(app);
		aggregator.receive(app2);
		aggregator.receive(app3);

		Assert.assertEquals(2, endpoint.fetchMetrics("httpIngest,woodchuck").getBody().getContent().size());
	}

	private ApplicationMetrics createMetrics(String streamName, String applicationName, String appGuid, Integer index,
			Double incomingRate, Double outgoingRate) {

		ApplicationMetrics applicationMetrics = new ApplicationMetrics(
				streamName + "." + applicationName + "." + appGuid, new LinkedList<>());
		Map<String, Object> properties = new HashMap<>();
		properties.put(ApplicationMetrics.STREAM_NAME, streamName);
		properties.put(ApplicationMetrics.APPLICATION_NAME, applicationName);
		properties.put(ApplicationMetrics.APPLICATION_GUID, appGuid);
		properties.put(ApplicationMetrics.INSTANCE_INDEX, index);
		applicationMetrics.getMetrics().add(new Metric<>(MetricsAggregator.INPUT_METRIC_NAME, incomingRate));
		applicationMetrics.getMetrics().add(new Metric<>(MetricsAggregator.OUTPUT_METRIC_NAME, outgoingRate));
		properties.put(MetricsAggregator.INPUT_METRIC_NAME, incomingRate);
		properties.put(MetricsAggregator.OUTPUT_METRIC_NAME, outgoingRate);
		applicationMetrics.setProperties(properties);
		return applicationMetrics;
	}
}
