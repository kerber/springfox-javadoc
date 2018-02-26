/*
 *
 *  Copyright 2018-2019 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package springfox.javadoc.doclet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

// the NOSONAR comment is added to ignore sonar warning about usage of Sun classes
// because doclets can only be written using Sun classes
import com.sun.javadoc.AnnotationDesc; // NOSONAR
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;
import com.sun.javadoc.ThrowsTag;

import springfox.javadoc.plugin.JavadocBuilderPlugin;

/**
 * Generate properties file based on Javadoc.
 * <p>
 * The generated properties file will then be read by the
 * {@link JavadocBuilderPlugin} to enhance the Swagger documentation.
 */
public class SwaggerPropertiesDoclet {

    private SwaggerPropertiesDoclet() {}

    private static final String CLASSDIR_OPTION = "-classdir";
    private static final String EXCEPTION_REF_OPTION = "-exceptionRef";

    public static final String SPRINGFOX_JAVADOC_PROPERTIES = "META-INF/springfox.javadoc.properties";

    private static final String REQUEST_MAPPING = "org.springframework.web.bind.annotation.RequestMapping";
    private static final String REQUEST_GET_MAPPING = "org.springframework.web.bind.annotation.RequestMethod.GET";
    private static final String REQUEST_POST_MAPPING = "org.springframework.web.bind.annotation.RequestMethod.POST";
    private static final String REQUEST_PUT_MAPPING = "org.springframework.web.bind.annotation.RequestMethod.PUT";
    private static final String REQUEST_PATCH_MAPPING = "org.springframework.web.bind.annotation.RequestMethod.PATCH";
    private static final String REQUEST_DELETE_MAPPING = "org.springframework.web.bind.annotation.RequestMethod.DELETE";
    private static final String DELETE_MAPPING = "org.springframework.web.bind.annotation.DeleteMapping";
    private static final String GET_MAPPING = "org.springframework.web.bind.annotation.GetMapping";
    private static final String PATCH_MAPPING = "org.springframework.web.bind.annotation.PatchMapping";
    private static final String POST_MAPPING = "org.springframework.web.bind.annotation.PostMapping";
    private static final String PUT_MAPPING = "org.springframework.web.bind.annotation.PutMapping";
    private static final String RETURN = "@return";
    private static final String PATH = "path";
    private static final String VALUE = "value";
    private static final String NEWLINE = "\n";
    private static final String EMPTY = "";
    private static final String METHOD = "method";

    private static final String[] MAPPINGS = new String[] {DELETE_MAPPING, GET_MAPPING, PATCH_MAPPING, POST_MAPPING,
            PUT_MAPPING, REQUEST_MAPPING};

    private static final String[][] REQUEST_MAPPINGS = new String[][] {{REQUEST_DELETE_MAPPING, "DELETE"},
            {REQUEST_GET_MAPPING, "GET"}, {REQUEST_PATCH_MAPPING, "PATCH"}, {REQUEST_POST_MAPPING, "POST"},
            {REQUEST_PUT_MAPPING, "PUT"}};

    /**
     * See <a href=
     * "https://docs.oracle.com/javase/8/docs/technotes/guides/javadoc/doclet/overview.html#options">Using
     * custom command-line options</a>
     */
    private static String getClassDir(String[][] options) {
        for(String[] opt : options) {
            if(opt[0].equalsIgnoreCase(CLASSDIR_OPTION)) {
                return opt[1];
            }
        }
        return null;
    }

    /**
     * See <a href=
     * "https://docs.oracle.com/javase/8/docs/technotes/guides/javadoc/doclet/overview.html#options">Using
     * custom command-line options</a>
     */
    private static boolean getExceptionRef(String[][] options) {
        for(String[] opt : options) {
            if(opt[0].equalsIgnoreCase(EXCEPTION_REF_OPTION)) {
                return Boolean.valueOf(opt[1]);
            }
        }
        return false;
    }

    /**
     * See <a href=
     * "https://docs.oracle.com/javase/8/docs/technotes/guides/javadoc/doclet/overview.html#options">Using
     * custom command-line options</a>
     */
    public static int optionLength(String option) {
        int length = 0;
        if(option.equalsIgnoreCase(CLASSDIR_OPTION)) {
            length = 2;
        }
        if(option.equalsIgnoreCase(EXCEPTION_REF_OPTION)) {
            length = 2;
        }
        return length;
    }

    /**
     * See <a href=
     * "https://docs.oracle.com/javase/8/docs/technotes/guides/javadoc/doclet/overview.html#options">Using
     * custom command-line options</a>
     */
    public static boolean validOptions(String[][] options, DocErrorReporter reporter) {
        boolean foundClassDir = false;
        boolean foundExceptionRef = false;
        for(String[] opt : options) {
            if(opt[0].equalsIgnoreCase(CLASSDIR_OPTION)) {
                if(foundClassDir) {
                    reporter.printError("Only one -classdir option allowed.");
                    return false;
                } else {
                    foundClassDir = true;
                }
            }
            if(opt[0].equalsIgnoreCase(EXCEPTION_REF_OPTION)) {
                if(foundExceptionRef) {
                    reporter.printError("Only one -exceptionRef option allowed.");
                    return false;
                } else {
                    foundExceptionRef = true;
                }
            }
        }
        if(!foundClassDir) {
            reporter.printError(
                    "Usage: javadoc -classdir classes directory [-exceptionRef true|false (generate references to excetion classes)] -doclet  ...");
        }
        return foundClassDir;
    }

    /**
     * See <a href=
     * "https://docs.oracle.com/javase/8/docs/technotes/guides/javadoc/doclet/overview.html#simple">A
     * Simple Example Doclet</a>
     */
    public static boolean start(RootDoc root) {

        String classDir = getClassDir(root.options());
        if(classDir == null || classDir.length() == 0) {
            root.printError("No output location was specified");
            return false;
        } else {
            StringBuilder sb = new StringBuilder(classDir);
            if(!classDir.endsWith("/")) {
                sb.append("/");
            }
            sb.append(SPRINGFOX_JAVADOC_PROPERTIES);
            String out = sb.toString();
            root.printNotice("Writing output to " + out);
            File file = new File(out);
            file.getParentFile().mkdirs();
            OutputStream javadoc = null;
            try {
                javadoc = new FileOutputStream(file);
                Properties properties = new Properties();

                for(ClassDoc classDoc : root.classes()) {
                    sb.setLength(0);
                    String defaultRequestMethod = processClass(classDoc, sb);
                    String pathRoot = sb.toString();
                    for(MethodDoc methodDoc : classDoc.methods()) {
                        processMethod(properties, methodDoc, defaultRequestMethod, pathRoot,
                                getExceptionRef(root.options()));

                    }
                }
                properties.store(javadoc, "Springfox javadoc properties");
            } catch (IOException e) {
                root.printError(e.getMessage());
            } finally {
                if(javadoc != null) {
                    try {
                        javadoc.close();
                    } catch (IOException e) {
                        // close for real
                    }
                }
            }
        }
        return true;
    }

    private static String processClass(ClassDoc classDoc, StringBuilder pathRoot) {
        String defaultRequestMethod = null;
        for(AnnotationDesc annotationDesc : classDoc.annotations()) {
            if(REQUEST_MAPPING.equals(annotationDesc.annotationType().qualifiedTypeName())) {
                for(AnnotationDesc.ElementValuePair pair : annotationDesc.elementValues()) {

                    if(VALUE.equals(pair.element().name()) || PATH.equals(pair.element().name())) {
                        setRoot(pathRoot, pair);
                    }
                    if(METHOD.equals(pair.element().name())) {
                        defaultRequestMethod = pair.value().toString();
                    }
                }
                break;
            }
        }
        return defaultRequestMethod;
    }

    private static void setRoot(StringBuilder pathRoot, AnnotationDesc.ElementValuePair pair) {
        String value = pair.value().toString().replaceAll("\"$|^\"", "");
        if(!value.startsWith("/")) {
            pathRoot.append("/");
        }
        if(value.endsWith("/")) {
            pathRoot.append(value.substring(0, value.length() - 1));
        } else {
            pathRoot.append(value);
        }
    }

    private static void processMethod(Properties properties, MethodDoc methodDoc, String defaultRequestMethod,
            String pathRoot, boolean excetionRef) {
        for(AnnotationDesc annotationDesc : methodDoc.annotations()) {
            String annotationType = annotationDesc.annotationType().toString();
            if(isMapping(annotationType)) {
                StringBuilder path = new StringBuilder();
                for (String pathRequest : getRequestPathValues(annotationDesc)) {
                    String requestMethod = getRequestMethod(annotationDesc, annotationType, defaultRequestMethod);
                    if(requestMethod != null) {
                        path.setLength(0);
                        path.append(pathRoot);
                        appendPath(path,pathRequest);
                        MethodDoc relevantMethod = methodDoc;
                        if (methodDoc.commentText() == null || "".equals(methodDoc.commentText())) {
                            relevantMethod = findMatchingInterfaceMethodDoc(methodDoc.containingClass().interfaces(), methodDoc);
                        }
                        if ( relevantMethod == null ) {
                            relevantMethod = methodDoc;
                        }
                        path.append(requestMethod);
                        saveProperty(properties, path.toString() + ".notes", relevantMethod.commentText());

                        for(ParamTag paramTag : relevantMethod.paramTags()) {
                            saveProperty(properties, path.toString() + ".param." + paramTag.parameterName(),
                                    paramTag.parameterComment());
                        }
                        for(Tag tag : relevantMethod.tags()) {
                            if(tag.name().equals(RETURN)) {
                                saveProperty(properties, path.toString() + ".return", tag.text());
                                break;
                            }
                        }
                        if(excetionRef) {
                            processThrows(properties, relevantMethod.throwsTags(), path);
                        }
                    }
                }
            }
        }
    }

    private static String[] getRequestPathValues(AnnotationDesc annotationDesc) {
        List<String> pathValues = new ArrayList<String>();
        for(AnnotationDesc.ElementValuePair pair : annotationDesc.elementValues()) {
            if(VALUE.equals(pair.element().name()) || PATH.equals(pair.element().name())) {
                String values = pair.value().toString().replaceAll("^\\{|\\}$", "");
                StringBuilder sb = new StringBuilder();
                return values.split(",\\ ");
            }
        }
        return new String[0];
    }

    private static void appendPath(StringBuilder path, String requestPath) {
        String value = requestPath.replaceAll("^\"|\"$", "").replaceAll("\\\\\\\\", "\\\\");
        if(value.startsWith("/")) {
            path.append(value).append(".");
        } else {
            path.append("/").append(value).append(".");
        }
    }

    /**
     * Scan the given list of ClassDoc for a MethodDoc with a matching signature.<br/>
     * <br/>
     * See <a href=
     * "https://github.com/gwtproject/gwt/blob/master/build_tools/doctool/src/com/google/doctool/Booklet.java#L436"/>
     * <b>Project</b>: GWT Open Source Project <b>File</b>: Booklet.java</a>
     */
    private static MethodDoc findMatchingInterfaceMethodDoc(ClassDoc[] interfaces, MethodDoc methodDoc) {
        if (interfaces != null) {
            // Look through the methods on superInterface for a matching methodDoc.
            //
            for (int intfIndex = 0; intfIndex < interfaces.length; ++intfIndex) {
                ClassDoc currentIntfDoc = interfaces[intfIndex];
                MethodDoc[] intfMethodDocs = currentIntfDoc.methods();
                for (int methodIndex = 0; methodIndex < intfMethodDocs.length; ++methodIndex) {
                    MethodDoc intfMethodDoc = intfMethodDocs[methodIndex];
                    String methodDocName = methodDoc.name();
                    String intfMethodDocName = intfMethodDoc.name();
                    if (methodDocName.equals(intfMethodDocName)) {
                        if (methodDoc.signature().equals(intfMethodDoc.signature())) {
                            // It's a match!
                            //
                            return intfMethodDoc;
                        }
                    }
                }

                // Try the superinterfaces of this interface.
                //
                MethodDoc foundMethodDoc = findMatchingInterfaceMethodDoc( currentIntfDoc.interfaces(), methodDoc);
                if (foundMethodDoc != null) {
                    return foundMethodDoc;
                }
            }
        }
        // Just didn't find it anywhere. Must not be based on an implemented
        // interface.
        //
        return null;
    }


    private static boolean isMapping(String name) {
        for(String mapping : MAPPINGS) {
            if(mapping.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static String getRequestMethod(AnnotationDesc annotationDesc, String name, String defaultRequestMethod) {
        if(REQUEST_MAPPING.equals(name)) {
            for(AnnotationDesc.ElementValuePair pair : annotationDesc.elementValues()) {
                if(METHOD.equals(pair.element().name())) {
                    return resolveRequestMethod(pair, defaultRequestMethod);
                }
            }
        } else if(PUT_MAPPING.equals(name)) {
            return "PUT";
        } else if(POST_MAPPING.equals(name)) {
            return "POST";
        } else if(PATCH_MAPPING.equals(name)) {
            return "PATCH";
        } else if(GET_MAPPING.equals(name)) {
            return "GET";
        } else if(DELETE_MAPPING.equals(name)) {
            return "DELETE";
        }
        return defaultRequestMethod;
    }

    private static String resolveRequestMethod(AnnotationDesc.ElementValuePair pair, String defaultRequestMethod) {
        String value = pair.value().toString();
        for(int i = 0; i < REQUEST_MAPPINGS.length; ++i) {
            if(REQUEST_MAPPINGS[i][0].equals(value)) {
                return REQUEST_MAPPINGS[i][1];
            }
        }
        return defaultRequestMethod;
    }

    private static void processThrows(Properties properties, ThrowsTag[] throwsTags, StringBuilder path) {
        for(int i = 0; i < throwsTags.length; i++) {
            String key = path.toString() + ".throws." + i;
            String value = throwsTags[i].exceptionType().typeName() + "-" + throwsTags[i].exceptionComment();
            saveProperty(properties, key, value);
        }
    }

    private static void saveProperty(Properties properties, String key, String value) {
        value = value.replaceAll(NEWLINE, EMPTY);
        if(value.length() > 0) {
            properties.setProperty(key, value);
        }
    }
}