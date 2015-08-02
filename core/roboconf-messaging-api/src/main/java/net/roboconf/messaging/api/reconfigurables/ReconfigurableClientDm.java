/**
 * Copyright 2014-2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.messaging.api.reconfigurables;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.messaging.api.client.IDmClient;
import net.roboconf.messaging.api.client.ListenerCommand;
import net.roboconf.messaging.api.factory.MessagingClientFactory;
import net.roboconf.messaging.api.factory.MessagingClientFactoryRegistry;
import net.roboconf.messaging.api.internal.client.dismiss.DismissClientDm;
import net.roboconf.messaging.api.messages.Message;
import net.roboconf.messaging.api.processors.AbstractMessageProcessor;

/**
 * @author Vincent Zurczak - Linagora
 * @author Pierre Bourret - Université Joseph Fourier
 */
public class ReconfigurableClientDm extends ReconfigurableClient<IDmClient> implements IDmClient {

	// Methods inherited from ReconfigurableClient

	@Override
	protected IDmClient createMessagingClient( String factoryName )
			throws IOException {
		IDmClient client = null;
		MessagingClientFactoryRegistry registry = getRegistry();
		if (registry != null) {
			MessagingClientFactory factory = registry.getMessagingClientFactory(factoryName);
			if (factory != null) {
				client = factory.createDmClient(this);
			}
		}
		return client;
	}


	@Override
	protected void openConnection( IDmClient newMessagingClient ) throws IOException {
		newMessagingClient.openConnection();
	}


	@Override
	protected IDmClient getDismissedClient() {
		return new DismissClientDm();
	}


	@Override
	protected void configureMessageProcessor( AbstractMessageProcessor<IDmClient> messageProcessor ) {
		messageProcessor.setMessagingClient( this );
	}


	// Wrapping of the internal client


	@Override
	public void setMessageQueue( LinkedBlockingQueue<Message> messageQueue ) {
		getMessagingClient().setMessageQueue( messageQueue );
	}


	@Override
	public boolean isConnected() {
		return getMessagingClient().isConnected();
	}


	@Override
	public void openConnection() throws IOException {
		getMessagingClient().openConnection();
	}


	@Override
	public void closeConnection() throws IOException {
		final IDmClient toClose = resetInternalClient();
		if (toClose != null) {
			toClose.closeConnection();
		}
	}


	@Override
	public void sendMessageToAgent( Application application, Instance instance, Message message ) throws IOException {
		getMessagingClient().sendMessageToAgent( application, instance, message );
	}


	@Override
	public void listenToAgentMessages( Application application, ListenerCommand command ) throws IOException {
		getMessagingClient().listenToAgentMessages( application, command );
	}


	@Override
	public void sendMessageToTheDm( Message msg ) throws IOException {
		getMessagingClient().sendMessageToTheDm( msg );
	}


	@Override
	public void listenToTheDm( ListenerCommand command ) throws IOException {
		getMessagingClient().listenToTheDm( command );
	}


	@Override
	public void deleteMessagingServerArtifacts( Application application ) throws IOException {
		getMessagingClient().deleteMessagingServerArtifacts( application );
	}


	@Override
	public void propagateAgentTermination( Application application, Instance rootInstance ) throws IOException {
		getMessagingClient().propagateAgentTermination( application, rootInstance );
	}
}