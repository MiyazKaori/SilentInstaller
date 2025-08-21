package io.github.miyazkaori.silentinstaller.logger;

import android.util.*;
import java.util.*;

public final class AppLog {

	private static int minPriority = Log.ASSERT + 1;

	public static void setMinPriority(int priority) {
		AppLog.minPriority = priority;
	}

	public static void v(String message, Object... args) {
		log(Log.VERBOSE, message, args);
	}

	public static void d(String message, Object... args) {
		log(Log.DEBUG, message, args);
	}

	public static void i(String message, Object... args) {
		log(Log.INFO, message, args);
	}

	public static void w(String message, Object... args) {
		log(Log.WARN, message, args);
	}

	public static void w(Throwable t, String message, Object... args) {
		log(Log.WARN, t, message, args);
	}

	public static void e(String message, Object... args) {
		log(Log.ERROR, message, args);
	}

	public static void e(Throwable t, String message, Object... args) {
		log(Log.ERROR, t, message, args);
	}

	private static void log(int priority, String message, Object... args) {
		log(priority, null, message, args);
	}

	private static void log(int priority, Throwable t, String message, Object... args) {
		if (priority < minPriority) {
			return;
		}

		String tag = getTag();
		message = formatMessage(t, message, args);
		Log.println(priority, tag, message);
	}

	private static String getTag() {
		StackTraceElement[] stack = new Throwable().getStackTrace();
		for (StackTraceElement element : stack) {
			if (!element.getClassName().equals(AppLog.class.getCanonicalName())) {
				String className = element.getClassName();
				return className.substring(className.lastIndexOf(".") + 1);
			}
		}
		return "AppLog";
	}

	private static String formatMessage(Throwable t, String message, Object... args) {
		if (message == null) {
			message = "null";
		}

		StringBuilder sb = new StringBuilder(message.length() + 64);

		if (args != null && args.length > 0) {
			try {
				sb.append(String.format(message, args));
			} catch (Exception e) {
				sb.append(" [Bad message] ").append(Arrays.toString(args));
			}
		} else {
			sb.append(message);
		}

		if (t != null) {
			sb.append("\n").append(Log.getStackTraceString(t));
		}

		return sb.toString();
	}
}
