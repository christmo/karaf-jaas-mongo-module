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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Principal;
import java.util.Set;

import javax.security.auth.Subject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.karaf.jaas.modules.mongo.ExtendedUserPrincipal;

@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
		MediaType.TEXT_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
		MediaType.TEXT_XML })
@Path("/")
public class EchoServiceImpl {

	protected Logger log = LoggerFactory.getLogger(getClass());

	@GET
	@Path("{message}")
	public Response echo(@Context SecurityContext context,
			@PathParam("message") String message) {

		Token token = new Token(message);

		// get access to subject in OSGi
		AccessControlContext acc = AccessController.getContext();
		if (acc == null) {
			token.appendError("access control context is null");
		}

		Subject subject = Subject.getSubject(acc);
		if (subject == null) {
			token.appendError("subject is null");
		} else {
			Set<Principal> principals = subject.getPrincipals();
		}

		Principal user = context.getUserPrincipal();
		if (user == null) {
			token.appendError("principal on security context is null");
		} else if (!(user instanceof ExtendedUserPrincipal)) {
			token.setError("principal on security context is not an extended type but ["
					+ user.getClass().getName() + "]");
		} else {

			ExtendedUserPrincipal extUser = (ExtendedUserPrincipal) user;
			token.setPrincipal(extUser.getName());

			for (String key : extUser.getProperties().keySet()) {
				token.addProperty(key, extUser.getProperties().get(key));
			}

		}

		log.info("User [{}] {} member of users.", user.getName(),
				(context.isUserInRole("users") ? "is" : "is not"));
		log.info("User [{}] {} member of admins.", user.getName(),
				(context.isUserInRole("admins") ? "is" : "is not"));

		return Response.ok(token).build();

	}
}