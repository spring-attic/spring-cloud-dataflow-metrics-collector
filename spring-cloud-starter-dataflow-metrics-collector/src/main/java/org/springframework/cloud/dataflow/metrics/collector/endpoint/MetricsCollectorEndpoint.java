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
import org.springframework.cloud.dataflow.metrics.collector.model.Stream;
import org.springframework.cloud.dataflow.metrics.collector.utils.YANUtils;
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
@RequestMapping("/collector")
public class MetricsCollectorEndpoint {

	private Cache<String,ApplicationMetrics> rawCache;

	public MetricsCollectorEndpoint(Cache<String, ApplicationMetrics> rawCache) {
		this.rawCache = rawCache;
	}

	@RequestMapping(value = "/metrics", produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<Collection<Stream>> fetchMetrics(@RequestParam(value = "name", defaultValue = "") String name){
		Collection<Stream> entries = new LinkedList<>();

		Set<String> streamNames = null;
		if(StringUtils.isEmpty(name)){
			streamNames = rawCache.asMap().values().stream().map(applicationMetrics -> String.valueOf(applicationMetrics.getProperties().get(ApplicationMetrics.STREAM_NAME))).collect(Collectors.toSet());
		}else{
			Set<String> names = StringUtils.commaDelimitedListToSet(name);
		}

		for(String streamName : streamNames){
			Stream stream = null;
			List<ApplicationMetrics> filteredList = rawCache.asMap().values().stream().filter(applicationMetrics ->
					applicationMetrics.getProperties().get(ApplicationMetrics.STREAM_NAME).equals(streamName)).collect(Collectors.toList());
			for(ApplicationMetrics applicationMetrics : filteredList){
				stream = convert(applicationMetrics,stream);
			}
			entries.add(stream);
		}
		return new ResponseEntity<Collection<Stream>>(entries, HttpStatus.OK);
	}

	/**
	 * Converts a denormalized view of each application instance metric ({@link ApplicationMetrics}) into a
	 * hierarchical model {@link Stream}
	 * @param applicationMetrics object to be converted
	 * @param root The root object of the hierarchy - null if the first conversion
	 * @return a hierarchical view of metrics using {@link Stream} as the root object
	 */
	private Stream convert(ApplicationMetrics applicationMetrics, Stream root){
		Assert.notNull(applicationMetrics.getProperties().get(ApplicationMetrics.STREAM_NAME),
				"Missing STREAM_NAME from metrics properties");
		Assert.notNull(applicationMetrics.getProperties().get(ApplicationMetrics.APPLICATION_NAME),
				"Missing APPLICATION_NAME from metrics properties");
		Assert.notNull(applicationMetrics.getProperties().get(ApplicationMetrics.APPLICATION_GUID),
				"Missing APPLICATION_GUID from metrics properties");
		Stream stream = (root == null) ? new Stream((String) applicationMetrics.getProperties().get(ApplicationMetrics.STREAM_NAME)) : root;
		Application application = new Application(
				(String) applicationMetrics.getProperties().get(ApplicationMetrics.APPLICATION_NAME));
		Double incomeRate = YANUtils
				.cast(findMetric(applicationMetrics.getMetrics(), MetricsAggregator.INPUT_METRIC_NAME).getValue(), Double.class)
				.orElse(0.0);
		Double outgoingRate = YANUtils
				.cast(findMetric(applicationMetrics.getMetrics(), MetricsAggregator.OUTPUT_METRIC_NAME).getValue(), Double.class)
				.orElse(0.0);
		Integer instanceIndex = YANUtils
				.cast(applicationMetrics.getProperties().get(ApplicationMetrics.INSTANCE_INDEX), Integer.class)
				.orElse(0);

		Instance instance = new Instance(
				applicationMetrics.getProperties().get(ApplicationMetrics.APPLICATION_GUID).toString(), incomeRate,
				outgoingRate);
		instance.setProperties(applicationMetrics.getProperties());
		instance.setIndex(instanceIndex);
		int applicationIndex = stream.getApplications().indexOf(application);
		if(applicationIndex < 0){
			application.getInstances().add(instance);
			stream.getApplications().add(application);
		}else{
			int idx = stream.getApplications().get(applicationIndex).getInstances().indexOf(instance);
			if(idx < 0){
				stream.getApplications().get(applicationIndex).getInstances().add(instance);
			}
		}
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
