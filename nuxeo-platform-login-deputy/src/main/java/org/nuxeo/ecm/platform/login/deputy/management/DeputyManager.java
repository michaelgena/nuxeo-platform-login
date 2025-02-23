/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.login.deputy.management;

import java.util.Calendar;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;

public interface DeputyManager {

    List<String> getPossiblesAlternateLogins(String userName);

    List<String> getAvalaibleDeputyIds(String userName);

    List<DocumentModel> getAvalaibleMandates(String userName);

    DocumentModel newMandate(String username, String deputy);

    DocumentModel newMandate(String username, String deputy, Calendar start, Calendar end);

    void addMandate(DocumentModel entry);

    void removeMandate(String username, String deputy);

    String getDeputySchemaName();
}
