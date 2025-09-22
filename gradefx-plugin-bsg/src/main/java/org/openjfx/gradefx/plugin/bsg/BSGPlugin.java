package org.openjfx.gradefx.plugin.bsg;

import java.util.ResourceBundle;

import org.openjfx.kafx.controller.Controller;
import org.openjfx.kafx.controller.TranslationController;
import org.pf4j.Plugin;

public class BSGPlugin extends Plugin {

	public BSGPlugin() {
		super();
	}

	@Override
	public void start() {
		Controller.log(Controller.DEBUG, "bsg plugin started");
		TranslationController.addBundle(ResourceBundle.getBundle("org.openjfx.gradefx.plugin.bsg.lang.bsg"));
	}

	@Override
	public void stop() {
		Controller.log(Controller.DEBUG, "bsg plugin stopped");
	}

}