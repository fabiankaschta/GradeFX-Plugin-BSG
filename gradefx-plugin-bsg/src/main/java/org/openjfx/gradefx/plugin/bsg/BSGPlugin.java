package org.openjfx.gradefx.plugin.bsg;

import java.util.ResourceBundle;

import org.openjfx.kafx.controller.LogController;
import org.openjfx.kafx.controller.PropertiesController;
import org.openjfx.kafx.controller.TranslationController;
import org.openjfx.kafx.controller.UpdateController;
import org.pf4j.Plugin;

public class BSGPlugin extends Plugin {

	public BSGPlugin() {
		super();
	}

	@Override
	public void start() {
		PropertiesController.addProperties(
				BSGPlugin.class.getResourceAsStream("/org/openjfx/gradefx/plugin/bsg/bsg-plugin.properties"));
		UpdateController.register("bsg-plugin", () -> PropertiesController.getProperty("bsg-plugin.version"),
				PropertiesController.getProperty("bsg-plugin.url"));
		TranslationController.addBundle(ResourceBundle.getBundle("org.openjfx.gradefx.plugin.bsg.lang.bsg"));
	}

	@Override
	public void stop() {
		LogController.log(LogController.DEBUG, "bsg plugin stopped");
	}

}