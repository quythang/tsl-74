package com.ts.platform.reflection.vfs;


import com.ts.platform.reflection.Reflections;
import com.ts.platform.reflection.util.Utils;
import com.ts.platform.utils.collect.Lists;
import com.ts.platform.utils.util.Iterables;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.jar.JarFile;


public abstract class Vfs {
    private static List<UrlType> defaultUrlTypes = Lists.<UrlType>newArrayList(DefaultUrlTypes.values());

    /** an abstract vfs dir */
    public interface Dir {
        String getPath();
        Iterable<File> getFiles();
        void close();
    }

    /** an abstract vfs file */
    public interface File {
        String getName();
        String getRelativePath();
        InputStream openInputStream() throws IOException;
    }

    /** a matcher and factory for a url */
    public interface UrlType {
        boolean matches(URL url) throws Exception;
        Dir createDir(URL url) throws Exception;
    }

    public static List<UrlType> getDefaultUrlTypes() {
        return defaultUrlTypes;
    }

    public static void setDefaultURLTypes(final List<UrlType> urlTypes) {
        defaultUrlTypes = urlTypes;
    }

    public static void addDefaultURLTypes(UrlType urlType) {
        defaultUrlTypes.add(0, urlType);
    }

    public static Dir fromURL(final URL url) {
        return fromURL(url, defaultUrlTypes);
    }

    public static Dir fromURL(final URL url, final List<UrlType> urlTypes) {
        for (UrlType type : urlTypes) {
            try {
                if (type.matches(url)) {
                    Dir dir = type.createDir(url);
                    if (dir != null) return dir;
                }
            } catch (Throwable e) {
                if (Reflections.log != null) {
                    Reflections.log.debug("could not create Dir using " + type + " from url " + url.toExternalForm() + ". skipping.", e);
                }
            }
        }

        throw new RuntimeException("could not create Vfs.Dir from url, no matching UrlType was found [" + url.toExternalForm() + "]\n" +
                "either use fromURL(final URL url, final List<UrlType> urlTypes) or " +
                "use the static setDefaultURLTypes(final List<UrlType> urlTypes) or addDefaultURLTypes(UrlType urlType) " +
                "with your specialized UrlType.");
    }

    /** tries to create a Dir from the given url, using the given urlTypes*/
    public static Dir fromURL(final URL url, final UrlType... urlTypes) {
        return fromURL(url, Lists.<UrlType>newArrayList(urlTypes));
    }

    public static Iterable<File> findFiles(final Collection<URL> inUrls, final String packagePrefix, final Predicate<String> nameFilter) {
        Predicate<File> fileNamePredicate = new Predicate<File>() {
            public boolean test(File file) {
                String path = file.getRelativePath();
                if (path.startsWith(packagePrefix)) {
                    String filename = path.substring(path.indexOf(packagePrefix) + packagePrefix.length());
                    return !Utils.isEmpty(filename) && nameFilter.test(filename.substring(1));
                } else {
                    return false;
                }
            }
        };

        return findFiles(inUrls, fileNamePredicate);
    }

    public static Iterable<File> findFiles(final Collection<URL> inUrls, final Predicate<File> filePredicate) {
        Iterable<File> result = new ArrayList<File>();

        for (final URL url : inUrls) {
            try {
                result = Iterables.concat(result,
                        Iterables.filter(new Iterable<File>() {
                            public Iterator<File> iterator() {
                                return fromURL(url).getFiles().iterator();
                            }
                        }, filePredicate));
            } catch (Throwable e) {
                if (Reflections.log != null) {
                    Reflections.log.warn("could not findFiles for url. continuing. [{}]", url, e);
                }
            }
        }

        return result;
    }

    public static  java.io.File getFile(URL url) {
        java.io.File file;
        String path;

        try {
            path = url.toURI().getSchemeSpecificPart();
            if ((file = new java.io.File(path)).exists()) return file;
        } catch (URISyntaxException e) {
        }

        try {
            path = URLDecoder.decode(url.getPath(), "UTF-8");
            if (path.contains(".jar!")) path = path.substring(0, path.lastIndexOf(".jar!") + ".jar".length());
            if ((file = new java.io.File(path)).exists()) return file;

        } catch (UnsupportedEncodingException e) {
        }

        try {
            path = url.toExternalForm();
            if (path.startsWith("jar:")) path = path.substring("jar:".length());
            if (path.startsWith("wsjar:")) path = path.substring("wsjar:".length());
            if (path.startsWith("file:")) path = path.substring("file:".length());
            if (path.contains(".jar!")) path = path.substring(0, path.indexOf(".jar!") + ".jar".length());
            if (path.contains(".war!")) path = path.substring(0, path.indexOf(".war!") + ".war".length());
            if ((file = new java.io.File(path)).exists()) return file;

            path = path.replace("%20", " ");
            if ((file = new java.io.File(path)).exists()) return file;

        } catch (Exception e) {
        }

        return null;
    }
    
    private static boolean hasJarFileInPath(URL url) {
		return url.toExternalForm().matches(".*\\.jar(\\!.*|$)");
	}


    public static enum DefaultUrlTypes implements UrlType {
        jarFile {
            public boolean matches(URL url) {
                return url.getProtocol().equals("file") && hasJarFileInPath(url);
            }

            public Dir createDir(final URL url) throws Exception {
                return new ZipDir(new JarFile(getFile(url)));
            }
        },

        jarUrl {
            public boolean matches(URL url) {
                return "jar".equals(url.getProtocol()) || "zip".equals(url.getProtocol()) || "wsjar".equals(url.getProtocol());
            }

            public Dir createDir(URL url) throws Exception {
                try {
                    URLConnection urlConnection = url.openConnection();
                    if (urlConnection instanceof JarURLConnection) {
                        urlConnection.setUseCaches(false);
                        return new ZipDir(((JarURLConnection) urlConnection).getJarFile());
                    }
                } catch (Throwable e) { /*fallback*/ }
                java.io.File file = getFile(url);
                return file != null ? new ZipDir(new JarFile(file)) : null;
            }
        },

        directory {
            public boolean matches(URL url) {
                if (url.getProtocol().equals("file") && !hasJarFileInPath(url)) {
                    java.io.File file = getFile(url);
                    return file != null && file.isDirectory();
                } else return false;
            }

            public Dir createDir(final URL url) throws Exception {
                return new SystemDir(getFile(url));
            }
        },

        jboss_vfs {
            public boolean matches(URL url) {
                return url.getProtocol().equals("vfs");
            }

            public Dir createDir(URL url) throws Exception {
                return null;
            }
        },

        jboss_vfsfile {
            public boolean matches(URL url) throws Exception {
                return "vfszip".equals(url.getProtocol()) || "vfsfile".equals(url.getProtocol());
            }

            public Dir createDir(URL url) throws Exception {
                return null;
            }
        },

        bundle {
            public boolean matches(URL url) throws Exception {
                return url.getProtocol().startsWith("bundle");
            }

            public Dir createDir(URL url) throws Exception {
                return  null;
            }
        },

        jarInputStream {
            public boolean matches(URL url) throws Exception {
                return url.toExternalForm().contains(".jar");
            }

            public Dir createDir(final URL url) throws Exception {
                return new JarInputDir(url);
            }
        }
    }
}
