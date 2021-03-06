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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * A temporary property holder used to pass values between the mongo database
 * and the login module. Only implemented for efficiency reasons.
 * 
 * @author Niels Bertram
 *
 */
public class UserInfo {

	private String name;

	private String password;

	private List<String> groups = new ArrayList<String>();

	public Map<String, String> properties = new HashMap<String, String>();

	public UserInfo() {
	}

	public UserInfo(String name, String password) {
		this.name = name;
		this.password = password;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public UserInfo withName(String name) {
		setName(name);
		return this;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public UserInfo withPassword(String password) {
		setPassword(password);
		return this;
	}

	public List<String> getGroups() {
		return this.groups;
	}

	public void setGroups(List<String> groups) {
		if (groups != null) {
			this.groups.addAll(groups);
		}
	}

	public void addGroup(String group) {
		this.groups.add(group);
	}

	public Map<String, String> getProperties() {
		return this.properties;
	}

	public void addProperty(String key, String value) {
		this.properties.put(key, value);
	}

}
