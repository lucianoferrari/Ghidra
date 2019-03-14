/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ghidra.server.remote;

import java.io.FileNotFoundException;
import java.io.IOException;

import ghidra.framework.ApplicationProperties;
import ghidra.util.SystemUtilities;
import utility.applicaiton.ApplicationLayout;
import utility.applicaiton.ApplicationUtilities;

/**
 * The Ghidra server application layout defines the customizable elements of the Ghidra 
 * server application's directory structure.
 */
public class GhidraServerApplicationLayout extends ApplicationLayout {

	/**
	 * Constructs a new Ghidra server application layout object.
	 * 
	 * @throws FileNotFoundException if there was a problem getting a user directory.
	 * @throws IOException if there was a problem getting the application properties.
	 */
	public GhidraServerApplicationLayout() throws FileNotFoundException, IOException {

		// Application root directories
		applicationRootDirs = ApplicationUtilities.findDefaultApplicationRootDirs();

		// Application properties
		applicationProperties = new ApplicationProperties(applicationRootDirs);

		// Application installation directory
		applicationInstallationDir = getApplicationRootDirs().iterator().next().getParentFile();
		if (SystemUtilities.isInDevelopmentMode()) {
			applicationInstallationDir = applicationInstallationDir.getParentFile();
		}

		// Extension directories
		extensionArchiveDir = null;
		extensionInstallationDir = null;

		// User directories (don't let anything use the user home directory...there may not be one)
		userTempDir = ApplicationUtilities.getDefaultUserTempDir(applicationProperties);
	}
}