/*
 * Copyright 2023 Guido Zeuner
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
package de.zeus.upcam.rest;

import de.zeus.upcam.rest.client.config.Config;

/**
 * Represents the main entry point for the UpCam RestClient application.
 */
public class UpCamClientApp {

    /**
     * The main method that starts the UpCamClientApp .
     *
     * @param args The command-line arguments. The first argument should be the path to the upcamclient.properties
     *             file, and the second argument should be the path to the log4j2.xml configuration file.
     */
    public static void main(String[] args) {
        // Create an instance of RestClient
        UpCamClientApp upCamClientApp = new UpCamClientApp();
        // Initialize the configuration with the provided properties and log4j2.xml files
        upCamClientApp.initConfig(args);

        // Check if the correct number of arguments is provided
        if (args.length != 2) {
            // If not, print usage information and exit the program
            upCamClientApp.exit();
        }

        // If the correct number of arguments is provided, start the image receive process
        UpCamClient upCamClient = new UpCamClient();
        upCamClient.startImageReceiveProcess();
    }

    /**
     * Initializes the configuration with the provided properties and log4j2.xml files.
     *
     * @param args The command-line arguments containing the paths to the configuration files.
     */
    public void initConfig(String[] args) {
        if (args.length != 2) {
            // If the correct number of arguments is not provided, print usage information and exit the program
            System.out.println("Usage: [upcamclient.properties] [log4j2.xml]");
            System.exit(0);
        }

        // Set config files and initialize configuration
        Config config = Config.getInstance();
        config.setPropertiesFile(args[0]);
        config.setLog4jConfigFile(args[1]);
        config.init();
    }

    /**
     * Prints usage information and exits the program.
     */
    public void exit() {
        System.out.println("Usage: [upcamclient.properties] [log4j2.xml]");
        System.exit(0);
    }

}
