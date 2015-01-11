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
package org.apache.karaf.jaas.modules.mongo.test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(name = "Token")
@XmlRootElement(name = "Token")
public class Token {

	private String echo;

	private String error;

	private String timestamp;

	private String principal;

	private List<String> groups = new ArrayList<String>();

	private Map<String, String> properties = new HashMap<String, String>();

	public Token() {
	}

	public Token(String message) {
		this.echo = message;
	}

	@XmlAttribute(name = "echo")
	public String getEcho() {
		return echo;
	}

	public void setEcho(String message) {
		this.echo = message;
	}

	@XmlAttribute(name = "timestamp")
	public String getTimestamp() {
		return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
				.format(new Date());
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	@XmlElement(name = "Error", nillable = true)
	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public Token appendError(String error) {
		if (this.error == null) {
			this.error = error;
		} else {
			this.error += "\n" + error;
		}

		return this;
	}

	@XmlElement(name = "Principal")
	public String getPrincipal() {
		return principal;
	}

	public void setPrincipal(String principal) {
		this.principal = principal;
	}

	@XmlElementWrapper(name = "Groups", nillable = true)
	@XmlElement(name = "Group")
	public List<String> getGroups() {
		return groups;
	}

	public void setGroups(List<String> groups) {
		this.groups.addAll(groups);
	}

	public Token addGroup(String group) {
		this.groups.add(group);
		return this;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties.putAll(properties);
	}

	public Token addProperty(String key, String value) {
		this.properties.put(key, value);
		return this;
	}

}
