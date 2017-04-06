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

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.github.benmanes.caffeine.cache.Cache;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.app.metrics.collector.model.Application;
import org.springframework.cloud.stream.app.metrics.collector.model.ApplicationMetrics;
import org.springframework.cloud.stream.app.metrics.collector.model.Instance;
import org.springframework.cloud.stream.app.metrics.collector.model.Stream;
import org.springframework.cloud.stream.app.metrics.collector.utils.YANUtils;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Listens for {@link ApplicationMetrics} coming from the metrics channel. Transform the
 * payload into an {@link Stream} hierarchical model to be consumed by the server. This
 * class handles conversion, update of deltas for the sendCount, as well as invalidation
 * of stale entries
 * @author Vinicius Carvalho
 */
@Component
public class MetricsAggregator {

	public static final String OUTPUT_METRIC_NAME = "integration.channel.output.sendRate.mean";
	public static final String INPUT_METRIC_NAME = "integration.channel.input.sendRate.mean";

	private Lock lock = new ReentrantLock();

	private Cache<String, Stream> streamCache;

	private Cache<String, ApplicationMetrics> rawCache;

	public MetricsAggregator(@Qualifier("streamCache") Cache<String, Stream> streamCache,
			@Qualifier("rawCache") Cache<String, ApplicationMetrics> rawCache) {
		this.streamCache = streamCache;
		this.rawCache = rawCache;
	}

	@StreamListener(Sink.INPUT)
	public void receive(ApplicationMetrics metrics) {
		this.rawCache.put(metrics.getName(), metrics);
		Stream stream = convert(metrics);
		Application application = stream.getApplications().get(0);
		Instance instance = application.getInstances().get(0);
		try {
			lock.lock();
			Stream cached = this.streamCache.getIfPresent(stream.getName());
			if (cached == null) {
				this.streamCache.put(stream.getName(), stream);
				return;
			}
			if (cached.getApplications().size() == 0) {
				cached.setApplications(stream.getApplications());
				return;
			}
			int applicationIndex = cached.getApplications().indexOf(application);
			if (applicationIndex >= 0) {
				Application cachedApplication = cached.getApplications().get(applicationIndex);
				if (cachedApplication.getInstances().size() == 0) {
					cachedApplication.getInstances().add(instance);
					return;
				}
				int idx = cachedApplication.getInstances().indexOf(instance);
				if (idx >= 0) {
					Instance cachedInstance = cachedApplication.getInstances().get(idx);
					cachedInstance.updateOutgoingRate(instance.getOutgoingRate());
					cachedInstance.updateIncomingRate(instance.getIncomingRate());
				}
				else {
					cachedApplication.getInstances().add(instance);
				}
			}
			else {
				cached.getApplications().add(application);
			}
		}
		catch (Exception e) {

		}
		finally {
			lock.unlock();
		}
	}

	private Stream convert(ApplicationMetrics applicationMetrics) {
		Assert.notNull(applicationMetrics.getProperties().get(ApplicationMetrics.STREAM_NAME),
				"Missing STREAM_NAME from metrics properties");
		Assert.notNull(applicationMetrics.getProperties().get(ApplicationMetrics.APPLICATION_NAME),
				"Missing APPLICATION_NAME from metrics properties");
		Assert.notNull(applicationMetrics.getProperties().get(ApplicationMetrics.APPLICATION_GUID),
				"Missing APPLICATION_GUID from metrics properties");

		Stream stream = new Stream((String) applicationMetrics.getProperties().get(ApplicationMetrics.STREAM_NAME));
		Application application = new Application(
				(String) applicationMetrics.getProperties().get(ApplicationMetrics.APPLICATION_NAME));

		Double incomeRate = YANUtils.cast(findMetric(applicationMetrics.getMetrics(), INPUT_METRIC_NAME).getValue(), Double.class)
				.orElse(0.0);
		Double outgoingRate = YANUtils
				.cast(findMetric(applicationMetrics.getMetrics(), OUTPUT_METRIC_NAME).getValue(), Double.class).orElse(0.0);
		Integer instanceIndex = YANUtils
				.cast(applicationMetrics.getProperties().get(ApplicationMetrics.INSTANCE_INDEX), Integer.class).orElse(0);

		Instance instance = new Instance(applicationMetrics.getProperties().get(ApplicationMetrics.APPLICATION_GUID).toString(),
				incomeRate, outgoingRate);
		instance.setProperties(applicationMetrics.getProperties());
		instance.setIndex(instanceIndex);
		application.getInstances().add(instance);
		stream.getApplications().add(application);
		return stream;
	}

	private Metric<?> findMetric(Collection<Metric<?>> metrics, String name) {
		Metric<?> result = null;
		Optional<Metric<?>> optinal = metrics.stream().filter(metric -> metric.getName().equals(name)).findFirst();
		if (optinal.isPresent()) {
			result = optinal.get();
		}
		else {
			result = new Metric<Double>(name, 0.0);
		}
		return result;
	}
}
