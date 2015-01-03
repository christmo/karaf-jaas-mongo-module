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
package org.apache.karaf.jaas.modules.mongo.internal;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.karaf.jaas.modules.mongo.MongoLoginModule;
import org.apache.karaf.jaas.modules.mongo.UserDetailService;
import org.apache.karaf.jaas.modules.mongo.UserInfo;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;

/**
 * A basic implementation of the {@link UserDetailService}.
 * 
 * @author Niels Bertram
 *
 */
public class DefaultUserDetailService implements UserDetailService {

	private static ExpiringMap<String, MongoClient> createdClients = new ExpiringMap<String, MongoClient>();

	static {

		createdClients
				.addExpirationListener(new MongoClientExpirationListener());

		createdClients.getExpirer().startExpiringIfNotStarted();

	}

	private String datasourceURL;

	private String databaseName;

	private String userCollectionName;

	private String groupCollectionName;

	private List<String> additionalProperties = new ArrayList<String>();

	public String getDatasourceURL() {
		return datasourceURL;
	}

	public void setDatasourceURL(String datasourceURL) {
		this.datasourceURL = datasourceURL;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	@Override
	public String getUserCollectionName() {
		return userCollectionName;
	}

	@Override
	public void setUserCollectionName(String userCollectionName) {
		this.userCollectionName = userCollectionName;
	}

	@Override
	public String getGroupCollectionName() {
		return this.groupCollectionName;
	}

	@Override
	public void setGroupCollectionName(String name) {
		this.groupCollectionName = name;
	}

	@Override
	public List<String> getAdditionalProperties() {
		return additionalProperties;
	}

	@Override
	public void setAdditionalProperties(List<String> additionalProperties) {
		this.additionalProperties.addAll(additionalProperties);
	}

	@Override
	public UserInfo getUserInfo(String username) throws Exception {

		UserInfo userInfo = null;

		// create a db connection
		MongoClient client = getClient();
		DB db = client.getDB(databaseName);

		DBCollection users = db
				.getCollection(MongoLoginModule.DEFAULT_USER_COLLECTION);

		// populate user
		DBObject userQuery = new BasicDBObject("username", username);

		BasicDBObjectBuilder userProjection = new BasicDBObjectBuilder()
				.add("_id", 0).add("username", 1).add("passwordHash", 1);

		// also add all custom user fields
		for (String prop : additionalProperties) {
			userProjection.add(prop, 1);
		}

		DBObject keys = userProjection.get();

		DBObject user = users.findOne(userQuery, keys);
		// if nothing comes back just return empty handed
		if (user == null) {
			return null;
		}

		userInfo = new UserInfo((String) user.get("username"),
				(String) user.get("passwordHash"));

		for (String prop : additionalProperties) {
			userInfo.addProperty(prop, (String) user.get(prop));
		}

		// populate group
		DBCollection groups = db
				.getCollection(MongoLoginModule.DEFAULT_GROUP_COLLECTION);

		DBObject groupQuery = new BasicDBObject("members", username);

		DBObject groupProjection = new BasicDBObjectBuilder().add("name", 1)
				.add("_id", 0).get();

		DBCursor gc = groups.find(groupQuery, groupProjection);
		while (gc.hasNext()) {
			DBObject group = gc.next();
			userInfo.addGroup((String) group.get("name"));
		}
		gc.close();

		return userInfo;

	}

	/**
	 * Needed to ensure we do not continuously creating mongo clients.
	 * MongoClient itself is thread safe.
	 * 
	 * TODO check if synchronsied is really needed
	 * 
	 * @param url
	 * @return
	 * @throws NumberFormatException
	 * @throws UnknownHostException
	 */
	protected synchronized MongoClient getClient()
			throws NumberFormatException, UnknownHostException {

		String hash = calculateDBHash();

		if (createdClients.containsKey(hash)) {
			return createdClients.get(hash);
		} else {
			MongoClient client = createClient();
			createdClients.put(hash, client);
			return client;
		}

	}

	protected MongoClient createClient() throws NumberFormatException,
			UnknownHostException {

		List<ServerAddress> servers = new ArrayList<ServerAddress>();
		StringTokenizer st = new StringTokenizer(datasourceURL, ",");
		while (st.hasMoreTokens()) {
			String serverURL = st.nextToken();
			if (serverURL.indexOf(':') == -1) {
				servers.add(new ServerAddress(serverURL));
			} else {
				String host = serverURL.substring(0, serverURL.indexOf(':'));
				String port = serverURL.substring(serverURL.indexOf(':') + 1);
				servers.add(new ServerAddress(host, Integer.parseInt(port)));
			}
		}
		return new MongoClient(servers);

	}

	private String calculateDBHash() {
		return "/" + this.datasourceURL + "/"; // for future password or cert
												// creds we need hashing too
	}
}
