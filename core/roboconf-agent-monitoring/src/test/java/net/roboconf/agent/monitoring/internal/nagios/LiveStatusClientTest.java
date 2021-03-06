/**
 * Copyright 2014-2017 Linagora, Université Joseph Fourier, Floralis
 *
 * The present code is developed in the scope of the joint LINAGORA -
 * Université Joseph Fourier - Floralis research program and is designated
 * as a "Result" pursuant to the terms and conditions of the LINAGORA
 * - Université Joseph Fourier - Floralis research program. Each copyright
 * holder of Results enumerated here above fully & independently holds complete
 * ownership of the complete Intellectual Property rights applicable to the whole
 * of said Results, and may freely exploit it in any manner which does not infringe
 * the moral rights of the other copyright holders.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.roboconf.agent.monitoring.internal.nagios;

import java.io.IOException;

import org.junit.Assert;

import org.junit.Test;

/**
 * A part of the tests for {@link LiveStatusClient} is in {@link NagiosHandlerTest}.
 * @author Vincent Zurczak - Linagora
 */
public class LiveStatusClientTest {

	@Test
	public void testConstructor() {

		LiveStatusClient client = new LiveStatusClient( null, -45 );
		Assert.assertEquals( LiveStatusClient.DEFAULT_HOST, client.host );
		Assert.assertEquals( LiveStatusClient.DEFAULT_PORT, client.port );

		client = new LiveStatusClient( null, 45 );
		Assert.assertEquals( LiveStatusClient.DEFAULT_HOST, client.host );
		Assert.assertEquals( 45, client.port );

		client = new LiveStatusClient( "toto", -45 );
		Assert.assertEquals( "toto", client.host );
		Assert.assertEquals( LiveStatusClient.DEFAULT_PORT, client.port );

		client = new LiveStatusClient( "titi", 1245 );
		Assert.assertEquals( "titi", client.host );
		Assert.assertEquals( 1245, client.port );
	}


	@Test( expected = IOException.class )
	public void testFailedConnection() throws Exception {

		LiveStatusClient client = new LiveStatusClient( null, -45 );
		client.queryLivestatus( "whatever" );
	}
}
