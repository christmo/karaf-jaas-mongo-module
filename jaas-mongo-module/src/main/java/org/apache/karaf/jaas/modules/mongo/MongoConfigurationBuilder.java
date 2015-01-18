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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.osgi.framework.BundleContext;

import org.apache.karaf.jaas.modules.mongo.internal.DefaultUserDetailService;

/**
 * 
 * A helper class to deal with the stateless nature of the JAAS framework. The
 * only way to construct this class is by supplying the bundle context and all
 * LoginModule properties that are configured in the JAAS configuration section
 * of the login module.
 * 
 * @author Niels Bertram
 *
 */
public class MongoConfigurationBuilder {

	private final static Logger logger = LoggerFactory
			.getLogger(MongoConfigurationBuilder.class);

	/**
	 * The bundle context used by the configuration builder to build the mongo
	 * configration/
	 */
	private BundleContext bundleContext;

	/**
	 * The supplied JAAS options used by the configuration builder to construct
	 * the {@link MongoConfiguration}
	 */
	Map<String, ?> options;

	@SuppressWarnings("unused")
	private MongoConfigurationBuilder() {
	}

	public MongoConfigurationBuilder(BundleContext bundleContext,
			Map<String, ?> options) {

		this.bundleContext = bundleContext;
		this.options = options;

	}

	@SuppressWarnings("unchecked")
	public MongoConfiguration build() throws ConfigurationException {

		try {
			getClass().getClassLoader().loadClass("com.mongodb.Mongo");
		} catch (ClassNotFoundException e) {
			throw new ConfigurationException(
					"Mongo library is not accessible by this login module.", e);
		}

		MongoConfiguration conf = new MongoConfiguration();

		String datasourceURL = (String) options
				.get(MongoConfiguration.DATASOURCE);
		if (datasourceURL == null || datasourceURL.trim().length() == 0) {
			logger.warn("No datasource was specified, using default localhost:27017");
			conf.setDatasourceURL("localhost:27017");
		} else {
			conf.setDatasourceURL(datasourceURL);
		}

		String dbName = (String) options.get(MongoConfiguration.DATABASE);
		if (dbName == null || dbName.trim().length() == 0) {
			logger.error("No database name was specified");
			// TODO review if this is the right error to throw
			throw new ConfigurationException("No database name was specified.");
		} else {
			conf.setDatabaseName(dbName);
		}

		// FIXME secure login to mongodb
		// http://docs.mongodb.org/ecosystem/tutorial/getting-started-with-java-driver/#authentication-optional

		String userSourceImplementationClassName = (String) options
				.get(MongoConfiguration.MONGO_SOURCE_IMPLEMENTATION_CLASS);

		if (userSourceImplementationClassName != null) {
			// TODO test this ... uh, ah lets see if we can load it from bundle
			// context
			try {
				Class<? extends UserDetailService> clazz = (Class<UserDetailService>) bundleContext
						.getBundle().loadClass(
								userSourceImplementationClassName);
				conf.setUserDetailServiceImplementationClass(clazz);
			} catch (ClassNotFoundException e) {
				throw new ConfigurationException(
						"Failed to load custom user detail service. "
								+ e.getMessage());
			}
		} else {
			// its a pale old white one
			conf.setUserDetailServiceImplementationClass(DefaultUserDetailService.class);
		}

		// get user collection name
		String userColl = (String) options
				.get(MongoConfiguration.USER_COLLECTION);
		if (userColl != null) {
			conf.setUserCollectionName(userColl);
		}

		// get group collection name
		String groupColl = (String) options
				.get(MongoConfiguration.GROUP_COLLECTION);

		if (groupColl != null) {
			conf.setGroupCollectionName(groupColl);
		}

		// additional attributes
		String optAttr = (String) options
				.get(MongoConfiguration.USER_ADDITIONAL_ATTRIBUTES);

		conf.setAdditionalAttributes(parseCommaList(optAttr));

		return conf;
	}

	private List<String> parseCommaList(String commaList) {

		List<String> result = new ArrayList<String>();

		if (commaList != null && !"".equals(commaList)) {
			StringTokenizer st = new StringTokenizer(commaList, ",");
			while (st.hasMoreTokens()) {
				result.add(st.nextToken());
			}
		}

		return result;

	}

}
