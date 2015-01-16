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

import java.util.LinkedList;
import java.util.List;

import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.mongo.internal.DefaultUserDetailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * A backing engine implementation for the {@link MongoLoginModule}
 * 
 * @author Niels Bertram
 *
 */
public class MongoBackingEngine implements BackingEngine {

	private static final transient Logger log = LoggerFactory
			.getLogger(MongoBackingEngine.class);

	UserDetailService service;

	public MongoBackingEngine() {
		log.error("Cant call default constructor, use factory");
	}

	public MongoBackingEngine(MongoConfiguration configuration) {
		// we just assume the default implementation will do here
		this.service = new DefaultUserDetailService(configuration);
	}

	@Override
	public void addUser(String username, String password) {
		log.info("add {}:{}", username, password);

		UserInfo userInfo = new UserInfo(username, password);
		try {
			service.addUser(userInfo);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}

	}

	@Override
	public void deleteUser(String username) {
		log.info("delete {}", username);

		try {
			service.deleteUser(username);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}

	}

	@Override
	public List<UserPrincipal> listUsers() {
		log.debug("list users");

		try {
			List<String> usernames = service.getUserNames();
			List<UserPrincipal> users = new LinkedList<UserPrincipal>();

			for (String username : usernames) {
				users.add(new UserPrincipal(username));
			}

			return users;
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}

	}

	@Override
	public List<RolePrincipal> listRoles(UserPrincipal user) {
		log.debug("list roles for {}", user.getName());

		try {

			UserInfo userInfo = service.getUserInfo(user.getName());
			List<RolePrincipal> roles = new LinkedList<RolePrincipal>();

			for (String group : userInfo.getGroups()) {
				roles.add(new RolePrincipal(group));
			}

			return roles;

		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}

	}

	@Override
	public void addRole(String username, String role) {
		log.debug("add role {} to {}", role, username);

		try {

			// first see if user exists
			UserInfo userInfo = service.getUserInfo(username);
			if (userInfo == null) {
				throw new RuntimeException("User [" + username
						+ "] does not exist.");
			}

			userInfo.getGroups().add(role);
			service.updateUser(userInfo);

		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}

	}

	@Override
	public void deleteRole(String username, String role) {
		log.debug("delete role {} from {}", role, username);

		try {
			// first see if user exists
			UserInfo userInfo = service.getUserInfo(username);
			if (userInfo == null) {
				throw new RuntimeException("User [" + username
						+ "] does not exist.");
			}

			userInfo.getGroups().remove(role);
			service.updateUser(userInfo);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}

	}

}
