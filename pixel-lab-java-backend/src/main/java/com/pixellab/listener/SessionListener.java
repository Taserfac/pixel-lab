package com.pixellab.listener;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.concurrent.atomic.AtomicInteger;

public class SessionListener implements HttpSessionListener {
  private static final AtomicInteger ONLINE_COUNT = new AtomicInteger(0);

  public static int getOnlineCount() {
    return ONLINE_COUNT.get();
  }

  @Override
  public void sessionCreated(HttpSessionEvent se) {
    int count = ONLINE_COUNT.incrementAndGet();
    se.getSession().getServletContext().log("[Pixel Lab] Session created: " + se.getSession().getId() + ", online=" + count);
  }

  @Override
  public void sessionDestroyed(HttpSessionEvent se) {
    int count = Math.max(0, ONLINE_COUNT.decrementAndGet());
    se.getSession().getServletContext().log("[Pixel Lab] Session destroyed: " + se.getSession().getId() + ", online=" + count);
  }
}
