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

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * The holder for prepared mongodb configuration that can be supplied to the
 * {@link UserDetailService} and also the {@link MongoBackingEngine}.
 * 
 * @author Niels Bertram
 *
 */
public class MongoConfiguration {

	/**
	 * The mongodb database connection configuration. To configure multi host
	 * connections such as mongo replica sets or shards just separate the server
	 * addresses with a comma e.g. <code>loclahost:27017,localhost:27018</code>
	 */
	public static final String DATASOURCE = "mongo.db.url";

	/**
	 * The name of the mongo database used to connect to.
	 */
	public static final String DATABASE = "mongo.db.name";

	/**
	 * The name of the users collection, default is
	 * {@link #DEFAULT_USER_COLLECTION}.
	 */
	public static final String USER_COLLECTION = "mongo.user.collection.name";

	/**
	 * The name of the groups collection, default is
	 * {@link #DEFAULT_GROUP_COLLECTION}.
	 */
	public static final String GROUP_COLLECTION = "mongo.group.collection.name";

	/**
	 * Can be used to retrieve additional user attributes. The login module
	 * currently only supports string values.
	 */
	public static final String USER_ADDITIONAL_ATTRIBUTES = "mongo.user.attributes";

	public static final String DEFAULT_USER_COLLECTION = "users";
	public static final String DEFAULT_GROUP_COLLECTION = "groups";

	/**
	 * Extended Configuration <br/>
	 * 
	 * Configure the implementation class to deliver user information and
	 * construct the user principal.
	 */
	public static final String MONGO_SOURCE_IMPLEMENTATION_CLASS = "mongo.source.implementation.class";

	private String datasourceURL;

	private String dbName;

	private Class<? extends UserDetailService> userDetailServiceImplementationClass;

	private String userCollectionName = DEFAULT_USER_COLLECTION;

	private String groupCollectionName = DEFAULT_GROUP_COLLECTION;

	private List<String> additionalAttributes = new ArrayList<String>();

	public String getDatabaseName() {
		return dbName;
	}

	public void setDatabaseName(String dbName) {
		this.dbName = dbName;
	}

	public String getDatasourceURL() {
		return datasourceURL;
	}

	public void setDatasourceURL(String datasourceURL) {
		this.datasourceURL = datasourceURL;
	}

	public Class<? extends UserDetailService> getUserDetailServiceImplementationClass() {
		return userDetailServiceImplementationClass;
	}

	public void setUserDetailServiceImplementationClass(
			Class<? extends UserDetailService> userDetailServiceImplementationClass) {
		this.userDetailServiceImplementationClass = userDetailServiceImplementationClass;
	}

	public String getUserCollectionName() {
		return userCollectionName;
	}

	public void setUserCollectionName(String userCollectionName) {
		this.userCollectionName = userCollectionName;
	}

	public String getGroupCollectionName() {
		return groupCollectionName;
	}

	public void setGroupCollectionName(String groupCollectionName) {
		this.groupCollectionName = groupCollectionName;
	}

	public List<String> getAdditionalAttributes() {
		return additionalAttributes;
	}

	public void setAdditionalAttributes(List<String> additionalAttributes) {
		this.additionalAttributes.addAll(additionalAttributes);
	}

}
