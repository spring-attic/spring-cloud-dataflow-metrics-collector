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
import java.util.regex.Pattern;

import org.springframework.cloud.dataflow.metrics.collector.model.StreamMetrics;
import org.springframework.cloud.dataflow.metrics.collector.services.ApplicationMetricsService;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

	private final Pattern pattern = Pattern.compile("integration\\.channel\\.(\\w*)\\.sendCount");

	private ApplicationMetricsService service;

	public MetricsCollectorEndpoint(ApplicationMetricsService service) {
		this.service = service;
	}

	@RequestMapping(produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<PagedResources<StreamMetrics>> fetchMetrics(
			@RequestParam(value = "name", defaultValue = "") String name) {

		Collection<StreamMetrics> entries = service.toStreamMetrics(name);

		int totalPages = (entries.size() == 0) ? 0 : entries.size() / entries.size();
		PagedResources.PageMetadata pageMetadata = new PagedResources.PageMetadata(entries.size(), 0, entries.size(),
				totalPages);
		PagedResources<StreamMetrics> pagedResources = new PagedResources<>(entries, pageMetadata,
				ControllerLinkBuilder.linkTo(MetricsCollectorEndpoint.class).withRel(Link.REL_SELF));

		return new ResponseEntity<PagedResources<StreamMetrics>>(pagedResources, HttpStatus.OK);
	}

}
