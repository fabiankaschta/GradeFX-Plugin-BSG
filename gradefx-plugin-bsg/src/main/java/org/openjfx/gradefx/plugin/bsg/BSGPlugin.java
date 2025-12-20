package org.openjfx.gradefx.plugin.bsg;

import java.util.ResourceBundle;

import org.openjfx.kafx.controller.LogController;
import org.openjfx.kafx.controller.TranslationController;
import org.pf4j.Plugin;

public class BSGPlugin extends Plugin {

	public BSGPlugin() {
		super();
	}

	@Override
	public void start() {
		LogController.log(LogController.DEBUG, "bsg plugin started");
		TranslationController.addBundle(ResourceBundle.getBundle("org.openjfx.gradefx.plugin.bsg.lang.bsg"));
	}

	@Override
	public void stop() {
		LogController.log(LogController.DEBUG, "bsg plugin stopped");
	}

}