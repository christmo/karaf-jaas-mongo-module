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

import java.io.IOException;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import org.apache.karaf.jaas.boot.principal.GroupPrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.AbstractKarafLoginModule;

/**
 * 
 * A mongo database login module that can be used in the Karaf container.
 * 
 * TODO check out <a href=
 * "http://stackoverflow.com/questions/27184877/how-to-add-a-custom-loginmodule-to-karaf-jaas-security-framework"
 * >this</a>
 * 
 * @author Niels Bertram
 *
 */
public class MongoLoginModule extends AbstractKarafLoginModule {

	private final static Logger logger = LoggerFactory
			.getLogger(MongoLoginModule.class);

	private MongoConfigurationBuilder configBuilder;

	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler,
			Map<String, ?> sharedState, Map<String, ?> options) {

		super.initialize(subject, callbackHandler, options);

		// build the database configuration from options
		configBuilder = new MongoConfigurationBuilder(this.bundleContext,
				options);

	}

	@Override
	public boolean login() throws LoginException {

		Callback[] callbacks = new Callback[2];
		callbacks[0] = new NameCallback("Username: ");
		callbacks[1] = new PasswordCallback("Password: ", false);

		try {
			callbackHandler.handle(callbacks);
		} catch (IOException ioe) {
			throw new LoginException(ioe.getMessage());
		} catch (UnsupportedCallbackException uce) {
			throw new LoginException(uce.getMessage()
					+ " not available to obtain information from user");
		}

		// get the username / identity identifier
		user = ((NameCallback) callbacks[0]).getName();
		if (user == null || user.length() < 1) {
			logger.debug("No valid user was not provided.");
			throw new LoginException("No valid user was not provided.");
		}

		// get the provided password
		char[] providedPwdChars = ((PasswordCallback) callbacks[1])
				.getPassword();
		if (providedPwdChars == null) {
			providedPwdChars = new char[0];
		}
		String providedPwd = new String(providedPwdChars);

		/**************************************************************************
		 * 
		 * setup user detail service
		 * 
		 **************************************************************************/

		// TODO check if mongo classes are available on the classpath

		MongoConfiguration config;
		try {
			config = configBuilder.build();
		} catch (ConfigurationException e) {
			throw new LoginException("Failed to configure login module: "
					+ e.getMessage());
		}

		UserDetailService userSource = null;
		try {
			userSource = config.getUserDetailServiceImplementationClass()
					.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new LoginException(
					"Failed to load custom user detail service. "
							+ e.getMessage());
		}

		userSource.setConfiguration(config);

		/**************************************************************************
		 * 
		 * get username and password from mongo collection
		 * 
		 **************************************************************************/
		UserInfo userInfo = null;

		try {
			userInfo = userSource.getUserInfo(user);
		} catch (Exception e) {
			logger.error("Failed to get user from mongodb.", e);
			throw new LoginException("Failed to retrieve user [" + user
					+ "] from mongo database." + e.getMessage());
		}

		// verify user exists
		if (userInfo == null) {
			throw new LoginException("User [" + user + "] does not exist.");
		}

		// TODO add password encryption
		// verify password matches
		if (!checkPassword(providedPwd, userInfo.getPassword())) {
			throw new LoginException("User [" + user
					+ "] password does not match.");
		}

		/**************************************************************************
		 * 
		 * populate the final principal
		 * 
		 **************************************************************************/

		// add the user principal to the security context
		principals.add(createUserPrincipal(userInfo));

		// populate roles
		for (String role : userInfo.getGroups()) {
			principals.add(new GroupPrincipal(role));
		}

		// release mongo resources, could do this when querying initial user and
		// role as hashmaps ???

		return true;
	}

	@Override
	public boolean abort() throws LoginException {
		if (debug) {
			logger.debug("abort login");
		}
		return true;
	}

	@Override
	public boolean logout() throws LoginException {
		subject.getPrincipals().removeAll(principals);
		principals.clear();
		if (debug) {
			logger.debug("logout");
		}
		return true;
	}

	/**
	 * @param userInfo
	 *            the user info to be used to construct the
	 *            {@link UserPrincipal}
	 * 
	 * @return a principal for the supplied {@link UserInfo}
	 */
	private UserPrincipal createUserPrincipal(UserInfo userInfo) {

		ExtendedUserPrincipal p = new ExtendedUserPrincipal(userInfo.getName());

		if (!userInfo.getProperties().isEmpty()) {
			p.setProperties(userInfo.getProperties());
		}

		return p;
	}

}
