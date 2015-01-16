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

	public MongoConfiguration getConfiguration();

	/**
	 * This method is called by the {@link MongoLoginModule} immediately after
	 * instantiation of the {@link UserDetailService}.
	 * 
	 * @param configuration
	 *            the mongo database configuration
	 */
	public void setConfiguration(MongoConfiguration configuration);

	/**
	 * @param username
	 *            the user identifier
	 * 
	 * @return the user info from the mongo source
	 * @throws Exception
	 */
	public UserInfo getUserInfo(String username) throws Exception;

	public List<String> getUserNames() throws Exception;

	public UserInfo addUser(UserInfo user) throws Exception;

	public UserInfo updateUser(UserInfo user) throws Exception;

	public void deleteUser(String username) throws Exception;

}
