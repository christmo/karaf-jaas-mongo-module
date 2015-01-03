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
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import org.apache.karaf.jaas.modules.mongo.MongoLoginModule;
import org.apache.karaf.jaas.modules.mongo.internal.DefaultUserDetailService;
import org.apache.karaf.jaas.modules.mongo.testutil.MongoRule;

import static org.mockito.Mockito.*;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class MongoLoginModuleTest {

	protected static final Logger logger = LoggerFactory
			.getLogger(MongoLoginModuleTest.class);

	@Rule
	// this will start and stop mongodb for every test
	public MongoRule mongo = new MongoRule().port(27099);

	@Mock
	public BundleContext mockBundleContext;

	@Mock
	Bundle mockBundle;

	@Mock
	CallbackHandler mockCallbackHandler;

	@Before
	public void initMocks() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testSuccessfulLogin() throws LoginException,
			ClassNotFoundException, IOException, UnsupportedCallbackException {

		final String testUser = "berti";
		final char[] testPassword = "testme".toCharArray();

		// setup mocks
		when(mockBundle.loadClass(anyString())).thenReturn(
				(Class) DefaultUserDetailService.class);

		when(mockBundleContext.getBundle()).thenReturn(mockBundle);

		doAnswer(new Answer<Void>() {

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {

				Callback[] callbacks = invocation.getArgumentAt(0,
						Callback[].class);

				((NameCallback) callbacks[0]).setName(testUser);
				((PasswordCallback) callbacks[1]).setPassword(testPassword);

				return null;
			}

		}).when(mockCallbackHandler).handle(any(Callback[].class));

		prepareTestDatabase(testUser, testPassword);

		MongoLoginModule mod = new MongoLoginModule();

		Map<String, Object> options = new HashMap<String, Object>();

		options.put(BundleContext.class.getName(), mockBundleContext);

		options.put(MongoLoginModule.DATASOURCE, mongo.getMongoHost() + ":"
				+ mongo.getMongoPort());

		options.put(MongoLoginModule.DATABASE, MongoRule.UNIT_TEST_DB);

		options.put(MongoLoginModule.USER_COLLECTION,
				MongoLoginModule.DEFAULT_USER_COLLECTION);
		options.put(MongoLoginModule.USER_ADDITIONAL_ATTRIBUTES, "email,phone");

		options.put(MongoLoginModule.GROUP_COLLECTION,
				MongoLoginModule.DEFAULT_GROUP_COLLECTION);

		options.put("debug", "true");
		options.put("detailed.login.exception", "true");

		Subject subject = new Subject();

		mod.initialize(subject, mockCallbackHandler, null, options);

		mod.login();

	}

	public void prepareTestDatabase(String testUser, char[] testPassword) {

		MongoClient client = mongo.getMongoClient();

		DB db = client.getDB(MongoRule.UNIT_TEST_DB);

		DBCollection users = db
				.getCollection(MongoLoginModule.DEFAULT_USER_COLLECTION);
		DBCollection groups = db
				.getCollection(MongoLoginModule.DEFAULT_GROUP_COLLECTION);

		// first insert a simple user
		users.insert(new BasicDBObjectBuilder().add("username", testUser)
				.add("email", testUser + "@zz.zz").add("phone", "0733446767")
				.add("passwordHash", new String(testPassword)).get());

		users.insert(new BasicDBObjectBuilder().add("username", "fred")
				.add("email", "fred@zz.zz").add("phone", "0822416957")
				.add("passwordHash", new String(testPassword)).get());

		// / insert a simple group with members

		BasicDBList usersGrp = new BasicDBList();
		usersGrp.add(testUser);
		usersGrp.add("fred");

		groups.insert(new BasicDBObject("name", "users").append("members",
				usersGrp));

		BasicDBList adminGrp = new BasicDBList();
		adminGrp.add(testUser);

		groups.insert(new BasicDBObject("name", "admins").append("members",
				adminGrp));

	}

}
