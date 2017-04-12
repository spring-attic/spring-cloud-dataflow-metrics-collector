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

package org.springframework.cloud.dataflow.metrics.collector.endpoint;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.benmanes.caffeine.cache.Cache;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.cloud.dataflow.metrics.collector.MetricsAggregator;
import org.springframework.cloud.dataflow.metrics.collector.model.Application;
import org.springframework.cloud.dataflow.metrics.collector.model.ApplicationMetrics;
import org.springframework.cloud.dataflow.metrics.collector.model.Instance;
import org.springframework.cloud.dataflow.metrics.collector.model.StreamMetrics;
import org.springframework.cloud.dataflow.metrics.collector.utils.YANUtils;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Vinicius Carvalho
 */
@RestController
@RequestMapping("/collector/metrics/streams")
@ExposesResourceFor(StreamMetrics.class)
public class MetricsCollectorEndpoint {

	private Cache<String, ApplicationMetrics> rawCache;

	public MetricsCollectorEndpoint(Cache<String, ApplicationMetrics> rawCache) {
		this.rawCache = rawCache;
	}

	@RequestMapping(produces = { "application/vnd.spring.cloud.dataflow.collector.v1.hal+json" })
	public ResponseEntity<PagedResources<StreamMetrics>> fetchMetrics(
			@RequestParam(value = "name", defaultValue = "") String name) {
		Collection<StreamMetrics> entries = new LinkedList<>();

		Set<String> streamNames = null;
		if (StringUtils.isEmpty(name)) {
			streamNames = rawCache.asMap().values().stream()
					.map(applicationMetrics -> String
							.valueOf(applicationMetrics.getProperties().get(ApplicationMetrics.STREAM_NAME)))
					.collect(Collectors.toSet());
		}
		else {
			streamNames = StringUtils.commaDelimitedListToSet(name);
		}

		for (String streamName : streamNames) {
			StreamMetrics streamMetrics = null;
			List<ApplicationMetrics> filteredList = rawCache.asMap().values().stream()
					.filter(applicationMetrics -> applicationMetrics.getProperties().get(ApplicationMetrics.STREAM_NAME)
							.equals(streamName))
					.collect(Collectors.toList());
			for (ApplicationMetrics applicationMetrics : filteredList) {
				streamMetrics = convert(applicationMetrics, streamMetrics);
			}
			if (streamMetrics != null) {
				entries.add(streamMetrics);
			}
		}

		int totalPages = (entries.size() == 0) ? 0 : entries.size() / entries.size();
		PagedResources.PageMetadata pageMetadata = new PagedResources.PageMetadata(entries.size(), 0, entries.size(),
				totalPages);
		PagedResources<StreamMetrics> pagedResources = new PagedResources<>(entries, pageMetadata,
				ControllerLinkBuilder.linkTo(MetricsCollectorEndpoint.class).withRel(Link.REL_SELF));
		return new ResponseEntity<PagedResources<StreamMetrics>>(pagedResources, HttpStatus.OK);
	}

	/**
	 * Converts a denormalized view of each application instance metric
	 * ({@link ApplicationMetrics}) into a hierarchical model {@link StreamMetrics}
	 * @param applicationMetrics object to be converted
	 * @param root The root object of the hierarchy - null if the first conversion
	 * @return a hierarchical view of metrics using {@link StreamMetrics} as the root
	 * object
	 */
	private StreamMetrics convert(ApplicationMetrics applicationMetrics, StreamMetrics root) {
		Assert.notNull(applicationMetrics.getProperties().get(ApplicationMetrics.STREAM_NAME),
				"Missing STREAM_NAME from metrics properties");
		Assert.notNull(applicationMetrics.getProperties().get(ApplicationMetrics.APPLICATION_NAME),
				"Missing APPLICATION_NAME from metrics properties");
		Assert.notNull(applicationMetrics.getProperties().get(ApplicationMetrics.APPLICATION_GUID),
				"Missing APPLICATION_GUID from metrics properties");
		StreamMetrics streamMetrics = (root == null)
				? new StreamMetrics((String) applicationMetrics.getProperties().get(ApplicationMetrics.STREAM_NAME))
				: root;
		Application application = new Application(
				(String) applicationMetrics.getProperties().get(ApplicationMetrics.APPLICATION_NAME));

		Double incomeRate = YANUtils.toDouble(findMetric(applicationMetrics.getMetrics(), MetricsAggregator.INPUT_METRIC_NAME).getValue());

		Double outgoingRate = YANUtils.toDouble(findMetric(applicationMetrics.getMetrics(), MetricsAggregator.OUTPUT_METRIC_NAME).getValue());

		Integer instanceIndex = YANUtils
				.toInteger(applicationMetrics.getProperties().get(ApplicationMetrics.INSTANCE_INDEX));

		Instance instance = new Instance(
				applicationMetrics.getProperties().get(ApplicationMetrics.APPLICATION_GUID).toString(), incomeRate,
				outgoingRate);
		instance.setProperties(applicationMetrics.getProperties());
		instance.setIndex(instanceIndex);
		instance.setKey(applicationMetrics.getName());
		int applicationIndex = streamMetrics.getApplications().indexOf(application);
		if (applicationIndex < 0) {
			application.getInstances().add(instance);
			streamMetrics.getApplications().add(application);
		}
		else {
			int idx = streamMetrics.getApplications().get(applicationIndex).getInstances().indexOf(instance);
			if (idx < 0) {
				streamMetrics.getApplications().get(applicationIndex).getInstances().add(instance);
			}
		}
		return streamMetrics;
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
