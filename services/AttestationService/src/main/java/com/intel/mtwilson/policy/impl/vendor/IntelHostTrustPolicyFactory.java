/*
 * Copyright (C) 2013 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.policy.impl.vendor;

import com.intel.mtwilson.policy.impl.JpaPolicyReader;
import com.intel.mtwilson.as.data.TblHosts;
import com.intel.mtwilson.crypto.X509Util;
import com.intel.mtwilson.model.Bios;
import com.intel.mtwilson.model.Vmm;
import com.intel.mtwilson.policy.Rule;
import com.intel.mtwilson.policy.impl.TrustMarker;
import com.intel.mtwilson.policy.impl.VendorHostTrustPolicyFactory;
import com.intel.mtwilson.policy.rule.AikCertificateTrusted;
import com.intel.mtwilson.util.ResourceFinder;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.IOUtils;

/**
 * Needs to create a policy to check AIK Certificate is signed by trusted Privacy CA
 * @author jbuhacoff
 */
public class IntelHostTrustPolicyFactory implements VendorHostTrustPolicyFactory {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IntelHostTrustPolicyFactory.class);
    private X509Certificate[] cacerts = null;
    private JpaPolicyReader reader;
    public IntelHostTrustPolicyFactory(JpaPolicyReader util) {
        this.reader = util;
    }

    @Override
    public Set<Rule> loadTrustRulesForBios(Bios bios, TblHosts host) {
        if( cacerts == null ) {
            cacerts = loadTrustedAikCertificateAuthorities();
        }
        HashSet<Rule> rules = new HashSet<Rule>();
        AikCertificateTrusted aikcert = new AikCertificateTrusted(cacerts);
        aikcert.setMarkers(TrustMarker.BIOS.name());
        rules.add(aikcert);
        Set<Rule> pcrConstantRules = reader.loadPcrMatchesConstantRulesForBios(bios, host);
        rules.addAll(pcrConstantRules);
        return rules;
    }

    @Override
    public Set<Rule> loadTrustRulesForVmm(Vmm vmm, TblHosts host) {
        if( cacerts == null ) {
            cacerts = loadTrustedAikCertificateAuthorities();
        }
        HashSet<Rule> rules = new HashSet<Rule>();
        AikCertificateTrusted aikcert = new AikCertificateTrusted(cacerts);
        aikcert.setMarkers(TrustMarker.VMM.name());
        rules.add(aikcert);
        // first, load the list of pcr's marked for this host's vmm mle 
        Set<Rule> pcrConstantRules = reader.loadPcrMatchesConstantRulesForVmm(vmm, host);
        rules.addAll(pcrConstantRules);

        // Next we need to add all the modules
        if( host.getVmmMleId().getRequiredManifestList().contains("19") ) {
            Set<Rule> pcrEventLogRules = reader.loadPcrEventLogIncludesRuleForVmm(vmm, host);
            rules.addAll(pcrEventLogRules);
        }
        return rules;    
    }

    // Since the open source tBoot does not support PCR 22, we will not support it here.
    @Override
    public Set<Rule> loadTrustRulesForLocation(String location, TblHosts host) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<Rule> loadComparisonRulesForVmm(Vmm vmm, TblHosts host) {
        HashSet<Rule> rules = new HashSet<Rule>();
        // first, load the list of pcr's marked for this host's vmm mle 
        Set<Rule> pcrConstantRules = reader.loadPcrMatchesConstantRulesForVmm(vmm, host);
        rules.addAll(pcrConstantRules);

        // Next we need to add all the modules
        if( host.getVmmMleId().getRequiredManifestList().contains("19") ) {
            Set<Rule> pcrEventLogRules = reader.loadPcrEventLogIncludesRuleForVmm(vmm, host);
            rules.addAll(pcrEventLogRules);
        }
        return rules;    
    }

    private X509Certificate[] loadTrustedAikCertificateAuthorities() {
        HashSet<X509Certificate> pcaList = new HashSet<X509Certificate>();
        try {
            InputStream privacyCaIn = new FileInputStream(ResourceFinder.getFile("PrivacyCA.p12.pem")); // may contain multiple trusted privacy CA certs from remove Privacy CAs
            List<X509Certificate> privacyCaCerts = X509Util.decodePemCertificates(IOUtils.toString(privacyCaIn));
            pcaList.addAll(privacyCaCerts);
            IOUtils.closeQuietly(privacyCaIn);
            log.debug("Added {} certificates from PrivacyCA.p12.pem", privacyCaCerts.size());
        }
        catch(Exception e) {
            // FileNotFoundException: cannot find PrivacyCA.pem
            // CertificateException: error while reading certificates from file
            log.warn("Cannot load PrivacyCA.p12.pem");            
        }
        try {
            InputStream privacyCaIn = new FileInputStream(ResourceFinder.getFile("PrivacyCA.cer")); // may contain one trusted privacy CA cert from local Privacy CA
            X509Certificate privacyCaCert = X509Util.decodeDerCertificate(IOUtils.toByteArray(privacyCaIn));
            pcaList.add(privacyCaCert);
            IOUtils.closeQuietly(privacyCaIn);
            log.debug("Added certificate from PrivacyCA.cer");
        }
        catch(Exception e) {
            // FileNotFoundException: cannot find PrivacyCA.cer
            // CertificateException: error while reading certificate from file
            log.warn("Cannot load PrivacyCA.cer", e);            
        }
        X509Certificate[] cas = pcaList.toArray(new X509Certificate[0]);
        return cas;
    }
}
