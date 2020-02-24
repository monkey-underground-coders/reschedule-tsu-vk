package com.a6raywa1cher.rescheduletsuvk.component.router;

public class PathMethods {
	public static String normalizePath(String path) {
		if (path == null || path.equals("")) {
			return "/";
		}
		if (path.charAt(path.length() - 1) != '/') {
			path = path + '/';
		}
		if (path.charAt(0) != '/') {
			path = '/' + path;
		}
		return path;
	}

	public static String resolve(String prefix, String suffix) {
		prefix = normalizePath(prefix);
		suffix = normalizePath(suffix);
		return prefix.substring(0, prefix.length() - 1) + suffix;
	}
}
