/**
 * Copyright 2015 Linagora, Université Joseph Fourier, Floralis
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

package net.roboconf.dm.internal.commands;

import java.io.File;
import java.util.logging.Logger;

import net.roboconf.dm.management.ManagedApplication;
import net.roboconf.dm.management.Manager;
import net.roboconf.dm.management.exceptions.CommandException;

/**
 * @author Vincent Zurczak - Linagora
 */
public class CommandsExecutor {

	private final Logger logger = Logger.getLogger( getClass().getName());

	private final File commandsFile;
	private final ManagedApplication ma;
	private final Manager manager;


	/**
	 * Constructor.
	 * @param manager the manager
	 * @param ma a managed application (not null)
	 * @param commandsFile a file containing commands (not null)
	 */
	public CommandsExecutor( Manager manager, ManagedApplication ma, File commandsFile ) {
		this.commandsFile = commandsFile;
		this.ma = ma;
		this.manager = manager;
	}


	/**
	 * Executes a set of commands.
	 * <p>
	 * It is assumed that {@link #validate()} was invoked first and was
	 * successful.
	 * </p>
	 *
	 * @throws CommandException if something went wrong
	 */
	public void execute() throws CommandException {

//		try {
//			for( ICommandInstruction instr : this.instructions )
//				instr.execute();
//
//		} catch( CommandException e ) {
//			throw e;
//
//		} catch( Exception e ) {
//			throw new CommandException( e );
//		}
	}
}
