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

package net.roboconf.dm.internal.autonomic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import net.roboconf.core.Constants;
import net.roboconf.core.model.beans.Component;
import net.roboconf.core.model.beans.Instance;
import net.roboconf.core.model.beans.Instance.InstanceStatus;
import net.roboconf.core.model.helpers.ComponentHelpers;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.utils.Utils;
import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.messaging.api.messages.from_agent_to_dm.MsgNotifAutonomic;

/**
 * An event handler to evaluate rules.
 * <p>
 * This handler can create new instances, including virtual machines.
 * It can also delete instances it has created.
 * </p>
 *
 * @author Pierre-Yves Gibello - Linagora
 */
public class RuleBasedEventHandler {

	static final String DELETE_SERVICE = "delete-service";
	static final String REPLICATE_SERVICE = "replicate-service";
	static final String REPLICATE_INSTANCE = "replicate-instance";
	static final String MAIL = "mail";

	static final String AUTONOMIC_MARKER = "autonomic";

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final Manager manager;
	private final AtomicInteger vmCount = new AtomicInteger( 0 );

	boolean disableChecks = false;

	/**
	 * This map acts as a cache whose key is bound to an application and a given reaction ID.
	 */
	private final Map<String,Instance> reactionKeyToLastInstance;


	/**
	 * Create a new rule-based event handler.
	 * @param manager the application manager
	 */
	public RuleBasedEventHandler( Manager manager ) {
		this.manager = manager;
		this.reactionKeyToLastInstance = new HashMap<String,Instance> ();
	}


	/**
	 * Reacts upon autonomic monitoring message (aka "autonomic event").
	 * @param event The autonomic event message
	 */
	public void handleEvent( ManagedApplication ma, MsgNotifAutonomic event ) {

		try {
			Map<String,AutonomicRule> rules = RulesParser.parseRules( ma );
			AutonomicRule rule = rules.get( event.getEventId());
			this.logger.fine( "Autonomic management event. Event ID = " + event.getEventId());

			// This key is bound to an application name and a reaction ID.
			String reactionKey = ma.getName() + "_";
			if( rule != null )
				reactionKey += rule.getReactionId();

			// Maximum number of machine the autonomic can create
			int maxVmCount = this.manager.getAutonomicMaxRoots();
			if( maxVmCount == -1 )
				maxVmCount = Integer.MAX_VALUE;

			// Permissions checks are handled entirely in this method.
			// Dealing with it in the (several) specific methods would make unit tests more complex.
			if( rule == null )
				this.logger.fine( "No rule was found to handle events with the '" + event.getEventId() + "' ID." );

			// EVENT_ID ReplicateService ComponentTemplate
			else if( REPLICATE_SERVICE.equalsIgnoreCase( rule.getReactionId())) {

				this.logger.fine( "Autonomic management: about to create a new instance based on '" + rule.getReactionInfo() + "'." );
				if( this.vmCount.get() >= maxVmCount ) {
					this.logger.info( "Autonomic management: the maximum number of instances created by the autonomic has been reached. Service replication is cancelled." );

				} else if( checkPermission( reactionKey, true )) {
					Instance inst = createInstances( ma, rule.getReactionInfo());
					ack( inst, reactionKey );

				} else {
					this.logger.warning(
							"Autonomic management: replication of service of '" + rule.getReactionId() + "' is cancelled. "
							+ "A previous execution is still in progress (reaction ID = " + rule.getReactionId() + ")." );
				}
			}

			// EVENT_ID ReplicateInstance RootInstanceName
			else if( REPLICATE_INSTANCE.equalsIgnoreCase( rule.getReactionId())) {

				this.logger.fine( "Autonomic management: about to replicate instance '/" + rule.getReactionInfo() + "'." );
				if( this.vmCount.get() >= maxVmCount ) {
					this.logger.info( "Autonomic management: the maximum number of instances created by the autonomic has been reached. Instance replication is cancelled." );

				} else if( checkPermission( reactionKey, true )) {
					Instance inst = replicateInstance( ma, rule.getReactionInfo());
					ack( inst, reactionKey );

				} else {
					this.logger.warning(
							"Autonomic management: replication of root instance '" + rule.getReactionId() + "' is cancelled. "
							+ "A previous execution is still in progress (reaction ID = " + rule.getReactionId() + ")." );
				}
			}

			// EVENT_ID StopService ComponentName
			else if( DELETE_SERVICE.equalsIgnoreCase( rule.getReactionId())) {

				this.logger.fine( "Autonomic management: about to delete an instance of '" + rule.getReactionInfo() + "'." );
				if( checkPermission( reactionKey, false )) {
					Instance inst = deleteInstances( ma, rule.getReactionInfo());
					ack( inst, reactionKey );

				} else {
					this.logger.warning(
							"Autonomic management: deletion of an instance of '" + rule.getReactionId() + "' is cancelled. "
							+ "A previous execution is still in progress (reaction ID = " + rule.getReactionId() + ")." );
				}
			}

			// EVENT_ID Mail DestinationEmail
			else if( MAIL.equalsIgnoreCase( rule.getReactionId()))
				sendEmail(ma, rule.getReactionInfo());

			// EVENT_ID Log LogMessage
			// And default behavior...
			else
				this.logger.fine( "AUTONOMIC Monitoring event. Info = " + rule.getReactionInfo());

		} catch( IOException e ) {
			this.logger.warning( "An autonomic event could not be handled. " + e.getMessage());
			Utils.logException( this.logger, e );
		}
	}


	/**
	 * Sends an email, according to credentials and configuration retrieved
	 * from a properties file (rules.cfg.properties).
	 * @param ma the managed application
	 * @param emailData the email message (may start with "Subject: {subject}\n)
	 * @throws IOException if the mail properties could not be read
	 */
	void sendEmail( ManagedApplication ma, String emailData ) throws IOException {

		this.logger.fine( "Autonomic management: about to send an e-mail." );

		/*
		 * Sample properties:
		 * mail.from: dm@roboconf.net
		 * mail.to: someone@domain.com
		 * mail.user: me
		 * mail.password: mypassword
		 * mail.smtp.host: my.mail.server
		 * mail.smtp.port: 1234
		 *
		 * mail.smtp.auth: true
		 * mail.smtp.starttls.enable: true
		 * mail.smtp.ssl.trust: my.mail.server
		 */
		File propFile = new File(
				ma.getApplication().getDirectory(),
				Constants.PROJECT_DIR_AUTONOMIC + "/" + Constants.FILE_RULES + ".properties" );

		Properties mailProperties = Utils.readPropertiesFile( propFile );
		String to = mailProperties.getProperty("mail.to");
		if( to == null )
			throw new IOException( "Cannot send email: no destination (mail.to) specified in the e-mail properties." );

		// Subject and message
		String subject = "Roboconf event";
		String data = emailData;

		final String emailSubjectPattern = "Subject: ([^\n]+)(\n|$)(.*)";
		Matcher m = Pattern.compile( emailSubjectPattern ).matcher( emailData );
		if( m.find()) {
			subject += ": " + m.group( 1 );
			data = m.group( 3 ).trim();
		}

		// Credentials
		String username = mailProperties.getProperty( "mail.user", "" );
		String password = mailProperties.getProperty( "mail.password", "" );

		// Obtain mail session object
		Session session = null;
		if("true".equalsIgnoreCase( mailProperties.getProperty("mail.smtp.auth")))
			session = Session.getInstance( mailProperties, new MailAuthenticator(username, password));
		else
			session = Session.getDefaultInstance( mailProperties );

		try {
			// Create a default MimeMessage object
			MimeMessage message = new MimeMessage(session);

			// Set From: and To: header fields
			message.setFrom( new InternetAddress( mailProperties.getProperty( "mail.from" )));
			message.addRecipient( Message.RecipientType.TO, new InternetAddress( to ));

			message.setSubject(subject);
			message.setText(data.trim());

			// Send email!
			Transport.send(message);

		} catch( MessagingException e ) {
			this.logger.severe( "Failed to send an email: " + e.getMessage());
			Utils.logException( this.logger, e );
		}
	}


	/**
	 * Instantiates a new VM with instances on it.
	 * @param ma the managed application
	 * @param componentTemplates the component names, separated by "/"
	 * <p>
	 * (e.g. VM_name/Software_container_name/App_artifact_name)
	 * </p>
	 *
	 * @return the created root instance (can be null in case of error)
	 */
	Instance createInstances( ManagedApplication ma, String componentTemplates ) {

		Instance result = null;
		try {
			if( componentTemplates.startsWith( "/" ))
				componentTemplates = componentTemplates.substring( 1 );

			String templates[] = componentTemplates.split( "/" );

			// First check that all component to instantiate are valid and found...
			// Necessary, not to create a VM then try to instantiate a fake component there!
			for( String s : templates) {
				Component compToInstantiate = ComponentHelpers.findComponent( ma.getApplication().getTemplate().getGraphs(), s);
				if( compToInstantiate == null )
					throw new IOException( "Component " + s + " was not found in application " + ma.getApplication().getName());
			}

			// We register new instances in the model
			Instance previousInstance = null;
			for( String s : templates) {
				Component compToInstantiate = ComponentHelpers.findComponent( ma.getApplication().getTemplate().getGraphs(), s);
				// compToInstantiate should never be null (check done above).

				// All the root instances must have a different name. Others do not matter.
				String instanceName;
				if( previousInstance == null )
					instanceName = compToInstantiate.getName() + "_" + System.currentTimeMillis();
				else
					instanceName = compToInstantiate.getName().toLowerCase();

				Instance currentInstance = new Instance( instanceName ).component(compToInstantiate);
				this.manager.instancesMngr().addInstance( ma, previousInstance, currentInstance );
				previousInstance = currentInstance;
			}

			// Now, deploy and start all
			Instance rootInstance = InstanceHelpers.findRootInstance( previousInstance );
			rootInstance.data.put( AUTONOMIC_MARKER, "true" );
			this.manager.instancesMngr().deployAndStartAll( ma, rootInstance );

			// Only update the result if everything went fine
			result = rootInstance;
			this.vmCount.incrementAndGet();

		} catch( Exception e ) {
			this.logger.warning( "The creation of instances (autonomic context) failed. " + e.getMessage());
			Utils.logException( this.logger, e );
		}

		return result;
	}


	/**
	 * Instantiates a new VM from another "template" instance.
	 * @param ma the managed application
	 * @param rootInstanceName the name of the root "template" instance
	 * @return the created root instance (can be null in case of error)
	 */
	Instance replicateInstance( ManagedApplication ma, String rootInstanceName ) {

		Instance result = null;
		try {
			// Find the instance to copy
			Instance rootInstanceToCopy = InstanceHelpers.findInstanceByPath( ma.getApplication(), "/" + rootInstanceName );
			if( rootInstanceToCopy == null )
				throw new IOException( "Instance " + rootInstanceName + " was not found in application " + ma.getApplication().getName());

			// Copy it
			Instance copy = InstanceHelpers.replicateInstance( rootInstanceToCopy );
			copy.setName( copy.getComponent().getName() + "_" + System.currentTimeMillis());

			// Register it in the model
			this.manager.instancesMngr().addInstance( ma, null, copy );

			// Now, deploy and start all
			copy.data.put( AUTONOMIC_MARKER, "true" );
			this.manager.instancesMngr().deployAndStartAll( ma, copy );

			// Only update the result if everything went fine
			result = copy;
			this.vmCount.incrementAndGet();

		} catch( Exception e ) {
			this.logger.warning( "The replication of an instance (autonomic context) failed. " + e.getMessage());
			Utils.logException( this.logger, e );
		}

		return result;
	}


	/**
	 * Deletes an instance and the VM that hosts it.
	 * <p>
	 * Only instances that were created by this class can be undeployed and removed from the model.
	 * </p>
	 *
	 * @param ma the managed application
	 * @param componentName the component name of an instance to delete
	 * @return the removed root instance (can be null in case of error)
	 */
	Instance deleteInstances( ManagedApplication ma, String componentName ) {

		Instance result = null;
		try {
			// Find an instance which was created by the autonomic.
			// Its root instance is annotated with the "autonomic" marker.
			List<Instance> instances = InstanceHelpers.findInstancesByComponentName( ma.getApplication(), componentName );
			Instance instanceToRemove = null;

			for( Instance instance : instances ) {
				Instance rootInstance = InstanceHelpers.findRootInstance( instance );
				String something = rootInstance.data.remove( AUTONOMIC_MARKER );

				if( something != null ) {
					instanceToRemove = rootInstance;
					break;
				}
			}

			// If there is one, delete it
			if( instanceToRemove != null ) {
				this.manager.instancesMngr().undeployAll( ma, instanceToRemove );
				this.manager.instancesMngr().removeInstance( ma, instanceToRemove );

				// Only update the result if everything went fine
				result = instanceToRemove;
				this.vmCount.decrementAndGet();
			}

		} catch( Exception e ) {
			this.logger.warning( "The deletion of an instance (autonomic context) failed. " + e.getMessage());
			Utils.logException( this.logger, e );
		}

		return result;
	}


	/**
	 * Checks some actions are allowed.
	 * <p>
	 * This method should only invoked for creation and deletion of instances.
	 * </p>
	 *
	 * @param reactionKey the reaction key
	 * @param add true if an instance is about to be added and started, false if it is about to be removed
	 * @return true if the reaction can be triggered, false otherwise
	 */
	boolean checkPermission( String reactionKey, boolean add ) {

		boolean permitted = true;
		Instance lastInstance = this.reactionKeyToLastInstance.get( reactionKey );

		// Disabling checks is used in some tests
		if( ! this.disableChecks && lastInstance != null ) {

			for( Iterator<Instance> it = InstanceHelpers.buildHierarchicalList( lastInstance ).iterator(); it.hasNext() && permitted; ) {
				Instance inst = it.next();

				// If we want to add a new instance, then all the instances "before"
				// must have been started.
				if( add && inst.getStatus() != InstanceStatus.DEPLOYED_STARTED ) {
					permitted = false;
					this.logger.warning( "Autonomic management: instance " + inst + " is not yet started (context = " + reactionKey + ")." );
				}

				// Otherwise, if we want to remove an instance, then all the instances "before"
				// must have been undeployed.
				else if( ! add && inst.getStatus() != InstanceStatus.NOT_DEPLOYED ) {
					permitted = false;
					this.logger.warning( "Autonomic management: instance " + inst + " is not yet undeployed (context = " + reactionKey + ")." );
				}
			}

			// Clean the cache
			if( permitted )
				this.reactionKeyToLastInstance.remove( reactionKey );
		}

		return permitted;
	}


	/**
	 * Acknowledges an instance was created or removed for a given reaction ID.
	 * @param instance the resulting instance (can be null)
	 * @param reactionKey a reaction key (not null)
	 */
	void ack( Instance instance, String reactionKey ) {

		if( instance == null )
			return;

		// Update the cache
		this.reactionKeyToLastInstance.put( reactionKey, instance );

		// Clean it too
		// Indeed, some actions may invalidate it. As an example, it is not worth
		// keeping an instance cached for a replicate reaction if it was removed.

		// We here consider an instance can only be cached for only one reaction ID.
		List<String> reactionKeys = new ArrayList<> ();
		for( Map.Entry<String,Instance> entry : this.reactionKeyToLastInstance.entrySet()) {
			if( entry.getValue().equals( instance ))
				reactionKeys.add( entry.getKey());
		}

		reactionKeys.remove( reactionKey );
		for( String key : reactionKeys )
			this.reactionKeyToLastInstance.remove( key );
	}


	/**
	 * @author Pierre-Yves Gibello - Linagora
	 */
	static class MailAuthenticator extends javax.mail.Authenticator {
		String username = "";
		String password = "";

		public MailAuthenticator(String username, String password) {
			super();
			this.username = username;
			this.password = password;
		}

		@Override
		protected PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(this.username, this.password);
		}
	}
}
