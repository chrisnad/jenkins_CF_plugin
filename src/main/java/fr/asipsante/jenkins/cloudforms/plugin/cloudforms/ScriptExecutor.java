package fr.asipsante.jenkins.cloudforms.plugin.cloudforms;

import hudson.FilePath;
import hudson.Launcher;
import hudson.tasks.BatchFile;
import hudson.tasks.CommandInterpreter;
import hudson.tasks.Shell;

import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import fr.asipsante.jenkins.cloudforms.plugin.exceptions.CfScriptLaunchException;

/**
 * @author CNader
 */
public class ScriptExecutor {

    @Nonnull
    private final Launcher launcher;

    public ScriptExecutor(@Nonnull Launcher launcher) {
        this.launcher = launcher;
    }

    public int executeScript(@CheckForNull FilePath scriptExecutionRoot,
                                    @CheckForNull String scriptContent) throws CfScriptLaunchException {

//        //Process the script file path
//        if (scriptFilePath != null) {
//            String scriptFilePathResolved = Util.replaceMacro(scriptFilePath, scriptPathExecutionEnvVars);
//            String scriptFilePathNormalized = scriptFilePathResolved.replace("\\", "/");
//            int resultCode = executeScriptPath(scriptExecutionRoot, scriptFilePathNormalized, scriptExecutionEnvVars);
//            if (resultCode != 0) {
//                return resultCode;
//            }
//
//        }

        //Process the script content
        if (scriptContent != null) {
            int resultCode = executeScriptContent(scriptExecutionRoot, scriptContent);
            if (resultCode != 0) {
                return resultCode;
            }

        }

        return 0;
    }

    // TODO: Null file path leads to NOP, maybe safe here
//    private int executeScriptPath(
//            @CheckForNull FilePath scriptExecutionRoot, @Nonnull String scriptFilePath, 
//            @Nonnull Map<String, String> scriptExecutionEnvVars) throws EnvInjectException {
//        try {
//            launcher.getListener().getLogger().println(String.format("Executing '%s'.", scriptFilePath));
//            ArgumentListBuilder cmds = new ArgumentListBuilder();
//            cmds.addTokenized(scriptFilePath);
//            int cmdCode = launcher.launch().cmds(cmds).stdout(launcher.getListener()).envs(scriptExecutionEnvVars).pwd(scriptExecutionRoot).join();
//            if (cmdCode != 0) {
//                logger.info(String.format("Script executed. The exit code is %s.", cmdCode));
//            } else {
//                logger.info("Script executed successfully.");
//            }
//            return cmdCode;
//        } catch (Throwable e) {
//            throw new EnvInjectException("Error occurs on execution script file path.", e);
//        }
//    }

    private int executeScriptContent(@Nonnull FilePath scriptExecutionRoot, 
            @Nonnull String scriptContent) 
            throws CfScriptLaunchException {

        try {

            CommandInterpreter batchRunner;
            if (launcher.isUnix()) {
                batchRunner = new Shell(scriptContent);
            } else {
                batchRunner = new BatchFile(scriptContent);
            }

            FilePath tmpFile = batchRunner.createScriptFile(scriptExecutionRoot);
//            logger.info(String.format("Executing and processing the following script content: %n%s%n", scriptContent));
            System.out.println("Executing and processing the following script content: " + scriptContent);
            int cmdCode = launcher.launch().cmds(batchRunner.buildCommandLine(tmpFile)).stdout(launcher.getListener())
                    .pwd(scriptExecutionRoot).join();
            if (cmdCode != 0) {
//                logger.info(String.format("Script executed. The exit code is %s.", cmdCode));
            	System.out.println("Script executed. The exit code is " + cmdCode);
            } else {
//                logger.info("Script executed successfully.");
            	System.out.println("Script executed successfully.");
            }
            return cmdCode;

        } catch (IOException ioe) {
            throw new CfScriptLaunchException("Error occurs on execution script file path", ioe);
        } catch (InterruptedException ie) {
            throw new CfScriptLaunchException("Error occurs on execution script file path", ie);
        }
    }

}
