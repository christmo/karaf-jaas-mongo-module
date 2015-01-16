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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.karaf.jaas.modules.mongo.MongoConfiguration;
import org.apache.karaf.jaas.modules.mongo.UserDetailService;
import org.apache.karaf.jaas.modules.mongo.UserInfo;

import com.mongodb.BasicDBList;
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

	protected transient Logger log = LoggerFactory.getLogger(getClass());

	private static ExpiringMap<String, MongoClient> createdClients = new ExpiringMap<String, MongoClient>();

	static {

		createdClients
				.addExpirationListener(new MongoClientExpirationListener());

		createdClients.getExpirer().startExpiringIfNotStarted();

	}

	private MongoConfiguration configuration;

	private static DBObject ROLE_PROJECTION = BasicDBObjectBuilder.start()
			.add("_id", 0).add("name", 1).add("members", 1).get();

	public DefaultUserDetailService() {
	}

	public DefaultUserDetailService(MongoConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public MongoConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	public void setConfiguration(MongoConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public UserInfo getUserInfo(String username) throws Exception {

		DB db = getDB();

		DBCollection users = db.getCollection(configuration
				.getUserCollectionName());

		// populate user
		DBObject userQuery = new BasicDBObject("username", username);

		BasicDBObjectBuilder userProjectionBuilder = BasicDBObjectBuilder
				.start().add("_id", 0).add("username", 1)
				.add("passwordHash", 1);

		// also add all custom user fields
		for (String prop : configuration.getAdditionalAttributes()) {
			userProjectionBuilder.add(prop, 1);
		}

		DBObject user = users.findOne(userQuery, userProjectionBuilder.get());
		// if nothing comes back just return empty handed
		if (user == null) {
			return null;
		}

		UserInfo userInfo = new UserInfo().withName(
				(String) user.get("username")).withPassword(
				(String) user.get("passwordHash"));

		for (String prop : configuration.getAdditionalAttributes()) {

			// only add if property is actually present in the database
			if (user.containsField(prop)) {
				userInfo.addProperty(prop, (String) user.get(prop));
			}

		}

		// populate group
		DBCollection groups = db.getCollection(configuration
				.getGroupCollectionName());

		DBObject groupQuery = new BasicDBObject("members", username);

		DBCursor gc = groups.find(groupQuery, BasicDBObjectBuilder.start()
				.append("_id", 0).append("name", 1).get());

		while (gc.hasNext()) {
			DBObject group = gc.next();
			userInfo.addGroup((String) group.get("name"));
		}
		gc.close();

		return userInfo;

	}

	public java.util.List<String> getUserNames() throws Exception {

		List<String> result = new LinkedList<String>();

		DBCollection users = getDB().getCollection(
				configuration.getUserCollectionName());

		DBObject userProjection = new BasicDBObjectBuilder().add("_id", 0)
				.add("username", 1).get();

		DBCursor uc = users.find(null, userProjection);
		while (uc.hasNext()) {
			DBObject group = uc.next();
			result.add((String) group.get("username"));
		}
		uc.close();

		return result;
	}

	@Override
	public UserInfo addUser(UserInfo user) throws Exception {

		DB db = getDB();

		DBCollection users = db.getCollection(configuration
				.getUserCollectionName());

		DBCollection roles = db.getCollection(configuration
				.getGroupCollectionName());

		DBObject storedUser = users.findOne(new BasicDBObject().append(
				"username", user.getName()));

		if (storedUser == null) {

			users.insert(BasicDBObjectBuilder.start("username", user.getName())
					.append("passwordHash", user.getPassword()).get());

		} else {
			// will not do anything here
		}

		for (String role : user.getGroups()) {

			DBObject roleQuery = new BasicDBObject("name", role);

			// roles are unique by name
			DBObject roleData = roles.findOne(roleQuery, ROLE_PROJECTION);

			if (roleData == null) {
				// add role with user as first member
				BasicDBList members = new BasicDBList();
				members.add(user.getName());
				roleData = BasicDBObjectBuilder.start().add("name", role)
						.add("members", members).get();

				roles.insert(roleData);

			} else {

				// add user to group if not already in the role's member list
				Object mo = roleData.get("members");
				if (mo == null) {

					// TODO what here?
					BasicDBObject updateObject = new BasicDBObject().append(
							"$push",
							new BasicDBObject("members", user.getName()));

					roles.update(roleQuery, updateObject);

				} else if (mo != null && mo instanceof List) {

					// if user is in group already we dont need to do anything
					List<?> existingMembers = (List<?>) mo;

					if (!existingMembers.contains(user.getName())) {
						// push this user to the members list
						BasicDBObject updateObject = new BasicDBObject()
								.append("$push", new BasicDBObject("members",
										user.getName()));

						roles.update(roleQuery, updateObject);

					}

				} else {
					log.warn(
							"The members collection of group [{}] is not a list but of type [{}].",
							role, mo.getClass().getName());
				}

			}

		}

		return user;
	}

	@Override
	public UserInfo updateUser(UserInfo user) throws Exception {
		// FIXME review this
		return addUser(user);
	}

	@Override
	public void deleteUser(String username) throws Exception {

		DBCollection users = getDB().getCollection(
				configuration.getUserCollectionName());

		DBObject userQuery = new BasicDBObject("username", username);
		users.remove(userQuery);

		// / FIXME also remove from all role definitions

	}

	private DB getDB() throws NumberFormatException, UnknownHostException {
		return getClient().getDB(configuration.getDatabaseName());
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
		StringTokenizer st = new StringTokenizer(
				configuration.getDatasourceURL(), ",");
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
		return "/" + this.configuration.getDatasourceURL() + "/";
		// TODO for future password or cert creds we need hashing too
	}
}
