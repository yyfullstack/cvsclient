/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package org.netbeans.lib.cvsclient.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Milos Kleint, Thomas Singer
 */
public class DefaultIgnoreFileFilter implements IgnoreFileFilter {
    private static final long serialVersionUID = -5534031149474237036L;

    private final List<StringPattern> patterns = new LinkedList<StringPattern>();

    private final List<StringPattern> localPatterns = new LinkedList<StringPattern>();
    private boolean processGlobalPatterns = true;
    private boolean processLocalPatterns = false;
    private File lastDirectory = null;

    public DefaultIgnoreFileFilter() {
    }

    /**
     * Creates new DefaultIgnoreFileFilter and fills in patterns.
     * 
     * @param patternList
     *            - list of objects, patterns are retrieved via the
     *            Object.toString() method.
     */
    public DefaultIgnoreFileFilter(final List<StringPattern> patternList) {
        for (final StringPattern stringPattern : patternList) {
            final String patternString = stringPattern.toString();
            final SimpleStringPattern pattern = new SimpleStringPattern(patternString);
            addPattern(pattern);
        }
    }

    /**
     * Adds a StringPattern to the list of ignore file patters.
     */
    public void addPattern(final StringPattern pattern) {
        if (pattern.toString().equals("!")) { // NOI18N
            clearPatterns();
        } else {
            patterns.add(pattern);
        }
    }

    /**
     * Adds a string to the list of ignore file patters using the
     * SimpleStringPattern.
     */
    public void addPattern(final String pattern) {
        if (pattern.equals("!")) { // NOI18N
            clearPatterns();
        } else {
            patterns.add(new SimpleStringPattern(pattern));
        }
    }

    /**
     * Clears the list of patters. To be used when the "!" character is used in
     * any of the .cvsignore lists.
     */
    public void clearPatterns() {
        patterns.clear();
    }

    /**
     * A file is checked against the patterns in the filter. If any of these
     * matches, the file should be ignored. A file will also be ignored, if its
     * name matches any local <code>.cvsignore</code> file entry.
     * 
     * @param directory
     *            is a file object that refers to the directory the file resides
     *            in.
     * @param noneCvsFile
     *            is the name of the file to be checked.
     */
    public boolean shouldBeIgnored(final File directory, final String noneCvsFile) {
        // current implementation ignores the directory parameter.
        // in future or different implementations can add the directory
        // dependant .cvsignore lists
        if (lastDirectory != directory) {
            lastDirectory = directory;
            processGlobalPatterns = true;
            processLocalPatterns = false;
            localPatterns.clear();
            final String filename = directory.getPath() + File.separator + ".cvsignore"; // NOI18N
            final File cvsIgnoreFile = new File(filename);
            if (cvsIgnoreFile.exists()) {
                try {
                    final List<String> list = parseCvsIgnoreFile(cvsIgnoreFile);
                    for (final String string : list) {
                        final String s = string.toString();
                        if (s.equals("!")) { // NOI18N
                            processGlobalPatterns = false;
                            localPatterns.clear();
                        } else {
                            localPatterns.add(new SimpleStringPattern(s));
                        }
                    }
                } catch (final IOException ex) {
                    // ignore exception
                }
            }
            processLocalPatterns = localPatterns.size() > 0;
        }
        if (processGlobalPatterns) {
            for (final StringPattern stringPattern : patterns) {
                final StringPattern pattern = stringPattern;
                if (pattern.doesMatch(noneCvsFile)) {
                    return true;
                }
            }
        }
        if (processLocalPatterns) {
            for (final StringPattern stringPattern : localPatterns) {
                final StringPattern pattern = stringPattern;
                if (pattern.doesMatch(noneCvsFile)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Utility method that reads the .cvsignore file and returns a list of
     * Strings. These strings represent the patterns read from the file.
     */
    public static List<String> parseCvsIgnoreFile(final File cvsIgnoreFile) throws IOException, FileNotFoundException {
        BufferedReader reader = null;
        final List<String> toReturn = new LinkedList<String>();
        try {
            reader = new BufferedReader(new FileReader(cvsIgnoreFile));
            String line;
            while ((line = reader.readLine()) != null) {
                final StringTokenizer token = new StringTokenizer(line, " ", false); // NOI18N
                while (token.hasMoreTokens()) {
                    final String tok = token.nextToken();
                    toReturn.add(tok);
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return toReturn;
    }
}
