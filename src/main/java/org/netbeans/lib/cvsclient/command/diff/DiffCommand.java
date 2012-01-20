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

package org.netbeans.lib.cvsclient.command.diff;

import java.io.File;

import org.netbeans.lib.cvsclient.ClientServices;
import org.netbeans.lib.cvsclient.command.BasicCommand;
import org.netbeans.lib.cvsclient.command.Builder;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;
import org.netbeans.lib.cvsclient.event.EventManager;
import org.netbeans.lib.cvsclient.event.TerminationEvent;
import org.netbeans.lib.cvsclient.request.ArgumentRequest;
import org.netbeans.lib.cvsclient.request.CommandRequest;

/**
 * The status command looks up the status of files in the repository
 * 
 * @author Robert Greig
 */
public class DiffCommand extends BasicCommand {
    /**
     * 
     */
    private static final long serialVersionUID = -5916219365477075719L;

    /**
     * The event manager to use
     */
    protected EventManager eventManager;

    /**
     * Holds value of property beforeDate.
     */
    private String beforeDate1;

    /**
     * Holds value of property firstRevision.
     */
    private String revision1;

    /**
     * Holds value of property secondRevision.
     */
    private String revision2;

    /**
     * Holds value of property beforeDate2.
     */
    private String beforeDate2;

    /**
     * Keyword substitution. The -k switch in command line cvs.
     */
    private String keywordSubst;

    /** Holds value of property ignoreAllWhitespace. */
    private boolean ignoreAllWhitespace;

    /** Holds value of property ignoreBlankLines. */
    private boolean ignoreBlankLines;

    /** Holds value of property ignoreCase. */
    private boolean ignoreCase;

    /** Holds value of property ignoreSpaceChange. */
    private boolean ignoreSpaceChange;

    /** Holds value of property contextDiff. */
    private boolean contextDiff;

    /** Holds value of property unifiedDiff. */
    private boolean unifiedDiff;

    /**
     * Construct a new diff command
     */
    public DiffCommand() {
    }

    /**
     * Create a builder for this command.
     * 
     * @param eventMan
     *            the event manager used to receive events.
     */
    @Override
    public Builder createBuilder(final EventManager eventMan) {
        if (isContextDiff() || isUnifiedDiff()) {
            return null;
        }
        return new SimpleDiffBuilder(eventMan, this);
    }

    /**
     * Execute a command
     * 
     * @param client
     *            the client services object that provides any necessary
     *            services to this command, including the ability to actually
     *            process all the requests.
     */
    @Override
    public void execute(final ClientServices client, final EventManager em) throws CommandException,
                    AuthenticationException {
        client.ensureConnection();

        eventManager = em;

        super.execute(client, em);

        try {
            // parameters come now..
            addRDSwitches();
            if ((getKeywordSubst() != null) && !getKeywordSubst().equals("")) { // NOI18N
                requests.add(new ArgumentRequest("-k" + getKeywordSubst())); // NOI18N
            }

            addArgumentRequest(isIgnoreAllWhitespace(), "-w"); // NOI18N
            addArgumentRequest(isIgnoreBlankLines(), "-B"); // NOI18N
            addArgumentRequest(isIgnoreSpaceChange(), "-b"); // NOI18N
            addArgumentRequest(isIgnoreCase(), "-i"); // NOI18N
            addArgumentRequest(isContextDiff(), "-c"); // NOI18N
            addArgumentRequest(isUnifiedDiff(), "-u"); // NOI18N

            addRequestForWorkingDirectory(client);
            addArgumentRequests();
            addRequest(CommandRequest.DIFF);
            client.processRequests(requests);
        } catch (final CommandException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new CommandException(ex, ex.getLocalizedMessage());
        } finally {
            requests.clear();
        }
    }

    /**
     * includes the logic of setting the -r and -D switches to the diff command
     */
    private void addRDSwitches() {
        if (getRevision2() != null) {
            requests.add(1, new ArgumentRequest("-r")); // NOI18N
            requests.add(2, new ArgumentRequest(getRevision2()));
        } else {
            if (getBeforeDate2() != null) {
                requests.add(1, new ArgumentRequest("-D " + getBeforeDate2())); // NOI18N
            }
        }
        // -r switch has precendence over the -d switch - is that right??
        if (getRevision1() != null) {
            requests.add(1, new ArgumentRequest("-r")); // NOI18N
            requests.add(2, new ArgumentRequest(getRevision1()));
        } else {
            if (getBeforeDate1() != null) {
                requests.add(1, new ArgumentRequest("-D " + getBeforeDate1())); // NOI18N
            } else {
                // when neither revision nor flag is set for the command, it is
                // assumed
                // that the second parameters are not set either..
                return;
            }
        }
    }

    /**
     * called when server responses with "ok" or "error", (when the command
     * finishes)
     */
    @Override
    public void commandTerminated(final TerminationEvent e) {
        if (builder != null) {
            builder.outputDone();
        }
    }

    /**
     * Getter for property beforeDate.
     * 
     * @return Value of property beforeDate.
     */
    public String getBeforeDate1() {
        return beforeDate1;
    }

    /**
     * Setter for property beforeDate.
     * 
     * @param beforeDate
     *            New value of property beforeDate.
     */
    public void setBeforeDate1(final String beforeDate) {
        beforeDate1 = beforeDate;
    }

    /**
     * Getter for property firstRevision.
     * 
     * @return Value of property firstRevision.
     */
    public String getRevision1() {
        return revision1;
    }

    /**
     * Setter for property firstRevision.
     * 
     * @param firstRevision
     *            New value of property firstRevision.
     */
    public void setRevision1(final String firstRevision) {
        revision1 = firstRevision;
    }

    /**
     * Getter for property secondRevision.
     * 
     * @return Value of property secondRevision.
     */
    public String getRevision2() {
        return revision2;
    }

    /**
     * Setter for property secondRevision.
     * 
     * @param secondRevision
     *            New value of property secondRevision.
     */
    public void setRevision2(final String secondRevision) {
        revision2 = secondRevision;
    }

    /**
     * Getter for property beforeDate2.
     * 
     * @return Value of property beforeDate2.
     */
    public String getBeforeDate2() {
        return beforeDate2;
    }

    /**
     * Setter for property beforeDate2.
     * 
     * @param beforeDate2
     *            New value of property beforeDate2.
     */
    public void setBeforeDate2(final String beforeDate2) {
        this.beforeDate2 = beforeDate2;
    }

    /**
     * Getter for property keywordSubst.
     * 
     * @return Value of property keywordSubst.
     */
    public String getKeywordSubst() {
        return keywordSubst;
    }

    /**
     * Setter for property keywordSubst.
     * 
     * @param keywordSubst
     *            New value of property keywordSubst.
     */
    public void setKeywordSubst(final String keywordSubst) {
        this.keywordSubst = keywordSubst;
    }

    /**
     * This method returns how the command would looklike when typed on the
     * command line. Each command is responsible for constructing this
     * information.
     * 
     * @returns <command's name> [<parameters>] files/dirs. Example: checkout -p
     *          CvsCommand.java
     * 
     */
    @Override
    public String getCVSCommand() {
        final StringBuffer toReturn = new StringBuffer("diff "); // NOI18N
        toReturn.append(getCVSArguments());
        final File[] files = getFiles();
        if (files != null) {
            for (final File file : files) {
                toReturn.append(file.getName() + " "); // NOI18N
            }
        }
        return toReturn.toString();
    }

    /**
     * takes the arguments and sets the command. To be mainly used for automatic
     * settings (like parsing the .cvsrc file)
     * 
     * @return true if the option (switch) was recognized and set
     */
    @Override
    public boolean setCVSCommand(final char opt, final String optArg) {
        if (opt == 'R') {
            setRecursive(true);
        } else if (opt == 'l') {
            setRecursive(false);
        } else if (opt == 'r') {
            if (getRevision1() == null) {
                setRevision1(optArg);
            } else {
                setRevision2(optArg);
            }
        } else if (opt == 'D') {
            if (getBeforeDate1() == null) {
                setBeforeDate1(optArg);
            } else {
                setBeforeDate2(optArg);
            }
        } else if (opt == 'k') {
            setKeywordSubst(optArg);
        } else if (opt == 'w') {
            setIgnoreAllWhitespace(true);
        } else if (opt == 'b') {
            setIgnoreSpaceChange(true);
        } else if (opt == 'B') {
            setIgnoreBlankLines(true);
        } else if (opt == 'i') {
            setIgnoreCase(true);
        } else if (opt == 'c') {
            setContextDiff(true);
        } else if (opt == 'u') {
            setUnifiedDiff(true);
        } else {
            return false;
        }
        return true;
    }

    /**
     * String returned by this method defines which options are available for
     * this particular command
     */
    @Override
    public String getOptString() {
        return "Rlr:D:k:wBbicu"; // NOI18N
    }

    /**
     * resets all switches in the command. After calling this method, the
     * command should have no switches defined and should behave defaultly.
     */
    @Override
    public void resetCVSCommand() {
        setRecursive(true);
        setRevision1(null);
        setRevision2(null);
        setBeforeDate1(null);
        setBeforeDate2(null);
        setKeywordSubst(null);
        setIgnoreAllWhitespace(false);
        setIgnoreBlankLines(false);
        setIgnoreCase(false);
        setIgnoreSpaceChange(false);
        setContextDiff(false);
        setUnifiedDiff(false);
    }

    /**
     * Returns the arguments of the command in the command-line style. Similar
     * to getCVSCommand() however without the files and command's name
     */
    @Override
    public String getCVSArguments() {
        final StringBuffer toReturn = new StringBuffer(""); // NOI18N
        if ((getKeywordSubst() != null) && (getKeywordSubst().length() > 0)) {
            toReturn.append("-k" + getKeywordSubst() + " "); // NOI18N
        }
        if (!isRecursive()) {
            toReturn.append("-l "); // NOI18N
        }
        if (getRevision1() != null) {
            toReturn.append("-r " + getRevision1() + " "); // NOI18N
        }
        if (getBeforeDate1() != null) {
            toReturn.append("-D " + getBeforeDate1() + " "); // NOI18N
        }
        if (getRevision2() != null) {
            toReturn.append("-r " + getRevision2() + " "); // NOI18N
        }
        if (getBeforeDate2() != null) {
            toReturn.append("-D " + getBeforeDate2() + " "); // NOI18N
        }
        if (isIgnoreAllWhitespace()) {
            toReturn.append("-w "); // NOI18N
        }
        if (isIgnoreBlankLines()) {
            toReturn.append("-B "); // NOI18N
        }
        if (isIgnoreCase()) {
            toReturn.append("-i "); // NOI18N
        }
        if (isIgnoreSpaceChange()) {
            toReturn.append("-b "); // NOI18N
        }
        if (isContextDiff()) {
            toReturn.append("-c ");// NOI18N
        }
        if (isUnifiedDiff()) {
            toReturn.append("-u ");// NOI18N
        }
        return toReturn.toString();
    }

    /**
     * true if all the whitespace differences should be ignored. (-w)
     * 
     * @return Value of property ignoreAllWhitespace.
     */
    public boolean isIgnoreAllWhitespace() {
        return ignoreAllWhitespace;
    }

    /**
     * Setter for property ignoreAllWhitespace. true if all the whitespace
     * differences should be ignored. (-w)
     * 
     * @param ignoreAllWhitespace
     *            New value of property ignoreAllWhitespace.
     */
    public void setIgnoreAllWhitespace(final boolean ignoreAllWhitespace) {
        this.ignoreAllWhitespace = ignoreAllWhitespace;
    }

    /**
     * Getter for property ignoreBlankLines.
     * 
     * @return Value of property ignoreBlankLines.
     */
    public boolean isIgnoreBlankLines() {
        return ignoreBlankLines;
    }

    /**
     * Setter for property ignoreBlankLines.
     * 
     * @param ignoreBlankLines
     *            New value of property ignoreBlankLines.
     */
    public void setIgnoreBlankLines(final boolean ignoreBlankLines) {
        this.ignoreBlankLines = ignoreBlankLines;
    }

    /**
     * Getter for property ignoreCase.
     * 
     * @return Value of property ignoreCase.
     */
    public boolean isIgnoreCase() {
        return ignoreCase;
    }

    /**
     * Setter for property ignoreCase.
     * 
     * @param ignoreCase
     *            New value of property ignoreCase.
     */
    public void setIgnoreCase(final boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    /**
     * Getter for property ignoreSpaceChange.
     * 
     * @return Value of property ignoreSpaceChange.
     */
    public boolean isIgnoreSpaceChange() {
        return ignoreSpaceChange;
    }

    /**
     * Setter for property ignoreSpaceChange.
     * 
     * @param ignoreSpaceChange
     *            New value of property ignoreSpaceChange.
     */
    public void setIgnoreSpaceChange(final boolean ignoreSpaceChange) {
        this.ignoreSpaceChange = ignoreSpaceChange;
    }

    /**
     * equals to the -c switch of cvs Getter for property contextDiff.
     * 
     * @return Value of property contextDiff.
     */
    public boolean isContextDiff() {
        return contextDiff;
    }

    /**
     * equals to the -c switch of cvs Setter for property contextDiff.
     * 
     * @param contextDiff
     *            New value of property contextDiff.
     */
    public void setContextDiff(final boolean contextDiff) {
        this.contextDiff = contextDiff;
    }

    /**
     * equals to the -u switch of cvs Getter for property unifiedDiff.
     * 
     * @return Value of property unifiedDiff.
     */
    public boolean isUnifiedDiff() {
        return unifiedDiff;
    }

    /**
     * equals to the -u switch of cvs. Setter for property unifiedDiff.
     * 
     * @param unifiedDiff
     *            New value of property unifiedDiff.
     */
    public void setUnifiedDiff(final boolean unifiedDiff) {
        this.unifiedDiff = unifiedDiff;
    }

}
