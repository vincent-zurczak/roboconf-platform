/**
 * Copyright 2014 Linagora, Université Joseph Fourier
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

package net.roboconf.dm.management;

import java.io.File;
import java.util.List;

import junit.framework.Assert;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.model.runtime.Instance.InstanceStatus;
import net.roboconf.messaging.messages.Message;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdAddInstance;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdRemoveInstance;
import net.roboconf.messaging.messages.from_dm_to_agent.MsgCmdSendInstances;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Vincent Zurczak - Linagora
 */
public class ManagedApplicationTest {

	private ManagedApplication ma;
	private TestApplication app;


	@Rule
	public final TemporaryFolder folder = new TemporaryFolder();


	@Before
	public void initializeMa() {

		File f = this.folder.newFolder( "Roboconf_test" );
		this.app = new TestApplication();
		this.ma = new ManagedApplication( this.app, f );
	}


	@Test
	public void testConstructor() throws Exception {
		Assert.assertEquals( this.app, this.ma.getApplication());
	}


	@Test
	public void testGetName() {

		ManagedApplication ma = new ManagedApplication( null, null );
		Assert.assertNull( ma.getName());

		ma = new ManagedApplication( this.app, null );
		Assert.assertEquals( this.app.getName(), ma.getName());
	}


	@Test
	public void testStoreAwaitingMessage() {

		Instance rootInstance = new Instance( "root" );
		Assert.assertEquals( 0, this.ma.getRootInstanceToAwaitingMessages().size());
		this.ma.storeAwaitingMessage( rootInstance, new MsgCmdSendInstances());
		Assert.assertEquals( 1, this.ma.getRootInstanceToAwaitingMessages().size());

		List<Message> messages = this.ma.getRootInstanceToAwaitingMessages().get( rootInstance );
		Assert.assertEquals( 1, messages.size());
		Assert.assertEquals( MsgCmdSendInstances.class, messages.get( 0 ).getClass());

		Instance childInstance = new Instance( "child" );
		InstanceHelpers.insertChild( rootInstance, childInstance );
		this.ma.storeAwaitingMessage( childInstance, new MsgCmdAddInstance( childInstance ));
		Assert.assertEquals( 1, this.ma.getRootInstanceToAwaitingMessages().size());

		Assert.assertEquals( 2, messages.size());
		Assert.assertEquals( MsgCmdSendInstances.class, messages.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdAddInstance.class, messages.get( 1 ).getClass());

		childInstance.setParent( null );
		this.ma.storeAwaitingMessage( childInstance, new MsgCmdRemoveInstance( childInstance ));
		Assert.assertEquals( 2, this.ma.getRootInstanceToAwaitingMessages().size());

		messages = this.ma.getRootInstanceToAwaitingMessages().get( childInstance );
		Assert.assertEquals( 1, messages.size());
		Assert.assertEquals( MsgCmdRemoveInstance.class, messages.get( 0 ).getClass());
	}


	@Test
	public void testRemoveAwaitingMessages() {

		Assert.assertEquals( 0, this.ma.getRootInstanceToAwaitingMessages().size());
		Assert.assertEquals( 0, this.ma.removeAwaitingMessages( new Instance( "whatever" )).size());

		Instance rootInstance = new Instance( "root" );
		this.ma.storeAwaitingMessage( rootInstance, new MsgCmdSendInstances());
		Assert.assertEquals( 1, this.ma.getRootInstanceToAwaitingMessages().size());
		Assert.assertEquals( 1, this.ma.removeAwaitingMessages( rootInstance ).size());
		Assert.assertEquals( 0, this.ma.getRootInstanceToAwaitingMessages().size());
		Assert.assertEquals( 0, this.ma.removeAwaitingMessages( rootInstance ).size());
	}


	@Test
	public void testStoreAndRemove() {

		Instance rootInstance = new Instance( "root" );
		Assert.assertEquals( 0, this.ma.getRootInstanceToAwaitingMessages().size());
		this.ma.storeAwaitingMessage( rootInstance, new MsgCmdSendInstances());
		this.ma.storeAwaitingMessage( rootInstance, new MsgCmdSendInstances());
		Assert.assertEquals( 1, this.ma.getRootInstanceToAwaitingMessages().size());

		List<Message> messages = this.ma.getRootInstanceToAwaitingMessages().get( rootInstance );
		Assert.assertEquals( 2, messages.size());
		Assert.assertEquals( MsgCmdSendInstances.class, messages.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdSendInstances.class, messages.get( 1 ).getClass());

		messages = this.ma.removeAwaitingMessages( rootInstance );
		Assert.assertEquals( 2, messages.size());
		Assert.assertEquals( MsgCmdSendInstances.class, messages.get( 0 ).getClass());
		Assert.assertEquals( MsgCmdSendInstances.class, messages.get( 1 ).getClass());
		Assert.assertEquals( 0, this.ma.getRootInstanceToAwaitingMessages().size());

		this.ma.storeAwaitingMessage( rootInstance, new MsgCmdRemoveInstance( rootInstance ));
		messages = this.ma.getRootInstanceToAwaitingMessages().get( rootInstance );
		Assert.assertEquals( 1, messages.size());
		Assert.assertEquals( MsgCmdRemoveInstance.class, messages.get( 0 ).getClass());
	}


	@Test
	public void testAcknowledgeHeartBeat() {

		Assert.assertNull( this.app.getMySqlVm().getData().get( ManagedApplication.MISSED_HEARTBEATS ));
		this.ma.acknowledgeHeartBeat( this.app.getMySqlVm());
		Assert.assertNull( this.app.getMySqlVm().getData().get( ManagedApplication.MISSED_HEARTBEATS ));

		this.app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		this.ma.acknowledgeHeartBeat( this.app.getMySqlVm());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, this.app.getMySqlVm().getStatus());
		Assert.assertNull( this.app.getMySqlVm().getData().get( ManagedApplication.MISSED_HEARTBEATS ));

		this.app.getMySqlVm().setStatus( InstanceStatus.PROBLEM );
		this.ma.acknowledgeHeartBeat( this.app.getMySqlVm());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, this.app.getMySqlVm().getStatus());
		Assert.assertNull( this.app.getMySqlVm().getData().get( ManagedApplication.MISSED_HEARTBEATS ));

		this.app.getMySqlVm().setStatus( InstanceStatus.PROBLEM );
		this.app.getMySqlVm().getData().put( ManagedApplication.MISSED_HEARTBEATS, "5" );
		this.ma.acknowledgeHeartBeat( this.app.getMySqlVm());
		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, this.app.getMySqlVm().getStatus());
		Assert.assertNull( this.app.getMySqlVm().getData().get( ManagedApplication.MISSED_HEARTBEATS ));
	}


	@Test
	public void testCheckStates() {

		Assert.assertNull( this.app.getMySqlVm().getData().get( ManagedApplication.MISSED_HEARTBEATS ));
		this.app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );
		this.ma.checkStates();
		Assert.assertEquals( "1", this.app.getMySqlVm().getData().get( ManagedApplication.MISSED_HEARTBEATS ));

		this.ma.acknowledgeHeartBeat( this.app.getMySqlVm());
		Assert.assertNull( this.app.getMySqlVm().getData().get( ManagedApplication.MISSED_HEARTBEATS ));

		Assert.assertEquals( InstanceStatus.DEPLOYED_STARTED, this.app.getMySqlVm().getStatus());
		for( int i=0; i<=ManagedApplication.THRESHOLD; i++ ) {
			this.ma.checkStates();
			Assert.assertEquals(
					String.valueOf( i ),
					String.valueOf( i+1 ),
					this.app.getMySqlVm().getData().get( ManagedApplication.MISSED_HEARTBEATS ));
		}

		Assert.assertEquals( InstanceStatus.PROBLEM, this.app.getMySqlVm().getStatus());
		this.app.getMySqlVm().setStatus( InstanceStatus.UNDEPLOYING );
		this.ma.checkStates();
		Assert.assertNull( this.app.getMySqlVm().getData().get( ManagedApplication.MISSED_HEARTBEATS ));

		this.app.getMySqlVm().setStatus( InstanceStatus.DEPLOYING );
		this.ma.checkStates();
		Assert.assertNull( this.app.getMySqlVm().getData().get( ManagedApplication.MISSED_HEARTBEATS ));

		this.app.getMySqlVm().setStatus( InstanceStatus.NOT_DEPLOYED );
		this.ma.checkStates();
		Assert.assertNull( this.app.getMySqlVm().getData().get( ManagedApplication.MISSED_HEARTBEATS ));
	}
}
