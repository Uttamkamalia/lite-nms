package com.motadata.nms.discovery.job;

import com.motadata.nms.commons.NMSException;
import com.motadata.nms.discovery.context.DiscoveryContext;
import com.motadata.nms.models.DeviceType.Protocol;
import com.motadata.nms.models.credential.CredentialProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Factory class for creating discovery jobs based on discovery context
 */
public class DiscoveryJobFactory {

    public static DiscoveryJob create(List<String> batch, DiscoveryContext context) {
        if (batch == null || batch.isEmpty()) {
            throw NMSException.badRequest("IP address batch cannot be null or empty");
        }
        if (context == null) {
            throw NMSException.badRequest("Discovery context cannot be null");
        }

        // TODO pas protocol as param so not repeated for each ip
        CredentialProfile credentialProfile = context.getCredentialProfile();
        if (credentialProfile == null) {
            throw NMSException.badRequest("Credential profile cannot be null");
        }

        Protocol protocol = credentialProfile.getCredential() != null
            ? credentialProfile.getCredential().getType()
            : null;

        if (protocol == null) {
            throw NMSException.badRequest("Protocol cannot be determined from credential profile");
        }

        // Create the appropriate job type based on the protocol
        switch (protocol) {
            case SNMP:
                return new SnmpDiscoveryJob(
                    batch,
                    context.getPort(),
                    credentialProfile,
                    context.getDiscoveryProfileId()
                );

            case SSH:
                return new SshDiscoveryJob(
                    batch,
                    context.getPort(),
                    credentialProfile,
                    context.getDiscoveryProfileId()
                );

            default:
                throw NMSException.badRequest("Unsupported protocol: " + protocol.getValue());
        }
    }
}
