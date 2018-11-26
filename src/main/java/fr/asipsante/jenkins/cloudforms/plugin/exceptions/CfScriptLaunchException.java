package fr.asipsante.jenkins.cloudforms.plugin.exceptions;

import javax.annotation.Nonnull;

public class CfScriptLaunchException  extends Exception {

	/**
	 * Default Serial identifier
	 */
	private static final long serialVersionUID = 1L;

	public CfScriptLaunchException(@Nonnull String s) {
        super(s);
    }

    public CfScriptLaunchException(@Nonnull Throwable throwable) {
        super(throwable);
    }

    public CfScriptLaunchException(@Nonnull String s, @Nonnull Throwable throwable) {
        super(s, throwable);
    }
}
