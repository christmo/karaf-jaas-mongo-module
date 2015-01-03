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

import java.util.List;

/**
 *
 * The {@link UserDetailService} is used by the MongoLoginModule to retrieve the
 * user information.
 * 
 * @author Niels Bertram
 *
 */
public interface UserDetailService {

	/**
	 * @param username
	 *            the user identifier
	 * 
	 * @return the user info from the mongo source
	 * @throws Exception
	 */
	UserInfo getUserInfo(String username) throws Exception;

	public String getDatasourceURL();

	public void setDatasourceURL(String url);

	public String getDatabaseName();

	public void setDatabaseName(String name);

	/**
	 * @return the name of the users collection used to fetch the user details
	 */
	public String getUserCollectionName();

	/**
	 * @param name
	 *            the name of the user collection
	 */
	public void setUserCollectionName(String name);

	/**
	 * @return the name of the group collection used to fetch the groups a user
	 *         belongs to
	 */
	public String getGroupCollectionName();

	/**
	 * @param name
	 *            the name of the group collection
	 */
	public void setGroupCollectionName(String name);

	/**
	 * @return a list of additional properties that are used to retrieve user
	 *         attributes from the database
	 */
	public List<String> getAdditionalProperties();

	public void setAdditionalProperties(List<String> additionalProperties);

}
