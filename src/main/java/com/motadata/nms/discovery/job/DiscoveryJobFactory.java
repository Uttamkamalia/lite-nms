package com.motadata.nms.discovery.job;

import com.motadata.nms.commons.NMSException;
import com.motadata.nms.discovery.context.DiscoveryContext;
import com.motadata.nms.models.DeviceType.Protocol;
import com.motadata.nms.models.credential.CredentialProfile;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory class for creating discovery jobs based on discovery context
 */
public class DiscoveryJobFactory {

    /**
     * Create a discovery job for a batch of IP addresses based on the discovery context
     *
     * @param ipBatch List of IP addresses for the job
     * @param context The discovery context containing job parameters
     * @return The appropriate discovery job instance (SnmpDiscoveryJob or SshDiscoveryJob)
     * @throws NMSException if the credential type is not supported
     */
    public static DiscoveryJob create(List<String> ipBatch, DiscoveryContext context) {
        if (ipBatch == null || ipBatch.isEmpty()) {
            throw NMSException.badRequest("IP address batch cannot be null or empty");
        }

        if (context == null) {
            throw NMSException.badRequest("Discovery context cannot be null");
        }

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
                    ipBatch,
                    context.getPort(),
                    credentialProfile,
                    context.getDiscoveryProfileId()
                );

            case SSH:
                return new SshDiscoveryJob(
                    ipBatch,
                    context.getPort(),
                    credentialProfile,
                    context.getDiscoveryProfileId()
                );

            default:
                throw NMSException.badRequest("Unsupported protocol: " + protocol.getValue());
        }
    }

    /**
     * Create discovery jobs by batching IP addresses from the discovery context
     *
     * @param context The discovery context containing job parameters
     * @param batchSize The maximum number of IPs to include in each batch
     * @return List of discovery job instances
     * @throws NMSException if the credential type is not supported
     */
    public static List<DiscoveryJob> createBatchedJobs(DiscoveryContext context, int batchSize) {
        if (context == null) {
            throw NMSException.badRequest("Discovery context cannot be null");
        }

        List<String> ips = context.getTargetIps();
        if (ips == null || ips.isEmpty()) {
            throw NMSException.badRequest("Target IPs cannot be null or empty");
        }

        if (batchSize <= 0) {
            throw NMSException.badRequest("Batch size must be greater than zero");
        }

        List<DiscoveryJob> jobs = new ArrayList<>();

        // Process IPs in batches
        for (int i = 0; i < ips.size(); i += batchSize) {
            int end = Math.min(i + batchSize, ips.size());
            List<String> batch = ips.subList(i, end);

            // Create a job for this batch
            DiscoveryJob job = create(batch, context);
            jobs.add(job);
        }

        return jobs;
    }
}
