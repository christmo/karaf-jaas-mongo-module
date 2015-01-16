/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.karaf.jaas.modules.mongo;

import java.util.Map;

import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.BackingEngineFactory;

/**
 * 
 * The backing engine for the {@link MongoLoginModule}.
 * 
 * @author Niels Bertram
 *
 */
public class MongoBackingEngineFactory implements BackingEngineFactory {

	@Override
	public String getModuleClass() {
		return MongoLoginModule.class.getName();
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public BackingEngine build(Map options) {

		MongoConfigurationBuilder confBuilder = new MongoConfigurationBuilder(
				null, options);

		try {
			BackingEngine engine = new MongoBackingEngine(confBuilder.build());
			return engine;
		} catch (ConfigurationException e) {
			throw new RuntimeException(
					"Failed to configure the backing engine.", e);
		}

	}

}
