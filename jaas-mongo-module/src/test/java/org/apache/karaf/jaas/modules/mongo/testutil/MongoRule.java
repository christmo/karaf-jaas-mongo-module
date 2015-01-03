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
package org.apache.karaf.jaas.modules.mongo.testutil;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.mongodb.MongoClient;

/**
 * 
 * This rule will start the mongo server before a test starts and ends the mongo
 * server when the test has finished.
 * 
 * <pre>
 * import static org.junit.Assert.*;
 * import org.junit.Rule;
 * import org.junit.Test;
 * 
 * import com.mongodb.MongoClient;
 * 
 * public class MongoRuleTest {
 * 
 *   {@literal @}Rule
 *   // this will start and stop mongodb for every test
 *   public MongoRule mongo = new MongoRule();
 * 
 *   {@literal @}Test
 *   public void testSomethingWithMongoRule() {
 * 
 *     MongoClient client = mongo.getMongoClient();
 *     assertNotNull(client);
 *     // whatever needs to be done in mongo
 * 
 *   }
 * 
 *   {@literal @}Test
 *   {@literal @}WithoutMongo
 *   public void testSomethingWithoutMongoRule() {
 * 
 *     MongoClient client = mongo.getMongoClient();
 *     assertNull(client);
 *     // whatever needs to be done without mongo
 * 
 *   }
 * 
 * }
 * </pre>
 * 
 * @author Niels Bertram
 * 
 */
public class MongoRule implements TestRule {

	public static final String UNIT_TEST_DB = "unitdb";

	private final MongoTestBase testCase;

	public MongoRule() {
		this(new MongoTestBase() {
		});
	}

	public MongoRule(final String host, int port) {
		this(new MongoTestBase() {
		}.withMongoHost(host).withMongoPort(port));
	}

	public MongoRule(MongoTestBase testCase) {
		this.testCase = testCase;
	}

	@Override
	public Statement apply(final Statement base, Description description) {

		if (description.getAnnotation(WithoutMongo.class) != null) // skip.
		{
			return base;
		}
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				testCase.startMongo();
				before();
				try {
					base.evaluate();
				} finally {
					after();
					testCase.shutdownMongo();
				}
			}
		};

	}

	/**
	 * May be overridden in the implementation to do stuff <em>after</em> the
	 * embedded test case is set up but <em>before</em> the current test is
	 * actually run.
	 * 
	 * @throws Throwable
	 */
	protected void before() throws Throwable {

	}

	/**
	 * May be overridden in the implementation to do stuff after the current
	 * test was run but <em>before<em> the mongo server is shutdown.
	 */
	protected void after() {

	}

	/**
	 * 
	 * 
	 * @return The host port used to start the mongo server
	 */
	public String getMongoHost() {
		return testCase.getMongoHost();
	}

	protected void setMogoHost(String host) {
		this.testCase.setMogoHost(host);
	}

	public int getMongoPort() {
		return testCase.getMongoPort();
	}

	protected void setMogoPort(int port) {
		this.testCase.setMogoPort(port);
	}

	public MongoClient getMongoClient() {
		return this.testCase.getMongoClient();
	}

	public MongoRule host(String host) {
		setMogoHost(host);
		return this;
	}

	public MongoRule port(int port) {
		setMogoPort(port);
		return this;
	}

}
