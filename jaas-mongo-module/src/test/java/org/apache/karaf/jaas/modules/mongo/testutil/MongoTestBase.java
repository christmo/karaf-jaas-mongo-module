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

import java.io.IOException;
import java.net.UnknownHostException;

import com.mongodb.MongoClient;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;

/**
 * 
 * The underlying test base that manages the mongo database process.
 * 
 * @author Niels Bertram
 * 
 */
public abstract class MongoTestBase {

	private String mongodHost = "localhost";
	private int mongodPort = 27017;

	protected MongodExecutable mongoExec = null;
	protected MongodProcess mongod = null;
	protected MongoClient mongoClient = null;

	/**
	 * 
	 * Starts a mongodb instance and returns a working client to it.
	 * 
	 * @return the client that can connect to the working instance
	 * 
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	protected MongoClient startMongo() throws UnknownHostException, IOException {

		// start embedded mongo
		MongodStarter runtime = MongodStarter.getDefaultInstance();

		IMongodConfig mongodConfig = new MongodConfigBuilder()
				.version(Version.Main.PRODUCTION)
				.net(new Net(mongodPort, Network.localhostIsIPv6())).build();

		mongoExec = runtime.prepare(mongodConfig);
		mongod = mongoExec.start();

		// setup the client for the test
		mongoClient = new MongoClient(getMongoHost(), getMongoPort());

		return mongoClient;

	}

	protected void shutdownMongo() {

		if (mongoClient != null) {
			mongoClient.close();
		}

		if (mongod != null)
			mongod.stop();

		if (mongoExec != null)
			mongoExec.stop();

	}

	public String getMongoHost() {
		return mongodHost;
	}

	protected void setMogoHost(String host) {
		this.mongodHost = host;
	}

	public int getMongoPort() {
		return mongodPort;
	}

	protected void setMogoPort(int port) {
		this.mongodPort = port;
	}

	protected MongoClient getMongoClient() {
		return mongoClient;
	}

	protected MongoTestBase withMongoHost(String host) {
		setMogoHost(host);
		return this;
	}

	protected MongoTestBase withMongoPort(int port) {
		setMogoPort(port);
		return this;
	}

}
