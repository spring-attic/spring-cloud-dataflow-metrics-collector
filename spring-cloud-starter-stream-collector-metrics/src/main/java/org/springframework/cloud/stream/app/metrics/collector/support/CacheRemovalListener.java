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

package org.springframework.cloud.stream.app.metrics.collector.support;

import java.util.Iterator;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;

import org.springframework.cloud.stream.app.metrics.collector.model.Application;
import org.springframework.cloud.stream.app.metrics.collector.model.ApplicationMetrics;
import org.springframework.cloud.stream.app.metrics.collector.model.Instance;
import org.springframework.cloud.stream.app.metrics.collector.model.Stream;


/**
 * This listener removes any instance from the Stream Cache once they get evicted on the raw cache.
 * @author Vinicius Carvalho
 */
public class CacheRemovalListener implements RemovalListener<String,ApplicationMetrics> {

	private Cache<String,Stream> streamCache;

	public CacheRemovalListener(Cache<String, Stream> streamCache) {
		this.streamCache = streamCache;
	}


	@Override
	public void onRemoval(String s, ApplicationMetrics applicationMetrics, RemovalCause removalCause) {
		Stream stream = streamCache.getIfPresent(applicationMetrics.getProperties().get(ApplicationMetrics.STREAM_NAME));
		for(Application app : stream.getApplications()){
			if(app.getName().equals(applicationMetrics.getProperties().get(ApplicationMetrics.APPLICATION_NAME))){
				Iterator<Instance> it = app.getInstances().iterator();
				while(it.hasNext()){
					Instance instance = it.next();
					if(instance.getGuid().equals(applicationMetrics.getProperties().get(ApplicationMetrics.APPLICATION_GUID))){
						it.remove();
						break;
					}
				}
				break;
			}
		}
	}
}
