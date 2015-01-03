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
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.StringTokenizer;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.apache.karaf.jaas.boot.principal.GroupPrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.AbstractKarafLoginModule;
import org.apache.karaf.jaas.modules.mongo.internal.DefaultUserDetailService;

/**
 * 
 * A mongo database login module that can be used in the Karaf container.
 * 
 * @author Niels Bertram
 *
 */
public class MongoLoginModule extends AbstractKarafLoginModule {

	private final Logger logger = LoggerFactory
			.getLogger(MongoLoginModule.class);

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

	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler,
			Map<String, ?> sharedState, Map<String, ?> options) {

		super.initialize(subject, callbackHandler, options);

		datasourceURL = (String) options.get(DATASOURCE);
		if (datasourceURL == null || datasourceURL.trim().length() == 0) {
			logger.error("No datasource was specified ");
		}

		dbName = (String) options.get(DATABASE);
		if (dbName == null || dbName.trim().length() == 0) {
			logger.error("No database name was specified ");
		}

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
		UserDetailService userSource = null;

		String userSourceImplementationClass = (String) options
				.get(MONGO_SOURCE_IMPLEMENTATION_CLASS);

		if (userSourceImplementationClass != null) {
			// TODO test this ... uh, ah lets see if we can load it from bundle
			// context
			BundleContext ctx = this.bundleContext;
			Bundle bundle = ctx.getBundle();
			try {
				userSource = (UserDetailService) bundle.loadClass(
						userSourceImplementationClass).newInstance();
			} catch (ClassNotFoundException | InstantiationException
					| IllegalAccessException e) {
				throw new LoginException(
						"Failed to load custom user detail service. "
								+ e.getMessage());
			}
		} else {
			// its a pale old white one
			userSource = new DefaultUserDetailService();
		}

		userSource.setDatasourceURL(datasourceURL);
		userSource.setDatabaseName(dbName);
		// TODO secure login to mongodb
		// http://docs.mongodb.org/ecosystem/tutorial/getting-started-with-java-driver/#authentication-optional

		// get user collection name
		String userColl = (String) options.get(USER_COLLECTION);
		userSource
				.setUserCollectionName((userColl == null ? DEFAULT_USER_COLLECTION
						: userColl));

		// get group collection name
		String groupColl = (String) options.get(GROUP_COLLECTION);
		userSource
				.setGroupCollectionName((groupColl == null ? DEFAULT_GROUP_COLLECTION
						: groupColl));

		// additional attributes
		String optAttr = (String) options.get(USER_ADDITIONAL_ATTRIBUTES);
		userSource.setAdditionalProperties(parseCommaList(optAttr));

		/**************************************************************************
		 * 
		 * get username and password from mongo collection
		 * 
		 **************************************************************************/
		UserInfo userInfo = null;

		try {
			userInfo = userSource.getUserInfo(user);
		} catch (Exception e) {
			throw new LoginException("Failed to retrieve user [" + user
					+ "] from mongo database." + e.getMessage());
		}

		// verify user exists
		if (userInfo == null) {
			throw new LoginException("User [" + user + "] does not exist.");
		}

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
