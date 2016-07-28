/**
 * Copyright 2015-2016 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.internal.api.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import net.roboconf.core.Constants;
import net.roboconf.core.internal.tests.TestApplication;
import net.roboconf.core.model.beans.Application;
import net.roboconf.core.model.beans.ApplicationTemplate;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.TargetUsageItem;
import net.roboconf.core.model.runtime.TargetWrapperDescriptor;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.internal.utils.ConfigurationUtils;
import net.roboconf.dm.management.api.IConfigurationMngr;
import net.roboconf.dm.management.api.ITargetsMngr;
import net.roboconf.dm.management.exceptions.UnauthorizedActionException;

/**
 * @author Vincent Zurczak - Linagora
 */
public class TargetsMngrImplTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private ITargetsMngr mngr;
	private IConfigurationMngr configurationMngr;


	@Before
	public void prepareMngr() throws Exception {

		this.configurationMngr = Mockito.mock( IConfigurationMngr.class );
		Mockito.when( this.configurationMngr.getWorkingDirectory()).thenReturn( this.folder.newFolder());

		this.mngr = new TargetsMngrImpl( this.configurationMngr );
	}


	@Test
	public void testNormalCrudScenarios() throws Exception {

		Assert.assertNull( this.mngr.findRawTargetProperties( "whatever" ));
		String targetId = this.mngr.createTarget( "id: tid\nprop: ok\nhandler: h" );
		Assert.assertEquals( "tid", targetId );

		String newTargetId = this.mngr.createTarget( "ok: ok\nid: tok\nhandler: h" );
		Assert.assertNotNull( newTargetId );

		String props = this.mngr.findRawTargetProperties( targetId );
		Assert.assertEquals( "prop: ok\nhandler: h", props );

		props = this.mngr.findRawTargetProperties( newTargetId );
		Assert.assertEquals( "ok: ok\nhandler: h", props.trim());

		this.mngr.updateTarget( targetId, "prop2: ko\nprop1: done" );
		props = this.mngr.findRawTargetProperties( targetId );
		Assert.assertEquals( "prop2: ko\nprop1: done", props );

		this.mngr.deleteTarget( targetId );
		Assert.assertNull( this.mngr.findRawTargetProperties( targetId ));
	}


	@Test
	public void testCreateTarget_targetIdIsRemoveCorrectly() throws Exception {

		String[] properties = {
				"id: tid\nprop: ok\nhandler: h\nprop-after: ok",
				"id: tid\r\nprop: ok\nhandler: h\nprop-after: ok",
				"prop: ok\nhandler: h\nprop-after: ok\nid = tid",
				"prop: ok\nhandler: h\r\nprop-after: ok\r\nid :tid",
				"prop: ok\nhandler: h\r\nprop-after: ok\r\nid :tid\n\n",
				"prop: ok\r\nid :tid\nhandler: h\nprop-after: ok",
				"prop: ok\r\nid :tid\nhandler: h\nprop-after: ok\n",
		};

		for( String s : properties ) {
			String targetId = this.mngr.createTarget( s );
			Assert.assertEquals( s, "tid", targetId );
			Assert.assertEquals( 1, ((TargetsMngrImpl) this.mngr).targetIds.size());

			String props = this.mngr.findRawTargetProperties( targetId );
			Assert.assertEquals( s, "prop: ok\nhandler: h\nprop-after: ok", props.replace( "\r", "" ).trim());

			this.mngr.deleteTarget( targetId );
			Assert.assertEquals( 0, ((TargetsMngrImpl) this.mngr).targetIds.size());
		}
	}


	@Test
	public void testCreateTargetFromFile() throws Exception {

		File f = this.folder.newFile();
		Utils.writeStringInto( "id: tid\nprop: value\nhandler: h", f );

		String targetId = this.mngr.createTarget( f );
		Assert.assertEquals( "tid", targetId );

		String props = this.mngr.findRawTargetProperties( targetId );
		Assert.assertEquals( "prop: value\nhandler: h", props );

		this.mngr.deleteTarget( targetId );
		Assert.assertNull( this.mngr.findRawTargetProperties( targetId ));
	}


	@Test( expected = UnauthorizedActionException.class )
	public void testUpdateTarget_whenTargetDoesNotExist() throws Exception {

		this.mngr.updateTarget( "inexisting", "prop: ok" );
	}


	@Test
	public void testAssociations() throws Exception {

		TestApplication app = new TestApplication();
		String mySqlPath = InstanceHelpers.computeInstancePath( app.getMySqlVm());
		String tomcatPath = InstanceHelpers.computeInstancePath( app.getTomcatVm());

		// Only MySQL has an associated target
		String targetId = this.mngr.createTarget( "prop: ok\nid: abc\nhandler: h" );
		this.mngr.associateTargetWithScopedInstance( targetId, app, mySqlPath );

		String associatedId = this.mngr.findTargetId( app, mySqlPath );
		Assert.assertEquals( targetId, associatedId );

		// There is no value for Tomcat, nor default target for the application
		Assert.assertNull( this.mngr.findTargetId( app, tomcatPath ));

		// Let's define a default target for the whole application
		String defaultTargetId = this.mngr.createTarget( "prop: ok\nid: def\nhandler: h" );
		this.mngr.associateTargetWithScopedInstance( defaultTargetId, app, null );

		associatedId = this.mngr.findTargetId( app, mySqlPath );
		Assert.assertEquals( targetId, associatedId );

		associatedId = this.mngr.findTargetId( app, mySqlPath, true );
		Assert.assertEquals( targetId, associatedId );

		associatedId = this.mngr.findTargetId( app, tomcatPath );
		Assert.assertEquals( defaultTargetId, associatedId );
		Assert.assertNull( this.mngr.findTargetId( app, tomcatPath, true ));

		// Remove the custom association for MySQL
		this.mngr.dissociateTargetFromScopedInstance( app, mySqlPath );
		associatedId = this.mngr.findTargetId( app, mySqlPath );
		Assert.assertEquals( defaultTargetId, associatedId );
		Assert.assertNull( this.mngr.findTargetId( app, mySqlPath, true ));

		// Make sure we cannot delete a default target
		this.mngr.dissociateTargetFromScopedInstance( app, null );
		associatedId = this.mngr.findTargetId( app, mySqlPath );
		Assert.assertEquals( defaultTargetId, associatedId );

		// Make sure we can override a default target
		this.mngr.associateTargetWithScopedInstance( targetId, app, null );
		associatedId = this.mngr.findTargetId( app, mySqlPath );
		Assert.assertEquals( targetId, associatedId );

		associatedId = this.mngr.findTargetId( app, tomcatPath );
		Assert.assertEquals( targetId, associatedId );
	}


	@Test( expected = IOException.class )
	public void testAssociationWithInvalidTargetId() throws Exception {

		TestApplication app = new TestApplication();
		String mySqlPath = InstanceHelpers.computeInstancePath( app.getMySqlVm());
		this.mngr.associateTargetWithScopedInstance( "invalid", app, mySqlPath );
	}


	@Test
	public void testDisssociationWithInvalidTargetId() throws Exception {

		TestApplication app = new TestApplication();
		String mySqlPath = InstanceHelpers.computeInstancePath( app.getMySqlVm());

		// No association
		this.mngr.dissociateTargetFromScopedInstance( app, mySqlPath );
		// No exception
	}


	@Test( expected = UnauthorizedActionException.class )
	public void testAssociations_onADeployedInstance() throws Exception {

		TestApplication app = new TestApplication();
		String instancePath = InstanceHelpers.computeInstancePath( app.getMySqlVm());
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		String targetId = this.mngr.createTarget( "prop: ok\nid: tid\nhandler: h" );
		this.mngr.associateTargetWithScopedInstance( targetId, app, instancePath );
	}


	@Test( expected = UnauthorizedActionException.class )
	public void testDisssociations_onADeployedInstance() throws Exception {

		TestApplication app = new TestApplication();
		String instancePath = InstanceHelpers.computeInstancePath( app.getMySqlVm());
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		this.mngr.dissociateTargetFromScopedInstance( app, instancePath );
	}


	@Test
	public void testHints_noHint() throws Exception {

		TestApplication app1 = new TestApplication();
		app1.name( "app1" );
		TestApplication app2 = new TestApplication();
		app2.name( "app2" );

		String t1 = this.mngr.createTarget( "id: t1\nprop: ok\nname: target 1\ndescription: t1's target\nhandler: h" );
		String t2 = this.mngr.createTarget( "id: t2\nprop: ok\nhandler: docker" );

		List<TargetWrapperDescriptor> beans = this.mngr.listPossibleTargets( app1 );
		Assert.assertEquals( 2, beans.size());

		TargetWrapperDescriptor b1 = beans.get( 0 );
		Assert.assertEquals( t1, b1.getId());
		Assert.assertEquals( "target 1", b1.getName());
		Assert.assertEquals( "t1's target", b1.getDescription());
		Assert.assertNotNull( b1.getHandler());
		Assert.assertFalse( b1.isDefault());

		TargetWrapperDescriptor b2 = beans.get( 1 );
		Assert.assertEquals( t2, b2.getId());
		Assert.assertEquals( "docker", b2.getHandler());
		Assert.assertNull( b2.getName() );
		Assert.assertNull( b2.getDescription());
		Assert.assertFalse( b2.isDefault());

		Assert.assertEquals( 2, this.mngr.listPossibleTargets( app2 ).size());
	}


	@Test
	public void testHints_hintOnApplication() throws Exception {

		TestApplication app1 = new TestApplication();
		app1.name( "app1" );
		TestApplication app2 = new TestApplication();
		app2.name( "app2" );

		String t1 = this.mngr.createTarget( "id: t1\nprop: ok\nname: target 1\ndescription: t1's target\nhandler: h" );
		String t2 = this.mngr.createTarget( "id: t2\nprop: ok\nhandler: docker" );

		// Hint between app1 and t1.
		// t1 has now a scope, which includes app1.
		// Therefore, t1 should not be listed for app2 (not in the scope).
		this.mngr.addHint( t1, app1 );

		List<TargetWrapperDescriptor> beans = this.mngr.listPossibleTargets( app1 );
		Assert.assertEquals( 2, beans.size());

		TargetWrapperDescriptor b1 = beans.get( 0 );
		Assert.assertEquals( t1, b1.getId());
		Assert.assertEquals( "target 1", b1.getName());
		Assert.assertEquals( "t1's target", b1.getDescription());
		Assert.assertNotNull( b1.getHandler());
		Assert.assertFalse( b1.isDefault());

		TargetWrapperDescriptor b2 = beans.get( 1 );
		Assert.assertEquals( t2, b2.getId());
		Assert.assertEquals( "docker", b2.getHandler());
		Assert.assertNull( b2.getName());
		Assert.assertNull( b2.getDescription());
		Assert.assertFalse( b2.isDefault());

		beans = this.mngr.listPossibleTargets( app2 );
		Assert.assertEquals( 1, beans.size());

		b2 = beans.get( 0 );
		Assert.assertEquals( t2, b2.getId());
		Assert.assertEquals( "docker", b2.getHandler());
		Assert.assertNull( b2.getName());
		Assert.assertNull( b2.getDescription());
		Assert.assertFalse( b2.isDefault());
	}


	@Test
	public void testHints_removeHintOnApplication() throws Exception {

		TestApplication app1 = new TestApplication();
		app1.name( "app1" );
		TestApplication app2 = new TestApplication();
		app2.name( "app2" );

		String t1 = this.mngr.createTarget( "id: t1\nprop: ok\nname: target 1\ndescription: t1's target\nhandler: h" );
		String t2 = this.mngr.createTarget( "id: t2\nprop: ok\nhandler: docker\nhandler: h" );

		// Hint between app1 and t1.
		// t1 has now a scope, which includes app1.
		// Therefore, t1 should not be listed for app2 (not in the scope).
		this.mngr.addHint( t1, app1 );

		List<TargetWrapperDescriptor> beans = this.mngr.listPossibleTargets( app1 );
		Assert.assertEquals( 2, beans.size());

		beans = this.mngr.listPossibleTargets( app2 );
		Assert.assertEquals( 1, beans.size());

		// Remove the hint on the WRONG application => nothing changes
		this.mngr.removeHint( t1, app2 );

		beans = this.mngr.listPossibleTargets( app1 );
		Assert.assertEquals( 2, beans.size());

		beans = this.mngr.listPossibleTargets( app2 );
		Assert.assertEquals( 1, beans.size());

		// Remove the hint on the WRONG application => nothing changes
		this.mngr.removeHint( t2, app1 );

		beans = this.mngr.listPossibleTargets( app1 );
		Assert.assertEquals( 2, beans.size());

		beans = this.mngr.listPossibleTargets( app2 );
		Assert.assertEquals( 1, beans.size());

		// Remove the hint on the one we used
		this.mngr.removeHint( t1, app1 );

		beans = this.mngr.listPossibleTargets( app1 );
		Assert.assertEquals( 2, beans.size());

		beans = this.mngr.listPossibleTargets( app2 );
		Assert.assertEquals( 2, beans.size());
	}


	@Test
	public void testHints_hintOnApplicationTemplate() throws Exception {

		TestApplication app1 = new TestApplication();
		app1.name( "app1" );
		TestApplication app2 = new TestApplication();
		app2.name( "app2" );
		Application app3 = new Application( "app3", new ApplicationTemplate( "tpl" ).qualifier( "v1" ));

		String t1 = this.mngr.createTarget( "id: t1\nprop: ok\nname: target 1\ndescription: t1's target\nhandler: h" );
		String t2 = this.mngr.createTarget( "id: t2\nprop: ok\nhandler: docker" );

		// Hint between app1 and app2's template and t1.
		// t1 has now a scope, which includes (indirectly) app1 and app2.
		// Therefore, t1 should not be listed for app3 (not in the scope).
		this.mngr.addHint( t1, app1.getTemplate());

		List<TargetWrapperDescriptor> beans = this.mngr.listPossibleTargets( app1 );
		Assert.assertEquals( 2, beans.size());

		TargetWrapperDescriptor b1 = beans.get( 0 );
		Assert.assertEquals( t1, b1.getId());
		Assert.assertEquals( "target 1", b1.getName());
		Assert.assertEquals( "t1's target", b1.getDescription());
		Assert.assertNotNull( b1.getHandler());
		Assert.assertFalse( b1.isDefault());

		TargetWrapperDescriptor b2 = beans.get( 1 );
		Assert.assertEquals( t2, b2.getId());
		Assert.assertEquals( "docker", b2.getHandler());
		Assert.assertNull( b2.getName());
		Assert.assertNull( b2.getDescription());
		Assert.assertFalse( b2.isDefault());

		List<TargetWrapperDescriptor> otherBeans = this.mngr.listPossibleTargets( app2 );
		Assert.assertEquals( beans, otherBeans );

		otherBeans = this.mngr.listPossibleTargets( app2.getTemplate());
		Assert.assertEquals( beans, otherBeans );

		beans = this.mngr.listPossibleTargets( app3 );
		Assert.assertEquals( 1, beans.size());

		b2 = beans.get( 0 );
		Assert.assertEquals( t2, b2.getId());
		Assert.assertEquals( "docker", b2.getHandler());
		Assert.assertNull( b2.getName());
		Assert.assertNull( b2.getDescription());
		Assert.assertFalse( b2.isDefault());
	}


	@Test
	public void testFindRawTargetProperties_noProperties() {
		Assert.assertEquals( 0, this.mngr.findRawTargetProperties( new TestApplication(), "/whatever" ).size());
	}


	@Test
	public void testFindRawTargetProperties_withProperties() throws Exception {

		TestApplication app = new TestApplication();
		String instancePath = InstanceHelpers.computeInstancePath( app.getMySqlVm());
		String targetId = this.mngr.createTarget( "prop: ok\nid: tid\nhandler: h" );
		this.mngr.associateTargetWithScopedInstance( targetId, app, instancePath );

		Map<String,String> props = this.mngr.findRawTargetProperties( app, instancePath );
		Assert.assertEquals( 2, props.size());
		Assert.assertEquals( "ok", props.get( "prop" ));
		Assert.assertEquals( "h", props.get( "handler" ));
	}


	@Test
	public void testRestoreCache() throws Exception {

		Assert.assertEquals( 0, ((TargetsMngrImpl) this.mngr).targetIds.size());
		Assert.assertEquals( "1", this.mngr.createTarget( "prop: ok\nid: 1\nhandler: h" ));
		Assert.assertEquals( "2", this.mngr.createTarget( "prop: ok\nid: 2\nhandler: h" ));
		Assert.assertEquals( "abc", this.mngr.createTarget( "prop: ok\nid: abc\nhandler: h" ));
		Assert.assertEquals( "4", this.mngr.createTarget( "prop: ok\nid: 4\nhandler: h" ));
		Assert.assertEquals( 4, ((TargetsMngrImpl) this.mngr).targetIds.size());

		// Delete a valid and an invalid ones
		this.mngr.deleteTarget( "abc" );
		this.mngr.deleteTarget( "invalid-id" );
		Assert.assertEquals( 3, ((TargetsMngrImpl) this.mngr).targetIds.size());

		// Create a new manager and check restoration works
		this.mngr = new TargetsMngrImpl( this.configurationMngr );
		Assert.assertEquals( 3, ((TargetsMngrImpl) this.mngr).targetIds.size());

		// Add associations and make sure it works
		TestApplication app = new TestApplication();
		String instancePath = InstanceHelpers.computeInstancePath( app.getMySqlVm());

		Assert.assertNull( this.mngr.findTargetId( app, instancePath ));
		this.mngr.associateTargetWithScopedInstance( "4", app, instancePath );
		Assert.assertEquals( "4", this.mngr.findTargetId( app, instancePath ));

		// Create another manager
		this.mngr = new TargetsMngrImpl( this.configurationMngr );
		Assert.assertEquals( "hop", this.mngr.createTarget( "id: hop\nprop: ok\nhandler: h" ));
		Assert.assertEquals( "4", this.mngr.findTargetId( app, instancePath ));
		Assert.assertEquals( 4, ((TargetsMngrImpl) this.mngr).targetIds.size());
	}


	@Test
	public void testApplicationWasDeleted() throws Exception {

		// Prepare the model
		TestApplication app1 = new TestApplication();
		app1.name( "app1" );
		TestApplication app2 = new TestApplication();
		app2.name( "app2" );

		String t1 = this.mngr.createTarget( "id: t1\nprop: ok\nname: target 1\ndescription: t1's target\nhandler: h" );
		String t2 = this.mngr.createTarget( "id: t2\nprop: ok\nhandler: docker" );

		String path = InstanceHelpers.computeInstancePath( app1.getTomcatVm());

		// Create hints and associations
		this.mngr.associateTargetWithScopedInstance( t1, app1.getTemplate(), null );
		this.mngr.associateTargetWithScopedInstance( t1, app1, null );
		this.mngr.associateTargetWithScopedInstance( t2, app1, path );
		this.mngr.associateTargetWithScopedInstance( t2, app2, null );

		this.mngr.addHint( t1, app1 );
		this.mngr.addHint( t2, app1 );
		this.mngr.addHint( t2, app2 );

		// Verify pre-conditions
		Assert.assertEquals( t1, this.mngr.findTargetId( app1, null ));
		Assert.assertEquals( t2, this.mngr.findTargetId( app1, path ));
		Assert.assertEquals( 2, this.mngr.listPossibleTargets( app1 ).size());

		Assert.assertEquals( t2, this.mngr.findTargetId( app2, null ));
		Assert.assertEquals( t2, this.mngr.findTargetId( app2, path ));
		Assert.assertEquals( 1, this.mngr.listPossibleTargets( app2 ).size());

		Assert.assertEquals( t1, this.mngr.findTargetId( app1.getTemplate(), path ));
		Assert.assertEquals( 0, this.mngr.listPossibleTargets( app1.getTemplate()).size());

		// Delete the application
		this.mngr.applicationWasDeleted( app1 );

		// Verify post-conditions
		Assert.assertNull( this.mngr.findTargetId( app1, null ));
		Assert.assertNull( this.mngr.findTargetId( app1, path ));

		// t1 has not hint anymore, so it becomes global
		List<TargetWrapperDescriptor> hints = this.mngr.listPossibleTargets( app1 );
		Assert.assertEquals( 1, hints.size());
		Assert.assertEquals( t1, hints.get( 0 ).getId());

		Assert.assertEquals( t2, this.mngr.findTargetId( app2, null ));
		Assert.assertEquals( t2, this.mngr.findTargetId( app2, path ));

		// t1 is global now
		Assert.assertEquals( 2, this.mngr.listPossibleTargets( app2 ).size());

		// t1 is global now
		Assert.assertEquals( t1, this.mngr.findTargetId( app1.getTemplate(), path ));
		Assert.assertEquals( 1, this.mngr.listPossibleTargets( app1.getTemplate()).size());

		// Delete the template of app1
		this.mngr.applicationWasDeleted( app1.getTemplate());

		// Verify post-conditions
		Assert.assertNull( this.mngr.findTargetId( app1, null ));
		Assert.assertNull( this.mngr.findTargetId( app1, path ));
		Assert.assertEquals( 1, this.mngr.listPossibleTargets( app1 ).size());

		Assert.assertEquals( t2, this.mngr.findTargetId( app2, null ));
		Assert.assertEquals( t2, this.mngr.findTargetId( app2, path ));
		Assert.assertEquals( 2, this.mngr.listPossibleTargets( app2 ).size());

		Assert.assertNull( this.mngr.findTargetId( app1.getTemplate(), path ));
		Assert.assertEquals( 1, this.mngr.listPossibleTargets( app1.getTemplate()).size());

		// Delete app2
		this.mngr.applicationWasDeleted( app2 );

		// Verify post-conditions
		// t2 does not have any hint anymore => it is global
		Assert.assertNull( this.mngr.findTargetId( app1, null ));
		Assert.assertNull( this.mngr.findTargetId( app1, path ));
		Assert.assertEquals( 2, this.mngr.listPossibleTargets( app1 ).size());

		Assert.assertNull( this.mngr.findTargetId( app2, null ));
		Assert.assertNull( this.mngr.findTargetId( app2, path ));
		Assert.assertEquals( 2, this.mngr.listPossibleTargets( app2 ).size());

		Assert.assertNull( this.mngr.findTargetId( app1.getTemplate(), path ));
		Assert.assertEquals( 2, this.mngr.listPossibleTargets( app1.getTemplate()).size());
	}


	@Test
	public void testLocking_ByOneInstance() throws Exception {

		TestApplication app = new TestApplication();
		String instancePath = InstanceHelpers.computeInstancePath( app.getMySqlVm());
		Assert.assertEquals( 0, this.mngr.listAllTargets().size());

		String targetId = this.mngr.createTarget( "prop: ok\nid=tid\nhandler: h" );
		this.mngr.associateTargetWithScopedInstance( targetId, app, instancePath );

		Map<String,String> props = this.mngr.lockAndGetTarget( app, app.getMySqlVm());
		Assert.assertEquals( 2, props.size());
		Assert.assertEquals( "ok", props.get( "prop" ));
		Assert.assertEquals( "h", props.get( "handler" ));

		Assert.assertEquals( 1, this.mngr.listAllTargets().size());
		try {
			this.mngr.deleteTarget( targetId );
			Assert.fail( "A target is locked <=> We cannot delete it." );

		} catch( UnauthorizedActionException e ) {
			// nothing
		}

		this.mngr.unlockTarget( app, app.getMySqlVm());
		Assert.assertEquals( 1, this.mngr.listAllTargets().size());

		this.mngr.deleteTarget( targetId );
		Assert.assertEquals( 0, this.mngr.listAllTargets().size());
	}


	@Test
	public void testLocking_ByTwoInstances() throws Exception {

		TestApplication app = new TestApplication();
		String instancePath = InstanceHelpers.computeInstancePath( app.getMySqlVm());
		Assert.assertEquals( 0, this.mngr.listAllTargets().size());

		String targetId = this.mngr.createTarget( "prop: ok\nid: tid\nhandler: h" );
		this.mngr.associateTargetWithScopedInstance( targetId, app, instancePath );
		this.mngr.associateTargetWithScopedInstance( targetId, app, null );

		Map<String,String> props = this.mngr.lockAndGetTarget( app, app.getMySqlVm());
		Assert.assertEquals( 2, props.size());
		Assert.assertEquals( "ok", props.get( "prop" ));
		Assert.assertEquals( "h", props.get( "handler" ));

		props = this.mngr.lockAndGetTarget( app, app.getTomcatVm());
		Assert.assertEquals( 2, props.size());
		Assert.assertEquals( "ok", props.get( "prop" ));
		Assert.assertEquals( "h", props.get( "handler" ));

		Assert.assertEquals( 1, this.mngr.listAllTargets().size());
		try {
			this.mngr.deleteTarget( targetId );
			Assert.fail( "A target is locked <=> We cannot delete it." );

		} catch( UnauthorizedActionException e ) {
			// nothing
		}

		this.mngr.unlockTarget( app, app.getMySqlVm());
		Assert.assertEquals( 1, this.mngr.listAllTargets().size());

		try {
			this.mngr.deleteTarget( targetId );
			Assert.fail( "A target is locked <=> We cannot delete it." );

		} catch( UnauthorizedActionException e ) {
			// nothing
		}

		this.mngr.unlockTarget( app, app.getTomcatVm());
		this.mngr.deleteTarget( targetId );
		Assert.assertEquals( 0, this.mngr.listAllTargets().size());
	}


	@Test( expected = IOException.class )
	public void testLocking_noTarget() throws Exception {

		TestApplication app = new TestApplication();
		Assert.assertEquals( 0, this.mngr.listAllTargets().size());
		this.mngr.lockAndGetTarget( app, app.getMySqlVm());
	}


	@Test
	public void testCopyOriginalMapping_onInstancePath() throws Exception {

		TestApplication app = new TestApplication();
		String instancePath = InstanceHelpers.computeInstancePath( app.getMySqlVm());
		String t1 = this.mngr.createTarget( "prop: ok\nid: t1\nhandler: h" );
		String t2 = this.mngr.createTarget( "prop: ok\nid: t2\nhandler: h" );

		// Association is on the template AND the instance
		Assert.assertNull( this.mngr.findTargetId( app, instancePath ));
		this.mngr.associateTargetWithScopedInstance( t1, app.getTemplate(), instancePath );
		Assert.assertNull( this.mngr.findTargetId( app, instancePath ));

		this.mngr.copyOriginalMapping( app );
		Assert.assertEquals( t1, this.mngr.findTargetId( app, instancePath ));

		// We can override the association
		this.mngr.associateTargetWithScopedInstance( t2, app, instancePath );
		Assert.assertEquals( t2, this.mngr.findTargetId( app, instancePath ));
	}


	@Test
	public void testCopyOriginalMapping_onDefault() throws Exception {

		TestApplication app = new TestApplication();
		String instancePath = InstanceHelpers.computeInstancePath( app.getMySqlVm());
		String t1 = this.mngr.createTarget( "prop: ok\nid: t1\nhandler: h" );
		String t2 = this.mngr.createTarget( "prop: ok\nid: t2\nhandler: h" );

		// Association is on the template and BY DEFAULT
		Assert.assertNull( this.mngr.findTargetId( app, instancePath ));
		this.mngr.associateTargetWithScopedInstance( t1, app.getTemplate(), null );
		Assert.assertNull( this.mngr.findTargetId( app, instancePath ));

		this.mngr.copyOriginalMapping( app );
		Assert.assertEquals( t1, this.mngr.findTargetId( app, instancePath ));

		// We can override the association
		this.mngr.associateTargetWithScopedInstance( t2, app, instancePath );
		Assert.assertEquals( t2, this.mngr.findTargetId( app, instancePath ));
	}


	@Test
	public void testCopyOriginalMapping_withException() throws Exception {

		// Check that when the association fails for one instance,
		// it does not prevent others from being processed.

		TestApplication app = new TestApplication();
		String instancePath = InstanceHelpers.computeInstancePath( app.getMySqlVm());
		String tomcatPath = InstanceHelpers.computeInstancePath( app.getTomcatVm());

		String t1 = this.mngr.createTarget( "prop: ok\nid: t1\nhandler: h" );
		String t2 = this.mngr.createTarget( "prop: ok\nid: t2\nhandler: h" );

		// Association is on the template
		Assert.assertNull( this.mngr.findTargetId( app, instancePath ));
		this.mngr.associateTargetWithScopedInstance( t1, app.getTemplate(), instancePath );
		this.mngr.associateTargetWithScopedInstance( t1, app.getTemplate(), tomcatPath );

		// Set a new default for the application
		this.mngr.associateTargetWithScopedInstance( t2, app, null );
		Assert.assertEquals( t2, this.mngr.findTargetId( app, instancePath ));
		Assert.assertEquals( t2, this.mngr.findTargetId( app, tomcatPath ));

		// Change the state
		app.getMySqlVm().setStatus( InstanceStatus.DEPLOYED_STARTED );

		// The mapping won't be overwritten for the running instance
		this.mngr.copyOriginalMapping( app );
		Assert.assertEquals( t2, this.mngr.findTargetId( app, instancePath ));
		Assert.assertEquals( t1, this.mngr.findTargetId( app, tomcatPath ));
	}


	@Test
	public void testBuildList_exception() throws Exception {

		// Two targets, but only one with valid properties.
		// The invalid one won't be listed.
		File dir1 = this.folder.newFolder();
		File dir2 = this.folder.newFolder();
		Utils.writeStringInto( "prop: done", new File( dir1, Constants.TARGET_PROPERTIES_FILE_NAME ));

		List<File> targetDirectories = new ArrayList<>( 2 );
		targetDirectories.add( dir1 );
		targetDirectories.add( dir2 );

		List<TargetWrapperDescriptor> beans = ((TargetsMngrImpl) this.mngr).buildList( targetDirectories, null );
		Assert.assertEquals( 1, beans.size());
		Assert.assertEquals( dir1.getName(), beans.get( 0 ).getId());
	}


	@Test
	public void testFindTargetById() throws Exception {

		File dir = new File( this.configurationMngr.getWorkingDirectory(), ConfigurationUtils.TARGETS + "/5" );
		Utils.createDirectory( dir );
		Utils.writeStringInto( "prop: done\nhandler = test", new File( dir, Constants.TARGET_PROPERTIES_FILE_NAME ));

		TargetWrapperDescriptor twb = this.mngr.findTargetById( dir.getName());
		Assert.assertNotNull( twb );
		Assert.assertEquals( dir.getName(), twb.getId());
		Assert.assertEquals( "test", twb.getHandler());
		Assert.assertFalse( twb.isDefault());
		Assert.assertNull( twb.getName());
		Assert.assertNull( twb.getDescription());
	}


	@Test
	public void testFindUsageStatistics_inexistingTarget() throws Exception {

		List<TargetUsageItem> items = this.mngr.findUsageStatistics( "4" );
		Assert.assertEquals( 0, items.size());
	}


	@Test
	public void testFindUsageStatistics() throws Exception {

		// Setup
		TestApplication app = new TestApplication();
		Instance newRootInstance = new Instance( "newRoot" ).component( app.getMySqlVm().getComponent());
		app.getRootInstances().add( newRootInstance );

		String t1 = this.mngr.createTarget( "prop: ok\nid: t1\nhandler: h" );
		String t2 = this.mngr.createTarget( "prop: ok\nid: t2\nhandler: h" );
		String t3 = this.mngr.createTarget( "prop: ok\nid: t3\nhandler: h" );

		this.mngr.associateTargetWithScopedInstance( t1, app, InstanceHelpers.computeInstancePath( app.getMySqlVm()));
		this.mngr.associateTargetWithScopedInstance( t1, app, InstanceHelpers.computeInstancePath( newRootInstance ));
		this.mngr.associateTargetWithScopedInstance( t2, app, null );

		// Checks
		List<TargetUsageItem> items = this.mngr.findUsageStatistics( t1 );
		Assert.assertEquals( 1, items.size());

		TargetUsageItem item = items.get( 0 );
		Assert.assertEquals( app.getName(), item.getName());
		Assert.assertNull( item.getQualifier());
		Assert.assertFalse( item.isUsing());
		Assert.assertTrue( item.isReferencing());

		items = this.mngr.findUsageStatistics( t2 );
		Assert.assertEquals( 1, items.size());

		item = items.get( 0 );
		Assert.assertEquals( app.getName(), item.getName());
		Assert.assertNull( item.getQualifier());
		Assert.assertFalse( item.isUsing());
		Assert.assertTrue( item.isReferencing());

		items = this.mngr.findUsageStatistics( t3 );
		Assert.assertEquals( 0, items.size());

		// Mark one as used
		this.mngr.lockAndGetTarget( app, app.getTomcatVm());

		items = this.mngr.findUsageStatistics( t1 );
		Assert.assertEquals( 1, items.size());

		item = items.get( 0 );
		Assert.assertEquals( app.getName(), item.getName());
		Assert.assertNull( item.getQualifier());
		Assert.assertFalse( item.isUsing());
		Assert.assertTrue( item.isReferencing());

		items = this.mngr.findUsageStatistics( t3 );
		Assert.assertEquals( 0, items.size());

		items = this.mngr.findUsageStatistics( t2 );
		Assert.assertEquals( 1, items.size());

		item = items.get( 0 );
		Assert.assertEquals( app.getName(), item.getName());
		Assert.assertNull( item.getQualifier());
		Assert.assertTrue( item.isReferencing());

		// The change is here!
		Assert.assertTrue( item.isUsing());

		// Release it
		this.mngr.unlockTarget( app, app.getTomcatVm());

		items = this.mngr.findUsageStatistics( t1 );
		Assert.assertEquals( 1, items.size());

		item = items.get( 0 );
		Assert.assertEquals( app.getName(), item.getName());
		Assert.assertNull( item.getQualifier());
		Assert.assertFalse( item.isUsing());
		Assert.assertTrue( item.isReferencing());

		items = this.mngr.findUsageStatistics( t2 );
		Assert.assertEquals( 1, items.size());

		item = items.get( 0 );
		Assert.assertEquals( app.getName(), item.getName());
		Assert.assertNull( item.getQualifier());
		Assert.assertFalse( item.isUsing());
		Assert.assertTrue( item.isReferencing());

		items = this.mngr.findUsageStatistics( t3 );
		Assert.assertEquals( 0, items.size());

		// Remove the association for the named instance
		this.mngr.dissociateTargetFromScopedInstance( app, InstanceHelpers.computeInstancePath( app.getMySqlVm()));
		items = this.mngr.findUsageStatistics( t1 );
		Assert.assertEquals( 1, items.size());

		this.mngr.dissociateTargetFromScopedInstance( app, InstanceHelpers.computeInstancePath( newRootInstance ));
		items = this.mngr.findUsageStatistics( t1 );
		Assert.assertEquals( 0, items.size());

		items = this.mngr.findUsageStatistics( t2 );
		Assert.assertEquals( 1, items.size());

		item = items.get( 0 );
		Assert.assertEquals( app.getName(), item.getName());
		Assert.assertNull( item.getQualifier());
		Assert.assertFalse( item.isUsing());
		Assert.assertTrue( item.isReferencing());

		items = this.mngr.findUsageStatistics( t3 );
		Assert.assertEquals( 0, items.size());
	}


	@Test
	public void verifyAssociationsPersistenceOnDissociation() throws Exception {

		// Unit test for #579
		// One target manager => write associations.
		String targetId_1 = this.mngr.createTarget( "id: t1\nhandler: h" );
		String targetId_2 = this.mngr.createTarget( "id: t2\nhandler: h" );
		TestApplication app = new TestApplication();
		String path = InstanceHelpers.computeInstancePath( app.getMySqlVm());

		this.mngr.associateTargetWithScopedInstance( targetId_1, app, null );
		this.mngr.associateTargetWithScopedInstance( targetId_2, app, path );

		Assert.assertEquals( targetId_1, this.mngr.findTargetId( app, null, true ));
		Assert.assertEquals( targetId_2, this.mngr.findTargetId( app, path, true ));

		// Create another manager and verify the associations
		ITargetsMngr newMngr = new TargetsMngrImpl( this.configurationMngr );
		Assert.assertEquals( targetId_1, newMngr.findTargetId( app, null, true ));
		Assert.assertEquals( targetId_2, newMngr.findTargetId( app, path, true ));

		// Now, dissociate target_2 and the root instance
		this.mngr.dissociateTargetFromScopedInstance( app, path );
		Assert.assertEquals( targetId_1, this.mngr.findTargetId( app, null, true ));
		Assert.assertNull( this.mngr.findTargetId( app, path, true ));

		// Create another manager and verify the associations
		newMngr = new TargetsMngrImpl( this.configurationMngr );
		Assert.assertEquals( targetId_1, newMngr.findTargetId( app, null, true ));
		Assert.assertNull( newMngr.findTargetId( app, path, true ));
	}
}
