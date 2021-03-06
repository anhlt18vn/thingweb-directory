/********************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
 * 
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the W3C Software Notice and
 * Document License (2015-05-13) which is available at
 * https://www.w3.org/Consortium/Legal/2015/copyright-software-and-document.
 * 
 * SPDX-License-Identifier: EPL-2.0 OR W3C-20150513
 ********************************************************************************/
package org.eclipse.thingweb.directory.rest;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;

import org.eclipse.thingweb.directory.BaseTest;
import org.eclipse.thingweb.directory.rest.CollectionItemServlet;
import org.eclipse.thingweb.directory.rest.CollectionServlet;
import org.eclipse.thingweb.directory.servlet.utils.MockHttpServletRequest;
import org.eclipse.thingweb.directory.servlet.utils.MockHttpServletResponse;
import org.junit.Test;

import com.github.jsonldjava.utils.JsonUtils;

public class CollectionServletTest extends BaseTest {
	
	private static class MockCollectionItemServlet extends CollectionItemServlet {
		
		private final String[] ct = { "text/plain" };
		
		@Override
		protected String[] getAcceptedContentTypes() {
			return ct;
		}
		
	}

	@Test
	public void testDoPost() throws ServletException, IOException {
		CollectionServlet servlet = new CollectionServlet(new MockCollectionItemServlet());
		
		MockHttpServletRequest req = new MockHttpServletRequest("/");
		MockHttpServletResponse resp = new MockHttpServletResponse();

		servlet.doPost(req, resp);
		
		req = new MockHttpServletRequest("/");
		resp = new MockHttpServletResponse();

		servlet.doPost(req, resp);
		
		req = new MockHttpServletRequest("/");
		resp = new MockHttpServletResponse();

		servlet.doGet(req, resp);
		
		byte[] b = resp.getBytes();
		Object o = JsonUtils.fromString(new String(b));
		assertTrue("Unexpected JSON representation (generic resource collection)", o instanceof List);
		assertEquals("The collection does not have the expected number of items", 2, ((List) o).size());
	}

}
