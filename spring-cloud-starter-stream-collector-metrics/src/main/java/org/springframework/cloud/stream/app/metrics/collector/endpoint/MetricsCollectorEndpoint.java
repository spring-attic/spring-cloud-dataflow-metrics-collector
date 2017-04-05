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

package org.springframework.cloud.stream.app.metrics.collector.endpoint;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.github.benmanes.caffeine.cache.Cache;

import org.springframework.cloud.stream.app.metrics.collector.model.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

	private Cache<String,Stream> streamCache;

	public MetricsCollectorEndpoint(Cache<String, Stream> streamCache) {
		this.streamCache = streamCache;
	}

	@RequestMapping(value = "/metrics", produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity<Collection<Stream>> fetchMetrics(@RequestParam(value = "name", defaultValue = "") String name){
		Collection<Stream> entries = null;
		if(StringUtils.isEmpty(name)){
			entries = streamCache.asMap().values();
		}else{
			Set<String> names = StringUtils.commaDelimitedListToSet(name);
			entries = streamCache.getAllPresent(names).values();
		}
		return new ResponseEntity<Collection<Stream>>(entries, HttpStatus.OK);
	}
}
