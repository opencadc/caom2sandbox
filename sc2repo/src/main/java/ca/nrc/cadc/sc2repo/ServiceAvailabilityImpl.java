/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2022.                            (c) 2022.
*  Government of Canada                 Gouvernement du Canada
*  National Research Council            Conseil national de recherches
*  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
*  All rights reserved                  Tous droits réservés
*
*  NRC disclaims any warranties,        Le CNRC dénie toute garantie
*  expressed, implied, or               énoncée, implicite ou légale,
*  statutory, of any kind with          de quelque nature que ce
*  respect to the software,             soit, concernant le logiciel,
*  including without limitation         y compris sans restriction
*  any warranty of merchantability      toute garantie de valeur
*  or fitness for a particular          marchande ou de pertinence
*  purpose. NRC shall not be            pour un usage particulier.
*  liable in any event for any          Le CNRC ne pourra en aucun cas
*  damages, whether direct or           être tenu responsable de tout
*  indirect, special or general,        dommage, direct ou indirect,
*  consequential or incidental,         particulier ou général,
*  arising from the use of the          accessoire ou fortuit, résultant
*  software.  Neither the name          de l'utilisation du logiciel. Ni
*  of the National Research             le nom du Conseil National de
*  Council of Canada nor the            Recherches du Canada ni les noms
*  names of its contributors may        de ses  participants ne peuvent
*  be used to endorse or promote        être utilisés pour approuver ou
*  products derived from this           promouvoir les produits dérivés
*  software without specific prior      de ce logiciel sans autorisation
*  written permission.                  préalable et particulière
*                                       par écrit.
*
*  This file is part of the             Ce fichier fait partie du projet
*  OpenCADC project.                    OpenCADC.
*
*  OpenCADC is free software:           OpenCADC est un logiciel libre ;
*  you can redistribute it and/or       vous pouvez le redistribuer ou le
*  modify it under the terms of         modifier suivant les termes de
*  the GNU Affero General Public        la “GNU Affero General Public
*  License as published by the          License” telle que publiée
*  Free Software Foundation,            par la Free Software Foundation
*  either version 3 of the              : soit la version 3 de cette
*  License, or (at your option)         licence, soit (à votre gré)
*  any later version.                   toute version ultérieure.
*
*  OpenCADC is distributed in the       OpenCADC est distribué
*  hope that it will be useful,         dans l’espoir qu’il vous
*  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
*  without even the implied             GARANTIE : sans même la garantie
*  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
*  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
*  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
*  General Public License for           Générale Publique GNU Affero
*  more details.                        pour plus de détails.
*
*  You should have received             Vous devriez avoir reçu une
*  a copy of the GNU Affero             copie de la Licence Générale
*  General Public License along         Publique GNU Affero avec
*  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
*  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
*                                       <http://www.gnu.org/licenses/>.
*
*  $Revision: 5 $
*
************************************************************************
 */

package ca.nrc.cadc.sc2repo;

import ca.nrc.cadc.auth.AuthMethod;
import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.auth.AuthenticatorImpl;
import ca.nrc.cadc.caom2.repo.CaomRepoConfig;
import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.reg.client.LocalAuthority;
import ca.nrc.cadc.reg.client.RegistryClient;
import ca.nrc.cadc.rest.RestAction;
import ca.nrc.cadc.vosi.Availability;
import ca.nrc.cadc.vosi.AvailabilityPlugin;
import ca.nrc.cadc.vosi.avail.CheckCertificate;
import ca.nrc.cadc.vosi.avail.CheckDataSource;
import ca.nrc.cadc.vosi.avail.CheckException;
import ca.nrc.cadc.vosi.avail.CheckResource;
import ca.nrc.cadc.vosi.avail.CheckWebService;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Principal;
import java.util.Iterator;
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import org.apache.log4j.Logger;

/**
 * VOSI-Availability plugin.
 * 
 * @author pdowler
 */
public class ServiceAvailabilityImpl implements AvailabilityPlugin {

    private static final Logger log = Logger.getLogger(ServiceAvailabilityImpl.class);

    // TODO: this should be configured someplace
    private static final Principal TRUSTED = new X500Principal("cn=servops_4a2,ou=cadc,o=hia,c=ca");

    private String appName;

    public ServiceAvailabilityImpl() {
    }

    @Override
    public void setAppName(String appName) {
        this.appName = appName;
    }

    @Override
    public boolean heartbeat() {
        return true;
    }

    public Availability getStatus() {
        boolean isGood = true;
        String note = "service is accepting queries";

        try {
            String state = getState();
            if (RestAction.STATE_OFFLINE.equals(state)) {
                return new Availability(false, RestAction.STATE_OFFLINE_MSG);
            }
            if (RestAction.STATE_READ_ONLY.equals(state)) {
                return new Availability(false, RestAction.STATE_READ_ONLY_MSG);
            }

            File config = new File(System.getProperty("user.home") + "/config/" + appName + ".properties");
            CaomRepoConfig rc = new CaomRepoConfig(config);
            if (rc.isEmpty()) {
                throw new IllegalStateException("no CaomRepoConfig.Item(s) found in " + config);
            }

            Iterator<CaomRepoConfig.Item> i = rc.iterator();
            while (i.hasNext()) {
                CaomRepoConfig.Item rci = i.next();
                String sql = "SELECT obsID FROM " + rci.getTestTable() + " LIMIT 1";
                CheckDataSource checkDataSource = new CheckDataSource(rci.getDataSourceName(), sql);
                checkDataSource.check();
            }

            // load this dynamically so that missing wcs won't break this class
            try {
                Class c = Class.forName("ca.nrc.cadc.caom2.repo.CheckWcsLib");
                CheckResource wcs = (CheckResource) c.newInstance();
                wcs.check();
            } catch (RuntimeException ex) {
                throw new CheckException("wcslib not available", ex);
            } catch (Error er) {
                throw new CheckException("wcslib not available", er);
            }
            
            // certificate for A&A
            File cert = new File(System.getProperty("user.home") + "/.ssl/cadcproxy.pem");
            CheckCertificate checkCert = new CheckCertificate(cert);
            checkCert.check();
            
            // check other services we depend on
            RegistryClient reg = new RegistryClient();
            URL url;
            CheckResource checkResource;

            LocalAuthority localAuthority = new LocalAuthority();

            URI credURI = localAuthority.getServiceURI(Standards.CRED_PROXY_10.toString());
            url = reg.getServiceURL(credURI, Standards.VOSI_AVAILABILITY, AuthMethod.ANON);
            checkResource = new CheckWebService(url);
            checkResource.check();

            URI usersURI = localAuthority.getServiceURI(Standards.UMS_USERS_01.toString());
            url = reg.getServiceURL(usersURI, Standards.VOSI_AVAILABILITY, AuthMethod.ANON);
            checkResource = new CheckWebService(url);
            checkResource.check();

            URI groupsURI = localAuthority.getServiceURI(Standards.GMS_SEARCH_01.toString());
            if (!groupsURI.equals(usersURI)) {
                url = reg.getServiceURL(groupsURI, Standards.VOSI_AVAILABILITY, AuthMethod.ANON);
                checkResource = new CheckWebService(url);
                checkResource.check();
            }
            
        } catch (CheckException ce) {
            // tests determined that the resource is not working
            isGood = false;
            note = ce.getMessage();
        } catch (Throwable t) {
            // the test itself failed
            isGood = false;
            note = "test failed, reason: " + t;
        }
        return new Availability(isGood, note);
    }

    @Override
    public void setState(String state) {
        AccessControlContext acContext = AccessController.getContext();
        Subject subject = Subject.getSubject(acContext);

        if (subject == null) {
            return;
        }

        Principal caller = AuthenticationUtil.getX500Principal(subject);
        if (AuthenticationUtil.equals(TRUSTED, caller)) {
            String key = appName + RestAction.STATE_MODE_KEY;
            if (RestAction.STATE_OFFLINE.equals(state)) {
                System.setProperty(key, RestAction.STATE_OFFLINE);
            } else if (RestAction.STATE_READ_ONLY.equals(state)) {
                System.setProperty(key, RestAction.STATE_READ_ONLY);
            } else if (RestAction.STATE_READ_WRITE.equals(state)) {
                System.setProperty(key, RestAction.STATE_READ_WRITE);
            }
            log.info("WebService state changed: " + key + "=" + state + " by " + caller + " [OK]");
        } else {
            log.warn("WebService state change by " + caller + " [DENIED]");
        }
    }

    private String getState() {
        String key = appName + RestAction.STATE_MODE_KEY;
        String ret = System.getProperty(key);
        if (ret == null) {
            return RestAction.STATE_READ_WRITE;
        }
        return ret;
    }
}
