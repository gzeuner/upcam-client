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
import de.zeus.upcam.rest.util.ProcessLock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Represents the main entry point for the UpCam RestClient application.
 */
public class UpCamClientApp {
    private static final String DEFAULT_PROPERTIES_FILE = "application.properties";
    private static final String LEGACY_PROPERTIES_FILE = "upcamclient.properties";
    private static final String DEFAULT_LOG4J_FILE = "log4j2.xml";

    public static void main(String[] args) {
        UpCamClientApp upCamClientApp = new UpCamClientApp();
        RunOptions runOptions = upCamClientApp.initConfig(args);

        Config config = Config.getInstance();
        if (!ProcessLock.acquire(config.getLockFile())) {
            System.out.println("UpCam-Client already running. Exiting.");
            System.exit(0);
        }

        UpCamClient upCamClient = new UpCamClient();

        if (runOptions.runOnce) {
            upCamClient.runSingleCycle();
            System.exit(0);
        }

        upCamClient.startImageReceiveProcess();
    }

    public RunOptions initConfig(String[] args) {
        RunOptions runOptions = parseArgs(args);
        if (runOptions == null) {
            exit();
        }

        if (!Files.exists(Paths.get(runOptions.propertiesFile))) {
            System.out.println("Error: Properties file not found: " + runOptions.propertiesFile);
            exit();
        }
        if (!Files.exists(Paths.get(runOptions.log4jConfigFile))) {
            System.out.println("Error: Log4j config file not found: " + runOptions.log4jConfigFile);
            exit();
        }

        Config config = Config.getInstance();
        config.setPropertiesFile(runOptions.propertiesFile);
        config.setLog4jConfigFile(runOptions.log4jConfigFile);
        config.init();
        return runOptions;
    }

    private RunOptions parseArgs(String[] args) {
        if (args == null || args.length == 0) {
            return new RunOptions(resolveDefaultPropertiesFile(), DEFAULT_LOG4J_FILE, false);
        }

        if (args.length == 1) {
            if ("--once".equalsIgnoreCase(args[0])) {
                return new RunOptions(resolveDefaultPropertiesFile(), DEFAULT_LOG4J_FILE, true);
            }
            return new RunOptions(args[0], DEFAULT_LOG4J_FILE, false);
        }

        if (args.length == 2) {
            if ("--once".equalsIgnoreCase(args[1])) {
                return new RunOptions(args[0], DEFAULT_LOG4J_FILE, true);
            }
            return new RunOptions(args[0], args[1], false);
        }

        if (args.length == 3 && "--once".equalsIgnoreCase(args[2])) {
            return new RunOptions(args[0], args[1], true);
        }

        return null;
    }

    private String resolveDefaultPropertiesFile() {
        Path applicationProperties = Paths.get(DEFAULT_PROPERTIES_FILE);
        if (Files.exists(applicationProperties)) {
            return DEFAULT_PROPERTIES_FILE;
        }

        Path legacyProperties = Paths.get(LEGACY_PROPERTIES_FILE);
        if (Files.exists(legacyProperties)) {
            return LEGACY_PROPERTIES_FILE;
        }

        return DEFAULT_PROPERTIES_FILE;
    }

    public void exit() {
        System.out.println("Usage:");
        System.out.println("  java -jar upcam-client-1.0-jar-with-dependencies.jar");
        System.out.println("  java -jar upcam-client-1.0-jar-with-dependencies.jar [application.properties|upcamclient.properties]");
        System.out.println("  java -jar upcam-client-1.0-jar-with-dependencies.jar [properties] [log4j2.xml] [--once]");
        System.exit(0);
    }

    public static final class RunOptions {
        private final String propertiesFile;
        private final String log4jConfigFile;
        private final boolean runOnce;

        private RunOptions(String propertiesFile, String log4jConfigFile, boolean runOnce) {
            this.propertiesFile = propertiesFile;
            this.log4jConfigFile = log4jConfigFile;
            this.runOnce = runOnce;
        }
    }
}
