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

package org.springframework.cloud.dataflow.metrics.collector.endpoint;

import org.springframework.cloud.dataflow.metrics.collector.model.RootResource;
import org.springframework.cloud.dataflow.metrics.collector.model.StreamMetrics;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Vinicius Carvalho
 */
@RestController
@ExposesResourceFor(RootEndpoint.class)
@RequestMapping("/")
public class RootEndpoint {

	private final EntityLinks entityLinks;

	public RootEndpoint(EntityLinks entityLinks) {
		this.entityLinks = entityLinks;
	}

	@RequestMapping(method = RequestMethod.GET, produces = MediaTypes.HAL_JSON_VALUE)
	public RootResource info() {
		String streamTemplated = entityLinks.linkToCollectionResource(StreamMetrics.class).getHref() + "?{name}";
		RootResource rootResource = new RootResource();
		rootResource.add(new Link(streamTemplated).withRel("/collector/metrics/streams"));
		return rootResource;
	}
}
