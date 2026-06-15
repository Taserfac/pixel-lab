package com.pixellab.listener;

import com.pixellab.context.AppContextKeys;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;

public class RequestListener implements ServletRequestListener {
  @Override
  public void requestInitialized(ServletRequestEvent sre) {
    sre.getServletRequest().setAttribute(AppContextKeys.REQUEST_START_NANOS, System.nanoTime());
  }

  @Override
  public void requestDestroyed(ServletRequestEvent sre) {
  }
}
