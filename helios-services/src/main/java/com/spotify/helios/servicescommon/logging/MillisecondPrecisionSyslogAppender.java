/**
 * Copyright (C) 2012 Spotify AB
 */

package com.spotify.helios.servicescommon.logging;

import java.io.IOException;
import java.io.OutputStream;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.net.SyslogAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.Layout;

import static com.google.common.base.Charsets.UTF_8;

/**
 * A {@link ch.qos.logback.classic.net.SyslogAppender} with millisecond timestamp precision.
 */
public class MillisecondPrecisionSyslogAppender extends SyslogAppender {

  PatternLayout stackTraceLayout = new PatternLayout();


  public void start() {
    super.start();
    setupStackTraceLayout();
  }

  String getPrefixPattern() {
    return "%syslogStart{" + getFacility() + "}%nopex{}";
  }

  @Override
  protected void postProcess(final Object eventObject, final OutputStream sw) {
    if (isThrowableExcluded()) {
      return;
    }

    final ILoggingEvent event = (ILoggingEvent) eventObject;
    IThrowableProxy tp = event.getThrowableProxy();

    if (tp == null) {
      return;
    }

    final String stackTracePrefix = stackTraceLayout.doLayout(event);
    boolean isRootException = true;
    while (tp != null) {
      final StackTraceElementProxy[] stepArray = tp.getStackTraceElementProxyArray();
      try {
        handleThrowableFirstLine(sw, tp, stackTracePrefix, isRootException);
        isRootException = false;
        for (final StackTraceElementProxy step : stepArray) {
          final StringBuilder sb = new StringBuilder();
          sb.append(stackTracePrefix).append(step);
          sw.write(sb.toString().getBytes(UTF_8));
          sw.flush();
        }
      } catch (IOException e) {
        break;
      }
      tp = tp.getCause();
    }
  }

  private void handleThrowableFirstLine(final OutputStream sw, final IThrowableProxy tp,
                                        final String stackTracePrefix,
                                        final boolean isRootException)
      throws IOException {
    final StringBuilder sb = new StringBuilder().append(stackTracePrefix);

    if (!isRootException) {
      sb.append(CoreConstants.CAUSED_BY);
    }
    sb.append(tp.getClassName()).append(": ").append(tp.getMessage());
    sw.write(sb.toString().getBytes(UTF_8));
    sw.flush();
  }

  @Override
  public Layout<ILoggingEvent> buildLayout() {
    final PatternLayout layout = new PatternLayout();
    layout.getInstanceConverterMap().put("syslogStart",
                                         MillisecondPrecisionSyslogStartConverter.class.getName());
    if (suffixPattern == null) {
      suffixPattern = DEFAULT_SUFFIX_PATTERN;
    }
    layout.setPattern(getPrefixPattern() + suffixPattern);
    layout.setContext(getContext());
    layout.start();
    return layout;
  }

  private void setupStackTraceLayout() {
    stackTraceLayout.getInstanceConverterMap().put(
        "syslogStart", MillisecondPrecisionSyslogStartConverter.class.getName());

    stackTraceLayout.setPattern(getPrefixPattern() + getStackTracePattern());
    stackTraceLayout.setContext(getContext());
    stackTraceLayout.start();
  }
}