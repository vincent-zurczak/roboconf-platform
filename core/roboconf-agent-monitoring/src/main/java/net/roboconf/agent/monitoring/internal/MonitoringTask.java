/**
 * Copyright 2014 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.agent.monitoring.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.roboconf.agent.AgentMessagingInterface;
import net.roboconf.agent.monitoring.internal.file.FileHandler;
import net.roboconf.agent.monitoring.internal.nagios.NagiosHandler;
import net.roboconf.core.model.helpers.InstanceHelpers;
import net.roboconf.core.model.runtime.Instance;
import net.roboconf.core.utils.Utils;
import net.roboconf.messaging.messages.Message;

/**
 * Scheduler for periodic monitoring checks (polling).
 * @author Pierre-Yves Gibello - Linagora
 */
public class MonitoringTask extends TimerTask {

	private static final String PARSER_FILE = "file";
	private static final String PARSER_NAGIOS = "nagios";

	private static final String COMMENT_DELIMITER = "#";
	private static final String EVENT_PATTERN = "\\[EVENT\\s+(\\w)\\s+(\\w)";

	private final Logger logger = Logger.getLogger( getClass().getName());
	private final AgentMessagingInterface agentInterface;
	private final Pattern eventPattern;


	/**
	 * Constructor.
	 * @param agentInterface
	 */
	public MonitoringTask( AgentMessagingInterface agentInterface ) {
		this.agentInterface = agentInterface;
		this.eventPattern = Pattern.compile( EVENT_PATTERN );
	}


	@Override
	public void run() {
		this.logger.fine( "Monitoring Task is being invoked." );

		// Root Instance may not yet have been injected: skip !
		if( this.agentInterface.getRootInstance() == null) {
			this.logger.fine( "The agent's model has not yet been initialized. Monitoring cannot work yet." );
			return;
		}

		// Otherwise, check all the instances
		for( Instance inst : InstanceHelpers.buildHierarchicalList( this.agentInterface.getRootInstance())) {
			File dir = InstanceHelpers.findInstanceDirectoryOnAgent( inst, inst.getComponent().getInstallerName());
			File measureFile = new File( dir, inst.getComponent().getName() + ".measures" );
			if( ! measureFile.exists()) {
				this.logger.finer( "No file with measure rules was found for instance '" + inst + "'." );
				continue;
			}

			// Read the file content
			this.logger.fine( "A file with measure rules was found for instance '" + inst + "'." );
			String fileContent;
			try {
				fileContent = Utils.readFileContent( measureFile );

			} catch( IOException e ) {
				this.logger.warning( "A problem occurred while reading the content for measure rules of instance '"+ inst + "'." );
				Utils.logException( this.logger, e );
				continue;
			}

			// Find the right handlers to process the rules
			for( MonitoringHandler handler : extractRuleSections( measureFile, fileContent )) {
				try {
					Message msg = handler.process();
					if( msg != null )
						this.agentInterface.getMessagingClient().sendMessageToTheDm( msg );

				} catch( IOException e ) {
					this.logger.warning( "A problem occurred while the agent monitoring was sending a message to the DM. " + e.getMessage());
					Utils.logException( this.logger, e );
				}
			}
		}
	}


	/**
	 * Reads the file content, extracts rules sections and creates the right handlers.
	 * @param file
	 * @param fileContent
	 * @return a non-null list of handlers (may be empty)
	 */
	List<MonitoringHandler> extractRuleSections( File file, String fileContent ) {

		// Find rules
		List<MonitoringHandler> result = new ArrayList<MonitoringHandler> ();
		StringBuilder sb = new StringBuilder();
		String parserId = null, eventId = null;
		for( String s : Arrays.asList( fileContent.trim().split( "\n" ))) {

			s = s.trim();
			if( s.startsWith( COMMENT_DELIMITER ))
				continue;

			Matcher m = this.eventPattern.matcher( s );
			if( m.find()) {

				// We have something to create rules
				if( parserId != null ) {
					MonitoringHandler handler = createHandler( parserId, eventId, sb.toString());
					if( handler != null )
						result.add( handler );
					else
						this.logger.warning( "No monitoring handler matched parser ID '" + m.group( 1 ) + "' in " + file + "." );
				}

				// Store the new section's properties
				sb.setLength( 0 );
				parserId = m.group( 1 );
				eventId = m.group( 2 );
			}

			// Otherwise, append non-empty lines for their upcoming parsing
			else if( s.length() > 0 )
				sb.append( s + "\n" );
		}

		return result;
	}


	/**
	 * Creates a handler.
	 * @param parserId
	 * @param eventId
	 * @param ruleContent
	 * @return a new handler, or null if none matched the parser ID
	 */
	MonitoringHandler createHandler( String parserId, String eventId, String ruleContent ) {

		String appName = this.agentInterface.getApplicationName();
		String rootInstanceName = this.agentInterface.getRootInstance().getName();

		MonitoringHandler result = null;
		if( PARSER_FILE.equalsIgnoreCase( parserId ))
			result = new FileHandler( eventId, appName, rootInstanceName, ruleContent );
		else if( PARSER_NAGIOS.equalsIgnoreCase( parserId ))
			result = new NagiosHandler( eventId, appName, rootInstanceName, ruleContent );

		return result;
	}
}
