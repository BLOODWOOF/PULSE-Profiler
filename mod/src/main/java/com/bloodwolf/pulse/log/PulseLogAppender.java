package com.bloodwolf.pulse.log;

import com.bloodwolf.pulse.collect.ErrorSink;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.LogManager;

public final class PulseLogAppender extends AbstractAppender {
	private PulseLogAppender(String name, Filter filter, Layout<?> layout) {
		super(name, filter, layout, true, Property.EMPTY_ARRAY);
	}

	public static void install() {
		try {
			LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
			PulseLogAppender appender = new PulseLogAppender("PulseErrors", null, PatternLayout.createDefaultLayout());
			appender.start();
			ctx.getConfiguration().addAppender(appender);
			ctx.getRootLogger().addAppender(ctx.getConfiguration().getAppender("PulseErrors"));
			ctx.updateLoggers();
		} catch (Exception ignored) {
			// logging hook is optional; uncaught handler still runs
		}
	}

	@Override
	public void append(LogEvent event) {
		ErrorSink.fromLog(event);
	}
}
