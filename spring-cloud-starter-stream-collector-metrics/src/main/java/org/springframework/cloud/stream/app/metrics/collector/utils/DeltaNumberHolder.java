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

package org.springframework.cloud.stream.app.metrics.collector.utils;

import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Ticker;


/**
 * A numeric holder for values that need to be computed using an offset as a base value. The value returned is always currentValue-offset.
 *
 * @author Vinicius Carvalho
 */
public class DeltaNumberHolder extends NumberHolder<Double>{

	private Double offset;
	private Long createdTime;
	private Ticker ticker;

	public DeltaNumberHolder(){
		this(null);
	}

	public DeltaNumberHolder(Double value){
		this.createdTime = System.nanoTime();
		this.offset = value;
		this.ticker = Ticker.systemTicker();
		update(value);
	}

	/**
	 *
	 * @return the delta computed as (currentValue - offset)
	 */
	@Override
	public Double getValue() {
		if(offset == null) {
			return null;
		}
		return currentValue - offset;
	}

	public Double getCurrentValue() {
		return this.currentValue;
	}

	/**
	 *
	 * @return The rate using Seconds as the unit for the window
	 */
	public Double getRate(){
		return this.getRate(TimeUnit.SECONDS);
	}

	/**
	 * Return the rate of change during a specific window of time defined as now-createdTime
	 * @param unit Time unit used to measure
	 * @return delta/window
	 */
	public Double getRate(TimeUnit unit){
		if(offset == null){
			return null;
		}
		long scale = TimeUnit.NANOSECONDS.convert(1,unit);
		if(getValue() == 0)
			return 0.0;
		return (getValue()/(this.ticker.read()-this.createdTime))*scale;
	}

	@Override
	public void update(Double value) {
		this.offset = this.currentValue;
		super.update(value);
	}


}
